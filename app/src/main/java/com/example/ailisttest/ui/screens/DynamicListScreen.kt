package com.example.ailisttest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.TagListItems
import com.example.ailisttest.data.local.BitTags
import com.example.ailisttest.data.local.CustomGroup
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.data.local.HeaderItem
import com.example.ailisttest.data.local.ListScreenItemWithTag
import com.example.ailisttest.data.local.ListScreenItems
import com.example.ailisttest.data.local.NodeEntity
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.PacketWithTags
import com.example.ailisttest.data.local.ScreenWithListItems
import com.example.ailisttest.data.local.Screens
import com.example.ailisttest.data.local.TagEntity
import com.example.ailisttest.data.modbus.PacketPollingStats
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.parseHexColor
import kotlinx.coroutines.launch

@Composable
fun TagValueChangeDialog(
    itemWithTag: ListScreenItemWithTag,
    handler: LiveDataDisplayType,
    context: LiveDataFieldContext,
    onDismiss: () -> Unit
) {
    val tag = itemWithTag.tag
    val rawDisplayType = context.item.DisplayType
    val isListDisplayType = rawDisplayType.contains("List", ignoreCase = true)

    val listItemsFlow = remember(tag?.id) { tag?.id?.let { context.viewModel.getListItemsForTag(it) } }
    val listItems by listItemsFlow?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val initialVal = if (context.isBitTagItem) (if (context.isBitSet) "1" else "0") else (tag?.storedValue ?: "0")
    var inputValue by remember { mutableStateOf(initialVal) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (context.isTwoTouch) "Two Touch Confirmation" else "Change Tag Value") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Tag: ${tag?.name ?: "Tag #${itemWithTag.item.parentTagId}"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text("Current Value: ${if (context.isBitTagItem) (if (context.isBitSet) "1 (ON)" else "0 (OFF)") else (tag?.storedValue ?: "(empty)")}")

                Spacer(modifier = Modifier.height(4.dp))

                if (context.isBitTagItem) {
                    Text("Select Target Bit State:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { inputValue = "1" },
                            border = BorderStroke(
                                2.dp,
                                if (inputValue == "1") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1 (ON)", fontWeight = if (inputValue == "1") FontWeight.Bold else FontWeight.Normal)
                        }

                        OutlinedButton(
                            onClick = { inputValue = "0" },
                            border = BorderStroke(
                                2.dp,
                                if (inputValue == "0") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("0 (OFF)", fontWeight = if (inputValue == "0") FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                } else if (isListDisplayType && listItems.isNotEmpty()) {
                    Text("Select Target List Option:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        listItems.sortedBy { it.number }.forEach { listItem ->
                            val isSelectedOption = inputValue == listItem.number.toString()
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelectedOption) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSelectedOption) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { inputValue = listItem.number.toString() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelectedOption,
                                        onClick = { inputValue = listItem.number.toString() }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = listItem.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelectedOption) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Value: ${listItem.number}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val dataTypes by context.viewModel.dataTypes.collectAsStateWithLifecycle()
                    val tagDataType = remember(tag?.dataTypeId, dataTypes) {
                        dataTypes.find { it.id == tag?.dataTypeId } ?: dataTypes.firstOrNull()
                    }

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { input ->
                            inputValue = filterTagInput(input, tagDataType)
                        },
                        label = { Text("New Value") },
                        singleLine = true,
                        keyboardOptions = getTagKeyboardOptions(tagDataType),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    handler.onAccept(inputValue, context)
                    onDismiss()
                }
            ) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun DynamicListScreen(
    screenId: Long,
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val screensWithItems by viewModel.listScreensWithItems.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    val statsMap by viewModel.packetPollingStatsMap.collectAsStateWithLifecycle()

    val screenWithItems = screensWithItems.find { it.screen.id == screenId }
    val screenName = screenWithItems?.screen?.Name ?: "List Screen"

    var selectedItem by remember { mutableStateOf<ListScreenItemWithTag?>(null) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
        scope.launch { navigator.navigateBack() }
    }

    val baseDirective = navigator.scaffoldDirective
    val customDirective = remember(baseDirective) {
        baseDirective.copy(defaultPanePreferredWidth = 450.dp)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(innerPadding),
            directive = customDirective,
            value = navigator.scaffoldValue,
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Column {
                        TopAppBar(
                            title = { Text(screenName) },
                            navigationIcon = {
                                IconButton(onClick = onOpenDrawer) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                }
                            }
                        )

                        if (screenWithItems == null || screenWithItems.items.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("This screen is empty. Configure items under 'Configure / Screens / List Screens'.")
                            }
                        } else {
                            DynamicTreeList(
                                screenWithItems = screenWithItems,
                                hierarchy = hierarchy,
                                dataTypes = dataTypes,
                                statsMap = statsMap,
                                viewModel = viewModel,
                                selectedItem = selectedItem,
                                onItemSelected = { item ->
                                    selectedItem = item
                                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                                },
                                onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    selectedItem?.let { itemWithTag ->
                        DynamicItemDetailPane(
                            itemWithTag = itemWithTag,
                            screenName = screenName,
                            hierarchy = hierarchy,
                            statsMap = statsMap,
                            viewModel = viewModel,
                            onBack = { scope.launch { navigator.navigateBack() } }
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a tag item from the list to view live monitoring details")
                    }
                }
            }
        )
    }
}

@Composable
fun DynamicTreeList(
    screenWithItems: ScreenWithListItems,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    statsMap: Map<Long, PacketPollingStats>,
    viewModel: MainViewModel,
    selectedItem: ListScreenItemWithTag?,
    onItemSelected: (ListScreenItemWithTag) -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val packetGroupPacketIds = remember(screenWithItems.items) {
        screenWithItems.items
            .filter { it.item.Type in 1000..9999 && it.item.parentCustomGroupId == null }
            .map { (it.item.Type - 1000).toLong() }
            .toSet()
    }

    val nodeGroupNodeIds = remember(screenWithItems.items) {
        screenWithItems.items
            .filter { it.item.Type in 10000..19999 && it.item.parentCustomGroupId == null }
            .map { (it.item.Type - 10000).toLong() }
            .toSet()
    }

    val tagsInGroupsOnScreen = remember(packetGroupPacketIds, nodeGroupNodeIds, hierarchy) {
        val nodeTagIds = hierarchy
            .filter { nodeGroupNodeIds.contains(it.node.id) }
            .flatMap { it.packetsWithTags }
            .flatMap { it.tagsWithBitTags }
            .map { it.tag.id }

        val packetTagIds = hierarchy
            .flatMap { it.packetsWithTags }
            .filter { packetGroupPacketIds.contains(it.packet.id) }
            .flatMap { it.tagsWithBitTags }
            .map { it.tag.id }

        (nodeTagIds + packetTagIds).toSet()
    }

    val topLevelItems = remember(screenWithItems.items, tagsInGroupsOnScreen) {
        screenWithItems.items
            .filter {
                it.item.parentCustomGroupId == null &&
                !(it.item.Type < 1000 && it.item.parentTagId != null && tagsInGroupsOnScreen.contains(it.item.parentTagId))
            }
            .sortedBy { it.item.ScreenIndex }
    }

    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(topLevelItems, key = { "dynamic_item_${it.item.id}_${it.item.ScreenIndex}" }) { itemWithTag ->
                val item = itemWithTag.item
                val tag = itemWithTag.tag
                val isHeaderItem = item.Type >= 40000
                val isCustomGroup = item.Type in 20000..39999
                val isNodeGroup = item.Type in 10000..19999
                val isPacketGroup = item.Type in 1000..9999

                if (isHeaderItem) {
                    val headerId = (item.Type - 40000).toLong()
                    var headerItem by remember(headerId.toString()) { mutableStateOf<HeaderItem?>(null) }
                    LaunchedEffect(headerId) {
                        headerItem = viewModel.getHeaderByIdSync(headerId)
                    }

                    val title = headerItem?.title ?: "Header Label"
                    val color = com.example.ailisttest.ui.parseHexColor(headerItem?.colorHex ?: "#2196F3")

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FontDownload,
                                contentDescription = "Header Icon",
                                tint = color,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(22.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (isCustomGroup) {
                    val groupId = (item.Type - 20000).toLong()
                    var customGroup by remember(groupId) { mutableStateOf<CustomGroup?>(null) }
                    LaunchedEffect(groupId) {
                        customGroup = viewModel.getCustomGroupByIdSync(groupId)
                    }
                    val groupChildItems = screenWithItems.items
                        .filter { it.item.parentCustomGroupId == customGroup?.id }
                        .sortedBy { it.item.ScreenIndex }

                    DynamicCustomGroupBox(
                        itemWithTag = itemWithTag,
                        childItems = groupChildItems,
                        customGroup = customGroup,
                        hierarchy = hierarchy,
                        dataTypes = dataTypes,
                        statsMap = statsMap,
                        viewModel = viewModel,
                        onSelectChild = onItemSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                } else if (isNodeGroup) {
                    val nodeId = (item.Type - 10000).toLong()
                    val nodeWithPackets = hierarchy.find { n -> n.node.id == nodeId }

                    DynamicNodeGroupBox(
                        nodeWithPackets = nodeWithPackets,
                        dataTypes = dataTypes,
                        statsMap = statsMap,
                        hierarchy = hierarchy,
                        viewModel = viewModel,
                        screenItems = screenWithItems.items,
                        onSelectChild = onItemSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                } else if (isPacketGroup) {
                    val packetId = (item.Type - 1000).toLong()
                    val packetWithTags = hierarchy.flatMap { node -> node.packetsWithTags }
                        .find { p -> p.packet.id == packetId }
                    val packetStats = statsMap[packetId]

                    DynamicPacketGroupBox(
                        packetWithTags = packetWithTags,
                        stats = packetStats,
                        dataTypes = dataTypes,
                        hierarchy = hierarchy,
                        viewModel = viewModel,
                        screenItems = screenWithItems.items,
                        onSelectChild = onItemSelected,
                        onShowSnackbar = onShowSnackbar
                    )
                } else {
                    val isSelected = selectedItem?.item?.id == item.id

                    DynamicTagWithBitExpandableRow(
                        tag = tag,
                        item = item,
                        itemWithTag = itemWithTag,
                        hierarchy = hierarchy,
                        dataTypes = dataTypes,
                        statsMap = statsMap,
                        viewModel = viewModel,
                        screenItems = screenWithItems.items,
                        isSelected = isSelected,
                        onSelect = { onItemSelected(itemWithTag) },
                        onShowSnackbar = onShowSnackbar
                    )
                }
            }
        }

        ScrollMoreDownIndicator(
            canScrollMore = canScrollDown,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun DynamicTagWithBitExpandableRow(
    tag: TagEntity?,
    item: ListScreenItems,
    itemWithTag: ListScreenItemWithTag,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    statsMap: Map<Long, PacketPollingStats>,
    viewModel: MainViewModel,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    isSelected: Boolean,
    onSelect: (ListScreenItemWithTag) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val isBitTagItem = item.Type > 0 && item.Type < 1000
    val bitIndex = if (isBitTagItem) (item.Type - 1) else null
    val parentTagVal = tag?.storedValue?.toLongOrNull() ?: 0L
    val isBitSet = if (isBitTagItem && bitIndex != null) ((parentTagVal and (1L shl bitIndex)) != 0L) else false

    val parentPacket = hierarchy.flatMap { node -> node.packetsWithTags }
        .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } }

    val bitTagObj = if (isBitTagItem && tag != null && bitIndex != null) {
        val tagWithBits = parentPacket?.tagsWithBitTags?.find { it.tag.id == tag.id }
        tagWithBits?.bitTags?.find { it.bitIndex == bitIndex }
    } else null

    val displayTitle = if (isBitTagItem) {
        bitTagObj?.name ?: if (tag != null) "${tag.name}-$bitIndex" else "Bit #$bitIndex"
    } else {
        tag?.name ?: "Tag #${item.parentTagId}"
    }
    val packetDataType = parentPacket?.packet?.type?.let { typeId -> dataTypes.find { dt -> dt.id == typeId.toLong() } }
        ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
        ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)

    val tagShortName = packetDataType.shortName.ifEmpty { packetDataType.description }
    val badgeColor = if (isBitTagItem) Color(0xFF00BCD4) else (getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784))

    val packetStats = parentPacket?.packet?.id?.let { statsMap[it] }

    val calcOffset = tag?.offset ?: (parentPacket?.packet?.offset ?: 1)
    val combinedBadgeText = if (isBitTagItem) "$tagShortName$calcOffset:$bitIndex" else "$tagShortName$calcOffset"

    val rawDisplayType = item.DisplayType.ifEmpty {
        DisplayTypes.getOptionsForDataType(
            if (isBitTagItem) "B" else packetDataType.shortName.ifEmpty { packetDataType.description }
        ).first()
    }

    val dataTypeShortName = if (isBitTagItem) "B" else packetDataType.shortName.ifEmpty { packetDataType.description }
    val handler = DisplayTypes.getHandler(dataTypeShortName, rawDisplayType)

    var showPopupDialog by remember { mutableStateOf(false) }

    val fieldContext = LiveDataFieldContext(
        item = item,
        tag = tag,
        isBitTagItem = isBitTagItem,
        bitIndex = bitIndex,
        isBitSet = isBitSet,
        isReadOnly = item.isReadOnly,
        isTwoTouch = item.isTwoTouch,
        viewModel = viewModel,
        onShowPopup = { showPopupDialog = true },
        onShowSnackbar = onShowSnackbar
    )

    val typeHasBits = packetDataType.hasBits && !isBitTagItem && tag != null
    val numBits = packetDataType.bytes * 8
    val showBitGroup = typeHasBits && item.isShowBits && tag != null
    val liveExpandedTagGroups by viewModel.liveExpandedTagGroupsMap.collectAsStateWithLifecycle()
    val isTagGroupExpanded = liveExpandedTagGroups[item.id] ?: false

    Column {
        if (showBitGroup && tag != null) {
            var bitTagsList by remember(tag.id) { mutableStateOf<List<BitTags>>(emptyList()) }
            LaunchedEffect(tag.id) {
                val loaded = viewModel.getBitTagsForTagSync(tag.id)
                if (loaded.isNotEmpty()) {
                    bitTagsList = loaded
                } else {
                    bitTagsList = (0 until numBits).map { bitIdx ->
                        BitTags(
                            id = (tag.id * 100 + bitIdx),
                            name = "${tag.name}-$bitIdx",
                            parentTagId = tag.id,
                            bitIndex = bitIdx
                        )
                    }
                }
            }

            val parentTagNum = tag.storedValue.toLongOrNull() ?: 0L

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    val isListDisplayType = rawDisplayType.contains("List", ignoreCase = true)
                    val tagListItemsFlow = remember(tag.id) { viewModel.getListItemsForTag(tag.id) }
                    val tagListItems by tagListItemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

                    val currentTag = hierarchy.flatMap { it.packetsWithTags }.flatMap { it.tagsWithBitTags }.find { it.tag.id == tag.id }?.tag ?: tag
                    val liveStoredVal = currentTag.storedValue
                    val formattedStoredVal = remember(liveStoredVal, isListDisplayType, tagListItems) {
                        if (isListDisplayType) {
                            val numVal = liveStoredVal.toIntOrNull()
                            val match = if (numVal != null) tagListItems.find { it.number == numVal } else null
                            match?.label ?: "Undefined"
                        } else {
                            liveStoredVal
                        }
                    }

                    DynamicItemRow(
                        title = displayTitle,
                        badgeText = combinedBadgeText,
                        badgeColor = badgeColor,
                        storedValue = formattedStoredVal,
                        displayType = rawDisplayType,
                        isReadOnly = item.isReadOnly,
                        isTwoTouch = item.isTwoTouch,
                        isBitTagItem = isBitTagItem,
                        isBitSet = isBitSet,
                        tagListItems = tagListItems,
                        onBitAction = { targetState, toggle ->
                            viewModel.updateBitValueAndWrite(
                                item = item,
                                targetState = targetState,
                                toggle = toggle,
                                onShowSnackbar = onShowSnackbar
                            )
                        },
                        onTagValueSelected = { newVal ->
                            viewModel.updateTagValueAndWrite(tag, newVal, onShowSnackbar)
                        },
                        onLiveDataClicked = {
                            handler.onItemClicked(fieldContext)
                        },
                        stats = packetStats,
                        isSelected = isSelected,
                        hasChildren = true,
                        isExpanded = isTagGroupExpanded,
                        onToggleExpand = { viewModel.toggleLiveTagGroupExpanded(item.id) },
                        onSelect = { onSelect(itemWithTag) }
                    )

                    if (isTagGroupExpanded && tag != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp, end = 8.dp)
                        ) {
                            bitTagsList.forEach { bitTag ->
                                val bitIdx = bitTag.bitIndex
                                val isBitSetVal = (parentTagNum and (1L shl bitIdx)) != 0L

                                val matchingBitItem = screenItems.find { it.item.parentTagId == tag.id && it.item.Type == (bitIdx + 1) }
                                val actualBitItem = matchingBitItem?.item ?: ListScreenItems(
                                    parentScreenId = item.parentScreenId,
                                    parentTagId = tag.id,
                                    Type = (bitIdx + 1),
                                    ScreenIndex = item.ScreenIndex
                                )
                                val actualBitItemWithTag = matchingBitItem ?: ListScreenItemWithTag(item = actualBitItem, tag = tag)

                                val bitRawDisplayType = actualBitItem.DisplayType.ifEmpty { "Default (Numeric entry)" }
                                val bitEffectiveReadOnly = item.isReadOnly || actualBitItem.isReadOnly

                                val bitFieldContext = LiveDataFieldContext(
                                    item = actualBitItem,
                                    tag = tag,
                                    isBitTagItem = true,
                                    bitIndex = bitIdx,
                                    isBitSet = isBitSetVal,
                                    isReadOnly = bitEffectiveReadOnly,
                                    isTwoTouch = actualBitItem.isTwoTouch,
                                    viewModel = viewModel,
                                    onShowPopup = {},
                                    onShowSnackbar = onShowSnackbar
                                )

                                val bitHandler = DisplayTypes.getHandler("B", bitRawDisplayType)

                                DynamicItemRow(
                                    title = bitTag.name,
                                    badgeText = "$tagShortName$calcOffset:$bitIdx",
                                    badgeColor = Color(0xFF00BCD4),
                                    storedValue = if (isBitSetVal) "1" else "0",
                                    displayType = bitRawDisplayType,
                                    isReadOnly = bitEffectiveReadOnly,
                                    isTwoTouch = actualBitItem.isTwoTouch,
                                    isBitTagItem = true,
                                    isBitSet = isBitSetVal,
                                    onBitAction = { targetState, toggle ->
                                        viewModel.updateBitValueAndWrite(
                                            item = actualBitItem,
                                            targetState = targetState,
                                            toggle = toggle,
                                            onShowSnackbar = onShowSnackbar
                                        )
                                    },
                                    onLiveDataClicked = {
                                        bitHandler.onItemClicked(bitFieldContext)
                                    },
                                    stats = packetStats,
                                    isSelected = false,
                                    hasChildren = false,
                                    isExpanded = false,
                                    onToggleExpand = {},
                                    onSelect = { onSelect(actualBitItemWithTag) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val isListDisplayType = rawDisplayType.contains("List", ignoreCase = true)
            val tagListItemsFlow = remember(tag?.id) { tag?.id?.let { viewModel.getListItemsForTag(it) } }
            val tagListItems by tagListItemsFlow?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }

            val currentTag = if (tag != null) hierarchy.flatMap { it.packetsWithTags }.flatMap { it.tagsWithBitTags }.find { it.tag.id == tag.id }?.tag ?: tag else null
            val liveStoredVal = currentTag?.storedValue ?: "0"
            val formattedStoredVal = remember(liveStoredVal, isListDisplayType, tagListItems) {
                if (isListDisplayType && tag != null) {
                    val numVal = liveStoredVal.toIntOrNull()
                    val match = if (numVal != null) tagListItems.find { it.number == numVal } else null
                    match?.label ?: "Undefined"
                } else {
                    liveStoredVal
                }
            }

            DynamicItemRow(
                title = displayTitle,
                badgeText = combinedBadgeText,
                badgeColor = badgeColor,
                storedValue = formattedStoredVal,
                displayType = rawDisplayType,
                isReadOnly = item.isReadOnly,
                isTwoTouch = item.isTwoTouch,
                isBitTagItem = isBitTagItem,
                isBitSet = isBitSet,
                tagListItems = tagListItems,
                onBitAction = { targetState, toggle ->
                    viewModel.updateBitValueAndWrite(
                        item = item,
                        targetState = targetState,
                        toggle = toggle,
                        onShowSnackbar = onShowSnackbar
                    )
                },
                onTagValueSelected = { newVal ->
                    if (tag != null) {
                        viewModel.updateTagValueAndWrite(tag, newVal, onShowSnackbar)
                    }
                },
                onLiveDataClicked = {
                    handler.onItemClicked(fieldContext)
                },
                stats = packetStats,
                isSelected = isSelected,
                hasChildren = false,
                isExpanded = false,
                onToggleExpand = {},
                onSelect = { onSelect(itemWithTag) }
            )
        }

        if (showPopupDialog) {
            TagValueChangeDialog(
                itemWithTag = itemWithTag,
                handler = handler,
                context = fieldContext,
                onDismiss = { showPopupDialog = false }
            )
        }
    }
}

@Composable
fun DynamicItemRow(
    title: String,
    badgeText: String?,
    badgeColor: Color?,
    offsetText: String? = null,
    storedValue: String,
    displayType: String? = null,
    isReadOnly: Boolean = true,
    isTwoTouch: Boolean = true,
    isBitTagItem: Boolean = false,
    isBitSet: Boolean = false,
    tagListItems: List<TagListItems> = emptyList(),
    onBitAction: (targetState: Boolean?, toggle: Boolean) -> Unit = { _, _ -> },
    onTagValueSelected: (String) -> Unit = {},
    onLiveDataClicked: () -> Unit = {},
    stats: PacketPollingStats?,
    isSelected: Boolean,
    hasChildren: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onSelect: () -> Unit
) {
    val bg = getTagAlarmBackgroundColor(stats, isSelected)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (hasChildren) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Icon(
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(20.dp)
            )

            if (!badgeText.isNullOrEmpty() && badgeColor != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Live Data Field
            if (isBitTagItem) {
                if (isReadOnly) {
                    // Read Only Bit Badge (Greyed out with lock icon)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable(onClick = onLiveDataClicked)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Read Only",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isBitSet) "1" else "0",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isBitSet) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Direct Interaction on Main Screen for BitType Controls
                    when (displayType) {
                        "On_Button" -> {
                            Button(
                                onClick = { onBitAction(true, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        "Off_Button" -> {
                            Button(
                                onClick = { onBitAction(false, false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFFE53935)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        "Toggle_Button" -> {
                            Button(
                                onClick = { onBitAction(null, true) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        "Switch" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBitSet) Color(0xFF4CAF50) else Color.Gray
                                )
                                Switch(
                                    checked = isBitSet,
                                    onCheckedChange = { checked -> onBitAction(checked, false) }
                                )
                            }
                        }
                        "CheckBox" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isBitSet,
                                    onCheckedChange = { onBitAction(null, true) }
                                )
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBitSet) Color(0xFF4CAF50) else Color.Gray
                                )
                            }
                        }
                        "Radio_Button" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isBitSet,
                                    onClick = { onBitAction(null, true) }
                                )
                                Text(
                                    text = if (isBitSet) "ON" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBitSet) Color(0xFF4CAF50) else Color.Gray
                                )
                            }
                        }
                        else -> {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBitSet) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable(onClick = { onBitAction(null, true) })
                            ) {
                                Text(
                                    text = if (isBitSet) "1" else "0",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBitSet) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Full Tag
                val isListType = displayType?.contains("List", ignoreCase = true) == true

                if (isListType && tagListItems.isNotEmpty() && !isReadOnly) {
                    var dropdownExpanded by remember { mutableStateOf(false) }

                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.clickable { dropdownExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = "Select List Option",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = storedValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            tagListItems.sortedBy { it.number }.forEach { listItem ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = listItem.label,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "(${listItem.number})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    },
                                    onClick = {
                                        dropdownExpanded = false
                                        onTagValueSelected(listItem.number.toString())
                                    }
                                )
                            }
                        }
                    }
                } else if (isReadOnly) {
                    // Read-Only Full Tag (Greyed out with lock icon)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable(onClick = onLiveDataClicked)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Read Only",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = storedValue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Editable Full Tag (Vibrant, interactive look with edit icon)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.clickable(onClick = onLiveDataClicked)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Editable",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = storedValue,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getTagAlarmBackgroundColor(stats: PacketPollingStats?, isSelected: Boolean): Color {
    if (isSelected) return Color(0xFFE1BEE7) // Highlight selection

    if (stats == null || stats.totalPackets == 0L) {
        return Color.Transparent
    }

    if (stats.commStatus) {
        return Color.Transparent
    }

    return when (stats.consecutiveFailures) {
        1 -> Color(0xFFFFF3E0)      // 1 error: Soft Warning Orange
        2 -> Color(0xFFFFE0B2)      // 2 errors: Warning Orange
        3 -> Color(0xFFFFCDD2)      // 3 errors: Light Red Alarm
        else -> Color(0xFFEF9A9A)   // 4+ errors: Deep Red Alarm
    }
}

@Composable
fun DynamicCustomGroupBox(
    itemWithTag: ListScreenItemWithTag,
    childItems: List<ListScreenItemWithTag>,
    customGroup: CustomGroup?,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    statsMap: Map<Long, PacketPollingStats>,
    viewModel: MainViewModel,
    onSelectChild: (ListScreenItemWithTag) -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val liveExpandedCustomGroups by viewModel.liveExpandedCustomGroupsMap.collectAsStateWithLifecycle()
    val groupId = customGroup?.id ?: 0L
    val isGroupExpanded = liveExpandedCustomGroups[groupId] ?: false

    val groupName = customGroup?.groupName ?: "Custom Group"
    val groupBgColor = parseHexColor(customGroup?.colorHex ?: "#F5F5F5")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = groupBgColor,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleLiveCustomGroupExpanded(groupId) }
            ) {
                IconButton(
                    onClick = { viewModel.toggleLiveCustomGroupExpanded(groupId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isGroupExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.FolderSpecial,
                    contentDescription = "Custom Group",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )

                Text(
                    text = groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${childItems.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isGroupExpanded && childItems.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    childItems.forEach { childWithTag ->
                        val childItem = childWithTag.item
                        val childTag = childWithTag.tag

                        DynamicTagWithBitExpandableRow(
                            tag = childTag,
                            item = childItem,
                            itemWithTag = childWithTag,
                            hierarchy = hierarchy,
                            dataTypes = dataTypes,
                            statsMap = statsMap,
                            viewModel = viewModel,
                            screenItems = childItems,
                            isSelected = false,
                            onSelect = { onSelectChild(childWithTag) },
                            onShowSnackbar = onShowSnackbar
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicNodeGroupBox(
    nodeWithPackets: NodeWithPacketsAndTags?,
    dataTypes: List<DataTypes>,
    statsMap: Map<Long, PacketPollingStats>,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    viewModel: MainViewModel? = null,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val node = nodeWithPackets?.node
    val nodeId = node?.id ?: 0L
    val liveExpandedNodeGroups = viewModel?.liveExpandedNodeGroupsMap?.collectAsStateWithLifecycle()?.value ?: emptyMap()
    val isNodeExpanded = liveExpandedNodeGroups[nodeId] ?: false

    val nodeName = node?.name ?: "Node Group"

    // Alarm bell status
    val hasCommFailure = nodeWithPackets?.packetsWithTags?.any { p ->
        val stats = statsMap[p.packet.id]
        stats != null && !stats.commStatus && stats.totalPackets > 0
    } == true
    val bellColor = if (hasCommFailure) Color(0xFFF44336) else Color(0xFF4CAF50)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel?.toggleLiveNodeGroupExpanded(nodeId) }
            ) {
                IconButton(
                    onClick = { viewModel?.toggleLiveNodeGroupExpanded(nodeId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isNodeExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "Alarm Indicator",
                    tint = bellColor,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(20.dp)
                )

                Icon(
                    imageVector = Icons.Rounded.NetworkCheck,
                    contentDescription = "Node Group",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(20.dp)
                )

                Text(
                    text = nodeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (isNodeExpanded && nodeWithPackets != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                        val packetStats = statsMap[packetWithTags.packet.id]
                        DynamicPacketGroupBox(
                            packetWithTags = packetWithTags,
                            stats = packetStats,
                            dataTypes = dataTypes,
                            hierarchy = hierarchy,
                            viewModel = viewModel,
                            screenItems = screenItems,
                            onSelectChild = onSelectChild,
                            onShowSnackbar = onShowSnackbar
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicPacketGroupBox(
    packetWithTags: PacketWithTags?,
    stats: PacketPollingStats?,
    dataTypes: List<DataTypes>,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    viewModel: MainViewModel? = null,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    val packet = packetWithTags?.packet
    val packetId = packet?.id ?: 0L
    val liveExpandedPacketGroups = viewModel?.liveExpandedPacketGroupsMap?.collectAsStateWithLifecycle()?.value ?: emptyMap()
    val isGroupExpanded = liveExpandedPacketGroups[packetId] ?: false

    val packetName = packet?.name ?: "Packet Group"
    val packetDataType = packet?.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
        ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
        ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
    val badgeText = packetDataType.shortName.ifEmpty { packetDataType.description }
    val badgeColor = getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel?.toggleLivePacketGroupExpanded(packetId) }
            ) {
                IconButton(
                    onClick = { viewModel?.toggleLivePacketGroupExpanded(packetId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isGroupExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.AccountTree,
                    contentDescription = "Packet Group",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = packetName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Comm status badge
                if (stats != null) {
                    val statusText = if (stats.commStatus) "COMM OK" else "FAIL"
                    val statusBg = if (stats.commStatus) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val statusFg = if (stats.commStatus) Color(0xFF2E7D32) else Color(0xFFC62828)

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (isGroupExpanded && packetWithTags != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    packetWithTags.tagsWithBitTags.forEach { tagWithBit ->
                        val tag = tagWithBit.tag
                        val matchingItemWithTag = screenItems.find { it.item.parentTagId == tag.id && it.item.Type == 0 }
                        val actualItem = matchingItemWithTag?.item ?: ListScreenItems(parentScreenId = 0, parentTagId = tag.id, Type = 0, ScreenIndex = 0)
                        val actualItemWithTag = matchingItemWithTag ?: ListScreenItemWithTag(item = actualItem, tag = tag)

                        if (viewModel != null) {
                            DynamicTagWithBitExpandableRow(
                                tag = tag,
                                item = actualItem,
                                itemWithTag = actualItemWithTag,
                                hierarchy = hierarchy.ifEmpty { listOf(NodeWithPacketsAndTags(NodeEntity(0, "Node", "127.0.0.1"), listOf(packetWithTags))) },
                                dataTypes = dataTypes,
                                statsMap = if (stats != null) mapOf(packetWithTags.packet.id to stats) else emptyMap(),
                                viewModel = viewModel,
                                screenItems = screenItems,
                                isSelected = false,
                                onSelect = { onSelectChild(actualItemWithTag) },
                                onShowSnackbar = onShowSnackbar
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = tagWithBit.tag.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = tagWithBit.tag.storedValue,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DynamicItemDetailPane(
    itemWithTag: ListScreenItemWithTag,
    screenName: String,
    hierarchy: List<NodeWithPacketsAndTags>,
    statsMap: Map<Long, PacketPollingStats>,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val listItem = itemWithTag.item
    val tag = itemWithTag.tag
    val isBitTagItem = listItem.Type > 0 && listItem.Type < 1000
    val bitIndex = if (isBitTagItem) (listItem.Type - 1) else null
    val parentNodeWithPackets = hierarchy.find { node -> node.packetsWithTags.any { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } } }
    val parentPacket = parentNodeWithPackets?.packetsWithTags?.find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } }
    val stats = parentPacket?.packet?.id?.let { statsMap[it] }

    val bitTagObj = if (isBitTagItem && tag != null && bitIndex != null) {
        val tagWithBits = parentPacket?.tagsWithBitTags?.find { it.tag.id == tag.id }
        tagWithBits?.bitTags?.find { it.bitIndex == bitIndex }
    } else null

    val displayTitle = if (isBitTagItem) {
        bitTagObj?.name ?: if (tag != null) "${tag.name}-$bitIndex" else "Bit #$bitIndex"
    } else {
        tag?.name ?: "Tag #${listItem.parentTagId}"
    }

    val scrollState = rememberScrollState()
    val canScrollMore by remember { derivedStateOf { scrollState.canScrollForward } }

    val isListDisplayType = listItem.DisplayType.contains("List", ignoreCase = true)
    val listItemsFlow = remember(tag?.id) { tag?.id?.let { viewModel.getListItemsForTag(it) } }
    val listItems by listItemsFlow?.collectAsStateWithLifecycle(initialValue = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val currentTag = if (tag != null) hierarchy.flatMap { it.packetsWithTags }.flatMap { it.tagsWithBitTags }.find { it.tag.id == tag.id }?.tag ?: tag else null
    val liveStoredVal = currentTag?.storedValue ?: "(empty)"

    val formattedLiveValue = remember(liveStoredVal, isListDisplayType, listItems) {
        if (isListDisplayType && tag != null) {
            val numVal = liveStoredVal.toIntOrNull()
            val match = if (numVal != null) listItems.find { it.number == numVal } else null
            match?.label ?: "Undefined"
        } else {
            liveStoredVal
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Live Tag Details",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Screen Name: $screenName", fontWeight = FontWeight.SemiBold)
                Text("Tag / Item Name: $displayTitle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Node: ${parentNodeWithPackets?.node?.name ?: "-"} (${parentNodeWithPackets?.node?.ipAddress ?: "-"})")
                Text("Packet: ${parentPacket?.packet?.name ?: "-"}")
                Text("Display Type: ${listItem.DisplayType.ifEmpty { "Default (Numeric entry)" }}", fontWeight = FontWeight.SemiBold)
                Text("Description: ${tag?.description ?: "-"}")

                HorizontalDivider()

                Text("Live Data & Comm Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Live Value:", fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = formattedLiveValue,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Comm Status:", fontWeight = FontWeight.SemiBold)
                    val (statusText, statusBg, statusFg) = when {
                        stats == null || stats.totalPackets == 0L -> Triple("WAITING", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                        stats.commStatus -> Triple("COMM OK", Color(0xFF4CAF50), Color.White)
                        else -> Triple("COMM FAIL", Color(0xFFF44336), Color.White)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (stats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("% Success Rate:", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${stats.successRatePercentage}%",
                            fontWeight = FontWeight.Bold,
                            color = if (stats.successRatePercentage >= 90) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }
        }

        ScrollMoreDownIndicator(
            canScrollMore = canScrollMore,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SwitchButtonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Switch Control Preview (Bit Display Type)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Switch (OFF):", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("OFF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(checked = false, onCheckedChange = {})
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Switch (ON):", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("ON", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(checked = true, onCheckedChange = {})
                }
            }
        }
    }
}
