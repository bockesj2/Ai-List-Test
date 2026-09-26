package com.example.ailisttest.data

import com.example.ailisttest.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

sealed class ResolvedTarget {
    data class TagTarget(val tag: TagEntity, val bitIndex: Int? = null) : ResolvedTarget()
    data class PacketTarget(val packet: PacketEntity) : ResolvedTarget()
    data class NodeTarget(val node: NodeEntity) : ResolvedTarget()
}

class MainRepository(
    private val nodeDao: NodeDao,
    private val packetDao: PacketDao,
    private val tagDao: TagDao,
    private val dataTypeDao: DataTypeDao,
    private val bitTagDao: BitTagDao,
    private val screensDao: ScreensDao,
    private val listScreenItemsDao: ListScreenItemsDao,
    private val customGroupDao: CustomGroupDao,
    private val headerItemDao: HeaderItemDao,
    private val tagListItemDao: TagListItemDao,
    private val userPreferences: UserPreferences
) {
    val isDebugMode: Flow<Boolean> = userPreferences.isDebugMode
    suspend fun setDebugMode(enabled: Boolean) = userPreferences.setDebugMode(enabled)
    val isConfigureMode: Flow<Boolean> = userPreferences.isConfigureMode
    suspend fun setConfigureMode(enabled: Boolean) = userPreferences.setConfigureMode(enabled)
    val isPollingEnabled: Flow<Boolean> = userPreferences.isPollingEnabled
    suspend fun setPollingEnabled(enabled: Boolean) = userPreferences.setPollingEnabled(enabled)
    val plcPreset: Flow<String> = userPreferences.plcPreset
    suspend fun setPlcPreset(preset: String) = userPreferences.setPlcPreset(preset)
    suspend fun getPlcPresetSync(): String = userPreferences.plcPreset.first()
    val byteOrder: Flow<String> = userPreferences.byteOrder
    suspend fun setByteOrder(order: String) = userPreferences.setByteOrder(order)
    suspend fun getByteOrderSync(): String = userPreferences.byteOrder.first()
    val dataTypePlcPreset: Flow<String> = userPreferences.dataTypePlcPreset
    suspend fun setDataTypePlcPreset(preset: String) = userPreferences.setDataTypePlcPreset(preset)
    val tagFileFormat: Flow<String> = userPreferences.tagFileFormat
    suspend fun setTagFileFormat(format: String) = userPreferences.setTagFileFormat(format)
    suspend fun updateDataType(dataType: DataTypes) {
        dataTypeDao.updateDataType(dataType)
        refreshHierarchy()
    }
    suspend fun resetDataTypesToClickPlusDefaults() {
        dataTypeDao.deleteAllDataTypes()
        dataTypeDao.insertAll(
            listOf(
                PlcDataTypes(id = 1, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true),
                PlcDataTypes(id = 2, description = "Data Register Double", shortName = "DD", dataType = "INT", bytes = 4, defaultModbusAddress = 416385L, isZeroBasedAddressing = false, hasBits = true),
                PlcDataTypes(id = 3, description = "Data Register Hex", shortName = "DH", dataType = "UINT", bytes = 2, defaultModbusAddress = 424577L, isZeroBasedAddressing = false, hasBits = true),
                PlcDataTypes(id = 4, description = "Data Register Float", shortName = "DF", dataType = "FLOAT", bytes = 4, defaultModbusAddress = 428673L, isZeroBasedAddressing = false, hasBits = false)
            )
        )
        refreshHierarchy()
    }

    // CustomGroup Operations
    fun getGroupsForScreen(screenId: Long): Flow<List<CustomGroup>> = customGroupDao.getGroupsForScreen(screenId)
    suspend fun getCustomGroupByIdSync(id: Long): CustomGroup? = customGroupDao.getGroupByIdSync(id)
    suspend fun insertCustomGroup(group: CustomGroup): Long = customGroupDao.insertGroup(group)
    suspend fun updateCustomGroup(group: CustomGroup) = customGroupDao.updateGroup(group)
    suspend fun deleteCustomGroup(group: CustomGroup) = customGroupDao.deleteGroup(group)

    // HeaderItem Operations
    fun getHeadersForScreen(screenId: Long): Flow<List<HeaderItem>> = headerItemDao.getHeadersForScreen(screenId)
    suspend fun getHeaderByIdSync(id: Long): HeaderItem? = headerItemDao.getHeaderByIdSync(id)
    suspend fun insertHeaderItem(header: HeaderItem): Long = headerItemDao.insertHeader(header)
    suspend fun updateHeaderItem(header: HeaderItem) = headerItemDao.updateHeader(header)
    suspend fun deleteHeaderItem(header: HeaderItem) = headerItemDao.deleteHeader(header)

    // Screens Operations
    fun getAllScreens(): Flow<List<Screens>> = screensDao.getAllScreens()
    suspend fun getAllScreensList(): List<Screens> = screensDao.getAllScreensList()
    suspend fun getScreenById(id: Long): Screens? = screensDao.getScreenById(id)
    fun getScreensByType(type: Int): Flow<List<Screens>> = screensDao.getScreensByType(type)
    fun getListScreensWithItems(): Flow<List<ScreenWithListItems>> = combine(
        screensDao.getListScreensWithItems(),
        _hierarchyRefresh
    ) { screensWithItems, _ ->
        val allTags = tagDao.getAllTagsSync()
        screensWithItems.map { screenWithItems ->
            screenWithItems.copy(
                items = screenWithItems.items.map { itemWithTag ->
                    val tagId = itemWithTag.tag?.id ?: itemWithTag.item.parentTagId
                    val freshTag = if (tagId != null) allTags.find { it.id == tagId } else itemWithTag.tag
                    itemWithTag.copy(tag = freshTag ?: itemWithTag.tag)
                }
            )
        }
    }
    suspend fun insertScreen(screen: Screens): Long = screensDao.insertScreen(screen)
    suspend fun updateScreen(screen: Screens) = screensDao.updateScreen(screen)
    suspend fun deleteScreen(screen: Screens) = screensDao.deleteScreen(screen)

    suspend fun findTagByName(tagName: String): TagEntity? {
        val cleanName = tagName.substringAfterLast("/").trim()
        return tagDao.getTagByNameSync(cleanName) ?: tagDao.getAllTagsSync().find { it.name == cleanName || it.name == tagName }
    }

    suspend fun resolvePickerSelection(selectedPath: String): ResolvedTarget? {
        val cleanName = selectedPath.substringAfterLast("/").trim()

        val allNodes = nodeDao.getAllNodesList()
        val node = allNodes.find { it.name == cleanName || selectedPath == it.name || selectedPath.startsWith("${it.name} (") }
        if (node != null && (cleanName == node.name || selectedPath == node.name || selectedPath.startsWith("${node.name} ("))) {
            return ResolvedTarget.NodeTarget(node)
        }

        val packet = packetDao.getAllPacketsSync().find { it.name == cleanName || it.name == selectedPath }
        if (packet != null) {
            return ResolvedTarget.PacketTarget(packet)
        }

        val tag = tagDao.getTagByNameSync(cleanName)
            ?: tagDao.getAllTagsSync().find { it.name == cleanName || it.name == selectedPath }
        if (tag != null) {
            return ResolvedTarget.TagTarget(tag = tag, bitIndex = null)
        }

        val bitTag = bitTagDao.getBitTagByNameSync(cleanName)
            ?: bitTagDao.getAllBitTagsSync().find { it.name == cleanName || it.name == selectedPath }
        if (bitTag != null) {
            val parentTag = tagDao.getTagByIdSync(bitTag.parentTagId)
            if (parentTag != null) {
                return ResolvedTarget.TagTarget(tag = parentTag, bitIndex = bitTag.bitIndex)
            }
        }

        // Parse dynamic BitTag format: "${parentTagName}-${bitIndex}" (e.g. "Packet1-1-0")
        if (cleanName.contains("-")) {
            val possibleBitIndex = cleanName.substringAfterLast("-").toIntOrNull()
            val possibleParentTagName = cleanName.substringBeforeLast("-").trim()

            if (possibleBitIndex != null && possibleBitIndex in 0..31 && possibleParentTagName.isNotEmpty()) {
                val parentTag = tagDao.getTagByNameSync(possibleParentTagName)
                    ?: tagDao.getAllTagsSync().find { it.name == possibleParentTagName }
                if (parentTag != null) {
                    return ResolvedTarget.TagTarget(tag = parentTag, bitIndex = possibleBitIndex)
                }
            }
        }

        return null
    }

    // ListScreenItems Operations (retrieved in chronological order based upon ScreenIndex)
    fun getItemsForScreen(screenId: Long): Flow<List<ListScreenItems>> = listScreenItemsDao.getItemsForScreen(screenId)
    suspend fun getItemsForScreenSync(screenId: Long): List<ListScreenItems> = listScreenItemsDao.getItemsForScreenSync(screenId)
    suspend fun getAllListScreenItemsSync(): List<ListScreenItems> = listScreenItemsDao.getAllItemsSync()
    fun getItemsWithTagForScreen(screenId: Long): Flow<List<ListScreenItemWithTag>> = listScreenItemsDao.getItemsWithTagForScreen(screenId)
    suspend fun insertListScreenItem(item: ListScreenItems): Long {
        val id = listScreenItemsDao.insertItem(item)
        refreshHierarchy()
        return id
    }
    suspend fun updateListScreenItem(item: ListScreenItems) {
        listScreenItemsDao.updateItem(item)
        refreshHierarchy()
    }
    suspend fun deleteListScreenItem(item: ListScreenItems) {
        listScreenItemsDao.deleteItem(item)
        refreshHierarchy()
    }

    suspend fun cleanupOrphanListScreenItems() {
        val allItems = listScreenItemsDao.getAllItemsSync()
        if (allItems.isEmpty()) return

        val allTagIds = tagDao.getAllTagsSync().map { it.id }.toSet()
        val allPacketIds = packetDao.getAllPacketsSync().map { it.id }.toSet()
        val allNodeIds = nodeDao.getAllNodesList().map { it.id }.toSet()

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
                listScreenItemsDao.deleteItem(item)
            }
        }
    }
    suspend fun getAllNodesList(): List<NodeEntity> = nodeDao.getAllNodesList()
    suspend fun getAllPacketsList(): List<PacketEntity> = packetDao.getAllPacketsSync()
    suspend fun getAllTagsList(): List<TagEntity> = tagDao.getAllTagsSync()
    suspend fun getPacketsForNodeSync(nodeId: Long): List<PacketEntity> = packetDao.getPacketsForNodeSync(nodeId)
    suspend fun getPacketByIdSync(id: Long): PacketEntity? = packetDao.getPacketByIdSync(id)
    fun resolveDataTypeForPacket(packet: PacketEntity, dataTypes: List<DataTypes>): DataTypes {
        if (dataTypes.isEmpty()) {
            return DataTypes(id = 1, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
        }
        val typeId = packet.type?.toLong()
        if (typeId != null) {
            val direct = dataTypes.find { it.id == typeId }
            if (direct != null) return direct

            val mapped = when (typeId.toInt()) {
                0 -> dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.description.contains("Short", ignoreCase = true) }
                1 -> dataTypes.find { it.shortName.equals("DD", ignoreCase = true) || it.description.contains("Double", ignoreCase = true) }
                2 -> dataTypes.find { it.shortName.equals("DH", ignoreCase = true) || it.description.contains("Hex", ignoreCase = true) }
                3 -> dataTypes.find { it.shortName.equals("DF", ignoreCase = true) || it.description.contains("Float", ignoreCase = true) }
                else -> null
            }
            if (mapped != null) return mapped
        }
        return dataTypes.find { it.shortName.equals("DS", ignoreCase = true) } ?: dataTypes.first()
    }

    suspend fun getTagsForPacketSync(packetId: Long): List<TagEntity> {
        val packet = packetDao.getPacketByIdSync(packetId) ?: return emptyList()
        ensureTagsExistForPacket(packet)
        val allDataTypes = dataTypeDao.getAllDataTypesList()
        val dataType = resolveDataTypeForPacket(packet, allDataTypes)

        val dataTypeId = dataType.id
        val startOffset = packet.offset
        val endOffset = packet.offset + packet.size
        return tagDao.getTagsForDataTypeAndOffsetRangeSync(dataTypeId, startOffset, endOffset)
    }

    suspend fun ensureTagsExistForPacket(packet: PacketEntity) {
        val allDataTypes = dataTypeDao.getAllDataTypesList()
        if (allDataTypes.isEmpty()) return

        val dataType = resolveDataTypeForPacket(packet, allDataTypes)
        val dataTypeId = dataType.id
        val hasBits = dataType.hasBits
        val bytes = dataType.bytes

        for (i in 0 until packet.size) {
            val tagOffset = packet.offset + i
            val existing = tagDao.getTagByDataTypeAndOffsetSync(dataTypeId, tagOffset)
            if (existing == null) {
                val dtShortName = dataType.shortName.ifEmpty { dataType.description }
                val tagName = "Tag $dtShortName$tagOffset"
                val newTagId = tagDao.insertTag(
                    TagEntity(
                        name = tagName,
                        description = "",
                        dataTypeId = dataTypeId,
                        storedValue = "0",
                        offset = tagOffset
                    )
                )
                if (hasBits && newTagId > 0) {
                    val numBits = bytes * 8
                    val bitTagsList = (0 until numBits).map { bitIndex ->
                        BitTags(
                            name = "$tagName-$bitIndex",
                            parentTagId = newTagId,
                            bitIndex = bitIndex
                        )
                    }
                    bitTagDao.insertAllBitTags(bitTagsList)
                }
            }
        }
    }

    private val _hierarchyRefresh = MutableStateFlow(0)
    fun refreshHierarchy() {
        _hierarchyRefresh.value = _hierarchyRefresh.value + 1
    }

    fun getFullHierarchy(): Flow<List<NodeWithPacketsAndTags>> = combine(
        nodeDao.getAllNodes(),
        _hierarchyRefresh
    ) { nodes, _ ->
        val dataTypes = dataTypeDao.getAllDataTypesList()
        nodes.map { node ->
            val packets = packetDao.getPacketsForNodeSync(node.id)
            val packetsWithTags = packets.map { packet ->
                val dataType = resolveDataTypeForPacket(packet, dataTypes)
                val dataTypeId = dataType.id
                val startOffset = packet.offset
                val endOffset = packet.offset + packet.size
                val tags = tagDao.getTagsForDataTypeAndOffsetRangeSync(dataTypeId, startOffset, endOffset)

                val tagsWithBitTags = tags.map { tag ->
                    val bitTags = bitTagDao.getBitTagsForTagSync(tag.id)
                    TagWithBitTags(tag = tag, bitTags = bitTags)
                }

                PacketWithTags(packet = packet, tagsWithBitTags = tagsWithBitTags)
            }
            NodeWithPacketsAndTags(node = node, packetsWithTags = packetsWithTags)
        }
    }

    fun getAllNodes(): Flow<List<NodeEntity>> = nodeDao.getAllNodes()

    fun getAllDataTypes(): Flow<List<DataTypes>> = dataTypeDao.getAllDataTypes()
    suspend fun getAllDataTypesList(): List<DataTypes> = dataTypeDao.getAllDataTypesList()
    suspend fun getDataTypeById(id: Long): DataTypes? = dataTypeDao.getDataTypeById(id)

    suspend fun getBitTagsForTagSync(tagId: Long): List<BitTags> = bitTagDao.getBitTagsForTagSync(tagId)
    suspend fun insertBitTag(bitTag: BitTags): Long {
        val id = bitTagDao.insertBitTag(bitTag)
        refreshHierarchy()
        return id
    }
    suspend fun insertAllBitTags(bitTags: List<BitTags>) {
        bitTagDao.insertAllBitTags(bitTags)
        refreshHierarchy()
    }
    suspend fun updateBitTag(bitTag: BitTags) {
        bitTagDao.updateBitTag(bitTag)
        refreshHierarchy()
    }
    suspend fun deleteBitTag(bitTag: BitTags) {
        bitTagDao.deleteBitTag(bitTag)
        refreshHierarchy()
    }

    // TagListItems Operations
    fun getListItemsForTag(tagId: Long): Flow<List<TagListItems>> = tagListItemDao.getListItemsForTag(tagId)
    suspend fun getListItemsForTagSync(tagId: Long): List<TagListItems> = tagListItemDao.getListItemsForTagSync(tagId)
    suspend fun insertTagListItem(item: TagListItems): Long {
        val id = tagListItemDao.insertItem(item)
        refreshHierarchy()
        return id
    }
    suspend fun updateTagListItem(item: TagListItems) {
        tagListItemDao.updateItem(item)
        refreshHierarchy()
    }
    suspend fun deleteTagListItem(item: TagListItems) {
        tagListItemDao.deleteItem(item)
        refreshHierarchy()
    }
    suspend fun deleteTagListItemsForTag(tagId: Long) {
        tagListItemDao.deleteItemsForTag(tagId)
        refreshHierarchy()
    }

    suspend fun ensureDefaultDataTypes() {
        val existing = dataTypeDao.getAllDataTypesList()
        if (existing.isEmpty()) {
            dataTypeDao.insertAll(
                listOf(
                    PlcDataTypes(description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true),
                    PlcDataTypes(description = "Data Register Double", shortName = "DD", dataType = "INT", bytes = 4, defaultModbusAddress = 416385L, isZeroBasedAddressing = false, hasBits = true),
                    PlcDataTypes(description = "Data Register Hex", shortName = "DH", dataType = "UINT", bytes = 2, defaultModbusAddress = 424577L, isZeroBasedAddressing = false, hasBits = true),
                    PlcDataTypes(description = "Data Register Float", shortName = "DF", dataType = "FLOAT", bytes = 4, defaultModbusAddress = 428673L, isZeroBasedAddressing = false, hasBits = false)
                )
            )
        } else {
            val duplicates = existing.groupBy { it.shortName.uppercase() }.filter { it.value.size > 1 }
            for ((_, list) in duplicates) {
                list.drop(1).forEach { dup ->
                    dataTypeDao.deleteById(dup.id)
                }
            }
        }
    }

    suspend fun recreateDatabaseWithDefaults() {
        bitTagDao.deleteAllBitTags()
        tagDao.deleteAllTags()
        packetDao.deleteAllPackets()
        nodeDao.deleteAllNodes()
        listScreenItemsDao.deleteAllListScreenItems()
        customGroupDao.deleteAllCustomGroups()
        headerItemDao.deleteAllHeaderItems()
        screensDao.deleteAllScreens()
        ensureDefaultDataTypes()
        refreshHierarchy()
    }

    suspend fun ensureSampleData() {
        ensureDefaultDataTypes()
        val packets = packetDao.getAllPacketsSync()
        packets.forEach { ensureTagsExistForPacket(it) }
    }

    suspend fun insertNode(node: NodeEntity): Long {
        val id = nodeDao.insertNode(node)
        refreshHierarchy()
        return id
    }
    suspend fun updateNode(node: NodeEntity) {
        nodeDao.updateNode(node)
        refreshHierarchy()
    }
    suspend fun deleteNode(node: NodeEntity) {
        nodeDao.deleteNode(node)
        refreshHierarchy()
    }

    suspend fun insertPacket(packet: PacketEntity): Long {
        val id = packetDao.insertPacket(packet)
        ensureTagsExistForPacket(packet.copy(id = id))
        refreshHierarchy()
        return id
    }
    suspend fun updatePacket(packet: PacketEntity) {
        packetDao.updatePacket(packet)
        ensureTagsExistForPacket(packet)
        refreshHierarchy()
    }
    suspend fun deletePacket(packet: PacketEntity) {
        packetDao.deletePacket(packet)
        refreshHierarchy()
    }

    suspend fun getTagByIdSync(id: Long) = tagDao.getTagByIdSync(id)
    suspend fun getTagByDataTypeAndOffsetSync(dataTypeId: Long, offset: Int): TagEntity? = tagDao.getTagByDataTypeAndOffsetSync(dataTypeId, offset)
    suspend fun insertTag(tag: TagEntity): Long {
        val id = tagDao.insertTag(tag)
        refreshHierarchy()
        return id
    }
    suspend fun updateTag(tag: TagEntity) {
        tagDao.updateTag(tag)
        refreshHierarchy()
    }
    suspend fun deleteTag(tag: TagEntity) {
        tagDao.deleteTag(tag)
        refreshHierarchy()
    }
}
