package com.example.ailisttest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import com.example.ailisttest.data.local.*
import com.example.ailisttest.ui.GroupColorPickerDialog
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.parseHexColor
import com.example.ailisttest.ui.TagNamePickerDialog
import kotlinx.coroutines.launch

sealed class SelectedListItem {
    data class Screen(val screen: Screens) : SelectedListItem()
    data class Item(val itemWithTag: ListScreenItemWithTag, val parentScreen: Screens, val isPacketSubItem: Boolean = false) : SelectedListItem()
    data class CustomGroupItem(val group: CustomGroup, val itemWithTag: ListScreenItemWithTag, val parentScreen: Screens) : SelectedListItem()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagListConfigDialog(
    tag: TagEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit
) {
    val listItemsFlow = remember(tag.id) { viewModel.getListItemsForTag(tag.id) }
    val listItems by listItemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()

    val tagDataType = remember(tag.dataTypeId, dataTypes) {
        dataTypes.find { it.id == tag.dataTypeId } ?: dataTypes.firstOrNull()
    }
    val typeName = tagDataType?.shortName?.ifEmpty { tagDataType.description } ?: "DS"

    var newLabelText by remember { mutableStateOf("") }
    var dialogErrorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FormatListNumbered,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Config List — ${tag.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Error Alert Banner at Top of Dialog Box (Fixed)
                AnimatedVisibility(
                    visible = dialogErrorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    dialogErrorMessage?.let { errorText ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Error,
                                        contentDescription = "Error",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = errorText,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                IconButton(
                                    onClick = { dialogErrorMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss Error",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Define text labels for numeric values. Items are displayed in ascending order by value.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input Row for New Item (Compact)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val nextNum = if (listItems.isEmpty()) 0 else (listItems.maxOf { it.number } + 1)
                    var newNumText by remember(listItems.size) { mutableStateOf(nextNum.toString()) }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .width(75.dp)
                            .height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            BasicTextField(
                                value = newNumText,
                                onValueChange = { input ->
                                    newNumText = input.filter { it.isDigit() || it == '-' }
                                    dialogErrorMessage = null
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            BasicTextField(
                                value = newLabelText,
                                onValueChange = {
                                    newLabelText = it
                                    dialogErrorMessage = null
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (newLabelText.isEmpty()) {
                                Text(
                                    text = "New Text Label...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            val parsedNum = newNumText.toLongOrNull()
                            if (parsedNum == null) {
                                dialogErrorMessage = "Please enter a valid integer for Value."
                            } else if (!isTagValueInRange(parsedNum, tagDataType)) {
                                dialogErrorMessage = "Value $parsedNum is out of valid range (${getTagValueRangeDesc(tagDataType)}) for $typeName."
                            } else {
                                viewModel.addTagListItem(tag.id, parsedNum.toInt(), newLabelText) { success, msg ->
                                    if (success) {
                                        dialogErrorMessage = null
                                        newLabelText = ""
                                        newNumText = (parsedNum + 1).toString()
                                    } else {
                                        dialogErrorMessage = msg
                                    }
                                }
                            }
                        },
                        enabled = newLabelText.trim().isNotEmpty() && newNumText.toLongOrNull() != null,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AddCircle,
                            contentDescription = "Add Item",
                            tint = if (newLabelText.trim().isNotEmpty() && newNumText.toLongOrNull() != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                HorizontalDivider()

                // Table Column Headers (Compact)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Value",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(75.dp)
                    )
                    Text(
                        text = "Label",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                }

                // SCROLLABLE VALUE / LABEL PAIRS SECTION
                if (listItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No list items defined yet.\nEnter a Value and Label above and tap '+' to add.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val listScrollState = rememberScrollState()
                    val canScrollMore by remember { derivedStateOf { listScrollState.canScrollForward } }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(listScrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listItems.sortedBy { it.number }.forEach { item ->
                                TagListItemRow(
                                    item = item,
                                    tagDataType = tagDataType,
                                    onUpdate = { numVal, labelVal ->
                                        viewModel.updateTagListItem(item, numVal, labelVal) { success, msg ->
                                            if (success) {
                                                dialogErrorMessage = null
                                            } else {
                                                dialogErrorMessage = msg
                                            }
                                        }
                                    },
                                    onShowError = { err -> dialogErrorMessage = err },
                                    onDelete = {
                                        viewModel.deleteTagListItem(item)
                                        dialogErrorMessage = null
                                    }
                                )
                            }
                        }

                        ScrollMoreDownIndicator(
                            canScrollMore = canScrollMore,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun TagListItemRow(
    item: TagListItems,
    tagDataType: DataTypes?,
    onUpdate: (numVal: Int, labelVal: String) -> Unit,
    onShowError: (String) -> Unit,
    onDelete: () -> Unit
) {
    var numStr by remember(item.id, item.number) { mutableStateOf(item.number.toString()) }
    var labelStr by remember(item.id, item.label) { mutableStateOf(item.label) }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .width(75.dp)
                    .height(34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = numStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '-' }
                            numStr = filtered
                            val parsed = filtered.toLongOrNull()
                            if (parsed == null) {
                                onShowError("Please enter a valid integer for Value.")
                            } else if (!isTagValueInRange(parsed, tagDataType)) {
                                onShowError("Value $parsed is out of valid range (${getTagValueRangeDesc(tagDataType)}).")
                            } else {
                                if (labelStr.trim().isNotEmpty() && (parsed.toInt() != item.number || labelStr.trim() != item.label)) {
                                    onUpdate(parsed.toInt(), labelStr)
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    BasicTextField(
                        value = labelStr,
                        onValueChange = { input ->
                            labelStr = input
                            val parsed = numStr.toLongOrNull()
                            if (parsed != null && isTagValueInRange(parsed, tagDataType) && input.trim().isNotEmpty() && (parsed.toInt() != item.number || input.trim() != item.label)) {
                                onUpdate(parsed.toInt(), input)
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ListScreensConfigScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val screensWithItems by viewModel.listScreensWithItems.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    var selectedItem by remember { mutableStateOf<SelectedListItem?>(null) }

    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showTagPicker by remember { mutableStateOf(false) }
    var pickerTargetScreenId by remember { mutableStateOf<Long?>(null) }
    var pickerTargetGroupId by remember { mutableStateOf<Long?>(null) }
    var pickerTargetIndex by remember { mutableStateOf<Int?>(null) }

    var showHeaderDialog by remember { mutableStateOf(false) }
    var headerTargetScreenId by remember { mutableStateOf<Long?>(null) }

    var itemToDelete by remember { mutableStateOf<SelectedListItem?>(null) }

    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
        scope.launch { navigator.navigateBack() }
    }

    val baseDirective = navigator.scaffoldDirective
    val customDirective = remember(baseDirective) {
        baseDirective.copy(defaultPanePreferredWidth = 450.dp)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(paddingValues),
            directive = customDirective,
            value = navigator.scaffoldValue,
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Column {
                        TopAppBar(
                            title = { Text("Configure List Screens") },
                            navigationIcon = {
                                IconButton(onClick = onOpenDrawer) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                IconButton(onClick = { viewModel.addListScreen(onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }) }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add List Screen")
                                }
                            }
                        )

                        ListScreensTreeList(
                            screensWithItems = screensWithItems,
                            hierarchy = hierarchy,
                            dataTypes = dataTypes,
                            viewModel = viewModel,
                            selectedItem = selectedItem,
                            onItemSelected = { item ->
                                selectedItem = item
                                scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                            },
                            onAddTagToScreen = { screen ->
                                pickerTargetScreenId = screen.id
                                pickerTargetGroupId = null
                                pickerTargetIndex = null
                                showTagPicker = true
                            },
                            onAddHeaderToScreen = { screen ->
                                headerTargetScreenId = screen.id
                                showHeaderDialog = true
                            },
                            onAddTagToCustomGroup = { screen, customGroup ->
                                pickerTargetScreenId = screen.id
                                pickerTargetGroupId = customGroup.id
                                pickerTargetIndex = null
                                showTagPicker = true
                            },
                            onInsertTagAtItem = { screen, itemWithTag ->
                                pickerTargetScreenId = screen.id
                                pickerTargetGroupId = itemWithTag.item.parentCustomGroupId
                                pickerTargetIndex = itemWithTag.item.ScreenIndex
                                showTagPicker = true
                            },
                            onDeleteScreen = { screen -> itemToDelete = SelectedListItem.Screen(screen) },
                            onDeleteItem = { screen, itemWithTag -> itemToDelete = SelectedListItem.Item(itemWithTag, screen) },
                            onDeleteCustomGroup = { group -> itemToDelete = SelectedListItem.CustomGroupItem(group, ListScreenItemWithTag(ListScreenItems(), null), Screens()) }
                        )
                    }
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    selectedItem?.let { item ->
                        ScreenDetailPane(
                            item = item,
                            screensWithItems = screensWithItems,
                            hierarchy = hierarchy,
                            dataTypes = dataTypes,
                            viewModel = viewModel,
                            onUpdateScreen = { updated ->
                                viewModel.updateScreen(updated)
                                selectedItem = SelectedListItem.Screen(updated)
                                scope.launch { snackbarHostState.showSnackbar("Screen saved successfully.") }
                            },
                            onUpdateGroup = { updatedGroup ->
                                viewModel.updateCustomGroup(updatedGroup)
                                if (item is SelectedListItem.CustomGroupItem) {
                                    selectedItem = item.copy(group = updatedGroup)
                                }
                                scope.launch { snackbarHostState.showSnackbar("Custom group saved successfully.") }
                            },
                            onAddTagToScreen = { screenId ->
                                pickerTargetScreenId = screenId
                                pickerTargetGroupId = null
                                pickerTargetIndex = null
                                showTagPicker = true
                            },
                            onInsertAbove = { screenId, groupId, targetIdx ->
                                pickerTargetScreenId = screenId
                                pickerTargetGroupId = groupId
                                pickerTargetIndex = targetIdx
                                showTagPicker = true
                            },
                            onInsertBelow = { screenId, groupId, targetIdx ->
                                pickerTargetScreenId = screenId
                                pickerTargetGroupId = groupId
                                pickerTargetIndex = targetIdx
                                showTagPicker = true
                            },
                            onDeleteItemWithSelection = { nextItem ->
                                selectedItem = nextItem
                            },
                            onBack = { scope.launch { navigator.navigateBack() } },
                            onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a screen or item to edit")
                    }
                }
            }
        )
    }

    // Tag Name Picker Dialog for adding or inserting tags into a screen or group
    if (showTagPicker && pickerTargetScreenId != null) {
        val baseIndex = pickerTargetIndex
        val targetGroupId = pickerTargetGroupId
        TagNamePickerDialog(
            hierarchy = hierarchy,
            dataTypes = dataTypes,
            onTagsSelected = { selectedNames, createNewGroup ->
                val screenId = pickerTargetScreenId ?: return@TagNamePickerDialog
                if (createNewGroup) {
                    viewModel.addTagsInNewGroup(
                        screenId = screenId,
                        selectedPaths = selectedNames,
                        targetIndex = baseIndex,
                        groupName = "Group (${selectedNames.size} tags)"
                    )
                    scope.launch { snackbarHostState.showSnackbar("Created new group with ${selectedNames.size} tag(s).") }
                } else {
                    viewModel.addOrInsertListScreenItems(
                        screenId = screenId,
                        selectedPaths = selectedNames,
                        targetIndex = baseIndex,
                        parentGroupId = targetGroupId
                    )
                    scope.launch { snackbarHostState.showSnackbar("Added ${selectedNames.size} item(s) to list screen.") }
                }
                showTagPicker = false
                pickerTargetGroupId = null
            },
            onDismiss = {
                showTagPicker = false
                pickerTargetGroupId = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        val title = when (item) {
            is SelectedListItem.Screen -> "Delete List Screen"
            is SelectedListItem.CustomGroupItem -> "Delete Custom Group"
            is SelectedListItem.Item -> {
                val listItem = item.itemWithTag.item
                when {
                    listItem.Type >= 10000 -> "Delete Node Group"
                    listItem.Type in 1000..9999 -> "Delete Packet Group"
                    listItem.Type > 0 && listItem.Type < 1000 -> "Remove BitTag Item"
                    else -> "Remove Tag Item"
                }
            }
        }
        val message = when (item) {
            is SelectedListItem.Screen -> "Are you sure you want to delete screen \"${item.screen.Name}\"?"
            is SelectedListItem.CustomGroupItem -> "Are you sure you want to delete custom group \"${item.group.groupName}\"?"
            is SelectedListItem.Item -> {
                val listItem = item.itemWithTag.item
                val tag = item.itemWithTag.tag
                val isBitTagItem = listItem.Type > 0 && listItem.Type < 1000
                val isNodeGroup = listItem.Type >= 10000
                val isPacketGroup = listItem.Type in 1000..9999

                val bitTagObj = if (isBitTagItem && tag != null && listItem.Type > 0) {
                    hierarchy.flatMap { node -> node.packetsWithTags }
                        .flatMap { p -> p.tagsWithBitTags }
                        .find { t -> t.tag.id == tag.id }
                        ?.bitTags?.find { b -> b.bitIndex == listItem.Type - 1 }
                } else null

                val name = when {
                    isBitTagItem && tag != null -> bitTagObj?.name ?: "${tag.name}-${listItem.Type - 1}"
                    isPacketGroup -> {
                        val packetId = (listItem.Type - 1000).toLong()
                        hierarchy.flatMap { node -> node.packetsWithTags }
                            .find { p -> p.packet.id == packetId }?.packet?.name ?: "Packet Group"
                    }
                    isNodeGroup -> {
                        val nodeId = (listItem.Type - 10000).toLong()
                        hierarchy.find { n -> n.node.id == nodeId }?.node?.name ?: "Node Group"
                    }
                    else -> tag?.name ?: "Tag #${listItem.parentTagId}"
                }
                "Are you sure you want to delete \"$name\" from this screen?"
            }
        }

        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        when (item) {
                            is SelectedListItem.Screen -> {
                                viewModel.deleteScreen(item.screen)
                                scope.launch { snackbarHostState.showSnackbar("Deleted list screen.") }
                            }
                            is SelectedListItem.Item -> {
                                viewModel.deleteListScreenItem(item.itemWithTag.item)
                                scope.launch { snackbarHostState.showSnackbar("Deleted item.") }
                            }
                            is SelectedListItem.CustomGroupItem -> {
                                viewModel.deleteCustomGroup(item.group)
                                scope.launch { snackbarHostState.showSnackbar("Deleted custom group.") }
                            }
                        }
                        if (selectedItem == item) selectedItem = null
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ListScreensTreeList(
    screensWithItems: List<ScreenWithListItems>,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    dataTypes: List<DataTypes> = emptyList(),
    viewModel: MainViewModel,
    selectedItem: SelectedListItem?,
    onItemSelected: (SelectedListItem) -> Unit,
    onAddTagToScreen: (Screens) -> Unit,
    onAddHeaderToScreen: (Screens) -> Unit,
    onAddTagToCustomGroup: (Screens, CustomGroup) -> Unit,
    onInsertTagAtItem: (Screens, ListScreenItemWithTag) -> Unit,
    onDeleteScreen: (Screens) -> Unit,
    onDeleteItem: (Screens, ListScreenItemWithTag) -> Unit,
    onDeleteCustomGroup: (CustomGroup) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val expandedScreens by viewModel.expandedScreensMap.collectAsStateWithLifecycle()
    val expandedCustomGroups by viewModel.expandedCustomGroupsMap.collectAsStateWithLifecycle()
    val expandedNodeGroups by viewModel.expandedNodeGroupsMap.collectAsStateWithLifecycle()
    val expandedPacketGroups by viewModel.expandedPacketGroupsMap.collectAsStateWithLifecycle()
    val expandedTagGroups by viewModel.expandedTagGroupsMap.collectAsStateWithLifecycle()

    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            screensWithItems.forEach { screenWithItems ->
                val screen = screenWithItems.screen
                val isExpanded = expandedScreens[screen.id] ?: false // Default COLLAPSED
                val isSelected = (selectedItem as? SelectedListItem.Screen)?.screen?.id == screen.id

                val packetGroupPacketIds = screenWithItems.items
                    .filter { it.item.Type in 1000..9999 && it.item.parentCustomGroupId == null }
                    .map { (it.item.Type - 1000).toLong() }
                    .toSet()

                val nodeGroupNodeIds = screenWithItems.items
                    .filter { it.item.Type in 10000..19999 && it.item.parentCustomGroupId == null }
                    .map { (it.item.Type - 10000).toLong() }
                    .toSet()

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

                val tagsInGroupsOnScreen = (nodeTagIds + packetTagIds).toSet()

                val topLevelItems = screenWithItems.items
                    .filter {
                        it.item.parentCustomGroupId == null &&
                        !(it.item.Type < 1000 && it.item.parentTagId != null && tagsInGroupsOnScreen.contains(it.item.parentTagId))
                    }
                    .sortedBy { it.item.ScreenIndex }

                item(key = "screen_${screen.id}") {
                    ListScreenRow(
                        text = screen.Name,
                        level = 0,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        hasChildren = topLevelItems.isNotEmpty(),
                        onToggleExpand = { viewModel.toggleScreenExpanded(screen.id) },
                        onSelect = { onItemSelected(SelectedListItem.Screen(screen)) },
                        onAdd = { onAddTagToScreen(screen) },
                        onAddHeader = null,
                        onDelete = { onDeleteScreen(screen) }
                    )
                }

                if (isExpanded) {
                    items(topLevelItems, key = { "item_${it.item.id}_${it.item.ScreenIndex}" }) { itemWithTag ->
                        val isItemMatches = (selectedItem as? SelectedListItem.Item)?.itemWithTag?.item?.id == itemWithTag.item.id
                        val item = itemWithTag.item
                        val tag = itemWithTag.tag
                        val isCustomGroup = item.Type >= 20000
                        val isNodeGroup = item.Type in 10000..19999
                        val isPacketGroup = item.Type in 1000..9999

                        if (isCustomGroup) {
                            val groupId = (item.Type - 20000).toLong()
                            var customGroup by remember(groupId) { mutableStateOf<CustomGroup?>(null) }
                            LaunchedEffect(groupId) {
                                customGroup = viewModel.getCustomGroupByIdSync(groupId)
                            }
                            val isGroupMatches = (selectedItem as? SelectedListItem.CustomGroupItem)?.group?.id == customGroup?.id
                            val groupChildItems = screenWithItems.items
                                .filter { it.item.parentCustomGroupId == customGroup?.id }
                                .sortedBy { it.item.ScreenIndex }

                            val isCustomGroupExpanded = expandedCustomGroups[groupId] ?: false // Default COLLAPSED

                            CustomGroupItemBox(
                                screen = screen,
                                itemWithTag = itemWithTag,
                                childItems = groupChildItems,
                                customGroup = customGroup,
                                hierarchy = hierarchy,
                                dataTypes = dataTypes,
                                isSelected = isGroupMatches,
                                isExpanded = isCustomGroupExpanded,
                                onToggleExpand = { viewModel.toggleCustomGroupExpanded(groupId) },
                                selectedItem = selectedItem,
                                onSelect = {
                                    if (customGroup != null) {
                                        onItemSelected(SelectedListItem.CustomGroupItem(customGroup!!, itemWithTag, screen))
                                    }
                                },
                                onSelectChild = { childWithTag ->
                                    onItemSelected(SelectedListItem.Item(childWithTag, screen))
                                },
                                onInsertAtItem = {
                                    if (customGroup != null) {
                                        onAddTagToCustomGroup(screen, customGroup!!)
                                    }
                                },
                                onDeleteItem = { childWithTag -> onDeleteItem(screen, childWithTag) },
                                onDeleteGroup = {
                                    if (customGroup != null) {
                                        onDeleteCustomGroup(customGroup!!)
                                    }
                                }
                            )
                        } else if (isNodeGroup) {
                            val nodeId = (item.Type - 10000).toLong()
                            val nodeWithPackets = hierarchy.find { n -> n.node.id == nodeId }
                            val isNodeGroupExpanded = expandedNodeGroups[item.id] ?: false // Default COLLAPSED

                            NodeGroupItemBox(
                                screen = screen,
                                itemWithTag = itemWithTag,
                                nodeWithPackets = nodeWithPackets,
                                dataTypes = dataTypes,
                                isSelected = isItemMatches,
                                isExpanded = isNodeGroupExpanded,
                                onToggleExpand = { viewModel.toggleNodeGroupExpanded(item.id) },
                                selectedItem = selectedItem,
                                screenItems = screenWithItems.items,
                                expandedPacketGroups = expandedPacketGroups,
                                viewModel = viewModel,
                                onSelect = { onItemSelected(SelectedListItem.Item(itemWithTag, screen)) },
                                onSelectChild = { childWithTag -> onItemSelected(SelectedListItem.Item(childWithTag, screen)) },
                                onInsertAtItem = { onInsertTagAtItem(screen, itemWithTag) },
                                onDeleteItem = { onDeleteItem(screen, itemWithTag) }
                            )
                        } else if (isPacketGroup) {
                            val packetId = (item.Type - 1000).toLong()
                            val packetWithTags = hierarchy.flatMap { node -> node.packetsWithTags }
                                .find { p -> p.packet.id == packetId }
                            val isPacketGroupExpanded = expandedPacketGroups[item.id] ?: false // Default COLLAPSED

                            PacketGroupItemBox(
                                screen = screen,
                                itemWithTag = itemWithTag,
                                packetWithTags = packetWithTags,
                                dataTypes = dataTypes,
                                isSelected = isItemMatches,
                                isExpanded = isPacketGroupExpanded,
                                onToggleExpand = { viewModel.togglePacketGroupExpanded(item.id) },
                                selectedItem = selectedItem,
                                screenItems = screenWithItems.items,
                                onSelect = { onItemSelected(SelectedListItem.Item(itemWithTag, screen)) },
                                onSelectChild = { childWithTag -> onItemSelected(SelectedListItem.Item(childWithTag, screen)) },
                                onInsertAtItem = { onInsertTagAtItem(screen, itemWithTag) },
                                onDeleteItem = { onDeleteItem(screen, itemWithTag) }
                            )
                        } else {
                            val isBitTagItem = item.Type > 0
                            val bitIndex = if (isBitTagItem) (item.Type - 1) else null

                            val packetDataType = hierarchy.flatMap { node -> node.packetsWithTags }
                                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } }
                                ?.packet?.type?.let { typeId -> dataTypes.find { dt -> dt.id == typeId.toLong() } }
                                ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                                ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)

                            val isTagGroup = item.Type == 0 && packetDataType.hasBits && item.isShowBits

                            if (isTagGroup) {
                                val tagWithBitTags = hierarchy.flatMap { node -> node.packetsWithTags }
                                    .flatMap { p -> p.tagsWithBitTags }
                                    .find { t -> t.tag.id == tag?.id }
                                val isTagGroupExpanded = expandedTagGroups[item.id] ?: false

                                TagGroupItemBox(
                                    screen = screen,
                                    itemWithTag = itemWithTag,
                                    tagWithBitTags = tagWithBitTags,
                                    dataType = packetDataType,
                                    isSelected = isItemMatches,
                                    isExpanded = isTagGroupExpanded,
                                    onToggleExpand = { viewModel.toggleTagGroupExpanded(item.id) },
                                    selectedItem = selectedItem,
                                    screenItems = screenWithItems.items,
                                    onSelect = { onItemSelected(SelectedListItem.Item(itemWithTag, screen)) },
                                    onSelectChild = { childWithTag -> onItemSelected(SelectedListItem.Item(childWithTag, screen)) },
                                    onDeleteItem = { onDeleteItem(screen, itemWithTag) }
                                )
                            } else {
                                val bitTagObj = if (isBitTagItem && tag != null && bitIndex != null) {
                                    hierarchy.flatMap { node -> node.packetsWithTags }
                                        .flatMap { p -> p.tagsWithBitTags }
                                        .find { t -> t.tag.id == tag.id }
                                        ?.bitTags?.find { b -> b.bitIndex == bitIndex }
                                } else null

                                val displayTitle = if (isBitTagItem) {
                                    bitTagObj?.name ?: if (tag != null) "${tag.name}-$bitIndex" else "Bit #$bitIndex"
                                } else {
                                    tag?.name ?: "Tag #${item.parentTagId}"
                                }

                                val tagShortName = packetDataType.shortName.ifEmpty { packetDataType.description }
                                val badgeColor = if (isBitTagItem) Color(0xFF00BCD4) else (getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784))

                                val parentPacket = hierarchy.flatMap { node -> node.packetsWithTags }
                                    .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } }?.packet
                                val calcOffset = tag?.offset ?: (parentPacket?.offset ?: 1)
                                val combinedBadgeText = if (isBitTagItem) "$tagShortName$calcOffset:$bitIndex" else "$tagShortName$calcOffset"

                                ListScreenRow(
                                    text = displayTitle,
                                    subtitle = "Index: ${item.ScreenIndex}" + (tag?.storedValue?.let { " (Val: $it)" } ?: ""),
                                    level = 1,
                                    isExpanded = false,
                                    isSelected = isItemMatches,
                                    hasChildren = false,
                                    badgeText = combinedBadgeText,
                                    badgeColor = badgeColor,
                                    onToggleExpand = {},
                                    onSelect = { onItemSelected(SelectedListItem.Item(itemWithTag, screen)) },
                                    onAdd = null,
                                    onDelete = { onDeleteItem(screen, itemWithTag) }
                                )
                            }
                        }
                    }
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
fun CustomGroupItemBox(
    screen: Screens,
    itemWithTag: ListScreenItemWithTag,
    childItems: List<ListScreenItemWithTag>,
    customGroup: CustomGroup?,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    isSelected: Boolean,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    selectedItem: SelectedListItem?,
    onSelect: () -> Unit,
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onInsertAtItem: () -> Unit,
    onDeleteItem: (ListScreenItemWithTag) -> Unit,
    onDeleteGroup: () -> Unit
) {
    val groupName = customGroup?.groupName ?: "Custom Group"
    val groupBgColor = parseHexColor(customGroup?.colorHex ?: "#F5F5F5")

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = groupBgColor,
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.5.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(onClick = onSelect)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Group" else "Expand Group",
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Custom Group Box (Index: ${itemWithTag.item.ScreenIndex}, ${childItems.size} items)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onInsertAtItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add sub-item to group",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteGroup,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded && childItems.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                ) {
                    childItems.sortedBy { it.item.ScreenIndex }.forEach { childWithTag ->
                        val childItem = childWithTag.item
                        val childTag = childWithTag.tag
                        val isBitTagItem = childItem.Type > 0 && childItem.Type < 1000
                        val bitIndex = if (isBitTagItem) (childItem.Type - 1) else null

                        val childBitTagObj = if (isBitTagItem && childTag != null && bitIndex != null) {
                            hierarchy.flatMap { node -> node.packetsWithTags }
                                .flatMap { p -> p.tagsWithBitTags }
                                .find { t -> t.tag.id == childTag.id }
                                ?.bitTags?.find { b -> b.bitIndex == bitIndex }
                        } else null

                        val displayTitle = if (isBitTagItem) {
                            childBitTagObj?.name ?: if (childTag != null) "${childTag.name}-$bitIndex" else "Bit #$bitIndex"
                        } else {
                            childTag?.name ?: "Tag #${childItem.parentTagId}"
                        }

                        val packetDataType = hierarchy.flatMap { node -> node.packetsWithTags }
                            .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == childTag?.id } }
                            ?.packet?.type?.let { typeId -> dataTypes.find { dt -> dt.id == typeId.toLong() } }
                            ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                            ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)

                        val tagShortName = packetDataType.shortName.ifEmpty { packetDataType.description }
                        val badgeColor = if (isBitTagItem) Color(0xFF00BCD4) else (getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784))

                        val childParentPacket = hierarchy.flatMap { node -> node.packetsWithTags }
                            .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == childTag?.id } }?.packet
                        val childCalcOffset = childTag?.offset ?: (childParentPacket?.offset ?: 1)
                        val childCombinedBadgeText = if (isBitTagItem) "$tagShortName$childCalcOffset:$bitIndex" else "$tagShortName$childCalcOffset"

                        val selectedItemWithTag = (selectedItem as? SelectedListItem.Item)?.itemWithTag
                        val isChildSelected = if (selectedItemWithTag != null) {
                            if (childItem.id > 0 && selectedItemWithTag.item.id > 0) {
                                selectedItemWithTag.item.id == childItem.id
                            } else {
                                selectedItemWithTag.tag?.id == childTag?.id && selectedItemWithTag.item.Type == childItem.Type
                            }
                        } else false

                        ListScreenRow(
                            text = displayTitle,
                            subtitle = "Index: ${childItem.ScreenIndex}" + (childTag?.storedValue?.let { " (Val: $it)" } ?: ""),
                            level = 2,
                            isExpanded = false,
                            isSelected = isChildSelected,
                            hasChildren = false,
                            badgeText = childCombinedBadgeText,
                            badgeColor = badgeColor,
                            onToggleExpand = {},
                            onSelect = { onSelectChild(childWithTag) },
                            onDelete = { onDeleteItem(childWithTag) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NodeGroupItemBox(
    screen: Screens,
    itemWithTag: ListScreenItemWithTag,
    nodeWithPackets: NodeWithPacketsAndTags?,
    dataTypes: List<DataTypes>,
    isSelected: Boolean,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    selectedItem: SelectedListItem? = null,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    expandedPacketGroups: Map<Long, Boolean> = emptyMap(),
    viewModel: MainViewModel? = null,
    onSelect: () -> Unit,
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onInsertAtItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val node = nodeWithPackets?.node
    val nodeName = node?.name ?: "Node Group"
    val nodeIp = node?.ipAddress ?: ""

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
            ) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Node" else "Expand Node",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.NetworkCheck,
                    contentDescription = "Node Group",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = nodeName + (if (nodeIp.isNotBlank()) " ($nodeIp)" else ""),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Node Group (Index: ${itemWithTag.item.ScreenIndex}, ${nodeWithPackets?.packetsWithTags?.size ?: 0} packets)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete node group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded && nodeWithPackets != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                ) {
                    nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                        val packetId = packetWithTags.packet.id
                        val matchingPacketItem = screenItems.find { it.item.Type == (1000 + packetId.toInt()) }
                        val targetItem = matchingPacketItem ?: itemWithTag
                        val isPacketSelected = matchingPacketItem != null && (selectedItem as? SelectedListItem.Item)?.itemWithTag?.item?.id == matchingPacketItem.item.id

                        PacketGroupItemBox(
                            screen = screen,
                            itemWithTag = targetItem,
                            packetWithTags = packetWithTags,
                            dataTypes = dataTypes,
                            isSelected = isPacketSelected,
                            isExpanded = expandedPacketGroups[targetItem.item.id] ?: false,
                            onToggleExpand = { viewModel?.togglePacketGroupExpanded(targetItem.item.id) },
                            selectedItem = selectedItem,
                            screenItems = screenItems,
                            onSelect = { onSelectChild(targetItem) },
                            onSelectChild = onSelectChild,
                            onInsertAtItem = onInsertAtItem,
                            onDeleteItem = onDeleteItem
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PacketGroupItemBox(
    screen: Screens,
    itemWithTag: ListScreenItemWithTag,
    packetWithTags: PacketWithTags?,
    dataTypes: List<DataTypes>,
    isSelected: Boolean,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    selectedItem: SelectedListItem? = null,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    onSelect: () -> Unit,
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onInsertAtItem: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val packet = packetWithTags?.packet
    val packetName = packet?.name ?: "Packet Group"
    val packetDataType = packet?.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
        ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
        ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
    val badgeText = packetDataType.shortName.ifEmpty { packetDataType.description }
    val badgeColor = getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
            ) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = packetName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Packet Group (Index: ${itemWithTag.item.ScreenIndex}, ${packetWithTags?.tagsWithBitTags?.size ?: 0} tags)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete packet group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded && packetWithTags != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                ) {
                    packetWithTags.tagsWithBitTags.forEach { tagWithBit ->
                        val tag = tagWithBit.tag
                        val matchingTagItem = screenItems.find { it.item.parentTagId == tag.id && it.item.Type == 0 }
                        val targetItem = matchingTagItem ?: ListScreenItemWithTag(
                            item = ListScreenItems(parentScreenId = screen.id, parentTagId = tag.id, Type = 0, ScreenIndex = itemWithTag.item.ScreenIndex),
                            tag = tag
                        )
                        val selectedItemWithTag = (selectedItem as? SelectedListItem.Item)?.itemWithTag
                        val isTagSelected = if (selectedItemWithTag != null) {
                            if (targetItem.item.id > 0 && selectedItemWithTag.item.id > 0) {
                                selectedItemWithTag.item.id == targetItem.item.id
                            } else {
                                selectedItemWithTag.tag?.id == tag.id && selectedItemWithTag.item.Type == 0
                            }
                        } else false

                        val tagCalcOffset = tag.offset
                        val combinedBadgeText = "$badgeText$tagCalcOffset"

                        ListScreenRow(
                            text = tag.name,
                            subtitle = "Tag #${tag.id} (Val: ${tag.storedValue})",
                            level = 2,
                            isExpanded = false,
                            isSelected = isTagSelected,
                            hasChildren = false,
                            badgeText = combinedBadgeText,
                            badgeColor = badgeColor,
                            onToggleExpand = {},
                            onSelect = { onSelectChild(targetItem) },
                            onDelete = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagGroupItemBox(
    screen: Screens,
    itemWithTag: ListScreenItemWithTag,
    tagWithBitTags: TagWithBitTags?,
    dataType: DataTypes,
    isSelected: Boolean,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    selectedItem: SelectedListItem? = null,
    screenItems: List<ListScreenItemWithTag> = emptyList(),
    onSelect: () -> Unit,
    onSelectChild: (ListScreenItemWithTag) -> Unit = {},
    onDeleteItem: () -> Unit
) {
    val tag = itemWithTag.tag
    val tagName = tag?.name ?: "Tag Group"
    val badgeText = dataType.shortName.ifEmpty { dataType.description }
    val badgeColor = getDataTypeBadgeColor(dataType) ?: Color(0xFF81C784)

    val numBits = dataType.bytes * 8

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSelect)
            ) {
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Tag Group" else "Expand Tag Group",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Rounded.Sell,
                    contentDescription = "Tag Group",
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tagName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tag Group Box (Index: ${itemWithTag.item.ScreenIndex}, $numBits bits)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDeleteItem,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete tag group",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                ) {
                    (0 until numBits).forEach { bitIdx ->
                        val matchingBitItem = screenItems.find { it.item.parentTagId == tag?.id && it.item.Type == (bitIdx + 1) }
                        val targetItem = matchingBitItem ?: ListScreenItemWithTag(
                            item = ListScreenItems(parentScreenId = screen.id, parentTagId = tag?.id, Type = (bitIdx + 1), ScreenIndex = itemWithTag.item.ScreenIndex),
                            tag = tag
                        )
                        val selectedItemWithTag = (selectedItem as? SelectedListItem.Item)?.itemWithTag
                        val isBitSelected = if (selectedItemWithTag != null) {
                            if (targetItem.item.id > 0 && selectedItemWithTag.item.id > 0) {
                                selectedItemWithTag.item.id == targetItem.item.id
                            } else {
                                selectedItemWithTag.item.parentTagId == tag?.id && selectedItemWithTag.item.Type == (bitIdx + 1)
                            }
                        } else false

                        val bitName = "$tagName-$bitIdx"
                        val bitBadgeText = "$badgeText${tag?.offset ?: 0}:$bitIdx"

                        ListScreenRow(
                            text = bitName,
                            subtitle = "Bit $bitIdx of $tagName",
                            level = 2,
                            isExpanded = false,
                            isSelected = isBitSelected,
                            hasChildren = false,
                            badgeText = bitBadgeText,
                            badgeColor = Color(0xFF00BCD4),
                            onToggleExpand = {},
                            onSelect = { onSelectChild(targetItem) },
                            onDelete = { onDeleteItem() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ListScreenRow(
    text: String,
    subtitle: String? = null,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    badgeText: String? = null,
    badgeColor: Color? = null,
    offsetText: String? = null,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onAdd: (() -> Unit)? = null,
    onAddHeader: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val startIndent = (level * 20).dp
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onSelect)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = startIndent, end = 8.dp)
                .height(48.dp)
        ) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(24.dp)
            ) {
                if (hasChildren) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Icon(
                imageVector = if (level == 0) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.Sell,
                contentDescription = null,
                tint = if (level == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            onAdd?.let {
                IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = "Insert Tag", modifier = Modifier.size(18.dp))
                }
            }
            onAddHeader?.let {
                IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.FontDownload,
                        contentDescription = "Add Header Label",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            onDelete?.let {
                IconButton(onClick = it, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenDetailPane(
    item: SelectedListItem,
    screensWithItems: List<ScreenWithListItems>,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    dataTypes: List<DataTypes> = emptyList(),
    viewModel: MainViewModel,
    onUpdateScreen: (Screens) -> Unit,
    onUpdateGroup: (CustomGroup) -> Unit = {},
    onAddTagToScreen: (screenId: Long) -> Unit = {},
    onInsertAbove: (screenId: Long, groupId: Long?, targetIndex: Int) -> Unit = { _, _, _ -> },
    onInsertBelow: (screenId: Long, groupId: Long?, targetIndex: Int) -> Unit = { _, _, _ -> },
    onDeleteItemWithSelection: (SelectedListItem?) -> Unit = {},
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    when (item) {
        is SelectedListItem.Screen -> {
            var name by remember(item.screen.id) { mutableStateOf(item.screen.Name) }
            val focusRequester = remember { FocusRequester() }

            val trimmedName = name.trim()
            val isNameEmpty = trimmedName.isEmpty()
            val isDuplicateName = screensWithItems.any {
                it.screen.id != item.screen.id && it.screen.Name.trim().equals(trimmedName, ignoreCase = true)
            }
            val isNameInvalid = isNameEmpty || isDuplicateName
            var isEdited by remember(item.screen.id, name) {
                mutableStateOf(item.screen.Name.trim() != trimmedName)
            }

            val currentScreenIdx = screensWithItems.indexOfFirst { it.screen.id == item.screen.id }
            val canMoveUp = currentScreenIdx > 0
            val canMoveDown = currentScreenIdx >= 0 && currentScreenIdx < screensWithItems.size - 1

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Edit List Screen",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isEdited) {
                        Button(
                            onClick = {
                                if (isNameInvalid) {
                                    onShowSnackbar(
                                        if (isDuplicateName) "Error: Screen name '$trimmedName' already exists. Screen names must be unique."
                                        else "Error: Screen name cannot be empty."
                                    )
                                } else {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onUpdateScreen(item.screen.copy(Name = trimmedName))
                                    isEdited = false
                                }
                            },
                            enabled = !isNameInvalid
                        ) {
                            Text("Save")
                        }
                    }
                }

                // Row 1 & Row 2: Prominent Screen Actions & Reordering right at top!
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Screen Actions & Reordering",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.addListScreenAbove(item.screen, onShowSnackbar = { msg -> onShowSnackbar(msg) })
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Above", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.addListScreenBelow(item.screen, onShowSnackbar = { msg -> onShowSnackbar(msg) })
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Below", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    onDeleteItemWithSelection(SelectedListItem.Screen(item.screen))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.moveScreenUp(item.screen.id)
                                },
                                enabled = canMoveUp,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Up", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.moveScreenDown(item.screen.id)
                                },
                                enabled = canMoveDown,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Down", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue
                        isEdited = (newValue.trim() != item.screen.Name.trim())
                    },
                    label = { Text("Screen Name") },
                    isError = isNameInvalid,
                    supportingText = {
                        if (isDuplicateName) {
                            Text("Screen name '$trimmedName' already exists. Screen names must be unique.")
                        } else if (isNameEmpty) {
                            Text("Screen name cannot be empty")
                        }
                    },
                    trailingIcon = {
                        if (name.isNotEmpty()) {
                            IconButton(onClick = {
                                name = ""
                                isEdited = true
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
        is SelectedListItem.CustomGroupItem -> {
            var groupName by remember(item.group.id) { mutableStateOf(item.group.groupName) }
            var groupColorHex by remember(item.group.id) { mutableStateOf(item.group.colorHex) }
            var showColorDialog by remember { mutableStateOf(false) }
            var isEdited by remember(item.group.id) { mutableStateOf(item.group.groupName.equals("New Custom Group", ignoreCase = true) || item.group.groupName.isBlank()) }
            val focusRequester = remember { FocusRequester() }

            val trimmedName = groupName.trim()
            val isNameEmpty = trimmedName.isEmpty()
            val isDefaultName = trimmedName.equals("New Custom Group", ignoreCase = true)
            val isNameInvalid = isNameEmpty || isDefaultName

            val groupListItem = item.itemWithTag.item
            val itemsInScreen = screensWithItems.find { it.screen.id == item.parentScreen.id }?.items?.sortedBy { it.item.ScreenIndex } ?: emptyList()
            val totalItemsCount = itemsInScreen.size
            val currentIdx = itemsInScreen.indexOfFirst { it.item.id == groupListItem.id }
            val canMoveUp = currentIdx > 0
            val canMoveDown = currentIdx >= 0 && currentIdx < totalItemsCount - 1

            val buttonBgColor = parseHexColor(groupColorHex)

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Edit Custom Group",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isEdited) {
                        Button(
                            onClick = {
                                if (isNameInvalid) {
                                    onShowSnackbar(
                                        if (isDefaultName) "Error: Please choose a unique name instead of 'New Custom Group'."
                                        else "Error: Group name cannot be empty."
                                    )
                                } else {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onUpdateGroup(item.group.copy(groupName = trimmedName, colorHex = groupColorHex))
                                    isEdited = false
                                }
                            },
                            enabled = !isNameInvalid
                        ) {
                            Text("Save")
                        }
                    }
                }

                // Row 1 & Row 2: Prominent Action Buttons right at top!
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Item Actions & Reordering",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onInsertAbove(item.parentScreen.id, groupListItem.parentCustomGroupId, groupListItem.ScreenIndex)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Above", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    onInsertBelow(item.parentScreen.id, groupListItem.parentCustomGroupId, groupListItem.ScreenIndex + 1)
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Insert Below", style = MaterialTheme.typography.labelSmall)
                            }

                            Button(
                                onClick = {
                                    onDeleteItemWithSelection(SelectedListItem.CustomGroupItem(item.group, item.itemWithTag, item.parentScreen))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.moveListScreenItemUp(item.parentScreen.id, groupListItem.id)
                                },
                                enabled = canMoveUp,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Up", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.moveListScreenItemDown(item.parentScreen.id, groupListItem.id)
                                },
                                enabled = canMoveDown,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Down", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { newValue ->
                        groupName = newValue
                        isEdited = true
                    },
                    label = { Text("Group Name") },
                    isError = isNameInvalid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                // Pick Group Background Color Button
                Button(
                    onClick = { showColorDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp)
                    )
                    Text("Pick Group Background Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (showColorDialog) {
                    GroupColorPickerDialog(
                        initialColorHex = groupColorHex,
                        onColorSelected = { newHex ->
                            groupColorHex = newHex
                            isEdited = true
                            onUpdateGroup(item.group.copy(groupName = trimmedName, colorHex = newHex))
                        },
                        onDismiss = { showColorDialog = false }
                    )
                }
            }
        }
        is SelectedListItem.Item -> {
            val currentItemWithTag = screensWithItems
                .find { it.screen.id == item.parentScreen.id }
                ?.items
                ?.find { it.item.id == item.itemWithTag.item.id }
                ?: item.itemWithTag

            val listItem = currentItemWithTag.item
            val tag = currentItemWithTag.tag
            val isBitTagItem = listItem.Type > 0 && listItem.Type < 1000
            val isNodeGroup = listItem.Type >= 10000
            val isPacketGroup = listItem.Type in 1000..9999
            val isTagItem = listItem.Type < 1000

            val bitIndex = if (isBitTagItem) (listItem.Type - 1) else null
            val bitTagObj = if (isBitTagItem && tag != null && bitIndex != null) {
                hierarchy.flatMap { node -> node.packetsWithTags }
                    .flatMap { p -> p.tagsWithBitTags }
                    .find { t -> t.tag.id == tag.id }
                    ?.bitTags?.find { b -> b.bitIndex == bitIndex }
            } else null

            val displayTitle = when {
                isBitTagItem -> bitTagObj?.name ?: if (tag != null) "${tag.name}-$bitIndex" else "Bit #$bitIndex"
                isPacketGroup -> {
                    val packetId = (listItem.Type - 1000).toLong()
                    hierarchy.flatMap { node -> node.packetsWithTags }
                        .find { p -> p.packet.id == packetId }?.packet?.name ?: "Packet Group"
                }
                isNodeGroup -> {
                    val nodeId = (listItem.Type - 10000).toLong()
                    hierarchy.find { n -> n.node.id == nodeId }?.node?.name ?: "Node Group"
                }
                else -> tag?.name ?: "Tag #${listItem.parentTagId}"
            }

            val packetDataType = hierarchy.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag?.id } }
                ?.packet?.type?.let { typeId -> dataTypes.find { dt -> dt.id == typeId.toLong() } }
                ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)

            val dataTypeShortName = if (isBitTagItem) "B" else packetDataType.shortName.ifEmpty { packetDataType.description }
            val options = DisplayTypes.getOptionsForDataType(dataTypeShortName)
            val initialDisplayType = if (options.contains(listItem.DisplayType)) listItem.DisplayType else options.first()

            val itemKey = if (listItem.id > 0) "item_${listItem.id}" else "tag_${tag?.id ?: listItem.parentTagId}_${listItem.Type}"

            var selectedDisplayType by remember(itemKey, listItem.DisplayType) {
                mutableStateOf(initialDisplayType)
            }
            var isReadOnlyState by remember(itemKey, listItem.isReadOnly) {
                mutableStateOf(listItem.isReadOnly)
            }
            var isTwoTouchState by remember(itemKey, listItem.isTwoTouch) {
                mutableStateOf(listItem.isTwoTouch)
            }
            var isShowBitsState by remember(itemKey, listItem.isShowBits) {
                mutableStateOf(listItem.isShowBits)
            }

            val itemsInContainer = screensWithItems
                .find { it.screen.id == item.parentScreen.id }
                ?.items
                ?.filter { it.item.parentCustomGroupId == listItem.parentCustomGroupId }
                ?.sortedBy { it.item.ScreenIndex } ?: emptyList()
            val totalItemsCount = itemsInContainer.size
            val currentIdx = itemsInContainer.indexOfFirst { it.item.id == listItem.id }
            val canMoveUp = currentIdx > 0
            val canMoveDown = currentIdx >= 0 && currentIdx < totalItemsCount - 1

            val isPacketSubItem = item.isPacketSubItem || (
                listItem.parentCustomGroupId == null &&
                tag != null &&
                !isPacketGroup &&
                !isNodeGroup &&
                hierarchy.flatMap { node -> node.packetsWithTags }.any { p -> p.tagsWithBitTags.any { t -> t.tag.id == tag.id } }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "List Item Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isPacketSubItem) {
                    // Row 1 & Row 2: Prominent Action Buttons right at top!
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Item Actions & Reordering",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onInsertAbove(item.parentScreen.id, listItem.parentCustomGroupId, listItem.ScreenIndex)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Insert Above", style = MaterialTheme.typography.labelSmall)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onInsertBelow(item.parentScreen.id, listItem.parentCustomGroupId, listItem.ScreenIndex + 1)
                                    },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Insert Below", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        onDeleteItemWithSelection(SelectedListItem.Item(item.itemWithTag, item.parentScreen, isPacketSubItem))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.moveListScreenItemUp(item.parentScreen.id, listItem.id)
                                    },
                                    enabled = canMoveUp,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Move Up", style = MaterialTheme.typography.labelSmall)
                                }

                                FilledTonalButton(
                                    onClick = {
                                        viewModel.moveListScreenItemDown(item.parentScreen.id, listItem.id)
                                    },
                                    enabled = canMoveDown,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Move Down", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Parent Screen: ${item.parentScreen.Name}", fontWeight = FontWeight.SemiBold)
                        Text("Tag / Item Name: $displayTitle")
                        Text("Item Type: ${if (isBitTagItem) "BitTag (Bit $bitIndex)" else if (isNodeGroup) "Node Group" else if (isPacketGroup) "Packet Group" else "Full Tag"}")
                        if (isTagItem) {
                            val dataTypeFullText = if (isBitTagItem) "B (BitTag)" else "${packetDataType.shortName} (${packetDataType.description})"
                            Text("Data Type: $dataTypeFullText")
                        }
                        Text("Position Index (ScreenIndex): ${listItem.ScreenIndex} of $totalItemsCount")
                        Text("Current Value: ${tag?.storedValue ?: "(empty)"}")
                        Text("Description: ${tag?.description ?: "-"}")

                        if (!isNodeGroup && !isPacketGroup) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Item Options & Behavior",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isReadOnlyState) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clickable {
                                                val newVal = !isReadOnlyState
                                                isReadOnlyState = newVal
                                                viewModel.updateListScreenItem(listItem.copy(isReadOnly = newVal))
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isReadOnlyState,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Read Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (!isBitTagItem && packetDataType.hasBits) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isShowBitsState) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable {
                                                    val newVal = !isShowBitsState
                                                    isShowBitsState = newVal
                                                    viewModel.updateListScreenItem(listItem.copy(isShowBits = newVal))
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Checkbox(
                                                checked = isShowBitsState,
                                                onCheckedChange = null
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Show Bits", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            var dropdownExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedDisplayType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("display type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    options.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                dropdownExpanded = false
                                                if (selectedDisplayType != option) {
                                                    selectedDisplayType = option
                                                    viewModel.updateListScreenItem(listItem.copy(DisplayType = option))
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            val isListDisplayType = selectedDisplayType.contains("List", ignoreCase = true)
                            var showConfigListDialog by remember { mutableStateOf(false) }

                            if (isListDisplayType && tag != null) {
                                OutlinedButton(
                                    onClick = { showConfigListDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FormatListNumbered,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Config List", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }

                                if (showConfigListDialog) {
                                    TagListConfigDialog(
                                        tag = tag,
                                        viewModel = viewModel,
                                        onDismiss = { showConfigListDialog = false },
                                        onShowSnackbar = onShowSnackbar
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
