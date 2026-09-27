package com.example.ailisttest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import android.content.Context
import android.net.Uri
import com.example.ailisttest.AiListApplication
import com.example.ailisttest.data.*
import com.example.ailisttest.data.local.*
import com.example.ailisttest.data.modbus.ModbusByteOrder
import com.example.ailisttest.data.modbus.ModbusByteOrderTransformer
import com.example.ailisttest.data.modbus.ModbusTcpClient
import com.example.ailisttest.data.modbus.PacketPollingStats
import com.example.ailisttest.data.modbus.toHexString
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

enum class NodeConnectionState {
    NOT_CONNECTED,      // Gray (#757575)
    CONNECTING,         // Yellow (#FFC107)
    COMMUNICATION_OK,   // Green (#4CAF50)
    CONNECTION_FAILURE  // Red (#F44336)
}

sealed class SelectedItem {
    data class Node(val node: NodeEntity) : SelectedItem()
    data class Packet(val packet: PacketEntity) : SelectedItem()
    data class Tag(val tag: TagEntity) : SelectedItem()
    data class BitTag(val bitTag: BitTags, val parentTag: TagEntity? = null) : SelectedItem()
}

data class ModbusAddressInfo(
    val startAddress: Int,
    val quantity: Int,
    val regsPerTag: Int,
    val dataType: DataTypes?,
    val calculatedModbusAddress: Long
)

data class ModbusDebugInfo(
    val title: String,
    val nodeName: String,
    val ipAddress: String,
    val port: Int,
    val slaveNode: Int,
    val packetName: String,
    val dataType: String,
    val defaultModbusAddress: Long,
    val packetOffset: Int,
    val calculatedModbusAddress: Long,
    val wireRegisterOffset: Int,
    val quantityRegisters: Int,
    val requestHex: String,
    val responseHex: String?,
    val isSuccess: Boolean,
    val errorMessage: String? = null,
    val registersText: String? = null,
    val tagsText: String? = null,
    val byteOrder: String? = null,
    val dataTypeObj: PlcDataTypes? = null
)

class MainViewModel(private val repository: MainRepository) : ViewModel() {

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as AiListApplication
                return MainViewModel(application.repository) as T
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.ensureSampleData()
            cleanupOrphanListScreenItems()
        }

        viewModelScope.launch {
            repository.isPollingEnabled.collect { enabled ->
                if (enabled) {
                    startBackgroundPolling()
                } else {
                    stopBackgroundPolling()
                }
            }
        }
    }

    val isPollingEnabled: StateFlow<Boolean> = repository.isPollingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setPollingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPollingEnabled(enabled)
        }
    }

    val plcPreset: StateFlow<String> = repository.plcPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Click Plus PLC")

    fun setPlcPreset(preset: String) {
        viewModelScope.launch {
            repository.setPlcPreset(preset)
        }
    }

    val byteOrder: StateFlow<String> = repository.byteOrder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "flip words")

    fun setByteOrder(order: String) {
        viewModelScope.launch {
            repository.setByteOrder(order)
        }
    }

    val dataTypePlcPreset: StateFlow<String> = repository.dataTypePlcPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Click Plus PLC")

    fun setDataTypePlcPreset(preset: String) {
        viewModelScope.launch {
            repository.setDataTypePlcPreset(preset)
            if (preset.equals("Click Plus PLC", ignoreCase = true)) {
                repository.resetDataTypesToClickPlusDefaults()
            }
        }
    }

    fun updateDataType(dataType: DataTypes) {
        viewModelScope.launch {
            repository.updateDataType(dataType)
        }
    }

    fun resetDataTypesToClickPlusDefaults() {
        viewModelScope.launch {
            repository.resetDataTypesToClickPlusDefaults()
        }
    }

    val tagFileFormat: StateFlow<String> = repository.tagFileFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Click Plus")

    fun setTagFileFormat(format: String) {
        viewModelScope.launch {
            repository.setTagFileFormat(format)
        }
    }

    private val _packetPollingStatsMap = MutableStateFlow<Map<Long, PacketPollingStats>>(emptyMap())
    val packetPollingStatsMap: StateFlow<Map<Long, PacketPollingStats>> = _packetPollingStatsMap.asStateFlow()

    // Session-persistent collapsed/expanded state maps (default = false / collapsed)
    private val _expandedScreensMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedScreensMap: StateFlow<Map<Long, Boolean>> = _expandedScreensMap.asStateFlow()

    private val _expandedCustomGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedCustomGroupsMap: StateFlow<Map<Long, Boolean>> = _expandedCustomGroupsMap.asStateFlow()

    private val _expandedNodeGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedNodeGroupsMap: StateFlow<Map<Long, Boolean>> = _expandedNodeGroupsMap.asStateFlow()

    private val _expandedPacketGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedPacketGroupsMap: StateFlow<Map<Long, Boolean>> = _expandedPacketGroupsMap.asStateFlow()

    private val _expandedTagGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedTagGroupsMap: StateFlow<Map<Long, Boolean>> = _expandedTagGroupsMap.asStateFlow()

    private val _newlyCreatedNodeIds = MutableStateFlow<Set<Long>>(emptySet())
    val newlyCreatedNodeIds: StateFlow<Set<Long>> = _newlyCreatedNodeIds.asStateFlow()

    private val _newlyCreatedPacketIds = MutableStateFlow<Set<Long>>(emptySet())
    val newlyCreatedPacketIds: StateFlow<Set<Long>> = _newlyCreatedPacketIds.asStateFlow()

    private val _newlyCreatedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val newlyCreatedTagIds: StateFlow<Set<Long>> = _newlyCreatedTagIds.asStateFlow()

    fun markNodeSaved(nodeId: Long) {
        _newlyCreatedNodeIds.value = _newlyCreatedNodeIds.value - nodeId
    }

    fun markPacketSaved(packetId: Long) {
        _newlyCreatedPacketIds.value = _newlyCreatedPacketIds.value - packetId
    }

    fun markTagSaved(tagId: Long) {
        _newlyCreatedTagIds.value = _newlyCreatedTagIds.value - tagId
    }

    fun toggleScreenExpanded(screenId: Long) {
        val current = _expandedScreensMap.value[screenId] ?: false
        _expandedScreensMap.value = _expandedScreensMap.value + (screenId to !current)
    }

    fun toggleCustomGroupExpanded(groupId: Long) {
        val current = _expandedCustomGroupsMap.value[groupId] ?: false
        _expandedCustomGroupsMap.value = _expandedCustomGroupsMap.value + (groupId to !current)
    }

    fun toggleNodeGroupExpanded(nodeId: Long) {
        val current = _expandedNodeGroupsMap.value[nodeId] ?: false
        _expandedNodeGroupsMap.value = _expandedNodeGroupsMap.value + (nodeId to !current)
    }

    fun togglePacketGroupExpanded(packetId: Long) {
        val current = _expandedPacketGroupsMap.value[packetId] ?: false
        _expandedPacketGroupsMap.value = _expandedPacketGroupsMap.value + (packetId to !current)
    }

    fun toggleTagGroupExpanded(itemId: Long) {
        val current = _expandedTagGroupsMap.value[itemId] ?: false
        _expandedTagGroupsMap.value = _expandedTagGroupsMap.value + (itemId to !current)
    }

    // Configure Tree List (Hardware / Nodes screen) expansion maps
    private val _expandedNodesMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedNodesMap: StateFlow<Map<Long, Boolean>> = _expandedNodesMap.asStateFlow()

    private val _expandedPacketsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedPacketsMap: StateFlow<Map<Long, Boolean>> = _expandedPacketsMap.asStateFlow()

    private val _expandedTagsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val expandedTagsMap: StateFlow<Map<Long, Boolean>> = _expandedTagsMap.asStateFlow()

    fun toggleNodeExpanded(nodeId: Long) {
        val current = _expandedNodesMap.value[nodeId] ?: false
        _expandedNodesMap.value = _expandedNodesMap.value + (nodeId to !current)
    }

    fun setNodeExpanded(nodeId: Long, expanded: Boolean) {
        _expandedNodesMap.value = _expandedNodesMap.value + (nodeId to expanded)
    }

    fun togglePacketExpanded(packetId: Long) {
        val current = _expandedPacketsMap.value[packetId] ?: false
        _expandedPacketsMap.value = _expandedPacketsMap.value + (packetId to !current)
    }

    fun setPacketExpanded(packetId: Long, expanded: Boolean) {
        _expandedPacketsMap.value = _expandedPacketsMap.value + (packetId to expanded)
    }

    fun toggleTagExpanded(tagId: Long) {
        val current = _expandedTagsMap.value[tagId] ?: false
        _expandedTagsMap.value = _expandedTagsMap.value + (tagId to !current)
    }

    fun setTagExpanded(tagId: Long, expanded: Boolean) {
        _expandedTagsMap.value = _expandedTagsMap.value + (tagId to expanded)
    }

    // Live Screen (DynamicListScreen) expansion maps
    private val _liveExpandedCustomGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val liveExpandedCustomGroupsMap: StateFlow<Map<Long, Boolean>> = _liveExpandedCustomGroupsMap.asStateFlow()

    private val _liveExpandedNodeGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val liveExpandedNodeGroupsMap: StateFlow<Map<Long, Boolean>> = _liveExpandedNodeGroupsMap.asStateFlow()

    private val _liveExpandedPacketGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val liveExpandedPacketGroupsMap: StateFlow<Map<Long, Boolean>> = _liveExpandedPacketGroupsMap.asStateFlow()

    private val _liveExpandedTagGroupsMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val liveExpandedTagGroupsMap: StateFlow<Map<Long, Boolean>> = _liveExpandedTagGroupsMap.asStateFlow()

    fun toggleLiveCustomGroupExpanded(groupId: Long) {
        val current = _liveExpandedCustomGroupsMap.value[groupId] ?: false
        _liveExpandedCustomGroupsMap.value = _liveExpandedCustomGroupsMap.value + (groupId to !current)
    }

    fun toggleLiveNodeGroupExpanded(itemId: Long) {
        val current = _liveExpandedNodeGroupsMap.value[itemId] ?: false
        _liveExpandedNodeGroupsMap.value = _liveExpandedNodeGroupsMap.value + (itemId to !current)
    }

    fun toggleLivePacketGroupExpanded(itemId: Long) {
        val current = _liveExpandedPacketGroupsMap.value[itemId] ?: false
        _liveExpandedPacketGroupsMap.value = _liveExpandedPacketGroupsMap.value + (itemId to !current)
    }

    fun toggleLiveTagGroupExpanded(itemId: Long) {
        val current = _liveExpandedTagGroupsMap.value[itemId] ?: false
        _liveExpandedTagGroupsMap.value = _liveExpandedTagGroupsMap.value + (itemId to !current)
    }

    private var pollingJob: Job? = null

    private fun startBackgroundPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                val currentHierarchy = repository.getFullHierarchy().firstOrNull() ?: emptyList()

                currentHierarchy.forEach { nodeWithPackets ->
                    val node = nodeWithPackets.node

                    var client = activeClients[node.id]
                    var nodeState = _nodeConnectionStates.value[node.id]

                    // Connect to node first before polling if not connected
                    if (client == null || !client.isConnected || nodeState != NodeConnectionState.COMMUNICATION_OK) {
                        val connected = internalConnectNode(node)
                        if (connected) {
                            client = activeClients[node.id]
                        }
                    }

                    if (client != null && client.isConnected) {
                        nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                            val packet = packetWithTags.packet
                            if (packet.pollData) {
                                val addrInfo = calculateModbusAddressInfo(packet)

                                val readResult = client!!.readHoldingRegistersDetailed(
                                    slaveNode = packet.slaveNode,
                                    startAddress = addrInfo.startAddress,
                                    quantity = addrInfo.quantity
                                )

                                val prevStats = _packetPollingStatsMap.value[packet.id] ?: PacketPollingStats(packet.id, packet.name)
                                val newSuccesses = (prevStats.recentSuccesses + readResult.isSuccess).takeLast(20)
                                val updatedStats = prevStats.copy(
                                    commStatus = readResult.isSuccess,
                                    totalPackets = prevStats.totalPackets + 1,
                                    recentSuccesses = newSuccesses,
                                    lastReadTimestamp = System.currentTimeMillis(),
                                    lastErrorMessage = readResult.errorMessage
                                )

                                _packetPollingStatsMap.value = _packetPollingStatsMap.value + (packet.id to updatedStats)

                                if (readResult.isSuccess && readResult.registers != null) {
                                    val mode = ModbusByteOrder.fromLabel(node.byteOrder)
                                    val transformedRegs = ModbusByteOrderTransformer.transformRegisters(readResult.registers, mode, addrInfo.regsPerTag)
                                    val typeName = addrInfo.dataType?.shortName?.uppercase() ?: addrInfo.dataType?.description?.uppercase() ?: "DS"
                                    val regsPerTag = addrInfo.regsPerTag

                                    val tags = packetWithTags.tags
                                    tags.forEachIndexed { tagIndex, tag ->
                                        val regOffset = tagIndex * regsPerTag
                                        if (regOffset + regsPerTag <= transformedRegs.size) {
                                            val newStoredVal = ModbusByteOrderTransformer.parseValueFromRegisters(
                                                transformedRegs,
                                                regOffset,
                                                addrInfo.dataType,
                                                typeName
                                            )
                                            if (tag.storedValue != newStoredVal) {
                                                repository.updateTag(tag.copy(storedValue = newStoredVal))
                                            }
                                        }
                                    }
                                } else if (!readResult.isSuccess) {
                                    if (readResult.errorMessage?.contains("connect", ignoreCase = true) == true || !client!!.isConnected) {
                                        _nodeConnectionStates.value = _nodeConnectionStates.value + (node.id to NodeConnectionState.CONNECTION_FAILURE)
                                    }
                                }
                            }
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    private fun stopBackgroundPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun parseRegisterValue(
        registers: IntArray,
        offsetIndex: Int,
        typeName: String,
        byteOrderLabel: String = "flip words"
    ): String {
        if (registers.isEmpty() || offsetIndex < 0 || offsetIndex >= registers.size) return "0"
        val mode = ModbusByteOrder.fromLabel(byteOrderLabel)
        val transformed = ModbusByteOrderTransformer.transformRegisters(registers, mode)
        val regsPerTag = if (typeName.contains("2", ignoreCase = true) || typeName.contains("DD", ignoreCase = true) || typeName.contains("DF", ignoreCase = true) || typeName.contains("FLOAT", ignoreCase = true)) 2 else 1
        return ModbusByteOrderTransformer.parseValueFromRegisters(transformed, offsetIndex, typeName, regsPerTag)
    }

    val hierarchy: StateFlow<List<NodeWithPacketsAndTags>> = repository.getFullHierarchy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dataTypes: StateFlow<List<DataTypes>> = repository.getAllDataTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listScreensWithItems: StateFlow<List<ScreenWithListItems>> = repository.getListScreensWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val graphicsScreensWithItems: StateFlow<List<ScreenWithGraphicsItems>> = repository.getGraphicsScreensWithItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScreens: StateFlow<List<Screens>> = repository.getAllScreens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun generateUniqueScreenName(allScreens: List<Screens>): String {
        val screenRegex = Regex("^Screen(\\d+)$", RegexOption.IGNORE_CASE)
        var maxNum = 0
        var foundNumberedScreen = false

        allScreens.forEach { screen ->
            val match = screenRegex.find(screen.Name.trim())
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull()
                if (num != null) {
                    foundNumberedScreen = true
                    if (num > maxNum) {
                        maxNum = num
                    }
                }
            }
        }

        var nextNum = if (foundNumberedScreen) (maxNum + 1) else 1
        var candidateName = "Screen$nextNum"

        while (allScreens.any { it.Name.trim().equals(candidateName, ignoreCase = true) }) {
            nextNum++
            candidateName = "Screen$nextNum"
        }
        return candidateName
    }

    private suspend fun reindexScreens() {
        val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
        allScreens.forEachIndexed { index, screen ->
            if (screen.Type != index) {
                repository.updateScreen(screen.copy(Type = index))
            }
        }
    }

    fun addListScreen(onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList()
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_LIST))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created list screen '$candidateName'")
        }
    }

    fun addGraphicsScreen(onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList()
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_GRAPHICS))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created graphics screen '$candidateName'")
        }
    }

    fun addListScreenAbove(targetScreen: Screens, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_LIST))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created list screen '$candidateName' above '${targetScreen.Name}'")
        }
    }

    fun addListScreenBelow(targetScreen: Screens, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_LIST))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created list screen '$candidateName' below '${targetScreen.Name}'")
        }
    }

    fun addGraphicsScreenAbove(targetScreen: Screens, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_GRAPHICS))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created graphics screen '$candidateName' above '${targetScreen.Name}'")
        }
    }

    fun addGraphicsScreenBelow(targetScreen: Screens, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val candidateName = generateUniqueScreenName(allScreens)

            val newId = repository.insertScreen(Screens(Name = candidateName, Type = Screens.TYPE_GRAPHICS))
            _expandedScreensMap.value = _expandedScreensMap.value + (newId to true)
            onShowSnackbar("Created graphics screen '$candidateName' below '${targetScreen.Name}'")
        }
    }

    fun insertGraphicsScreenItem(item: GraphicsScreenItems, onResult: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertGraphicsScreenItem(item)
            onResult(id)
        }
    }

    fun updateGraphicsScreenItem(item: GraphicsScreenItems) {
        viewModelScope.launch {
            repository.updateGraphicsScreenItem(item)
        }
    }

    fun deleteGraphicsScreenItem(item: GraphicsScreenItems) {
        viewModelScope.launch {
            repository.deleteGraphicsScreenItem(item)
        }
    }

    fun moveScreenUp(screenId: Long) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val idx = allScreens.indexOfFirst { it.id == screenId }
            if (idx > 0) {
                val current = allScreens[idx]
                val above = allScreens[idx - 1]
                val currentType = current.Type
                val aboveType = above.Type
                repository.updateScreen(current.copy(Type = if (currentType != aboveType) aboveType else (idx - 1)))
                repository.updateScreen(above.copy(Type = if (currentType != aboveType) currentType else idx))
                reindexScreens()
            }
        }
    }

    fun moveScreenDown(screenId: Long) {
        viewModelScope.launch {
            val allScreens = repository.getAllScreensList().sortedWith(compareBy({ it.Type }, { it.id }))
            val idx = allScreens.indexOfFirst { it.id == screenId }
            if (idx >= 0 && idx < allScreens.size - 1) {
                val current = allScreens[idx]
                val below = allScreens[idx + 1]
                val currentType = current.Type
                val belowType = below.Type
                repository.updateScreen(current.copy(Type = if (currentType != belowType) belowType else (idx + 1)))
                repository.updateScreen(below.copy(Type = if (currentType != belowType) currentType else idx))
                reindexScreens()
            }
        }
    }

    fun updateScreen(screen: Screens) {
        viewModelScope.launch {
            repository.updateScreen(screen)
        }
    }

    fun deleteScreen(screen: Screens) {
        viewModelScope.launch {
            repository.deleteScreen(screen)
            reindexScreens()
        }
    }

data class ListScreenItemSpec(
    val parentTagId: Long?,
    val type: Int,
    val isShowBits: Boolean = false
)

    private suspend fun resolveItemSpecsFromPath(selectedPath: String): List<ListScreenItemSpec> {
        val target = repository.resolvePickerSelection(selectedPath) ?: return emptyList()
        return when (target) {
            is ResolvedTarget.TagTarget -> {
                val tagId = target.tag.id
                val typeVal = if (target.bitIndex != null) (target.bitIndex + 1) else 0
                listOf(ListScreenItemSpec(parentTagId = tagId, type = typeVal, isShowBits = false))
            }
            is ResolvedTarget.PacketTarget -> {
                val packetGroupType = 1000 + target.packet.id.toInt()
                listOf(ListScreenItemSpec(parentTagId = null, type = packetGroupType, isShowBits = false))
            }
            is ResolvedTarget.NodeTarget -> {
                val nodeGroupType = 10000 + target.node.id.toInt()
                listOf(ListScreenItemSpec(parentTagId = null, type = nodeGroupType, isShowBits = false))
            }
        }
    }

    fun addOrInsertListScreenItems(
        screenId: Long,
        selectedPaths: List<String>,
        targetIndex: Int? = null,
        parentGroupId: Long? = null
    ) {
        viewModelScope.launch {
            if (selectedPaths.isEmpty()) return@launch

            _expandedScreensMap.value = _expandedScreensMap.value + (screenId to true)
            if (parentGroupId != null) {
                _expandedCustomGroupsMap.value = _expandedCustomGroupsMap.value + (parentGroupId to true)
            }

            val resolvedSpecs = selectedPaths.flatMap { resolveItemSpecsFromPath(it) }
            if (resolvedSpecs.isEmpty()) return@launch

            val allItems = repository.getItemsForScreenSync(screenId)
            val containerItems = allItems
                .filter { it.parentCustomGroupId == parentGroupId }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val insertPos = targetIndex ?: (containerItems.size + 1)
            val shiftCount = resolvedSpecs.size

            // Shift container items with ScreenIndex >= insertPos
            containerItems.filter { it.ScreenIndex >= insertPos }.forEach { item ->
                repository.updateListScreenItem(item.copy(ScreenIndex = item.ScreenIndex + shiftCount))
            }

            // Insert new items starting at insertPos
            resolvedSpecs.forEachIndexed { idx, spec ->
                val newPos = insertPos + idx

                repository.insertListScreenItem(
                    ListScreenItems(
                        parentScreenId = screenId,
                        parentCustomGroupId = parentGroupId,
                        parentTagId = spec.parentTagId,
                        Type = spec.type,
                        ScreenIndex = newPos,
                        isShowBits = spec.isShowBits
                    )
                )
            }

            reindexContainerItems(screenId, parentGroupId)
        }
    }

    fun addTagsInNewGroup(
        screenId: Long,
        selectedPaths: List<String>,
        targetIndex: Int? = null,
        groupName: String = "New Group"
    ) {
        viewModelScope.launch {
            if (selectedPaths.isEmpty()) return@launch

            _expandedScreensMap.value = _expandedScreensMap.value + (screenId to true)

            val resolvedSpecs = selectedPaths.flatMap { resolveItemSpecsFromPath(it) }
            if (resolvedSpecs.isEmpty()) return@launch

            val allItems = repository.getItemsForScreenSync(screenId)
            val topLevelItems = allItems
                .filter { it.parentCustomGroupId == null }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val insertPos = targetIndex ?: (topLevelItems.size + 1)

            topLevelItems.filter { it.ScreenIndex >= insertPos }.forEach { item ->
                repository.updateListScreenItem(item.copy(ScreenIndex = item.ScreenIndex + 1))
            }

            val newGroupId = repository.insertCustomGroup(
                CustomGroup(
                    parentScreenId = screenId,
                    groupName = groupName,
                    ScreenIndex = insertPos
                )
            )

            _expandedCustomGroupsMap.value = _expandedCustomGroupsMap.value + (newGroupId to true)

            repository.insertListScreenItem(
                ListScreenItems(
                    parentScreenId = screenId,
                    parentCustomGroupId = null,
                    parentTagId = null,
                    Type = (20000 + newGroupId.toInt()),
                    ScreenIndex = insertPos,
                    isShowBits = false
                )
            )

            resolvedSpecs.forEachIndexed { idx, spec ->
                val newPos = idx + 1

                repository.insertListScreenItem(
                    ListScreenItems(
                        parentScreenId = screenId,
                        parentCustomGroupId = newGroupId,
                        parentTagId = spec.parentTagId,
                        Type = spec.type,
                        ScreenIndex = newPos,
                        isShowBits = spec.isShowBits
                    )
                )
            }

            reindexContainerItems(screenId, null)
            reindexContainerItems(screenId, newGroupId)
        }
    }

    fun addOrInsertListScreenItem(
        screenId: Long,
        selectedPath: String,
        targetIndex: Int? = null,
        parentGroupId: Long? = null
    ) {
        addOrInsertListScreenItems(screenId, listOf(selectedPath), targetIndex, parentGroupId)
    }

    fun setCustomGroupReadOnly(screenId: Long, groupId: Long, readOnly: Boolean) {
        viewModelScope.launch {
            val items = repository.getItemsForScreenSync(screenId).filter { it.parentCustomGroupId == groupId }
            items.forEach { item ->
                repository.updateListScreenItem(item.copy(isReadOnly = readOnly))
            }
            repository.refreshHierarchy()
        }
    }

    fun setPacketGroupReadOnly(screenId: Long, packetId: Long, readOnly: Boolean) {
        viewModelScope.launch {
            val hierarchyList = repository.getFullHierarchy().firstOrNull() ?: emptyList()
            val packetWithTags = hierarchyList.flatMap { it.packetsWithTags }.find { it.packet.id == packetId } ?: return@launch
            val screenItems = repository.getItemsForScreenSync(screenId)

            packetWithTags.tagsWithBitTags.forEach { tagWithBits ->
                val tag = tagWithBits.tag
                val existing = screenItems.find { it.parentTagId == tag.id && it.Type == 0 }
                if (existing != null) {
                    repository.updateListScreenItem(existing.copy(isReadOnly = readOnly))
                } else {
                    repository.insertListScreenItem(
                        ListScreenItems(
                            parentScreenId = screenId,
                            parentTagId = tag.id,
                            Type = 0,
                            isReadOnly = readOnly
                        )
                    )
                }
            }
            repository.refreshHierarchy()
        }
    }

    fun setNodeGroupReadOnly(screenId: Long, nodeId: Long, readOnly: Boolean) {
        viewModelScope.launch {
            val hierarchyList = repository.getFullHierarchy().firstOrNull() ?: emptyList()
            val nodeWithPackets = hierarchyList.find { it.node.id == nodeId } ?: return@launch
            nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                setPacketGroupReadOnly(screenId, packetWithTags.packet.id, readOnly)
            }
        }
    }

    fun setParentTagAndBitsReadOnly(screenId: Long, tagId: Long, numBits: Int, readOnly: Boolean) {
        viewModelScope.launch {
            val screenItems = repository.getItemsForScreenSync(screenId)
            val parentItem = screenItems.find { it.parentTagId == tagId && it.Type == 0 }
            if (parentItem != null) {
                repository.updateListScreenItem(parentItem.copy(isReadOnly = readOnly))
            } else {
                repository.insertListScreenItem(
                    ListScreenItems(
                        parentScreenId = screenId,
                        parentTagId = tagId,
                        Type = 0,
                        isReadOnly = readOnly
                    )
                )
            }

            for (bitIdx in 0 until numBits) {
                val bitType = bitIdx + 1
                val bitItem = screenItems.find { it.parentTagId == tagId && it.Type == bitType }
                if (bitItem != null) {
                    repository.updateListScreenItem(bitItem.copy(isReadOnly = readOnly))
                } else {
                    repository.insertListScreenItem(
                        ListScreenItems(
                            parentScreenId = screenId,
                            parentTagId = tagId,
                            Type = bitType,
                            isReadOnly = readOnly
                        )
                    )
                }
            }
            repository.refreshHierarchy()
        }
    }

    fun updateListScreenItem(item: ListScreenItems) {
        viewModelScope.launch {
            if (item.id > 0) {
                repository.updateListScreenItem(item)
            } else {
                val existingItems = repository.getItemsForScreenSync(item.parentScreenId)
                val match = existingItems.find { it.parentTagId == item.parentTagId && it.Type == item.Type }
                if (match != null) {
                    repository.updateListScreenItem(item.copy(id = match.id))
                } else {
                    val nextIndex = if (existingItems.isEmpty()) 0 else (existingItems.maxOf { it.ScreenIndex } + 1)
                    repository.insertListScreenItem(item.copy(id = 0, ScreenIndex = nextIndex))
                }
            }
        }
    }

    fun deleteListScreenItem(item: ListScreenItems) {
        viewModelScope.launch {
            repository.deleteListScreenItem(item)
            reindexContainerItems(item.parentScreenId, item.parentCustomGroupId)
        }
    }

    fun moveListScreenItemUp(screenId: Long, itemId: Long) {
        viewModelScope.launch {
            val allItems = repository.getItemsForScreenSync(screenId)
            val itemToMove = allItems.find { it.id == itemId } ?: return@launch
            val parentGroupId = itemToMove.parentCustomGroupId

            val containerItems = allItems
                .filter { it.parentCustomGroupId == parentGroupId }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val idx = containerItems.indexOfFirst { it.id == itemId }
            if (idx > 0) {
                val current = containerItems[idx]
                val above = containerItems[idx - 1]
                repository.updateListScreenItem(current.copy(ScreenIndex = above.ScreenIndex))
                repository.updateListScreenItem(above.copy(ScreenIndex = current.ScreenIndex))
                reindexContainerItems(screenId, parentGroupId)
            }
        }
    }

    fun moveListScreenItemDown(screenId: Long, itemId: Long) {
        viewModelScope.launch {
            val allItems = repository.getItemsForScreenSync(screenId)
            val itemToMove = allItems.find { it.id == itemId } ?: return@launch
            val parentGroupId = itemToMove.parentCustomGroupId

            val containerItems = allItems
                .filter { it.parentCustomGroupId == parentGroupId }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val idx = containerItems.indexOfFirst { it.id == itemId }
            if (idx >= 0 && idx < containerItems.size - 1) {
                val current = containerItems[idx]
                val below = containerItems[idx + 1]
                repository.updateListScreenItem(current.copy(ScreenIndex = below.ScreenIndex))
                repository.updateListScreenItem(below.copy(ScreenIndex = current.ScreenIndex))
                reindexContainerItems(screenId, parentGroupId)
            }
        }
    }

    fun recreateDatabaseWithDefaults(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            repository.recreateDatabaseWithDefaults()
            _selectedItem.value = null
            onComplete("Database re-created with default tags successfully.")
        }
    }

    fun addCustomGroup(screenId: Long, groupName: String = "New Custom Group", targetIndex: Int? = null) {
        viewModelScope.launch {
            _expandedScreensMap.value = _expandedScreensMap.value + (screenId to true)

            val containerItems = repository.getItemsForScreenSync(screenId)
                .filter { it.parentCustomGroupId == null }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val insertPos = targetIndex ?: (containerItems.size + 1)

            containerItems.filter { it.ScreenIndex >= insertPos }.forEach { item ->
                repository.updateListScreenItem(item.copy(ScreenIndex = item.ScreenIndex + 1))
            }

            val newGroupId = repository.insertCustomGroup(
                CustomGroup(
                    parentScreenId = screenId,
                    groupName = groupName,
                    ScreenIndex = insertPos
                )
            )

            _expandedCustomGroupsMap.value = _expandedCustomGroupsMap.value + (newGroupId to true)

            repository.insertListScreenItem(
                ListScreenItems(
                    parentScreenId = screenId,
                    parentCustomGroupId = null,
                    parentTagId = null,
                    Type = (20000 + newGroupId.toInt()),
                    ScreenIndex = insertPos
                )
            )

            reindexContainerItems(screenId, null)
        }
    }

    fun updateCustomGroup(group: CustomGroup) {
        viewModelScope.launch {
            repository.updateCustomGroup(group)
        }
    }

    fun deleteCustomGroup(group: CustomGroup) {
        viewModelScope.launch {
            repository.deleteCustomGroup(group)
            val items = repository.getItemsForScreenSync(group.parentScreenId)
            val groupItem = items.find { it.Type == (20000 + group.id.toInt()) }
            if (groupItem != null) {
                repository.deleteListScreenItem(groupItem)
            }
            reindexContainerItems(group.parentScreenId, null)
        }
    }

    suspend fun getCustomGroupByIdSync(id: Long): CustomGroup? {
        return repository.getCustomGroupByIdSync(id)
    }

    fun addHeaderItem(
        screenId: Long,
        title: String,
        colorHex: String = "#2196F3",
        targetIndex: Int? = null,
        parentGroupId: Long? = null
    ) {
        viewModelScope.launch {
            _expandedScreensMap.value = _expandedScreensMap.value + (screenId to true)
            if (parentGroupId != null) {
                _expandedCustomGroupsMap.value = _expandedCustomGroupsMap.value + (parentGroupId to true)
            }

            val allItems = repository.getItemsForScreenSync(screenId)
            val containerItems = allItems
                .filter { it.parentCustomGroupId == parentGroupId }
                .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

            val insertPos = targetIndex ?: (containerItems.size + 1)

            containerItems.filter { it.ScreenIndex >= insertPos }.forEach { item ->
                repository.updateListScreenItem(item.copy(ScreenIndex = item.ScreenIndex + 1))
            }

            val newHeaderId = repository.insertHeaderItem(
                HeaderItem(
                    parentScreenId = screenId,
                    title = title,
                    colorHex = colorHex,
                    ScreenIndex = insertPos
                )
            )

            repository.insertListScreenItem(
                ListScreenItems(
                    parentScreenId = screenId,
                    parentCustomGroupId = parentGroupId,
                    parentTagId = null,
                    Type = (40000 + newHeaderId.toInt()),
                    ScreenIndex = insertPos
                )
            )

            reindexContainerItems(screenId, parentGroupId)
        }
    }

    fun updateHeaderItem(header: HeaderItem) {
        viewModelScope.launch {
            repository.updateHeaderItem(header)
        }
    }

    fun deleteHeaderItem(header: HeaderItem) {
        viewModelScope.launch {
            repository.deleteHeaderItem(header)
            val items = repository.getItemsForScreenSync(header.parentScreenId)
            val headerListItem = items.find { it.Type == (40000 + header.id.toInt()) }
            if (headerListItem != null) {
                repository.deleteListScreenItem(headerListItem)
            }
            reindexContainerItems(header.parentScreenId, headerListItem?.parentCustomGroupId)
        }
    }

    suspend fun getHeaderByIdSync(id: Long): HeaderItem? {
        return repository.getHeaderByIdSync(id)
    }

    suspend fun getBitTagsForTagSync(tagId: Long): List<BitTags> {
        return repository.getBitTagsForTagSync(tagId)
    }

    private suspend fun reindexContainerItems(screenId: Long, parentGroupId: Long?) {
        val containerItems = repository.getItemsForScreenSync(screenId)
            .filter { it.parentCustomGroupId == parentGroupId }
            .sortedWith(compareBy({ it.ScreenIndex }, { it.id }))

        containerItems.forEachIndexed { index, item ->
            val newIndex = index + 1
            if (item.ScreenIndex != newIndex) {
                repository.updateListScreenItem(item.copy(ScreenIndex = newIndex))
            }
        }
    }

    val isDebugMode: StateFlow<Boolean> = repository.isDebugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDebugMode(enabled)
        }
    }

    fun toggleDebugMode() {
        setDebugMode(!isDebugMode.value)
    }

    val isConfigureMode: StateFlow<Boolean> = repository.isConfigureMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setConfigureMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setConfigureMode(enabled)
        }
    }

    private val _selectedItem = MutableStateFlow<SelectedItem?>(null)
    val selectedItem: StateFlow<SelectedItem?> = _selectedItem.asStateFlow()

    private val _nodeConnectionStates = MutableStateFlow<Map<Long, NodeConnectionState>>(emptyMap())
    val nodeConnectionStates: StateFlow<Map<Long, NodeConnectionState>> = _nodeConnectionStates.asStateFlow()

    private val _modbusDebugInfo = MutableStateFlow<ModbusDebugInfo?>(null)
    val modbusDebugInfo: StateFlow<ModbusDebugInfo?> = _modbusDebugInfo.asStateFlow()

    private val activeClients = mutableMapOf<Long, ModbusTcpClient>()

    fun selectItem(item: SelectedItem?) {
        _selectedItem.value = item
    }

    fun clearDebugInfo() {
        _modbusDebugInfo.value = null
    }

    private suspend fun calculateModbusAddressInfo(packet: PacketEntity): ModbusAddressInfo {
        val dataType = if (packet.type != null) repository.getDataTypeById(packet.type.toLong()) else null
        val defaultAddr = dataType?.defaultModbusAddress ?: 400001L
        val bytes = dataType?.bytes ?: 2
        val regsPerTag = (bytes / 2).coerceAtLeast(1)
        val isZeroBased = dataType?.isZeroBasedAddressing == true

        val offsetMult = if (isZeroBased) packet.offset else if (packet.offset >= 1) (packet.offset - 1) else packet.offset
        val startingModbusAddr = defaultAddr + (offsetMult.toLong() * regsPerTag)
        val wireStartAddress = if (startingModbusAddr >= 400001L) (startingModbusAddr - 400001L).toInt() else startingModbusAddr.toInt()

        val totalQuantity = packet.size * regsPerTag

        return ModbusAddressInfo(
            startAddress = wireStartAddress.coerceAtLeast(0),
            quantity = totalQuantity.coerceAtLeast(1),
            regsPerTag = regsPerTag,
            dataType = dataType,
            calculatedModbusAddress = startingModbusAddr
        )
    }

    // Node Operations & Modbus Connection
    private suspend fun internalConnectNode(node: NodeEntity): Boolean {
        _nodeConnectionStates.value = _nodeConnectionStates.value + (node.id to NodeConnectionState.CONNECTING)

        val client = ModbusTcpClient(ipAddress = node.ipAddress)
        val success = client.connect()

        if (success) {
            activeClients[node.id] = client
            _nodeConnectionStates.value = _nodeConnectionStates.value + (node.id to NodeConnectionState.COMMUNICATION_OK)
        } else {
            activeClients.remove(node.id)
            _nodeConnectionStates.value = _nodeConnectionStates.value + (node.id to NodeConnectionState.CONNECTION_FAILURE)
        }
        return success
    }

    fun connectNode(node: NodeEntity) {
        viewModelScope.launch {
            internalConnectNode(node)
        }
    }

    fun readPacket(packet: PacketEntity, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val parentNode = repository.getAllNodesList().find { it.id == packet.parentNodeId }
            var nodeState = _nodeConnectionStates.value[packet.parentNodeId]
            var client = activeClients[packet.parentNodeId]

            if (nodeState != NodeConnectionState.COMMUNICATION_OK || client == null || !client.isConnected) {
                if (parentNode == null) {
                    onShowSnackbar("Error: Parent node not found.")
                    return@launch
                }

                onShowSnackbar("Connecting to ${parentNode.name} (${parentNode.ipAddress})...")

                val connected = internalConnectNode(parentNode)
                if (!connected) {
                    onShowSnackbar("Error: Automatic connection to ${parentNode.name} failed.")
                }
                client = activeClients[packet.parentNodeId]
            }

            if (client == null || !client.isConnected) {
                onShowSnackbar("Error: Parent node connection unavailable.")
                return@launch
            }

            val addrInfo = calculateModbusAddressInfo(packet)

            val result = client.readHoldingRegistersDetailed(
                slaveNode = packet.slaveNode,
                startAddress = addrInfo.startAddress,
                quantity = addrInfo.quantity
            )

            val nodeName = parentNode?.name ?: "Node #${packet.parentNodeId}"
            val ip = parentNode?.ipAddress ?: client.ipAddress
            val defaultAddr = addrInfo.dataType?.defaultModbusAddress ?: 400001L
            val startingModbusAddr = addrInfo.calculatedModbusAddress

            val tagResultsText = StringBuilder()
            val registersText = result.registers?.joinToString(", ") { "0x${Integer.toHexString(it).uppercase()} ($it)" }

            if (result.registers != null) {
                onShowSnackbar("Read FC 03 successful (${result.registers.size} registers).")
                val tags = repository.getTagsForPacketSync(packet.id).sortedBy { it.offset }
                val typeName = addrInfo.dataType?.description?.uppercase() ?: addrInfo.dataType?.shortName?.uppercase() ?: "INT"
                val regsPerTag = addrInfo.regsPerTag
                val mode = ModbusByteOrder.fromLabel(parentNode?.byteOrder)
                val transformedRegs = ModbusByteOrderTransformer.transformRegisters(result.registers, mode, addrInfo.regsPerTag)
                val bytes = addrInfo.dataType?.bytes ?: 2
                val defaultAddrVal = addrInfo.dataType?.defaultModbusAddress ?: 400001L
                val isZeroBasedVal = addrInfo.dataType?.isZeroBasedAddressing == true

                tags.forEachIndexed { tagIndex, tag ->
                    val regOffset = tagIndex * regsPerTag
                    if (regOffset + regsPerTag <= transformedRegs.size) {
                        val newStoredVal = ModbusByteOrderTransformer.parseValueFromRegisters(
                            transformedRegs,
                            regOffset,
                            addrInfo.dataType,
                            typeName
                        )
                        repository.updateTag(tag.copy(storedValue = newStoredVal))
                        tagResultsText.append("${tag.name}: $newStoredVal\n")
                    }
                }
            } else {
                onShowSnackbar("Error: Read FC 03 failed.")
            }

            _modbusDebugInfo.value = ModbusDebugInfo(
                title = "Modbus Read Debug (FC 03)",
                nodeName = nodeName,
                ipAddress = ip,
                port = client.port,
                slaveNode = packet.slaveNode,
                packetName = packet.name,
                dataType = addrInfo.dataType?.description ?: "INT",
                defaultModbusAddress = defaultAddr,
                packetOffset = packet.offset,
                calculatedModbusAddress = startingModbusAddr,
                wireRegisterOffset = addrInfo.startAddress,
                quantityRegisters = addrInfo.quantity,
                requestHex = result.requestBytes.toHexString(),
                responseHex = result.responseBytes?.toHexString(),
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                registersText = registersText,
                tagsText = tagResultsText.toString().ifEmpty { null },
                byteOrder = parentNode?.byteOrder ?: "CDAB (3412) — Word-Swap",
                dataTypeObj = addrInfo.dataType
            )
        }
    }

    fun writePacket(packet: PacketEntity, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val parentNode = repository.getAllNodesList().find { it.id == packet.parentNodeId }
            var nodeState = _nodeConnectionStates.value[packet.parentNodeId]
            var client = activeClients[packet.parentNodeId]

            if (nodeState != NodeConnectionState.COMMUNICATION_OK || client == null || !client.isConnected) {
                if (parentNode == null) {
                    onShowSnackbar("Error: Parent node not found.")
                    return@launch
                }

                onShowSnackbar("Connecting to ${parentNode.name} (${parentNode.ipAddress})...")

                val connected = internalConnectNode(parentNode)
                if (!connected) {
                    onShowSnackbar("Error: Automatic connection to ${parentNode.name} failed.")
                }
                client = activeClients[packet.parentNodeId]
            }

            if (client == null || !client.isConnected) {
                onShowSnackbar("Error: Parent node connection unavailable.")
                return@launch
            }

            val addrInfo = calculateModbusAddressInfo(packet)
            val tags = repository.getTagsForPacketSync(packet.id).sortedBy { it.offset }
            val typeName = addrInfo.dataType?.description?.uppercase() ?: addrInfo.dataType?.shortName?.uppercase() ?: "INT"
            val regsPerTag = addrInfo.regsPerTag
            val regValues = IntArray(addrInfo.quantity)

            val mode = ModbusByteOrder.fromLabel(parentNode?.byteOrder)
            val bytesVal = addrInfo.dataType?.bytes ?: 2
            val defaultAddrVal = addrInfo.dataType?.defaultModbusAddress ?: 400001L
            val isZeroBasedVal = addrInfo.dataType?.isZeroBasedAddressing == true

            for (tagIndex in 0 until packet.size) {
                val tag = tags.getOrNull(tagIndex)
                val storedVal = tag?.storedValue ?: "0"
                val regOffset = tagIndex * regsPerTag

                if (regOffset + regsPerTag <= regValues.size) {
                    if (regsPerTag == 1) {
                        val val16 = when (typeName) {
                            "INT", "DS" -> (storedVal.toIntOrNull() ?: 0) and 0xFFFF
                            "HEX", "DH" -> (storedVal.toLongOrNull() ?: 0L).toInt() and 0xFFFF
                            else -> (storedVal.toIntOrNull() ?: 0) and 0xFFFF
                        }
                        regValues[regOffset] = val16
                    } else {
                        val val32: Long = when (typeName) {
                            "INT2", "DD" -> (storedVal.toLongOrNull() ?: 0L) and 0xFFFFFFFFL
                            "FLOAT", "DF" -> (storedVal.toFloatOrNull() ?: 0f).toRawBits().toLong() and 0xFFFFFFFFL
                            else -> (storedVal.toLongOrNull() ?: 0L) and 0xFFFFFFFFL
                        }
                        val regHigh = ((val32 ushr 16) and 0xFFFFL).toInt()
                        val regLow = (val32 and 0xFFFFL).toInt()
                        regValues[regOffset] = regHigh     // Word 0 = High Word
                        regValues[regOffset + 1] = regLow  // Word 1 = Low Word
                    }
                }
            }

            val finalRegValues = ModbusByteOrderTransformer.transformRegisters(regValues, mode, addrInfo.regsPerTag)

            val result = client.writeMultipleRegistersDetailed(
                slaveNode = packet.slaveNode,
                startAddress = addrInfo.startAddress,
                values = finalRegValues
            )

            val nodeName = parentNode?.name ?: "Node #${packet.parentNodeId}"
            val ip = parentNode?.ipAddress ?: client.ipAddress
            val defaultAddr = addrInfo.dataType?.defaultModbusAddress ?: 400001L
            val startingModbusAddr = addrInfo.calculatedModbusAddress

            val tagValuesText = tags.joinToString("\n") { "${it.name}: ${it.storedValue}" }
            val regText = regValues.joinToString(", ") { "0x${Integer.toHexString(it).uppercase()} ($it)" }

            if (result.isSuccess) {
                onShowSnackbar("Write FC 16 successful (${regValues.size} registers).")
            } else {
                onShowSnackbar("Error: Write FC 16 failed.")
            }

            _modbusDebugInfo.value = ModbusDebugInfo(
                title = "Modbus Write Debug (FC 16)",
                nodeName = nodeName,
                ipAddress = ip,
                port = client.port,
                slaveNode = packet.slaveNode,
                packetName = packet.name,
                dataType = addrInfo.dataType?.description ?: "INT",
                defaultModbusAddress = defaultAddr,
                packetOffset = packet.offset,
                calculatedModbusAddress = startingModbusAddr,
                wireRegisterOffset = addrInfo.startAddress,
                quantityRegisters = addrInfo.quantity,
                requestHex = result.requestBytes.toHexString(),
                responseHex = result.responseBytes?.toHexString(),
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                registersText = regText,
                tagsText = tagValuesText.ifEmpty { null },
                byteOrder = parentNode?.byteOrder ?: "CDAB (3412) — Word-Swap",
                dataTypeObj = addrInfo.dataType
            )
        }
    }

    fun updateBitValueAndWrite(
        item: ListScreenItems,
        targetState: Boolean? = null,
        toggle: Boolean = false,
        onShowSnackbar: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val isBitTagItem = item.Type > 0 && item.Type < 1000
            if (!isBitTagItem || item.parentTagId == null) return@launch

            val bitIndex = item.Type - 1
            val tag = repository.getTagByIdSync(item.parentTagId!!) ?: return@launch
            val oldVal = tag.storedValue.toLongOrNull() ?: 0L

            val currentBitSet = (oldVal and (1L shl bitIndex)) != 0L
            val newBitSet = when {
                toggle -> !currentBitSet
                targetState != null -> targetState
                else -> currentBitSet
            }

            val newVal = if (newBitSet) {
                oldVal or (1L shl bitIndex)
            } else {
                oldVal and (1L shl bitIndex).inv()
            }

            val updatedTag = tag.copy(storedValue = newVal.toString())
            repository.updateTag(updatedTag)

            val packet = hierarchy.value.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag.id } }?.packet
                ?: return@launch
            val parentNode = repository.getAllNodesList().find { it.id == packet.parentNodeId }
            var nodeState = _nodeConnectionStates.value[packet.parentNodeId]
            var client = activeClients[packet.parentNodeId]

            if (nodeState != NodeConnectionState.COMMUNICATION_OK || client == null || !client.isConnected) {
                if (parentNode == null) {
                    onShowSnackbar("Updated bit locally (Parent node not found)")
                    return@launch
                }

                val connected = internalConnectNode(parentNode)
                if (!connected) {
                    onShowSnackbar("Updated bit locally (Auto-connect to ${parentNode.name} failed)")
                }
                client = activeClients[packet.parentNodeId]
            }

            if (client == null || !client.isConnected) {
                onShowSnackbar("Updated bit locally (Modbus disconnected)")
                return@launch
            }

            val dataType = repository.getDataTypeById(tag.dataTypeId) ?: (if (packet.type != null) repository.getDataTypeById(packet.type.toLong()) else null)
            val defaultAddr = dataType?.defaultModbusAddress ?: 400001L
            val bytes = dataType?.bytes ?: 2
            val regsPerTag = (bytes / 2).coerceAtLeast(1)

            val isZeroBased = dataType?.isZeroBasedAddressing == true
            val offsetMult = if (isZeroBased) tag.offset else if (tag.offset >= 1) (tag.offset - 1) else tag.offset
            val tagCalculatedAddr = defaultAddr + (offsetMult.toLong() * regsPerTag)
            val tagWireAddress = if (tagCalculatedAddr >= 400001L) (tagCalculatedAddr - 400001L).toInt() else tagCalculatedAddr.toInt()

            val regValues = if (regsPerTag == 1) {
                intArrayOf((newVal and 0xFFFFL).toInt())
            } else {
                val regHigh = ((newVal ushr 16) and 0xFFFFL).toInt()
                val regLow = (newVal and 0xFFFFL).toInt()
                intArrayOf(regLow, regHigh)
            }

            val result = client.writeMultipleRegistersDetailed(
                slaveNode = packet.slaveNode,
                startAddress = tagWireAddress,
                values = regValues
            )

            val nodeName = parentNode?.name ?: "Node #${packet.parentNodeId}"
            val ip = parentNode?.ipAddress ?: client.ipAddress
            val tagRegText = regValues.joinToString(", ") { "0x${Integer.toHexString(it).uppercase()} ($it)" }

            if (result.isSuccess) {
                onShowSnackbar("Modbus write tag '${tag.name}' bit $bitIndex -> ${if (newBitSet) "1 (ON)" else "0 (OFF)"} successful")
            } else {
                onShowSnackbar("Error: Modbus write failed for tag '${tag.name}'")
            }

            _modbusDebugInfo.value = ModbusDebugInfo(
                title = "Modbus Tag Write Debug (FC 16)",
                nodeName = nodeName,
                ipAddress = ip,
                port = client.port,
                slaveNode = packet.slaveNode,
                packetName = packet.name,
                dataType = dataType?.description ?: "INT",
                defaultModbusAddress = defaultAddr,
                packetOffset = packet.offset,
                calculatedModbusAddress = 400001L + tagWireAddress,
                wireRegisterOffset = tagWireAddress,
                quantityRegisters = regsPerTag,
                requestHex = result.requestBytes.toHexString(),
                responseHex = result.responseBytes?.toHexString(),
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                registersText = tagRegText,
                tagsText = "${tag.name}: ${tag.storedValue}",
                byteOrder = parentNode?.byteOrder ?: "CDAB (3412) — Word-Swap",
                dataTypeObj = dataType
            )
        }
    }

    fun updateTagValueAndWrite(
        tag: TagEntity,
        newValue: String,
        onShowSnackbar: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val updatedTag = tag.copy(storedValue = newValue)
            repository.updateTag(updatedTag)

            val packet = hierarchy.value.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag.id } }?.packet
                ?: return@launch
            val parentNode = repository.getAllNodesList().find { it.id == packet.parentNodeId }
            var nodeState = _nodeConnectionStates.value[packet.parentNodeId]
            var client = activeClients[packet.parentNodeId]

            if (nodeState != NodeConnectionState.COMMUNICATION_OK || client == null || !client.isConnected) {
                if (parentNode == null) {
                    onShowSnackbar("Updated tag '${tag.name}' locally (Parent node not found)")
                    return@launch
                }

                val connected = internalConnectNode(parentNode)
                if (!connected) {
                    onShowSnackbar("Updated tag '${tag.name}' locally (Auto-connect to ${parentNode.name} failed)")
                }
                client = activeClients[packet.parentNodeId]
            }

            if (client == null || !client.isConnected) {
                onShowSnackbar("Updated tag '${tag.name}' locally (Modbus disconnected)")
                return@launch
            }

            val dataType = repository.getDataTypeById(tag.dataTypeId) ?: (if (packet.type != null) repository.getDataTypeById(packet.type.toLong()) else null)
            val defaultAddr = dataType?.defaultModbusAddress ?: 400001L
            val bytes = dataType?.bytes ?: 2
            val regsPerTag = (bytes / 2).coerceAtLeast(1)
            val typeName = dataType?.shortName?.uppercase() ?: dataType?.description?.uppercase() ?: "DS"

            val isZeroBased = dataType?.isZeroBasedAddressing == true
            val offsetMult = if (isZeroBased) tag.offset else if (tag.offset >= 1) (tag.offset - 1) else tag.offset
            val tagCalculatedAddr = defaultAddr + (offsetMult.toLong() * regsPerTag)
            val tagWireAddress = if (tagCalculatedAddr >= 400001L) (tagCalculatedAddr - 400001L).toInt() else tagCalculatedAddr.toInt()

            val regValues = IntArray(regsPerTag)
            if (regsPerTag == 1) {
                val val16 = when (typeName) {
                    "INT", "DS" -> (newValue.toIntOrNull() ?: 0) and 0xFFFF
                    "HEX", "DH" -> (newValue.toLongOrNull() ?: 0L).toInt() and 0xFFFF
                    else -> (newValue.toIntOrNull() ?: 0) and 0xFFFF
                }
                regValues[0] = val16
            } else {
                val val32: Long = when (typeName) {
                    "INT2", "DD" -> (newValue.toLongOrNull() ?: 0L) and 0xFFFFFFFFL
                    "FLOAT", "DF" -> (newValue.toFloatOrNull() ?: 0f).toRawBits().toLong() and 0xFFFFFFFFL
                    else -> (newValue.toLongOrNull() ?: 0L) and 0xFFFFFFFFL
                }
                val regHigh = ((val32 ushr 16) and 0xFFFFL).toInt()
                val regLow = (val32 and 0xFFFFL).toInt()
                regValues[0] = regLow
                regValues[1] = regHigh
            }

            val result = client.writeMultipleRegistersDetailed(
                slaveNode = packet.slaveNode,
                startAddress = tagWireAddress,
                values = regValues
            )

            val nodeName = parentNode?.name ?: "Node #${packet.parentNodeId}"
            val ip = parentNode?.ipAddress ?: client.ipAddress
            val tagRegText = regValues.joinToString(", ") { "0x${Integer.toHexString(it).uppercase()} ($it)" }

            if (result.isSuccess) {
                onShowSnackbar("Modbus write tag '${tag.name}' -> $newValue successful")
            } else {
                onShowSnackbar("Error: Modbus write failed for tag '${tag.name}'")
            }

            _modbusDebugInfo.value = ModbusDebugInfo(
                title = "Modbus Tag Write Debug (FC 16)",
                nodeName = nodeName,
                ipAddress = ip,
                port = client.port,
                slaveNode = packet.slaveNode,
                packetName = packet.name,
                dataType = dataType?.description ?: "INT",
                defaultModbusAddress = defaultAddr,
                packetOffset = packet.offset,
                calculatedModbusAddress = 400001L + tagWireAddress,
                wireRegisterOffset = tagWireAddress,
                quantityRegisters = regsPerTag,
                requestHex = result.requestBytes.toHexString(),
                responseHex = result.responseBytes?.toHexString(),
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                registersText = tagRegText,
                tagsText = "${tag.name}: ${tag.storedValue}",
                byteOrder = parentNode?.byteOrder ?: "CDAB (3412) — Word-Swap",
                dataTypeObj = dataType
            )
        }
    }

    fun readTag(
        tag: TagEntity,
        onShowSnackbar: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val packet = hierarchy.value.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag.id } }?.packet
                ?: return@launch
            val parentNode = repository.getAllNodesList().find { it.id == packet.parentNodeId }
            var nodeState = _nodeConnectionStates.value[packet.parentNodeId]
            var client = activeClients[packet.parentNodeId]

            if (nodeState != NodeConnectionState.COMMUNICATION_OK || client == null || !client.isConnected) {
                if (parentNode == null) {
                    onShowSnackbar("Error: Parent node not found.")
                    return@launch
                }

                val connected = internalConnectNode(parentNode)
                if (!connected) {
                    onShowSnackbar("Error: Automatic connection to ${parentNode.name} failed.")
                }
                client = activeClients[packet.parentNodeId]
            }

            if (client == null || !client.isConnected) {
                onShowSnackbar("Error: Modbus disconnected.")
                return@launch
            }

            val dataType = repository.getDataTypeById(tag.dataTypeId) ?: (if (packet.type != null) repository.getDataTypeById(packet.type.toLong()) else null)
            val defaultAddr = dataType?.defaultModbusAddress ?: 400001L
            val bytes = dataType?.bytes ?: 2
            val regsPerTag = (bytes / 2).coerceAtLeast(1)

            val isZeroBased = dataType?.isZeroBasedAddressing == true
            val offsetMult = if (isZeroBased) tag.offset else if (tag.offset >= 1) (tag.offset - 1) else tag.offset
            val tagCalculatedAddr = defaultAddr + (offsetMult.toLong() * regsPerTag)
            val tagWireAddress = if (tagCalculatedAddr >= 400001L) (tagCalculatedAddr - 400001L).toInt() else tagCalculatedAddr.toInt()

            val result = client.readHoldingRegistersDetailed(
                slaveNode = packet.slaveNode,
                startAddress = tagWireAddress,
                quantity = regsPerTag
            )

            val nodeName = parentNode?.name ?: "Node #${packet.parentNodeId}"
            val ip = parentNode?.ipAddress ?: client.ipAddress
            val typeName = dataType?.shortName?.uppercase() ?: dataType?.description?.uppercase() ?: "DS"

            if (result.isSuccess && result.registers != null) {
                val mode = ModbusByteOrder.fromLabel(parentNode?.byteOrder)
                val transformedRegs = ModbusByteOrderTransformer.transformRegisters(result.registers, mode, regsPerTag)
                val parsedVal = ModbusByteOrderTransformer.parseValueFromRegisters(
                    transformedRegs,
                    0,
                    dataType,
                    typeName
                )

                val updatedTag = tag.copy(storedValue = parsedVal)
                repository.updateTag(updatedTag)
                onShowSnackbar("Read tag '${tag.name}' -> $parsedVal successful")
            } else {
                onShowSnackbar("Error: Modbus read failed for tag '${tag.name}'")
            }

            val registersText = result.registers?.joinToString(", ") { "0x${Integer.toHexString(it).uppercase()} ($it)" }

            _modbusDebugInfo.value = ModbusDebugInfo(
                title = "Modbus Tag Read Debug (FC 03)",
                nodeName = nodeName,
                ipAddress = ip,
                port = client.port,
                slaveNode = packet.slaveNode,
                packetName = packet.name,
                dataType = dataType?.description ?: "INT",
                defaultModbusAddress = defaultAddr,
                packetOffset = packet.offset,
                calculatedModbusAddress = 400001L + tagWireAddress,
                wireRegisterOffset = tagWireAddress,
                quantityRegisters = regsPerTag,
                requestHex = result.requestBytes.toHexString(),
                responseHex = result.responseBytes?.toHexString(),
                isSuccess = result.isSuccess,
                errorMessage = result.errorMessage,
                registersText = registersText,
                tagsText = "${tag.name}: ${tag.storedValue}",
                byteOrder = parentNode?.byteOrder ?: "CDAB (3412) — Word-Swap",
                dataTypeObj = dataType
            )
        }
    }

    fun addNode(onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allNodes = repository.getAllNodesList()
            val nodeRegex = Regex("^Node(\\d+)$", RegexOption.IGNORE_CASE)

            var maxNum = 0
            var foundNumberedNode = false

            allNodes.forEach { node ->
                val match = nodeRegex.find(node.name.trim())
                if (match != null) {
                    val num = match.groupValues[1].toIntOrNull()
                    if (num != null) {
                        foundNumberedNode = true
                        if (num > maxNum) {
                            maxNum = num
                        }
                    }
                }
            }

            var nextNum = if (foundNumberedNode) (maxNum + 1) else 1
            var candidateName = "Node$nextNum"

            while (allNodes.any { it.name.trim().equals(candidateName, ignoreCase = true) }) {
                nextNum++
                candidateName = "Node$nextNum"
            }

            val defaultIp = "127.0.0.1"
            val newId = repository.insertNode(NodeEntity(name = candidateName, ipAddress = defaultIp))
            _newlyCreatedNodeIds.value = _newlyCreatedNodeIds.value + newId
            val newNode = NodeEntity(id = newId, name = candidateName, ipAddress = defaultIp)
            _selectedItem.value = SelectedItem.Node(newNode)
            onShowSnackbar("Created node '$candidateName'")
        }
    }

    fun updateNode(node: NodeEntity) {
        viewModelScope.launch {
            repository.updateNode(node)
            _selectedItem.value = SelectedItem.Node(node)
        }
    }

    fun cleanupOrphanListScreenItems() {
        viewModelScope.launch {
            val allItems = repository.getAllListScreenItemsSync()
            if (allItems.isEmpty()) return@launch

            val allTagIds = repository.getAllTagsList().map { it.id }.toSet()
            val allPacketIds = repository.getAllPacketsList().map { it.id }.toSet()
            val allNodeIds = repository.getAllNodesList().map { it.id }.toSet()

            allItems.forEach { item ->
                val type = item.Type
                val shouldDelete = when {
                    type in 0..999 -> {
                        item.parentTagId == null || item.parentTagId !in allTagIds
                    }
                    type in 1000..9999 -> {
                        val packetId = (type - 1000).toLong()
                        packetId !in allPacketIds
                    }
                    type in 10000..19999 -> {
                        val nodeId = (type - 10000).toLong()
                        nodeId !in allNodeIds
                    }
                    else -> false
                }

                if (shouldDelete) {
                    repository.deleteListScreenItem(item)
                }
            }
        }
    }

    fun deleteNode(node: NodeEntity) {
        viewModelScope.launch {
            repository.deleteNode(node)
            activeClients.remove(node.id)?.disconnect()
            _nodeConnectionStates.value = _nodeConnectionStates.value - node.id
            if ((_selectedItem.value as? SelectedItem.Node)?.node?.id == node.id) {
                _selectedItem.value = null
            }
            cleanupOrphanListScreenItems()
        }
    }

    // Packet Operations
    fun addPacket(parentNodeId: Long, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val allPackets = repository.getAllPacketsList()
            val packetRegex = Regex("^Packet(\\d+)$", RegexOption.IGNORE_CASE)

            var maxNum = 0
            var foundNumberedPacket = false

            allPackets.forEach { packet ->
                val match = packetRegex.find(packet.name.trim())
                if (match != null) {
                    val num = match.groupValues[1].toIntOrNull()
                    if (num != null) {
                        foundNumberedPacket = true
                        if (num > maxNum) {
                            maxNum = num
                        }
                    }
                }
            }

            var nextNum = if (foundNumberedPacket) (maxNum + 1) else 1
            var candidateName = "Packet$nextNum"

            while (allPackets.any { it.name.trim().equals(candidateName, ignoreCase = true) }) {
                nextNum++
                candidateName = "Packet$nextNum"
            }

            val defaultDataTypeId = dataTypes.value.firstOrNull()?.id ?: 0L
            val initialSize = 1
            val newPacket = PacketEntity(
                parentNodeId = parentNodeId,
                name = candidateName,
                description = "",
                type = defaultDataTypeId.toInt(),
                period = 1000,
                config = "",
                offset = 1,
                slaveNode = 1,
                pollData = true,
                size = initialSize
            )
            val packetId = repository.insertPacket(newPacket)
            _newlyCreatedPacketIds.value = _newlyCreatedPacketIds.value + packetId
            val createdPacket = newPacket.copy(id = packetId)

            _selectedItem.value = SelectedItem.Packet(createdPacket)
            onShowSnackbar("Created packet '$candidateName'")
        }
    }

    fun updatePacket(packet: PacketEntity) {
        viewModelScope.launch {
            val oldPacket = repository.getPacketByIdSync(packet.id)
            repository.updatePacket(packet)

            val dataTypes = repository.getAllDataTypesList()
            val packetDt = repository.resolveDataTypeForPacket(packet, dataTypes)

            if (packetDt.hasBits && oldPacket != null && oldPacket.name != packet.name) {
                val tags = repository.getTagsForPacketSync(packet.id)
                tags.forEach { tag ->
                    val bitTags = repository.getBitTagsForTagSync(tag.id)
                    bitTags.forEach { bitTag ->
                        val defaultName1 = "${tag.name}-${bitTag.bitIndex}"
                        val defaultName2 = "${oldPacket.name}-${tag.offset}-${bitTag.bitIndex}"
                        val defaultName3 = "Tag #${tag.id}-${bitTag.bitIndex}"
                        val isUnedited = bitTag.name == defaultName1 ||
                                bitTag.name == defaultName2 ||
                                bitTag.name == defaultName3 ||
                                bitTag.name.endsWith("-${bitTag.bitIndex}")

                        if (isUnedited) {
                            val newBitName = "${tag.name}-${bitTag.bitIndex}"
                            if (bitTag.name != newBitName) {
                                repository.updateBitTag(bitTag.copy(name = newBitName))
                            }
                        }
                    }
                }
            }

            _selectedItem.value = SelectedItem.Packet(packet)
            cleanupOrphanListScreenItems()
        }
    }

    // Backup & Restore Operations
    fun performBackup(context: Context, onResult: (BackupResult) -> Unit) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.performBackup(context, repository)
            onResult(result)
        }
    }

    fun getAvailableBackupFiles(context: Context): List<BackupFileInfo> {
        return DatabaseBackupManager.getAvailableBackupFiles(context)
    }

    fun performRestore(file: File, mode: RestoreMode, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.executeRestore(file, mode, repository)
            onResult(result)
        }
    }

    fun performRestoreFromUri(context: Context, uri: Uri, mode: RestoreMode, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.executeRestoreFromUri(context, uri, mode, repository)
            onResult(result)
        }
    }

    fun exportAllTags(onResult: (ExportResult) -> Unit) {
        viewModelScope.launch {
            val format = tagFileFormat.value
            val result = TagCsvImporterExporter.exportAllTags(repository, format)
            onResult(result)
        }
    }

    fun importAllTagsFromFile(context: Context, file: File, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val format = tagFileFormat.value
                val csvText = TagCsvImporterExporter.readFileContent(context, file)
                val records = TagCsvImporterExporter.parseCsvContent(csvText)
                val result = TagCsvImporterExporter.importAllTags(records, repository, format)
                onResult(result)
            } catch (e: Exception) {
                onResult(
                    ImportResult(
                        isSuccess = false,
                        nodeName = "All Packets",
                        totalTagsImported = 0,
                        packetsCreated = 0,
                        errorMessage = "Failed to read file '${file.name}': ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    fun importAllTagsFromUri(context: Context, uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val format = tagFileFormat.value
                val csvText = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                } ?: throw IOException("Unable to open stream for $uri")

                val records = TagCsvImporterExporter.parseCsvContent(csvText)
                val result = TagCsvImporterExporter.importAllTags(records, repository, format)
                onResult(result)
            } catch (e: Exception) {
                onResult(
                    ImportResult(
                        isSuccess = false,
                        nodeName = "All Packets",
                        totalTagsImported = 0,
                        packetsCreated = 0,
                        errorMessage = "Failed to import tags from file: ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    fun importTagsFromCsvText(node: NodeEntity, csvText: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val records = TagCsvImporterExporter.parseCsvContent(csvText)
            val result = TagCsvImporterExporter.importCsvRecordsToNode(records, node, repository)
            _selectedItem.value = SelectedItem.Node(node)
            onResult(result)
        }
    }

    fun exportTagsForNode(node: NodeEntity, onResult: (ExportResult) -> Unit) {
        viewModelScope.launch {
            val result = TagCsvImporterExporter.exportTagsForNode(node, repository)
            onResult(result)
        }
    }

    fun importTagsForPacketFromCsvText(packet: PacketEntity, csvText: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val records = TagCsvImporterExporter.parseCsvContent(csvText)
            val result = TagCsvImporterExporter.importCsvRecordsToPacket(records, packet, repository)
            _selectedItem.value = SelectedItem.Packet(packet)
            onResult(result)
        }
    }

    fun getAvailableCsvFiles(context: Context): List<CsvFileInfo> {
        return TagCsvImporterExporter.getAvailableCsvFiles(context)
    }

    fun getScannedDirectories(context: Context): List<String> {
        return TagCsvImporterExporter.getScannedDirectories(context)
    }

    fun importTagsForPacketFromFile(context: Context, packet: PacketEntity, file: File, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val csvText = TagCsvImporterExporter.readFileContent(context, file)
                val records = TagCsvImporterExporter.parseCsvContent(csvText)
                val result = TagCsvImporterExporter.importCsvRecordsToPacket(records, packet, repository)
                _selectedItem.value = SelectedItem.Packet(packet)
                onResult(result)
            } catch (e: Exception) {
                onResult(
                    ImportResult(
                        isSuccess = false,
                        nodeName = packet.name,
                        totalTagsImported = 0,
                        packetsCreated = 0,
                        errorMessage = "Failed to read file '${file.name}': ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    fun importTagsForPacketFromUri(context: Context, packet: PacketEntity, uri: Uri, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            try {
                val csvText = TagCsvImporterExporter.readUriContent(context, uri)
                val records = TagCsvImporterExporter.parseCsvContent(csvText)
                val result = TagCsvImporterExporter.importCsvRecordsToPacket(records, packet, repository)
                _selectedItem.value = SelectedItem.Packet(packet)
                onResult(result)
            } catch (e: Exception) {
                onResult(
                    ImportResult(
                        isSuccess = false,
                        nodeName = packet.name,
                        totalTagsImported = 0,
                        packetsCreated = 0,
                        errorMessage = "Failed to read content URI: ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    fun exportTagsForPacket(packet: PacketEntity, onResult: (ExportResult) -> Unit) {
        viewModelScope.launch {
            val result = TagCsvImporterExporter.exportTagsForPacket(packet, repository)
            onResult(result)
        }
    }

    fun deletePacket(packet: PacketEntity) {
        viewModelScope.launch {
            repository.deletePacket(packet)
            if ((_selectedItem.value as? SelectedItem.Packet)?.packet?.id == packet.id) {
                _selectedItem.value = null
            }
            cleanupOrphanListScreenItems()
        }
    }

    // Tag Operations
    fun addTag(parentPacketId: Long, onShowSnackbar: (String) -> Unit = {}) {
        viewModelScope.launch {
            val parentPacket = repository.getPacketByIdSync(parentPacketId) ?: return@launch
            val dataTypeId = parentPacket.type?.toLong() ?: 0L
            val dataType = repository.getDataTypeById(dataTypeId)
            val dtShortName = dataType?.shortName?.ifEmpty { dataType.description } ?: "DS"

            val existingTags = repository.getTagsForPacketSync(parentPacketId)
            val nextOffset = parentPacket.offset + existingTags.size
            val candidateName = "Tag $dtShortName$nextOffset"

            val tagId = repository.insertTag(
                TagEntity(
                    name = candidateName,
                    description = "",
                    dataTypeId = dataTypeId,
                    storedValue = "0",
                    offset = nextOffset
                )
            )
            _newlyCreatedTagIds.value = _newlyCreatedTagIds.value + tagId
            val createdTag = TagEntity(
                id = tagId,
                name = candidateName,
                description = "",
                dataTypeId = dataTypeId,
                storedValue = "0",
                offset = nextOffset
            )
            _selectedItem.value = SelectedItem.Tag(createdTag)
            onShowSnackbar("Created tag '$candidateName'")

            if (dataType?.hasBits == true) {
                val numBits = dataType.bytes * 8
                val bitTagsList = (0 until numBits).map { bitIndex ->
                    BitTags(
                        name = "$candidateName-$bitIndex",
                        parentTagId = tagId,
                        bitIndex = bitIndex
                    )
                }
                repository.insertAllBitTags(bitTagsList)
            }
        }
    }

    fun updateTag(tag: TagEntity) {
        viewModelScope.launch {
            val oldTag = repository.getTagByIdSync(tag.id)
            repository.updateTag(tag)
            _selectedItem.value = SelectedItem.Tag(tag)

            if (oldTag != null && oldTag.name != tag.name) {
                val bitTags = repository.getBitTagsForTagSync(tag.id)
                val oldName = oldTag.name
                val newName = tag.name

                bitTags.forEach { bitTag ->
                    val defaultName1 = "$oldName-${bitTag.bitIndex}"
                    val defaultName2 = "Tag #${tag.id}-${bitTag.bitIndex}"
                    val isUnedited = bitTag.name == defaultName1 ||
                            bitTag.name == defaultName2 ||
                            (bitTag.name.endsWith("-${bitTag.bitIndex}") && bitTag.name.substringBeforeLast("-") == oldName)

                    if (isUnedited) {
                        val newBitName = "$newName-${bitTag.bitIndex}"
                        if (bitTag.name != newBitName) {
                            repository.updateBitTag(bitTag.copy(name = newBitName))
                        }
                    }
                }
            }
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            if ((_selectedItem.value as? SelectedItem.Tag)?.tag?.id == tag.id) {
                _selectedItem.value = null
            }
            cleanupOrphanListScreenItems()
        }
    }

    // BitTag Operations
    fun updateBitTag(bitTag: BitTags, parentTag: TagEntity? = null) {
        viewModelScope.launch {
            repository.updateBitTag(bitTag)
            _selectedItem.value = SelectedItem.BitTag(bitTag, parentTag)
        }
    }

    fun deleteBitTag(bitTag: BitTags) {
        viewModelScope.launch {
            repository.deleteBitTag(bitTag)
            if ((_selectedItem.value as? SelectedItem.BitTag)?.bitTag?.id == bitTag.id) {
                _selectedItem.value = null
            }
        }
    }

    // TagListItems Operations
    fun getListItemsForTag(tagId: Long): Flow<List<TagListItems>> {
        return repository.getListItemsForTag(tagId)
    }

    suspend fun getListItemsForTagSync(tagId: Long): List<TagListItems> {
        return repository.getListItemsForTagSync(tagId)
    }

    fun addTagListItem(tagId: Long, numberVal: Int, label: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getListItemsForTagSync(tagId)
            val trimmedLabel = label.trim()
            if (trimmedLabel.isEmpty()) {
                onResult(false, "Label cannot be empty")
                return@launch
            }

            val duplicateNum = existing.find { it.number == numberVal }
            if (duplicateNum != null) {
                onResult(false, "Value $numberVal is already assigned to '${duplicateNum.label}'")
                return@launch
            }

            val newItem = TagListItems(
                parentTagId = tagId,
                number = numberVal,
                label = trimmedLabel
            )
            repository.insertTagListItem(newItem)
            onResult(true, "Added item $numberVal '$trimmedLabel'")
        }
    }

    fun updateTagListItem(item: TagListItems, newNumber: Int, newLabel: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = repository.getListItemsForTagSync(item.parentTagId)
            val trimmedLabel = newLabel.trim()
            if (trimmedLabel.isEmpty()) {
                onResult(false, "Label cannot be empty")
                return@launch
            }

            val duplicateNum = existing.find { it.id != item.id && it.number == newNumber }
            if (duplicateNum != null) {
                onResult(false, "Value $newNumber is already assigned to '${duplicateNum.label}'")
                return@launch
            }

            repository.updateTagListItem(item.copy(number = newNumber, label = trimmedLabel))
            onResult(true, "Updated item $newNumber")
        }
    }

    fun deleteTagListItem(item: TagListItems) {
        viewModelScope.launch {
            repository.deleteTagListItem(item)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeClients.values.forEach { it.disconnect() }
        activeClients.clear()
    }
}
