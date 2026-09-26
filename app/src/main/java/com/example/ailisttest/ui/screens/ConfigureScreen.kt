package com.example.ailisttest.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ailisttest.data.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.*
import com.example.ailisttest.data.modbus.ModbusByteOrder
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.ModbusDebugInfo
import com.example.ailisttest.ui.NodeConnectionState
import com.example.ailisttest.ui.SelectedItem
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ConfigureScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    val nodeConnectionStates by viewModel.nodeConnectionStates.collectAsStateWithLifecycle()
    val modbusDebugInfo by viewModel.modbusDebugInfo.collectAsStateWithLifecycle()
    val isDebugMode by viewModel.isDebugMode.collectAsStateWithLifecycle()
    val isPollingEnabled by viewModel.isPollingEnabled.collectAsStateWithLifecycle()

    ConfigureScreenContent(
        hierarchy = hierarchy,
        selectedItem = selectedItem,
        dataTypes = dataTypes,
        nodeConnectionStates = nodeConnectionStates,
        isDebugMode = isDebugMode,
        isPollingEnabled = isPollingEnabled,
        viewModel = viewModel,
        onSetPollingEnabled = { viewModel.setPollingEnabled(it) },
        onToggleDebugMode = { viewModel.toggleDebugMode() },
        onOpenDrawer = onOpenDrawer,
        onSelectItem = { viewModel.selectItem(it) },
        onAddNode = { viewModel.addNode() },
        onUpdateNode = { viewModel.updateNode(it) },
        onDeleteNode = { viewModel.deleteNode(it) },
        onConnectNode = { viewModel.connectNode(it) },
        onReadPacket = { packet, onShowSnackbar -> viewModel.readPacket(packet, onShowSnackbar) },
        onWritePacket = { packet, onShowSnackbar -> viewModel.writePacket(packet, onShowSnackbar) },
        onReadTag = { tag, onShowSnackbar -> viewModel.readTag(tag, onShowSnackbar) },
        onWriteTag = { tag, onShowSnackbar -> viewModel.updateTagValueAndWrite(tag, tag.storedValue, onShowSnackbar) },
        onAddPacket = { viewModel.addPacket(it.id) },
        onUpdatePacket = { viewModel.updatePacket(it) },
        onDeletePacket = { viewModel.deletePacket(it) },
        onAddTag = { viewModel.addTag(it.id) },
        onUpdateTag = { viewModel.updateTag(it) },
        onDeleteTag = { viewModel.deleteTag(it) },
        onUpdateBitTag = { viewModel.updateBitTag(it) },
        onDeleteBitTag = { viewModel.deleteBitTag(it) }
    )

    if (isDebugMode) {
        modbusDebugInfo?.let { debugInfo ->
            ModbusDebugDialog(
                debugInfo = debugInfo,
                onDismiss = { viewModel.clearDebugInfo() }
            )
        }
    }
}

@Composable
fun ImeValuePreviewBar(
    fieldName: String,
    currentValue: String,
    onClear: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val imeBottomPadding = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPadding > 0

    if (isImeVisible) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 12.dp,
            modifier = modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = fieldName.ifEmpty { "Field Value" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentValue.ifEmpty { "(empty)" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = onClear,
                    enabled = currentValue.isNotEmpty(),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .padding(end = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Clear Field",
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 4.dp)
                    )
                    Text("Clear", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = onDone,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ConfigureScreenContent(
    hierarchy: List<NodeWithPacketsAndTags>,
    selectedItem: SelectedItem?,
    dataTypes: List<DataTypes> = emptyList(),
    nodeConnectionStates: Map<Long, NodeConnectionState> = emptyMap(),
    isDebugMode: Boolean = false,
    isPollingEnabled: Boolean = false,
    viewModel: MainViewModel? = null,
    onSetPollingEnabled: (Boolean) -> Unit = {},
    onToggleDebugMode: () -> Unit = {},
    onOpenDrawer: () -> Unit,
    onSelectItem: (SelectedItem?) -> Unit,
    onAddNode: () -> Unit,
    onUpdateNode: (NodeEntity) -> Unit,
    onDeleteNode: (NodeEntity) -> Unit,
    onConnectNode: (NodeEntity) -> Unit = {},
    onReadPacket: (PacketEntity, (String) -> Unit) -> Unit = { _, _ -> },
    onWritePacket: (PacketEntity, (String) -> Unit) -> Unit = { _, _ -> },
    onReadTag: (TagEntity, (String) -> Unit) -> Unit = { _, _ -> },
    onWriteTag: (TagEntity, (String) -> Unit) -> Unit = { _, _ -> },
    onAddPacket: (NodeEntity) -> Unit,
    onUpdatePacket: (PacketEntity) -> Unit,
    onDeletePacket: (PacketEntity) -> Unit,
    onAddTag: (PacketEntity) -> Unit,
    onUpdateTag: (TagEntity) -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onUpdateBitTag: (BitTags) -> Unit = {},
    onDeleteBitTag: (BitTags) -> Unit = {}
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var activeFocusedLabel by remember { mutableStateOf("") }
    var activeFocusedValue by remember { mutableStateOf("") }
    var activeClearAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val unsavedEditedNodes = remember { mutableStateMapOf<Long, Boolean>() }
    val unsavedEditedPackets = remember { mutableStateMapOf<Long, Boolean>() }
    val unsavedEditedTags = remember { mutableStateMapOf<Long, Boolean>() }
    val savedNodes = remember { mutableStateMapOf<Long, Boolean>() }
    val savedPackets = remember { mutableStateMapOf<Long, Boolean>() }
    val savedTags = remember { mutableStateMapOf<Long, Boolean>() }
    val activeSaveActions = remember { mutableStateMapOf<Long, () -> Unit>() }

    val newlyCreatedNodeIdsState = viewModel?.newlyCreatedNodeIds?.collectAsStateWithLifecycle()
    val newlyCreatedNodeIds = newlyCreatedNodeIdsState?.value ?: emptySet()

    val newlyCreatedPacketIdsState = viewModel?.newlyCreatedPacketIds?.collectAsStateWithLifecycle()
    val newlyCreatedPacketIds = newlyCreatedPacketIdsState?.value ?: emptySet()

    val newlyCreatedTagIdsState = viewModel?.newlyCreatedTagIds?.collectAsStateWithLifecycle()
    val newlyCreatedTagIds = newlyCreatedTagIdsState?.value ?: emptySet()

    // Handle system back button for adaptive layout
    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    // Handle navigation back if selection is cleared (e.g. on deletion)
    LaunchedEffect(selectedItem) {
        if (selectedItem == null && navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
            navigator.navigateBack()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Prevent software keyboard from popping up automatically on initial screen load
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    val tagFileFormatState = viewModel?.tagFileFormat?.collectAsStateWithLifecycle()
    val tagFileFormat = tagFileFormatState?.value ?: "Click Plus"
    var showExportDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var showImportSourceDialog by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var selectedCsvFile by remember { mutableStateOf<CsvFileInfo?>(null) }
    var importResultState by remember { mutableStateOf<ImportResult?>(null) }
    var exportResultState by remember { mutableStateOf<ExportResult?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val displayPath = uri.path ?: uri.toString()
            val pickedInfo = CsvFileInfo(
                file = if (uri.path != null) File(uri.path!!) else null,
                name = fileName,
                dirPath = "System File Picker",
                path = displayPath,
                sizeText = "Available",
                dateText = "System Selected",
                isSample = false,
                uri = uri
            )

            selectedCsvFile = pickedInfo
        }
    }

    val baseDirective = navigator.scaffoldDirective
    val customDirective = remember(baseDirective) {
        baseDirective.copy(defaultPanePreferredWidth = 450.dp)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                                title = { Text("Configuration / Tags") },
                                navigationIcon = {
                                    IconButton(onClick = onOpenDrawer) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    val isNoNodes = hierarchy.isEmpty()
                                    val topAddNodeTransition = rememberInfiniteTransition(label = "topAddNodeBlink")
                                    val topAddNodeBlinkAlpha by topAddNodeTransition.animateFloat(
                                        initialValue = 1f,
                                        targetValue = 0.25f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(durationMillis = 500, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "topAddNodeBlinkAlpha"
                                    )

                                    IconButton(
                                        onClick = onAddNode,
                                        modifier = if (isNoNodes) Modifier.alpha(topAddNodeBlinkAlpha) else Modifier
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add Node",
                                            tint = if (isNoNodes) MaterialTheme.colorScheme.primary.copy(alpha = topAddNodeBlinkAlpha) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            )

                            TreeList(
                                hierarchy = hierarchy,
                                selectedItem = selectedItem,
                                dataTypes = dataTypes,
                                nodeConnectionStates = nodeConnectionStates,
                                isPollingEnabled = isPollingEnabled,
                                unsavedEditedNodes = unsavedEditedNodes,
                                unsavedEditedPackets = unsavedEditedPackets,
                                unsavedEditedTags = unsavedEditedTags,
                                savedNodes = savedNodes,
                                savedPackets = savedPackets,
                                savedTags = savedTags,
                                newlyCreatedNodeIds = newlyCreatedNodeIds,
                                newlyCreatedPacketIds = newlyCreatedPacketIds,
                                newlyCreatedTagIds = newlyCreatedTagIds,
                                activeSaveActions = activeSaveActions,
                                viewModel = viewModel,
                                onItemSelected = {
                                    onSelectItem(it)
                                    scope.launch {
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                    }
                                },
                                onAddNode = onAddNode,
                                onAddPacket = onAddPacket,
                                onAddTag = onAddTag,
                                onDeleteNode = onDeleteNode,
                                onConnectNode = onConnectNode,
                                onReadPacket = { packet ->
                                    onReadPacket(packet) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                onWritePacket = { packet ->
                                    onWritePacket(packet) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                onReadTag = { tag ->
                                    onReadTag(tag) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                onWriteTag = { tag ->
                                    onWriteTag(tag) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                onDeletePacket = onDeletePacket,
                                onDeleteTag = onDeleteTag,
                                onUpdateTag = onUpdateTag,
                                onDeleteBitTag = onDeleteBitTag,
                                onFocusField = { label, valStr, clearAct ->
                                    activeFocusedLabel = label
                                    activeFocusedValue = valStr
                                    activeClearAction = clearAct
                                }
                            )
                        }
                    }
                },
                detailPane = {
                    AnimatedPane(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Top Control Section (above right pane!)
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Line 1: Debug Mode button on left + Polling Switch on right
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilterChip(
                                            selected = isDebugMode,
                                            onClick = onToggleDebugMode,
                                            label = { Text("Debug Mode") },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.BugReport,
                                                    contentDescription = "Debug Mode",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Autorenew,
                                                contentDescription = null,
                                                tint = if (isPollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = if (isPollingEnabled) "Polling: ON" else "Polling: OFF",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Switch(
                                                checked = isPollingEnabled,
                                                onCheckedChange = { enabled ->
                                                    onSetPollingEnabled(enabled)
                                                }
                                            )
                                        }
                                    }

                                    // Line 2: Import Tag Names & Export Tag Names (Equal size buttons!)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                selectedCsvFile = null
                                                showImportSourceDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FileUpload,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Import Tags",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                showExportDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FileDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Export Tags",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            selectedItem?.let { item ->
                                EditForm(
                                    item = item,
                                    hierarchy = hierarchy,
                                    dataTypes = dataTypes,
                                    viewModel = viewModel,
                                    isPacketSaved = savedPackets[(item as? SelectedItem.Packet)?.packet?.id ?: -1L] == true ||
                                        ((item as? SelectedItem.Packet)?.packet?.let { p ->
                                            p.id !in newlyCreatedPacketIds
                                        } ?: false),
                                    onUpdateNode = onUpdateNode,
                                    onUpdatePacket = onUpdatePacket,
                                    onUpdateTag = onUpdateTag,
                                    onUpdateBitTag = onUpdateBitTag,
                                    onNodeEditStateChange = { nodeId, isEdited ->
                                        unsavedEditedNodes[nodeId] = isEdited
                                    },
                                    onPacketEditStateChange = { packetId, isEdited ->
                                        unsavedEditedPackets[packetId] = isEdited
                                    },
                                    onTagEditStateChange = { tagId, isEdited ->
                                        unsavedEditedTags[tagId] = isEdited
                                    },
                                    onNodeSaved = { nodeId ->
                                        savedNodes[nodeId] = true
                                        unsavedEditedNodes[nodeId] = false
                                        viewModel?.markNodeSaved(nodeId)
                                    },
                                    onPacketSaved = { packetId ->
                                        savedPackets[packetId] = true
                                        unsavedEditedPackets[packetId] = false
                                        viewModel?.markPacketSaved(packetId)
                                    },
                                    onTagSaved = { tagId ->
                                        savedTags[tagId] = true
                                        unsavedEditedTags[tagId] = false
                                        viewModel?.markTagSaved(tagId)
                                    },
                                    onRegisterSaveAction = { itemId, action ->
                                        activeSaveActions[itemId] = action
                                    },
                                    onBack = {
                                        scope.launch {
                                            navigator.navigateBack()
                                        }
                                    },
                                    onShowSnackbar = { message ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    },
                                    onFocusField = { label, valStr, clearAct ->
                                        activeFocusedLabel = label
                                        activeFocusedValue = valStr
                                        activeClearAction = clearAct
                                    }
                                )
                            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select an item to edit")
                            }
                        }
                    }
                }
            )
        }

        ImeValuePreviewBar(
            fieldName = activeFocusedLabel,
            currentValue = activeFocusedValue,
            onClear = {
                activeFocusedValue = ""
                activeClearAction?.invoke()
            },
            onDone = { focusManager.clearFocus() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showImportSourceDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isImporting) showImportSourceDialog = false
            },
            title = { Text("Select File to Import") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isImporting) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Crunching data and importing tags, please wait...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    var formatDropdownExpanded by remember { mutableStateOf(false) }
                    val formats = listOf("Click Plus", "Raw Data")

                    ExposedDropdownMenuBox(
                        expanded = formatDropdownExpanded && !isImporting,
                        onExpandedChange = {
                            if (!isImporting) formatDropdownExpanded = !formatDropdownExpanded
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tagFileFormat,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isImporting,
                            label = { Text("Format") },
                            trailingIcon = {
                                if (!isImporting) ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        if (!isImporting) {
                            ExposedDropdownMenu(
                                expanded = formatDropdownExpanded,
                                onDismissRequest = { formatDropdownExpanded = false }
                            ) {
                                formats.forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text(fmt) },
                                        onClick = {
                                            viewModel?.setTagFileFormat(fmt)
                                            formatDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("text/*", "text/csv", "application/csv", "*/*"))
                        },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse System Files...")
                    }

                    Text(
                        text = "Selected File Path:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val chosen = selectedCsvFile
                    if (chosen != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = chosen.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = chosen.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No file selected yet.\nTap 'Browse System Files...' to select a CSV file.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = selectedCsvFile
                        if (chosen != null) {
                            isImporting = true
                            if (chosen.uri != null) {
                                viewModel?.importAllTagsFromUri(context, chosen.uri) { res ->
                                    importResultState = res
                                    isImporting = false
                                    showImportSourceDialog = false
                                }
                            } else if (chosen.file != null && chosen.file.exists()) {
                                viewModel?.importAllTagsFromFile(context, chosen.file) { res ->
                                    importResultState = res
                                    isImporting = false
                                    showImportSourceDialog = false
                                }
                            } else if (chosen.path.isNotEmpty()) {
                                val f = File(chosen.path)
                                viewModel?.importAllTagsFromFile(context, f) { res ->
                                    importResultState = res
                                    isImporting = false
                                    showImportSourceDialog = false
                                }
                            } else {
                                isImporting = false
                                scope.launch { snackbarHostState.showSnackbar("Error: Selected CSV file does not exist.") }
                            }
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Please select a CSV file.") }
                        }
                    },
                    enabled = !isImporting && selectedCsvFile != null
                ) {
                    if (isImporting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Importing...")
                        }
                    } else {
                        Text("Import Selected File")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportSourceDialog = false },
                    enabled = !isImporting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportDialog) {
        var formatDropdownExpanded by remember { mutableStateOf(false) }
        val formats = listOf("Click Plus", "Raw Data")

        AlertDialog(
            onDismissRequest = { if (!isExporting) showExportDialog = false },
            title = { Text("Export Tags") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isExporting) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Exporting tag data ($tagFileFormat format), please wait...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    Text(
                        text = "Select the export file structure format.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ExposedDropdownMenuBox(
                        expanded = formatDropdownExpanded && !isExporting,
                        onExpandedChange = { if (!isExporting) formatDropdownExpanded = !formatDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = tagFileFormat,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isExporting,
                            label = { Text("Format") },
                            trailingIcon = {
                                if (!isExporting) ExposedDropdownMenuDefaults.TrailingIcon(expanded = formatDropdownExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        if (!isExporting) {
                            ExposedDropdownMenu(
                                expanded = formatDropdownExpanded,
                                onDismissRequest = { formatDropdownExpanded = false }
                            ) {
                                formats.forEach { fmt ->
                                    DropdownMenuItem(
                                        text = { Text(fmt) },
                                        onClick = {
                                            viewModel?.setTagFileFormat(fmt)
                                            formatDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExporting = true
                        viewModel?.exportAllTags { res ->
                            exportResultState = res
                            isExporting = false
                            showExportDialog = false
                        }
                    },
                    enabled = !isExporting
                ) {
                    if (isExporting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Exporting...")
                        }
                    } else {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Tags")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    enabled = !isExporting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    importResultState?.let { res ->
        AlertDialog(
            onDismissRequest = { importResultState = null },
            title = { Text(if (res.isSuccess) "Tag Import Successful" else "Tag Import Failed") },
            text = {
                if (res.isSuccess) {
                    Text("Successfully imported ${res.totalTagsImported} tag(s) across all packets!")
                } else {
                    Text("Failed to import tags.\n\nError: ${res.errorMessage ?: "Unknown error"}")
                }
            },
            confirmButton = {
                Button(onClick = { importResultState = null }) {
                    Text("OK")
                }
            }
        )
    }

    exportResultState?.let { res ->
        AlertDialog(
            onDismissRequest = { exportResultState = null },
            title = { Text(if (res.isSuccess) "Export Completed" else "Tag Export Failed") },
            text = {
                if (res.isSuccess) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tags exported successfully to public storage!")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("File Name: ${res.fileName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Saved Path:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(res.filePath, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Total Tags Exported: ${res.totalTagsExported}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    Text("Failed to export tags.\n\nError: ${res.errorMessage ?: "Unknown error"}")
                }
            },
            confirmButton = {
                Button(onClick = { exportResultState = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
fun ConfigureScreenPreview() {
    val mockNode = NodeEntity(id = 1, name = "Mock Node", ipAddress = "192.168.1.1")
    val mockPacket = PacketEntity(
        id = 1,
        parentNodeId = 1,
        name = "Mock Packet",
        description = "Desc",
        type = 1,
        period = 1000,
        config = "",
        offset = 0,
        slaveNode = 0,
        pollData = true,
        size = 10
    )
    val mockTag = TagEntity(
        id = 1,
        name = "Mock Tag",
        description = "",
        dataTypeId = 0,
        storedValue = "0",
        offset = 1
    )

    val hierarchy = listOf(
        NodeWithPacketsAndTags(
            node = mockNode,
            packetsWithTags = listOf(
                PacketWithTags(
                    packet = mockPacket,
                    tagsWithBitTags = listOf(
                        TagWithBitTags(
                            tag = mockTag,
                            bitTags = emptyList()
                        )
                    )
                )
            )
        )
    )

    MaterialTheme {
        ConfigureScreenContent(
            hierarchy = hierarchy,
            selectedItem = null,
            onOpenDrawer = {},
            onSelectItem = {},
            onAddNode = {},
            onUpdateNode = {},
            onDeleteNode = {},
            onAddPacket = {},
            onUpdatePacket = {},
            onDeletePacket = {},
            onAddTag = {},
            onUpdateTag = {},
            onDeleteTag = {}
        )
    }
}

fun getDataTypeBadgeColor(dataType: DataTypes?): Color? {
    if (dataType == null) return null
    return when (dataType.dataType.uppercase()) {
        "HEX", "UINT" -> Color(0xFF2196F3)       // Blue
        "INT" -> if (dataType.bytes == 4) Color(0xFF2E7D32) else Color(0xFF81C784)
        "FLOAT" -> Color(0xFF9C27B0)     // Purple
        else -> when (dataType.shortName.uppercase()) {
            "DH" -> Color(0xFF2196F3)    // Blue
            "DS" -> Color(0xFF81C784)    // Light Green
            "DD" -> Color(0xFF2E7D32)    // Green
            "DF" -> Color(0xFF9C27B0)    // Purple
            else -> Color.Gray
        }
    }
}

fun isTagValueValid(value: String, dataType: DataTypes?): Boolean {
    if (value.isBlank()) return true
    val v = value.toLongOrNull() ?: return value.toFloatOrNull() != null
    return isTagValueInRange(v, dataType)
}

fun getTagValueRangeDesc(dataType: DataTypes?): String {
    val kind = dataType?.dataType?.uppercase(Locale.US) ?: "INT"
    val shortName = dataType?.shortName?.uppercase(Locale.US) ?: "DS"
    val bytes = dataType?.bytes ?: 2

    return when {
        kind == "UINT" || shortName == "DH" -> if (bytes == 4) "0 to 4,294,967,295" else "0 to 65,535"
        kind == "INT" || shortName == "DS" -> if (bytes == 4) "-2,147,483,648 to 2,147,483,647" else "-32,768 to 32,767 (or 0 to 65,535)"
        else -> if (bytes == 4) "-2,147,483,648 to 2,147,483,647" else "0 to 65,535"
    }
}

fun isTagValueInRange(valNumber: Long, dataType: DataTypes?): Boolean {
    val kind = dataType?.dataType?.uppercase(Locale.US) ?: "INT"
    val shortName = dataType?.shortName?.uppercase(Locale.US) ?: "DS"
    val bytes = dataType?.bytes ?: 2

    return when {
        kind == "UINT" || shortName == "DH" -> {
            val maxVal = if (bytes == 4) 4294967295L else 65535L
            valNumber in 0L..maxVal
        }
        bytes == 4 -> valNumber in -2147483648L..2147483647L
        else -> { // 16-bit INT (e.g. DS)
            valNumber in -32768L..65535L
        }
    }
}

fun filterTagInput(input: String, dataType: DataTypes?): String {
    val kind = dataType?.dataType?.uppercase() ?: "INT"
    return when (kind) {
        "FLOAT" -> {
            var hasDot = false
            input.filterIndexed { index, c ->
                if (c == '-' && index == 0) true
                else if (c == '.' && !hasDot) {
                    hasDot = true
                    true
                } else c.isDigit()
            }
        }
        "UINT" -> input.filter { it.isDigit() }
        else -> { // INT
            input.filterIndexed { index, c ->
                if (c == '-' && index == 0) true else c.isDigit()
            }
        }
    }
}

fun getTagKeyboardOptions(dataType: DataTypes?): KeyboardOptions {
    val kind = dataType?.dataType?.uppercase() ?: "INT"
    return when (kind) {
        "FLOAT" -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
        else -> KeyboardOptions(keyboardType = KeyboardType.Number)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModbusDebugDialog(
    debugInfo: ModbusDebugInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (debugInfo.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                    contentDescription = null,
                    tint = if (debugInfo.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(debugInfo.title, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Node: ${debugInfo.nodeName} (${debugInfo.ipAddress}:${debugInfo.port})", fontWeight = FontWeight.Bold)
                        Text("Packet: ${debugInfo.packetName}")
                        Text("DataType: ${debugInfo.dataType} (defaultModbusAddress: ${debugInfo.defaultModbusAddress})")
                        Text("Packet Offset: ${debugInfo.packetOffset}")
                        val formulaText = if (debugInfo.dataTypeObj?.isZeroBasedAddressing == true) {
                            "${debugInfo.defaultModbusAddress} + (${debugInfo.packetOffset} * bytes / 2)"
                        } else {
                            "${debugInfo.defaultModbusAddress} + ((${debugInfo.packetOffset} - 1) * bytes / 2)"
                        }
                        Text("Formula: $formulaText", style = MaterialTheme.typography.bodySmall)
                        Text("Calculated Modbus Address: ${debugInfo.calculatedModbusAddress}", fontWeight = FontWeight.SemiBold)
                        Text("Wire Register Offset (FC 03/16): ${debugInfo.wireRegisterOffset} (0x${Integer.toHexString(debugInfo.wireRegisterOffset).uppercase()})")
                        Text("Slave Node (Unit ID): ${debugInfo.slaveNode}")
                        Text("Quantity Registers: ${debugInfo.quantityRegisters}")
                    }
                }

                if (debugInfo.errorMessage != null) {
                    Text(
                        text = "Status: ${debugInfo.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("Status: SUCCESS", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }

                Text("Request Packet (Hex):", fontWeight = FontWeight.Bold)
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = debugInfo.requestHex,
                        color = Color(0xFF00FF00),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Text("Response Packet (Hex):", fontWeight = FontWeight.Bold)
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = debugInfo.responseHex ?: "None (No Response / Timeout)",
                        color = if (debugInfo.responseHex != null) Color(0xFF00FF00) else Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                val rawOrder = debugInfo.byteOrder?.trim() ?: "CDAB (3412) — Word-Swap"
                val displayByteOrder = ModbusByteOrder.fromLabel(rawOrder).label

                Text("Byte Order", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = displayByteOrder,
                    onValueChange = {},
                    readOnly = true,
                    enabled = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (debugInfo.registersText != null) {
                    Text("Parsed Registers:", fontWeight = FontWeight.Bold)
                    Text(debugInfo.registersText, style = MaterialTheme.typography.bodyMedium)
                }

                if (debugInfo.tagsText != null) {
                    Text("Parsed Tags:", fontWeight = FontWeight.Bold)
                    Text(debugInfo.tagsText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun CompactTagDataField(
    value: String,
    onValueChange: (String) -> Unit,
    dataType: DataTypes?,
    tagName: String = "",
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var localValue by remember(value) { mutableStateOf(value) }
    val isValid = remember(localValue, dataType) { isTagValueValid(localValue, dataType) }
    val fieldLabel = tagName.ifEmpty { "Tag Data" }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun normalizeEmptyToZero() {
        if (localValue.isBlank()) {
            localValue = "0"
            onValueChange("0")
        }
    }

    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            normalizeEmptyToZero()
        }
    }

    val doClear: () -> Unit = {
        localValue = ""
        onValueChange("")
        onFocusField(fieldLabel, "", {})
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(115.dp)
            .height(32.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = if (!isValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(start = 8.dp, end = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = localValue,
                onValueChange = { newValue ->
                    val filtered = filterTagInput(newValue, dataType)
                    localValue = filtered
                    onValueChange(filtered)
                    onFocusField(fieldLabel, filtered, doClear)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = getTagKeyboardOptions(dataType),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocusField(fieldLabel, localValue, doClear)
                        } else {
                            normalizeEmptyToZero()
                        }
                    }
            )

            if (localValue.isNotEmpty()) {
                IconButton(
                    onClick = doClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear field",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TreeList(
    hierarchy: List<NodeWithPacketsAndTags>,
    selectedItem: SelectedItem?,
    dataTypes: List<DataTypes> = emptyList(),
    nodeConnectionStates: Map<Long, NodeConnectionState> = emptyMap(),
    isPollingEnabled: Boolean = false,
    unsavedEditedNodes: Map<Long, Boolean> = emptyMap(),
    unsavedEditedPackets: Map<Long, Boolean> = emptyMap(),
    unsavedEditedTags: Map<Long, Boolean> = emptyMap(),
    savedNodes: Map<Long, Boolean> = emptyMap(),
    savedPackets: Map<Long, Boolean> = emptyMap(),
    savedTags: Map<Long, Boolean> = emptyMap(),
    newlyCreatedNodeIds: Set<Long> = emptySet(),
    newlyCreatedPacketIds: Set<Long> = emptySet(),
    newlyCreatedTagIds: Set<Long> = emptySet(),
    activeSaveActions: Map<Long, () -> Unit> = emptyMap(),
    viewModel: MainViewModel? = null,
    onItemSelected: (SelectedItem) -> Unit,
    onAddNode: () -> Unit = {},
    onAddPacket: (NodeEntity) -> Unit,
    onAddTag: (PacketEntity) -> Unit,
    onDeleteNode: (NodeEntity) -> Unit,
    onConnectNode: (NodeEntity) -> Unit = {},
    onReadPacket: (PacketEntity) -> Unit = {},
    onWritePacket: (PacketEntity) -> Unit = {},
    onReadTag: (TagEntity) -> Unit = {},
    onWriteTag: (TagEntity) -> Unit = {},
    onDeletePacket: (PacketEntity) -> Unit,
    onDeleteTag: (TagEntity) -> Unit,
    onUpdateTag: (TagEntity) -> Unit = {},
    onDeleteBitTag: (BitTags) -> Unit = {},
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val expandedNodesMapState = viewModel?.expandedNodesMap?.collectAsStateWithLifecycle()
    val expandedNodes = expandedNodesMapState?.value ?: emptyMap()

    val expandedPacketsMapState = viewModel?.expandedPacketsMap?.collectAsStateWithLifecycle()
    val expandedPackets = expandedPacketsMapState?.value ?: emptyMap()

    val expandedTagsMapState = viewModel?.expandedTagsMap?.collectAsStateWithLifecycle()
    val expandedTags = expandedTagsMapState?.value ?: emptyMap()

    var itemToDelete by remember { mutableStateOf<SelectedItem?>(null) }

    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    LaunchedEffect(selectedItem) {
        when (selectedItem) {
            is SelectedItem.Node -> {
                viewModel?.setNodeExpanded(selectedItem.node.id, true)
            }
            is SelectedItem.Packet -> {
                viewModel?.setNodeExpanded(selectedItem.packet.parentNodeId, true)
            }
            is SelectedItem.Tag -> {
                val parentPacket = hierarchy.flatMap { it.packetsWithTags }
                    .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == selectedItem.tag.id } }
                if (parentPacket != null) {
                    viewModel?.setNodeExpanded(parentPacket.packet.parentNodeId, true)
                    viewModel?.setPacketExpanded(parentPacket.packet.id, true)
                }
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
        if (hierarchy.isEmpty()) {
            item(key = "no_nodes_banner") {
                val bannerTransition = rememberInfiniteTransition(label = "noNodesBlink")
                val bannerAlpha by bannerTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bannerAlpha"
                )

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = bannerAlpha * 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "No Nodes Defined",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Tap the '+' button above to add your first PLC node.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = { onAddNode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = bannerAlpha)
                            )
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add Node")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Node")
                        }
                    }
                }
            }
        }
        hierarchy.forEach { nodeWithPackets ->
            val node = nodeWithPackets.node
            val nodeDisplayText = if (node.ipAddress.isNotBlank()) "${node.name} - (${node.ipAddress})" else node.name
            val nodeState = nodeConnectionStates[node.id] ?: NodeConnectionState.NOT_CONNECTED
            val bellColor = when (nodeState) {
                NodeConnectionState.NOT_CONNECTED -> Color.Gray
                NodeConnectionState.CONNECTING -> Color(0xFFFFC107)
                NodeConnectionState.COMMUNICATION_OK -> Color(0xFF4CAF50)
                NodeConnectionState.CONNECTION_FAILURE -> Color(0xFFF44336)
            }

            val isDefaultNewNode = (node.id in newlyCreatedNodeIds) && savedNodes[node.id] != true
            val hasNodeUnsavedEdits = unsavedEditedNodes[node.id] == true

            val isNodeBlinking = isDefaultNewNode || hasNodeUnsavedEdits
            val nodeHighlightColor = when {
                isDefaultNewNode -> Color(0xFFFF9800) // Warning Orange for default "NodeX"
                hasNodeUnsavedEdits -> Color(0xFFE91E63) // Magenta/Pink for unsaved edits
                else -> null
            }
            val nodeHighlightTextColor = when {
                isDefaultNewNode -> Color(0xFFE65100)
                hasNodeUnsavedEdits -> Color(0xFF880E4F)
                else -> null
            }

            val isNoPacketsInNode = nodeWithPackets.packetsWithTags.isEmpty()

            item(key = "node_${node.id}") {
                TreeItemRow(
                    text = nodeDisplayText,
                    level = 0,
                    isExpanded = expandedNodes[node.id] ?: false,
                    isSelected = (selectedItem as? SelectedItem.Node)?.node?.id == node.id,
                    isBlinking = isNodeBlinking,
                    isAddBlinking = isNoPacketsInNode,
                    highlightColor = nodeHighlightColor,
                    highlightTextColor = nodeHighlightTextColor,
                    onSave = {
                        onItemSelected(SelectedItem.Node(node))
                        activeSaveActions[node.id]?.invoke()
                    },
                    onToggleExpand = { viewModel?.toggleNodeExpanded(node.id) },
                    onSelect = { onItemSelected(SelectedItem.Node(node)) },
                    onConnect = { onConnectNode(node) },
                    nodeConnectionState = nodeState,
                    onAdd = {
                        viewModel?.setNodeExpanded(node.id, true)
                        onAddPacket(node)
                    },
                    onDelete = { itemToDelete = SelectedItem.Node(node) },
                    hasChildren = nodeWithPackets.packetsWithTags.isNotEmpty(),
                    leadingIcon = Icons.Rounded.Notifications,
                    leadingIconTint = bellColor
                )
            }

            if (expandedNodes[node.id] == true) {
                nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                    val packet = packetWithTags.packet
                    val packetDataType = packet.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
                        ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                        ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
                    val packetShortName = packetDataType.shortName.ifEmpty { packetDataType.description }
                    val packetBadgeText = "$packetShortName${packet.offset}"
                    val badgeColor = getDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784)

                    val isDefaultNewPacket = (packet.id in newlyCreatedPacketIds) && savedPackets[packet.id] != true
                    val hasUnsavedEdits = unsavedEditedPackets[packet.id] == true

                    val isBlinking = isDefaultNewPacket || hasUnsavedEdits
                    val highlightColor = when {
                        isDefaultNewPacket -> Color(0xFFFF9800) // Warning Orange for default "PacketX"
                        hasUnsavedEdits -> Color(0xFFE91E63)    // Magenta/Pink for unsaved edits
                        else -> null
                    }
                    val highlightTextColor = when {
                        isDefaultNewPacket -> Color(0xFFE65100)
                        hasUnsavedEdits -> Color(0xFF880E4F)
                        else -> null
                    }

                    item(key = "packet_${packet.id}") {
                        TreeItemRow(
                            text = packet.name,
                            level = 1,
                            isExpanded = expandedPackets[packet.id] ?: false,
                            isSelected = (selectedItem as? SelectedItem.Packet)?.packet?.id == packet.id,
                            isBlinking = isBlinking,
                            highlightColor = highlightColor,
                            highlightTextColor = highlightTextColor,
                            onSave = {
                                onItemSelected(SelectedItem.Packet(packet))
                                activeSaveActions[packet.id]?.invoke()
                            },
                            onToggleExpand = { viewModel?.togglePacketExpanded(packet.id) },
                            onSelect = { onItemSelected(SelectedItem.Packet(packet)) },
                            onAdd = null,
                            onRead = { onReadPacket(packet) },
                            onWrite = { onWritePacket(packet) },
                            onDelete = { itemToDelete = SelectedItem.Packet(packet) },
                            hasChildren = packetWithTags.tagsWithBitTags.isNotEmpty(),
                            badgeText = packetBadgeText,
                            badgeColor = badgeColor,
                            leadingIcon = Icons.Rounded.AccountTree,
                            leadingIconTint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (expandedPackets[packet.id] == true) {
                        packetWithTags.tagsWithBitTags.forEach { tagWithBitTags ->
                            val tag = tagWithBitTags.tag
                            val hasBitTags = tagWithBitTags.bitTags.isNotEmpty()
                            val tagDataType = dataTypes.find { it.id == tag.dataTypeId } ?: packetDataType
                            val tagShortName = tagDataType.shortName.ifEmpty { tagDataType.description }
                            val tagBadgeText = "$tagShortName${tag.offset}"
                            val tagBadgeColor = getDataTypeBadgeColor(tagDataType) ?: Color(0xFF81C784)

                            val isDefaultNewTag = (tag.id in newlyCreatedTagIds) && savedTags[tag.id] != true
                            val hasTagUnsavedEdits = unsavedEditedTags[tag.id] == true

                            val isTagBlinking = isDefaultNewTag || hasTagUnsavedEdits
                            val tagHighlightColor = when {
                                isDefaultNewTag -> Color(0xFFFF9800)
                                hasTagUnsavedEdits -> Color(0xFFE91E63)
                                else -> null
                            }
                            val tagHighlightTextColor = when {
                                isDefaultNewTag -> Color(0xFFE65100)
                                hasTagUnsavedEdits -> Color(0xFF880E4F)
                                else -> null
                            }

                            item(key = "tag_${tag.id}") {
                                TreeItemRow(
                                    text = tag.name,
                                    level = 2,
                                    isExpanded = expandedTags[tag.id] ?: false,
                                    isSelected = (selectedItem as? SelectedItem.Tag)?.tag?.id == tag.id,
                                    isBlinking = isTagBlinking,
                                    highlightColor = tagHighlightColor,
                                    highlightTextColor = tagHighlightTextColor,
                                    onSave = {
                                        onItemSelected(SelectedItem.Tag(tag))
                                        activeSaveActions[tag.id]?.invoke()
                                    },
                                    onToggleExpand = { viewModel?.toggleTagExpanded(tag.id) },
                                    onSelect = { onItemSelected(SelectedItem.Tag(tag)) },
                                    onRead = null,
                                    onWrite = null,
                                    onDelete = null,
                                    hasChildren = hasBitTags,
                                    badgeText = tagBadgeText,
                                    badgeColor = tagBadgeColor,
                                    tagValue = tag.storedValue,
                                    dataType = tagDataType,
                                    tagName = tag.name,
                                    onTagValueChange = { newValue -> onUpdateTag(tag.copy(storedValue = newValue)) },
                                    onFocusField = onFocusField
                                )
                            }

                            if (expandedTags[tag.id] == true && hasBitTags) {
                                val parentTagNum = tag.storedValue.toLongOrNull() ?: 0L
                                items(tagWithBitTags.bitTags, key = { "bitTag_${it.id}" }) { bitTag ->
                                    val bitIndex = bitTag.bitIndex
                                    val isBitSet = (parentTagNum and (1L shl bitIndex)) != 0L

                                    TreeItemRow(
                                        text = bitTag.name,
                                        level = 3,
                                        isExpanded = false,
                                        isSelected = (selectedItem as? SelectedItem.BitTag)?.bitTag?.id == bitTag.id,
                                        onToggleExpand = {},
                                        onSelect = { onItemSelected(SelectedItem.BitTag(bitTag, tag)) },
                                        onDelete = null,
                                        hasChildren = false,
                                        badgeText = "${tagShortName}${tag.offset}:${bitTag.bitIndex}",
                                        badgeColor = Color(0xFF00BCD4),
                                        bitValue = isBitSet,
                                        onBitValueChange = { newBitState ->
                                            val updatedNum = if (newBitState) {
                                                parentTagNum or (1L shl bitIndex)
                                            } else {
                                                parentTagNum and (1L shl bitIndex).inv()
                                            }

                                            val newStoredValue = when (tagDataType.dataType.uppercase()) {
                                                "UINT" -> (updatedNum and 0xFFFFL).toString()
                                                "FLOAT" -> updatedNum.toString()
                                                else -> if (tagDataType.bytes == 4) updatedNum.toInt().toString() else updatedNum.toShort().toString()
                                            }

                                            onUpdateTag(tag.copy(storedValue = newStoredValue))
                                        }
                                    )
                                }
                            }
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

    itemToDelete?.let { item ->
        val title = when (item) {
            is SelectedItem.Node -> "Delete Node"
            is SelectedItem.Packet -> "Delete Packet"
            is SelectedItem.Tag -> "Delete Tag"
            is SelectedItem.BitTag -> "Delete Bit Tag"
        }
        val message = when (item) {
            is SelectedItem.Node -> "Are you sure you want to delete node \"${item.node.name}\"?"
            is SelectedItem.Packet -> "Are you sure you want to delete packet \"${item.packet.name}\"?"
            is SelectedItem.Tag -> "Are you sure you want to delete tag \"${item.tag.name}\"?"
            is SelectedItem.BitTag -> "Are you sure you want to delete bit tag \"${item.bitTag.name}\"?"
        }

        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (item) {
                            is SelectedItem.Node -> onDeleteNode(item.node)
                            is SelectedItem.Packet -> onDeletePacket(item.packet)
                            is SelectedItem.Tag -> onDeleteTag(item.tag)
                            is SelectedItem.BitTag -> onDeleteBitTag(item.bitTag)
                        }
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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
fun TreeItemRow(
    text: String,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    isBlinking: Boolean = false,
    isAddBlinking: Boolean = false,
    highlightColor: Color? = null,
    highlightTextColor: Color? = null,
    onSave: (() -> Unit)? = null,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onRead: (() -> Unit)? = null,
    onWrite: (() -> Unit)? = null,
    onConnect: (() -> Unit)? = null,
    nodeConnectionState: NodeConnectionState? = null,
    hasChildren: Boolean,
    badgeText: String? = null,
    badgeColor: Color? = null,
    offsetText: String? = null,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color? = null,
    tagValue: String? = null,
    dataType: DataTypes? = null,
    tagName: String = "",
    onTagValueChange: ((String) -> Unit)? = null,
    bitValue: Boolean? = null,
    onBitValueChange: ((Boolean) -> Unit)? = null,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    val isCompact = level >= 3
    val rowHeight = if (isCompact) 24.dp else if (level == 2) 44.dp else 48.dp
    val textStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
    val iconButtonModifier = if (isCompact) Modifier.size(18.dp) else Modifier
    val iconModifier = if (isCompact) Modifier.size(14.dp) else Modifier
    val startIndent = if (isCompact) (level * 16).dp else (level * 20).dp

    val isAnyBlinking = isBlinking || isAddBlinking
    val blinkAlpha = if (isAnyBlinking) {
        val infiniteTransition = rememberInfiniteTransition(label = "blinkTransition")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blinkAlpha"
        )
        alpha
    } else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .padding(start = startIndent, end = 4.dp)
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleExpand,
                modifier = iconButtonModifier
            ) {
                if (hasChildren) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = iconModifier
                    )
                }
            }

            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = "Communication Status",
                    tint = leadingIconTint ?: Color.Gray,
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(20.dp)
                )
            }

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

            if (isBlinking && highlightColor != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = highlightColor.copy(alpha = blinkAlpha * 0.35f),
                    border = BorderStroke(1.5.dp, highlightColor.copy(alpha = blinkAlpha)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = text,
                        style = textStyle,
                        fontWeight = FontWeight.Bold,
                        color = (highlightTextColor ?: highlightColor).copy(alpha = blinkAlpha),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = text,
                    style = textStyle,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Connect button for Node header
            if (onConnect != null) {
                val state = nodeConnectionState ?: NodeConnectionState.NOT_CONNECTED
                val (buttonBgColor, buttonTextColor) = when (state) {
                    NodeConnectionState.NOT_CONNECTED -> Color(0xFF757575) to Color.White
                    NodeConnectionState.CONNECTING -> Color(0xFFFFC107) to Color.Black
                    NodeConnectionState.COMMUNICATION_OK -> Color(0xFF4CAF50) to Color.White
                    NodeConnectionState.CONNECTION_FAILURE -> Color(0xFFF44336) to Color.White
                }

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonTextColor
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .padding(end = 4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Connect",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Boolean indicator for BitTag rows
            if (onBitValueChange != null && bitValue != null) {
                Checkbox(
                    checked = bitValue,
                    onCheckedChange = onBitValueChange,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 4.dp)
                )
            }

            // Edit box for Tag data value
            if (onTagValueChange != null && tagValue != null) {
                CompactTagDataField(
                    value = tagValue,
                    onValueChange = onTagValueChange,
                    dataType = dataType,
                    tagName = tagName,
                    onFocusField = onFocusField,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            if (onRead != null || onWrite != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    onRead?.let { action ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier
                                .height(28.dp)
                                .width(56.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Read", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    onWrite?.let { action ->
                        OutlinedButton(
                            onClick = action,
                            modifier = Modifier
                                .height(28.dp)
                                .width(56.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Write", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            // Save button on flashing line (on the right side) - always shown when blinking
            if (isBlinking) {
                Button(
                    onClick = {
                        onSave?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = highlightColor ?: MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .padding(end = 4.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            onAdd?.let {
                val addAlpha = if (isAddBlinking) blinkAlpha else 1f
                IconButton(
                    onClick = it,
                    modifier = iconButtonModifier.then(if (isAddBlinking) Modifier.alpha(addAlpha) else Modifier)
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Add",
                        tint = if (isAddBlinking) MaterialTheme.colorScheme.primary.copy(alpha = addAlpha) else LocalContentColor.current,
                        modifier = iconModifier
                    )
                }
            }
            onDelete?.let {
                IconButton(
                    onClick = it,
                    modifier = iconButtonModifier
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        modifier = iconModifier
                    )
                }
            }
        }
    }
}

@Composable
fun EditForm(
    item: SelectedItem,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    dataTypes: List<DataTypes> = emptyList(),
    isPacketSaved: Boolean = false,
    viewModel: MainViewModel? = null,
    onUpdateNode: (NodeEntity) -> Unit,
    onUpdatePacket: (PacketEntity) -> Unit,
    onUpdateTag: (TagEntity) -> Unit,
    onUpdateBitTag: (BitTags) -> Unit = {},
    onNodeEditStateChange: (Long, Boolean) -> Unit = { _, _ -> },
    onPacketEditStateChange: (Long, Boolean) -> Unit = { _, _ -> },
    onTagEditStateChange: (Long, Boolean) -> Unit = { _, _ -> },
    onNodeSaved: (Long) -> Unit = {},
    onPacketSaved: (Long) -> Unit = {},
    onTagSaved: (Long) -> Unit = {},
    onRegisterSaveAction: (Long, () -> Unit) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(item) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    var isNodeEdited by remember(item) { mutableStateOf(false) }
    var nodeSaveAction by remember(item) { mutableStateOf<(() -> Unit)?>(null) }

    var isPacketEdited by remember(item) { mutableStateOf(false) }
    var packetSaveAction by remember(item) { mutableStateOf<(() -> Unit)?>(null) }

    var isTagEdited by remember(item) { mutableStateOf(false) }
    var tagSaveAction by remember(item) { mutableStateOf<(() -> Unit)?>(null) }

    var isBitTagEdited by remember(item) { mutableStateOf(false) }
    var bitTagSaveAction by remember(item) { mutableStateOf<(() -> Unit)?>(null) }

    val packetTags = remember(hierarchy, item) {
        if (item is SelectedItem.Packet) {
            hierarchy.flatMap { node -> node.packetsWithTags }
                .find { it.packet.id == item.packet.id }
                ?.tagsWithBitTags?.map { it.tag } ?: emptyList()
        } else emptyList()
    }

    val parentPacket = remember(hierarchy, item) {
        if (item is SelectedItem.Tag) {
            hierarchy.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == item.tag.id } }
                ?.packet
        } else null
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val canFormScrollDown by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(4.dp)
                .verticalScroll(scrollState)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (item) {
                    is SelectedItem.Node -> "Edit Node"
                    is SelectedItem.Packet -> "Edit Packet"
                    is SelectedItem.Tag -> "Edit Tag"
                    is SelectedItem.BitTag -> "Edit Bit Tag"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))

        when (item) {
            is SelectedItem.Node -> NodeForm(
                node = item.node,
                hierarchy = hierarchy,
                viewModel = viewModel,
                onUpdate = onUpdateNode,
                onShowSnackbar = onShowSnackbar,
                onEditStateChange = { isEdited ->
                    isNodeEdited = isEdited
                    onNodeEditStateChange(item.node.id, isEdited)
                },
                onNodeSaved = onNodeSaved,
                registerSaveAction = { action ->
                    nodeSaveAction = action
                    onRegisterSaveAction(item.node.id, action)
                },
                onFocusField = onFocusField
            )
            is SelectedItem.Packet -> PacketForm(
                packet = item.packet,
                hierarchy = hierarchy,
                dataTypes = dataTypes,
                packetTags = packetTags,
                isSaved = isPacketSaved,
                viewModel = viewModel,
                onUpdate = onUpdatePacket,
                onShowSnackbar = onShowSnackbar,
                onEditStateChange = { isEdited ->
                    isPacketEdited = isEdited
                    onPacketEditStateChange(item.packet.id, isEdited)
                },
                onPacketSaved = onPacketSaved,
                registerSaveAction = { action ->
                    packetSaveAction = action
                    onRegisterSaveAction(item.packet.id, action)
                },
                onFocusField = onFocusField
            )
            is SelectedItem.Tag -> TagForm(
                tag = item.tag,
                hierarchy = hierarchy,
                parentPacket = parentPacket,
                dataTypes = dataTypes,
                onUpdate = onUpdateTag,
                onShowSnackbar = onShowSnackbar,
                onEditStateChange = { isEdited ->
                    isTagEdited = isEdited
                    onTagEditStateChange(item.tag.id, isEdited)
                },
                onTagSaved = onTagSaved,
                registerSaveAction = { action ->
                    tagSaveAction = action
                    onRegisterSaveAction(item.tag.id, action)
                },
                onFocusField = onFocusField
            )
            is SelectedItem.BitTag -> BitTagForm(
                bitTag = item.bitTag,
                parentTag = item.parentTag,
                onUpdate = onUpdateBitTag,
                onShowSnackbar = onShowSnackbar,
                onEditStateChange = { isBitTagEdited = it },
                registerSaveAction = { action ->
                    bitTagSaveAction = action
                    onRegisterSaveAction(item.bitTag.id, action)
                },
                onFocusField = onFocusField
            )
        }
        }

        ScrollMoreDownIndicator(
            canScrollMore = canFormScrollDown,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ClearIconButton(
    text: String,
    onClear: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    if (text.isNotEmpty()) {
        IconButton(
            onClick = {
                onClear()
                focusRequester?.requestFocus()
                keyboardController?.show()
            },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Clear text",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun NodeForm(
    node: NodeEntity,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    viewModel: MainViewModel? = null,
    onUpdate: (NodeEntity) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onEditStateChange: (Boolean) -> Unit,
    onNodeSaved: (Long) -> Unit = {},
    registerSaveAction: (() -> Unit) -> Unit,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    var name by remember(node.id) { mutableStateOf(node.name) }
    var ipAddress by remember(node.id) { mutableStateOf(node.ipAddress) }
    var isEdited by remember(node.id) { mutableStateOf(false) }

    val nameFocusRequester = remember { FocusRequester() }
    val ipFocusRequester = remember { FocusRequester() }

    fun markEdited() {
        isEdited = true
        onEditStateChange(true)
    }

    val ipRegex = remember { Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$") }
    val isIpValid = remember(ipAddress) { ipAddress.trim().matches(ipRegex) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val performSave = remember(node, name, ipAddress, hierarchy) {
        {
            keyboardController?.hide()
            focusManager.clearFocus()

            val trimmedName = name.trim()
            val trimmedIp = ipAddress.trim()
            val isDuplicate = hierarchy.any { it.node.id != node.id && it.node.name.trim().equals(trimmedName, ignoreCase = true) }

            if (trimmedName.isEmpty()) {
                onShowSnackbar("Warning: Name field cannot be empty.")
            } else if (isDuplicate) {
                onShowSnackbar("Warning: Node name '$trimmedName' already exists. Node names must be unique.")
            } else if (!trimmedIp.matches(ipRegex)) {
                onShowSnackbar("Warning: Invalid IP address format (e.g., 192.168.1.1).")
            } else {
                onUpdate(node.copy(name = trimmedName, ipAddress = trimmedIp))
                isEdited = false
                onEditStateChange(false)
                onNodeSaved(node.id)
                onShowSnackbar("Information saved successfully.")
            }
        }
    }

    LaunchedEffect(performSave) {
        registerSaveAction(performSave)
    }

    // Sync local state when the prop changes (e.g. from DB)
    LaunchedEffect(node) {
        if (name != node.name) name = node.name
        if (ipAddress != node.ipAddress) ipAddress = node.ipAddress
        isEdited = false
        onEditStateChange(false)
    }

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { newValue ->
                    name = newValue
                    markEdited()
                    onFocusField("Node Name", newValue) {
                        name = ""
                        markEdited()
                        onFocusField("Node Name", "", {})
                        nameFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Name", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = {
                    ClearIconButton(
                        text = name,
                        onClear = {
                            name = ""
                            markEdited()
                            onFocusField("Node Name", "", {})
                        },
                        focusRequester = nameFocusRequester
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(nameFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Node Name", name) {
                                name = ""
                                markEdited()
                                onFocusField("Node Name", "", {})
                                nameFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = ipAddress,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { char -> char.isDigit() || char == '.' }
                    ipAddress = filtered
                    markEdited()
                    onFocusField("IP Address", filtered) {
                        ipAddress = ""
                        markEdited()
                        onFocusField("IP Address", "", {})
                        ipFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("IP Address", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = {
                    ClearIconButton(
                        text = ipAddress,
                        onClear = {
                            ipAddress = ""
                            markEdited()
                            onFocusField("IP Address", "", {})
                        },
                        focusRequester = ipFocusRequester
                    )
                },
                isError = !isIpValid,
                supportingText = if (!isIpValid) { { Text("Invalid IP", style = MaterialTheme.typography.labelSmall) } } else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(ipFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("IP Address", ipAddress) {
                                ipAddress = ""
                                markEdited()
                                onFocusField("IP Address", "", {})
                                ipFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                )
            )
        }
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name: String? = null
    try {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) name = cursor.getString(index)
                }
            }
        }
    } catch (_: Exception) {}
    if (name.isNullOrEmpty()) {
        name = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected_File.csv"
    }
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketForm(
    packet: PacketEntity,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    dataTypes: List<DataTypes> = emptyList(),
    packetTags: List<TagEntity> = emptyList(),
    isSaved: Boolean = false,
    viewModel: MainViewModel? = null,
    onUpdate: (PacketEntity) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onEditStateChange: (Boolean) -> Unit,
    onPacketSaved: (Long) -> Unit = {},
    registerSaveAction: (() -> Unit) -> Unit,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var availableCsvFiles by remember { mutableStateOf<List<CsvFileInfo>>(emptyList()) }
    var selectedCsvFile by remember { mutableStateOf<CsvFileInfo?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val displayPath = uri.path ?: uri.toString()
            val pickedInfo = CsvFileInfo(
                file = if (uri.path != null) File(uri.path!!) else null,
                name = fileName,
                dirPath = "System File Picker",
                path = displayPath,
                sizeText = "Available",
                dateText = "System Selected",
                isSample = false,
                uri = uri
            )

            val existing = availableCsvFiles.find { it.path == pickedInfo.path || (it.uri != null && it.uri == uri) }
            if (existing != null) {
                selectedCsvFile = existing
            } else {
                availableCsvFiles = listOf(pickedInfo) + availableCsvFiles
                selectedCsvFile = pickedInfo
            }
        }
    }
    val defaultIntTypeId = remember(dataTypes) {
        dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }?.id?.toString() ?: "0"
    }

    var name by remember(packet.id) { mutableStateOf(packet.name) }
    var description by remember(packet.id) { mutableStateOf(packet.description) }
    var type by remember(packet.id) { mutableStateOf(packet.type?.toString() ?: defaultIntTypeId) }
    var period by remember(packet.id) { mutableStateOf(packet.period.toString()) }
    var config by remember(packet.id) { mutableStateOf(packet.config) }
    var offset by remember(packet.id) { mutableStateOf(packet.offset.toString()) }
    var size by remember(packet.id) { mutableStateOf(packet.size.toString()) }
    var slaveNode by remember(packet.id) { mutableStateOf(packet.slaveNode.toString()) }
    var pollData by remember(packet.id) { mutableStateOf(packet.pollData) }

    var isEdited by remember(packet.id) { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    fun markEdited() {
        isEdited = true
        onEditStateChange(true)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible) {
            if (offset.isBlank()) { offset = "1"; markEdited() }
            if (size.isBlank()) { size = "1"; markEdited() }
            if (slaveNode.isBlank()) { slaveNode = "1"; markEdited() }
            if (period.isBlank()) { period = "1000"; markEdited() }
        }
    }

    val selectedDataType = remember(type, dataTypes) {
        type.toIntOrNull()?.let { id -> dataTypes.find { it.id == id.toLong() } }
    }
    val typeDisplayText = selectedDataType?.let { "${it.shortName} (${it.description})" }
        ?: if (type.isEmpty()) "Select Type" else type

    val isPeriodValid = remember(period) {
        val p = period.toIntOrNull()
        p != null && p in 100..60000
    }
    val isZeroBased = selectedDataType?.isZeroBasedAddressing == true
    val isOffsetValid = remember(offset, isZeroBased) {
        val o = offset.toIntOrNull()
        if (isZeroBased) o != null && o in 0..2000 else o != null && o in 1..2000
    }
    val isSizeValid = remember(size) {
        val s = size.toIntOrNull()
        s != null && s in 1..50
    }
    val isSlaveNodeValid = remember(slaveNode) {
        val sn = slaveNode.toIntOrNull()
        sn != null && sn in 1..200
    }

    var showConfirmationDialog by remember { mutableStateOf(false) }

    fun executeSave() {
        keyboardController?.hide()
        focusManager.clearFocus()

        val trimmedName = name.trim()
        val parsedType = if (type.isBlank()) null else type.toIntOrNull()
        val parsedPeriod = period.toIntOrNull()
        val parsedOffset = offset.toIntOrNull()
        val parsedSlaveNode = slaveNode.toIntOrNull()
        val parsedSize = size.toIntOrNull()

        onUpdate(
            packet.copy(
                name = trimmedName,
                description = description.trim(),
                type = parsedType,
                period = parsedPeriod ?: packet.period,
                config = config.trim(),
                offset = parsedOffset ?: packet.offset,
                slaveNode = parsedSlaveNode ?: packet.slaveNode,
                pollData = pollData,
                size = parsedSize ?: packet.size
            )
        )
        isEdited = false
        onEditStateChange(false)
        onPacketSaved(packet.id)
        onShowSnackbar("Packet saved successfully.")
    }

    val performSave = remember(packet, name, description, type, period, config, offset, slaveNode, pollData, size, hierarchy) {
        {
            keyboardController?.hide()
            focusManager.clearFocus()

            val trimmedName = name.trim()
            val parsedType = if (type.isBlank()) null else type.toIntOrNull()
            val isTypeValid = parsedType != null && dataTypes.any { it.id == parsedType.toLong() }
            val parsedSize = size.toIntOrNull()

            val allPackets = hierarchy.flatMap { it.packetsWithTags }.map { it.packet }
            val isDuplicate = allPackets.any { it.id != packet.id && it.name.trim().equals(trimmedName, ignoreCase = true) }

            when {
                trimmedName.isEmpty() -> {
                    onShowSnackbar("Error: Packet name cannot be empty.")
                }
                isDuplicate -> {
                    onShowSnackbar("Error: Packet name '$trimmedName' already exists. Packet names must be unique.")
                }
                !isTypeValid -> {
                    onShowSnackbar("Error: Please select a valid Data Type for the packet.")
                }
                !isOffsetValid -> {
                    onShowSnackbar("Error: Offset must be between 1 and 2000.")
                }
                !isSizeValid -> {
                    onShowSnackbar("Error: Size must be between 1 and 50.")
                }
                !isSlaveNodeValid -> {
                    onShowSnackbar("Error: Slave Node must be between 1 and 200.")
                }
                pollData && !isPeriodValid -> {
                    onShowSnackbar("Error: Period must be between 100 and 60000 ms.")
                }
                parsedSize != null && parsedSize < packet.size -> {
                    showConfirmationDialog = true
                }
                else -> {
                    executeSave()
                }
            }
        }
    }

    LaunchedEffect(performSave) {
        registerSaveAction(performSave)
    }

    // Sync local state when the prop changes
    LaunchedEffect(packet) {
        if (name != packet.name) name = packet.name
        if (description != packet.description) description = packet.description
        val expectedType = packet.type?.toString() ?: defaultIntTypeId
        if (type != expectedType) type = expectedType
        if (period != packet.period.toString()) period = packet.period.toString()
        if (config != packet.config) config = packet.config
        if (offset != packet.offset.toString()) offset = packet.offset.toString()
        if (slaveNode != packet.slaveNode.toString()) slaveNode = packet.slaveNode.toString()
        if (pollData != packet.pollData) pollData = packet.pollData
        if (size != packet.size.toString()) size = packet.size.toString()
        isEdited = false
        onEditStateChange(false)
    }

    val isNameInvalid = name.trim().isEmpty() || name.trim().equals("New Packet", ignoreCase = true)

    val nameFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }
    val configFocusRequester = remember { FocusRequester() }
    val offsetFocusRequester = remember { FocusRequester() }
    val sizeFocusRequester = remember { FocusRequester() }
    val slaveNodeFocusRequester = remember { FocusRequester() }
    val periodFocusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    markEdited()
                    onFocusField("Packet Name", it) {
                        name = ""
                        markEdited()
                        nameFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Name", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = { ClearIconButton(text = name, onClear = { name = ""; markEdited() }, focusRequester = nameFocusRequester) },
                isError = isNameInvalid,
                supportingText = if (name.trim().equals("New Packet", ignoreCase = true)) {
                    { Text("Choose unique name", style = MaterialTheme.typography.labelSmall) }
                } else if (name.trim().isEmpty()) {
                    { Text("Name empty", style = MaterialTheme.typography.labelSmall) }
                } else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(nameFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Packet Name", name) {
                                name = ""
                                markEdited()
                                nameFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    markEdited()
                    onFocusField("Description", it) {
                        description = ""
                        markEdited()
                        descFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Description", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = { ClearIconButton(text = description, onClear = { description = ""; markEdited() }, focusRequester = descFocusRequester) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(descFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Description", description) {
                                description = ""
                                markEdited()
                                descFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val isTypeEditable = !isSaved

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded && isTypeEditable,
                onExpandedChange = {
                    if (isTypeEditable) {
                        dropdownExpanded = !dropdownExpanded
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = typeDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    enabled = isTypeEditable,
                    label = { Text(if (isTypeEditable) "Type" else "Type (Locked)", style = MaterialTheme.typography.labelSmall) },
                    textStyle = MaterialTheme.typography.bodySmall,
                    trailingIcon = {
                        if (isTypeEditable) {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        }
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                if (isTypeEditable) {
                    val uniqueDataTypes = remember(dataTypes) {
                        dataTypes.distinctBy { "${it.shortName}_${it.description}".uppercase() }
                    }
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        if (uniqueDataTypes.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No data types available") },
                                onClick = { dropdownExpanded = false }
                            )
                        } else {
                            uniqueDataTypes.forEach { dataType ->
                                val displayLabel = "${dataType.shortName} (${dataType.description})"
                                DropdownMenuItem(
                                    text = { Text(displayLabel) },
                                    onClick = {
                                        type = dataType.id.toString()
                                        markEdited()
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = config,
                onValueChange = {
                    config = it
                    markEdited()
                    onFocusField("Config", it) {
                        config = ""
                        markEdited()
                        configFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Config", style = MaterialTheme.typography.labelSmall) },
                textStyle = MaterialTheme.typography.bodySmall,
                trailingIcon = { ClearIconButton(text = config, onClear = { config = ""; markEdited() }, focusRequester = configFocusRequester) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(configFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Config", config) {
                                config = ""
                                markEdited()
                                configFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = offset,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { char -> char.isDigit() }
                    offset = filtered
                    markEdited()
                    onFocusField("Offset", filtered) {
                        offset = ""
                        markEdited()
                        offsetFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Offset") },
                trailingIcon = { ClearIconButton(text = offset, onClear = { offset = ""; markEdited() }, focusRequester = offsetFocusRequester) },
                isError = !isOffsetValid,
                supportingText = if (!isOffsetValid) { { Text(if (isZeroBased) "Offset: 0-2000" else "Offset: 1-2000") } } else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(offsetFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Offset", offset) {
                                offset = ""
                                markEdited()
                                offsetFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = size,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { char -> char.isDigit() }
                    size = filtered
                    markEdited()
                    onFocusField("Size", filtered) {
                        size = ""
                        markEdited()
                        sizeFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Size") },
                trailingIcon = { ClearIconButton(text = size, onClear = { size = ""; markEdited() }, focusRequester = sizeFocusRequester) },
                isError = !isSizeValid,
                supportingText = if (!isSizeValid) { { Text("Size: 1-50") } } else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(sizeFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Size", size) {
                                size = ""
                                markEdited()
                                sizeFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = slaveNode,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { char -> char.isDigit() }
                    slaveNode = filtered
                    markEdited()
                    onFocusField("Slave Node", filtered) {
                        slaveNode = ""
                        markEdited()
                        slaveNodeFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Slave Node") },
                trailingIcon = { ClearIconButton(text = slaveNode, onClear = { slaveNode = ""; markEdited() }, focusRequester = slaveNodeFocusRequester) },
                isError = !isSlaveNodeValid,
                supportingText = if (!isSlaveNodeValid) { { Text("Slave Node: 1-200") } } else null,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(slaveNodeFocusRequester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            onFocusField("Slave Node", slaveNode) {
                                slaveNode = ""
                                markEdited()
                                slaveNodeFocusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            AnimatedVisibility(visible = pollData, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = period,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { char -> char.isDigit() }
                        period = filtered
                        markEdited()
                        onFocusField("Period (ms)", filtered) {
                            period = ""
                            markEdited()
                            periodFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    label = { Text("Period (ms)") },
                    trailingIcon = { ClearIconButton(text = period, onClear = { period = ""; markEdited() }, focusRequester = periodFocusRequester) },
                    isError = pollData && !isPeriodValid,
                    supportingText = if (pollData && !isPeriodValid) { { Text("Period: 100-60000ms") } } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(periodFocusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                onFocusField("Period (ms)", period) {
                                    period = ""
                                    markEdited()
                                    periodFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 2.dp)
        ) {
            Checkbox(
                checked = pollData,
                onCheckedChange = {
                    pollData = it
                    markEdited()
                }
            )
            Text("pollData", style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Confirm Tag Erase") },
            text = { Text("This action will erase existing tags, are you sure that you want to proceed?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        executeSave()
                    }
                ) {
                    Text("Proceed", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TagForm(
    tag: TagEntity,
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    parentPacket: PacketEntity? = null,
    dataTypes: List<DataTypes> = emptyList(),
    onUpdate: (TagEntity) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onEditStateChange: (Boolean) -> Unit,
    onTagSaved: (Long) -> Unit = {},
    registerSaveAction: (() -> Unit) -> Unit,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    var name by remember(tag.id) { mutableStateOf(tag.name) }
    var description by remember(tag.id) { mutableStateOf(tag.description) }
    var storedValue by remember(tag.id) { mutableStateOf(tag.storedValue) }
    var offset by remember(tag.id) { mutableStateOf(tag.offset.toString()) }

    var isEdited by remember(tag.id) { mutableStateOf(false) }

    fun markEdited() {
        isEdited = true
        onEditStateChange(true)
    }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(isImeVisible) {
        if (!isImeVisible && storedValue.isBlank()) {
            storedValue = "0"
            markEdited()
        }
    }

    val tagDataType = remember(tag.dataTypeId, dataTypes, parentPacket) {
        dataTypes.find { it.id == tag.dataTypeId }
            ?: parentPacket?.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
            ?: dataTypes.firstOrNull()
    }
    val typeDisplayText = tagDataType?.let { "${it.shortName} (${it.description})" } ?: "DS (Data Register Short)"

    val performSave = remember(
        tag, name, description, storedValue, offset, hierarchy
    ) {
        {
            keyboardController?.hide()
            focusManager.clearFocus()

            val trimmedName = name.trim()
            val parsedOffset = offset.toIntOrNull()

            val allTags = hierarchy.flatMap { it.packetsWithTags }.flatMap { it.tagsWithBitTags }.map { it.tag }
            val isDuplicate = allTags.any { it.id != tag.id && it.name.trim().equals(trimmedName, ignoreCase = true) }

            val isZeroBasedTag = tagDataType?.isZeroBasedAddressing == true
            val isTagOffsetValid = parsedOffset != null && (if (isZeroBasedTag) parsedOffset in 0..2000 else parsedOffset in 1..2000)

            when {
                trimmedName.isEmpty() -> {
                    onShowSnackbar("Error: Tag name cannot be empty.")
                }
                isDuplicate -> {
                    onShowSnackbar("Error: Tag name '$trimmedName' already exists. Tag names must be unique.")
                }
                !isTagOffsetValid -> {
                    onShowSnackbar("Error: Offset must be a valid number (${if (isZeroBasedTag) "0-2000" else "1-2000"}).")
                }
                else -> {
                    onUpdate(
                        tag.copy(
                            name = trimmedName,
                            description = description.trim(),
                            storedValue = storedValue.trim(),
                            offset = parsedOffset
                        )
                    )
                    isEdited = false
                    onEditStateChange(false)
                    onTagSaved(tag.id)
                    onShowSnackbar("Tag saved successfully.")
                }
            }
        }
    }

    LaunchedEffect(performSave) {
        registerSaveAction(performSave)
    }

    // Sync local state when the prop changes
    LaunchedEffect(tag) {
        if (name != tag.name) name = tag.name
        if (description != tag.description) description = tag.description
        if (storedValue != tag.storedValue) storedValue = tag.storedValue
        if (offset != tag.offset.toString()) offset = tag.offset.toString()
        isEdited = false
        onEditStateChange(false)
    }

    val nameFocusRequester = remember { FocusRequester() }
    val descFocusRequester = remember { FocusRequester() }
    val storedValueFocusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                markEdited()
                onFocusField("Tag Name", it) {
                    name = ""
                    markEdited()
                    nameFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            label = { Text("Name") },
            trailingIcon = { ClearIconButton(text = name, onClear = { name = ""; markEdited() }, focusRequester = nameFocusRequester) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
                .onFocusChanged { if (it.isFocused) onFocusField("Tag Name", name) { name = ""; markEdited(); nameFocusRequester.requestFocus(); keyboardController?.show() } },
            singleLine = true
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = typeDisplayText,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("DataType") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            OutlinedTextField(
                value = offset,
                onValueChange = {
                    val filtered = it.filter { char -> char.isDigit() }
                    offset = filtered
                    markEdited()
                },
                label = { Text("Offset") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = storedValue,
                onValueChange = {
                    storedValue = it
                    markEdited()
                    onFocusField(tag.name.ifEmpty { "Stored Value" }, it) {
                        storedValue = ""
                        markEdited()
                        storedValueFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Stored Value") },
                trailingIcon = { ClearIconButton(text = storedValue, onClear = { storedValue = ""; markEdited() }, focusRequester = storedValueFocusRequester) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(storedValueFocusRequester)
                    .onFocusChanged { if (it.isFocused) onFocusField(tag.name.ifEmpty { "Stored Value" }, storedValue) { storedValue = ""; markEdited(); storedValueFocusRequester.requestFocus(); keyboardController?.show() } },
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    markEdited()
                    onFocusField("Description", it) {
                        description = ""
                        markEdited()
                        descFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
                label = { Text("Description") },
                trailingIcon = { ClearIconButton(text = description, onClear = { description = ""; markEdited() }, focusRequester = descFocusRequester) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(descFocusRequester)
                    .onFocusChanged { if (it.isFocused) onFocusField("Description", description) { description = ""; markEdited(); descFocusRequester.requestFocus(); keyboardController?.show() } },
                singleLine = true
            )
        }
    }
}

@Composable
fun BitTagForm(
    bitTag: BitTags,
    parentTag: TagEntity? = null,
    onUpdate: (BitTags) -> Unit,
    onShowSnackbar: (String) -> Unit,
    onEditStateChange: (Boolean) -> Unit,
    registerSaveAction: (() -> Unit) -> Unit,
    onFocusField: (label: String, value: String, clearAction: () -> Unit) -> Unit = { _, _, _ -> }
) {
    var name by remember(bitTag.id) { mutableStateOf(bitTag.name) }
    var isEdited by remember(bitTag.id) { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun markEdited() {
        isEdited = true
        onEditStateChange(true)
    }

    val performSave = remember(bitTag, name) {
        {
            keyboardController?.hide()
            focusManager.clearFocus()

            val trimmedName = name.trim()

            when {
                trimmedName.isEmpty() -> {
                    onShowSnackbar("Error: Bit Tag name cannot be empty.")
                }
                else -> {
                    onUpdate(bitTag.copy(name = trimmedName))
                    isEdited = false
                    onEditStateChange(false)
                    onShowSnackbar("Bit Tag saved successfully.")
                }
            }
        }
    }

    LaunchedEffect(performSave) {
        registerSaveAction(performSave)
    }

    // Sync local state when the prop changes
    LaunchedEffect(bitTag) {
        if (name != bitTag.name) name = bitTag.name
        isEdited = false
        onEditStateChange(false)
    }

    val nameFocusRequester = remember { FocusRequester() }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                markEdited()
                onFocusField("Bit Tag Name", it) {
                    name = ""
                    markEdited()
                    nameFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            },
            label = { Text("Name") },
            trailingIcon = { ClearIconButton(text = name, onClear = { name = ""; markEdited() }, focusRequester = nameFocusRequester) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { performSave() }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        onFocusField("Bit Tag Name", name) {
                            name = ""
                            markEdited()
                            nameFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    } else if (isEdited) {
                        performSave()
                    }
                }
        )

        OutlinedTextField(
            value = bitTag.bitIndex.toString(),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Bit Index") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = parentTag?.name ?: "Tag #${bitTag.parentTagId}",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Parent Tag") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}
