package com.example.ailisttest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.CustomGroup
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.data.local.GraphicsScreenItemWithTag
import com.example.ailisttest.data.local.GraphicsScreenItems
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.ScreenWithGraphicsItems
import com.example.ailisttest.data.local.Screens
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import kotlinx.coroutines.launch
import kotlin.math.abs

sealed class SelectedGraphicsListItem {
    data class Screen(val screen: Screens) : SelectedGraphicsListItem()
    data class Item(val itemWithTag: GraphicsScreenItemWithTag, val parentScreen: Screens) : SelectedGraphicsListItem()
    data class CustomGroupItem(val group: CustomGroup, val itemWithTag: GraphicsScreenItemWithTag, val parentScreen: Screens) : SelectedGraphicsListItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GraphicsScreensConfigScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val graphicsScreensWithItems by viewModel.graphicsScreensWithItems.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var selectedItem by remember { mutableStateOf<SelectedGraphicsListItem?>(null) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()

    // For editing blank canvas screen
    var editingBlankCanvasScreen by remember { mutableStateOf<Screens?>(null) }
    var itemToDelete by remember { mutableStateOf<SelectedGraphicsListItem?>(null) }
    var itemForInfoDialog by remember { mutableStateOf<Pair<GraphicsScreenItemWithTag, Screens>?>(null) }

    BackHandler(enabled = editingBlankCanvasScreen != null) {
        editingBlankCanvasScreen = null
    }

    BackHandler(enabled = editingBlankCanvasScreen == null && navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
        scope.launch { navigator.navigateBack() }
    }

    val baseDirective = navigator.scaffoldDirective
    val customDirective = remember(baseDirective) {
        baseDirective.copy(defaultPanePreferredWidth = 450.dp)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    if (editingBlankCanvasScreen != null) {
        // Blank Canvas Screen with Selection Rectangles & Layout Editor
        GraphicsBlankEditCanvasScreen(
            screen = editingBlankCanvasScreen!!,
            viewModel = viewModel,
            onBack = { editingBlankCanvasScreen = null }
        )
    } else {
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
                                title = { Text("Configure Graphics Screens") },
                                navigationIcon = {
                                    IconButton(onClick = onOpenDrawer) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel.addGraphicsScreen(onShowSnackbar = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            })
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Add Graphics Screen")
                                    }
                                }
                            )

                            if (graphicsScreensWithItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Dashboard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No Graphics Screens Configured",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap the '+' button above to add a new Graphics Screen.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                GraphicsScreensTreeList(
                                    graphicsScreensWithItems = graphicsScreensWithItems,
                                    hierarchy = hierarchy,
                                    dataTypes = dataTypes,
                                    viewModel = viewModel,
                                    selectedItem = selectedItem,
                                    onItemSelected = { item ->
                                        selectedItem = item
                                        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                                    },
                                    onItemClick = { screen, itemWithTag ->
                                        itemForInfoDialog = Pair(itemWithTag, screen)
                                    },
                                    onEditCanvas = { screen ->
                                        editingBlankCanvasScreen = screen
                                    },
                                    onDeleteScreen = { screen -> itemToDelete = SelectedGraphicsListItem.Screen(screen) },
                                    onDeleteItem = { screen, itemWithTag -> itemToDelete = SelectedGraphicsListItem.Item(itemWithTag, screen) }
                                )
                            }
                        }
                    }
                },
                detailPane = {
                    AnimatedPane(modifier = Modifier.fillMaxSize()) {
                        selectedItem?.let { item ->
                            GraphicsScreenDetailPane(
                                item = item,
                                graphicsScreensWithItems = graphicsScreensWithItems,
                                viewModel = viewModel,
                                onUpdateScreen = { updated ->
                                    viewModel.updateScreen(updated)
                                    selectedItem = SelectedGraphicsListItem.Screen(updated)
                                },
                                onEditCanvas = { screen ->
                                    editingBlankCanvasScreen = screen
                                },
                                onDeleteItemWithSelection = { target ->
                                    if (target != null) itemToDelete = target
                                },
                                onBack = { scope.launch { navigator.navigateBack() } },
                                onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a Graphics Screen or item from the list to view and configure details.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        itemToDelete?.let { item ->
            val (titleText, bodyText) = when (item) {
                is SelectedGraphicsListItem.Screen -> Pair("Delete Graphics Screen", "Are you sure you want to delete graphics screen \"${item.screen.Name}\"?")
                is SelectedGraphicsListItem.CustomGroupItem -> Pair("Delete Group", "Are you sure you want to delete group \"${item.group.groupName}\"?")
                is SelectedGraphicsListItem.Item -> Pair("Delete Item", "Are you sure you want to delete this graphics item?")
            }

            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(titleText) },
                text = { Text(bodyText) },
                confirmButton = {
                    Button(
                        onClick = {
                            when (item) {
                                is SelectedGraphicsListItem.Screen -> {
                                    viewModel.deleteScreen(item.screen)
                                    scope.launch { snackbarHostState.showSnackbar("Deleted graphics screen '${item.screen.Name}'.") }
                                }
                                is SelectedGraphicsListItem.Item -> {
                                    viewModel.deleteGraphicsScreenItem(item.itemWithTag.item)
                                    scope.launch { snackbarHostState.showSnackbar("Deleted item.") }
                                }
                                is SelectedGraphicsListItem.CustomGroupItem -> {
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

        // Item Info Popup Dialog Box (When clicking an existing item in Configure Graphics Screens)
        itemForInfoDialog?.let { (itemWithTag, parentScreen) ->
            val graphicsItem = itemWithTag.item
            val tag = itemWithTag.tag

            AlertDialog(
                onDismissRequest = { itemForInfoDialog = null },
                title = { Text("Graphics Item Info") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = tag?.name ?: "Item #${graphicsItem.id}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Display Type: ${graphicsItem.DisplayType.ifEmpty { "Rectangle" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Parent Screen:", fontWeight = FontWeight.SemiBold)
                                    Text(parentScreen.Name, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Position (X, Y):", fontWeight = FontWeight.SemiBold)
                                    Text("(${graphicsItem.OffsetX.toInt()} px, ${graphicsItem.OffsetY.toInt()} px)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Size (Width x Height):", fontWeight = FontWeight.SemiBold)
                                    Text("${graphicsItem.Width.toInt()} x ${graphicsItem.Height.toInt()} px", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                if (tag != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Mapped Tag:", fontWeight = FontWeight.SemiBold)
                                        Text(tag.name, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteGraphicsScreenItem(graphicsItem)
                            scope.launch { snackbarHostState.showSnackbar("Deleted item #${graphicsItem.id}.") }
                            itemForInfoDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Item")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemForInfoDialog = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun GraphicsScreensTreeList(
    graphicsScreensWithItems: List<ScreenWithGraphicsItems>,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    viewModel: MainViewModel,
    selectedItem: SelectedGraphicsListItem?,
    onItemSelected: (SelectedGraphicsListItem) -> Unit,
    onItemClick: (Screens, GraphicsScreenItemWithTag) -> Unit,
    onEditCanvas: (Screens) -> Unit,
    onDeleteScreen: (Screens) -> Unit,
    onDeleteItem: (Screens, GraphicsScreenItemWithTag) -> Unit
) {
    val listState = rememberLazyListState()
    val expandedScreens by viewModel.expandedScreensMap.collectAsStateWithLifecycle()

    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            graphicsScreensWithItems.forEach { screenWithItems ->
                val screen = screenWithItems.screen
                val isExpanded = expandedScreens[screen.id] ?: false
                val isSelected = (selectedItem as? SelectedGraphicsListItem.Screen)?.screen?.id == screen.id

                item(key = "graphics_screen_${screen.id}") {
                    GraphicsScreenRowHeader(
                        screen = screen,
                        itemCount = screenWithItems.items.size,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        onToggleExpand = { viewModel.toggleScreenExpanded(screen.id) },
                        onSelect = { onItemSelected(SelectedGraphicsListItem.Screen(screen)) },
                        onEditCanvas = { onEditCanvas(screen) },
                        onDelete = { onDeleteScreen(screen) }
                    )
                }

                if (isExpanded) {
                    items(screenWithItems.items, key = { "graphics_item_${it.item.id}" }) { itemWithTag ->
                        val isItemMatches = (selectedItem as? SelectedGraphicsListItem.Item)?.itemWithTag?.item?.id == itemWithTag.item.id
                        val item = itemWithTag.item
                        val tag = itemWithTag.tag

                        val displayTitle = tag?.name ?: "Item #${item.id}"

                        GraphicsScreenItemRow(
                            text = displayTitle,
                            subtitle = "Pos: (${item.OffsetX.toInt()}, ${item.OffsetY.toInt()}) • Size: ${item.Width.toInt()}x${item.Height.toInt()}",
                            level = 1,
                            isSelected = isItemMatches,
                            onSelect = {
                                onItemSelected(SelectedGraphicsListItem.Item(itemWithTag, screen))
                                onItemClick(screen, itemWithTag)
                            },
                            onDelete = { onDeleteItem(screen, itemWithTag) }
                        )
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
fun GraphicsScreenRowHeader(
    screen: Screens,
    itemCount: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onEditCanvas: () -> Unit,
    onDelete: () -> Unit
) {
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
                .padding(horizontal = 8.dp)
                .height(52.dp)
        ) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                imageVector = Icons.Rounded.Dashboard,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = screen.Name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Graphics Screen ($itemCount items)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Prominent "Edit" Button on the Header
            FilledTonalButton(
                onClick = onEditCanvas,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Edit Canvas",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun GraphicsScreenItemRow(
    text: String,
    subtitle: String? = null,
    level: Int = 1,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
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
                .height(44.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sell,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(18.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
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

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsScreenDetailPane(
    item: SelectedGraphicsListItem,
    graphicsScreensWithItems: List<ScreenWithGraphicsItems>,
    viewModel: MainViewModel,
    onUpdateScreen: (Screens) -> Unit,
    onEditCanvas: (Screens) -> Unit,
    onDeleteItemWithSelection: (SelectedGraphicsListItem?) -> Unit = {},
    onBack: () -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    when (item) {
        is SelectedGraphicsListItem.Screen -> {
            var name by remember(item.screen.id) { mutableStateOf(item.screen.Name) }
            val focusRequester = remember { FocusRequester() }

            val trimmedName = name.trim()
            val isNameEmpty = trimmedName.isEmpty()
            val isDuplicateName = graphicsScreensWithItems.any {
                it.screen.id != item.screen.id && it.screen.Name.trim().equals(trimmedName, ignoreCase = true)
            }
            val isNameInvalid = isNameEmpty || isDuplicateName
            var isEdited by remember(item.screen.id, name) {
                mutableStateOf(item.screen.Name.trim() != trimmedName)
            }

            val currentScreenIdx = graphicsScreensWithItems.indexOfFirst { it.screen.id == item.screen.id }
            val canMoveUp = currentScreenIdx > 0
            val canMoveDown = currentScreenIdx >= 0 && currentScreenIdx < graphicsScreensWithItems.size - 1

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
                        text = "Edit Graphics Screen",
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

                // Prominent "Edit Canvas Layout" Button in Details Pane
                Button(
                    onClick = { onEditCanvas(item.screen) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DesignServices,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Canvas Layout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Actions & Reordering Card
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
                                    viewModel.addGraphicsScreenAbove(item.screen, onShowSnackbar = { msg -> onShowSnackbar(msg) })
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
                                    viewModel.addGraphicsScreenBelow(item.screen, onShowSnackbar = { msg -> onShowSnackbar(msg) })
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
                                    onDeleteItemWithSelection(SelectedGraphicsListItem.Screen(item.screen))
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
                    onValueChange = { name = it },
                    label = { Text("Screen Name") },
                    isError = isNameInvalid,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Graphics Item Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("Select an item to view or edit its canvas coordinates and properties.")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsBlankEditCanvasScreen(
    screen: Screens,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val graphicsScreensWithItems by viewModel.graphicsScreensWithItems.collectAsStateWithLifecycle()
    val screenWithItems = remember(graphicsScreensWithItems, screen.id) {
        graphicsScreensWithItems.find { it.screen.id == screen.id }
    }
    val itemsList = remember(screenWithItems) {
        screenWithItems?.items?.map { it.item } ?: emptyList()
    }

    var activeItemId by remember { mutableStateOf<Long?>(null) }
    var debugItemForDialog by remember { mutableStateOf<GraphicsScreenItems?>(null) }

    // Transient in-memory rectangle during active dragging for instant 60/120 FPS responsiveness
    var draggingRectOverride by remember { mutableStateOf<GraphicsScreenItems?>(null) }

    // In-progress creation gesture states
    var isDrawingNewRect by remember { mutableStateOf(false) }
    var dragStartPoint by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPoint by remember { mutableStateOf<Offset?>(null) }

    // Active item moving/resizing drag gesture states
    var isMovingActiveItem by remember { mutableStateOf(false) }
    var activeGripIndex by remember { mutableIntStateOf(-1) } // 0: TL, 1: TR, 2: BL, 3: BR
    var activeItemStartRect by remember { mutableStateOf<GraphicsScreenItems?>(null) }
    var touchOffsetInRect by remember { mutableStateOf(Offset.Zero) }
    var hasDraggedSinceTouch by remember { mutableStateOf(false) }

    // Pulsing/Blinking Infinite Transition for Active Selection Border
    val infiniteTransition = rememberInfiniteTransition(label = "active_border_blink")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_alpha"
    )

    val activeItemFromDb = remember(itemsList, activeItemId) {
        itemsList.find { it.id == activeItemId }
    }
    val displayActiveItem = draggingRectOverride ?: activeItemFromDb

    val currentItemsList by rememberUpdatedState(itemsList)
    val currentActiveItemFromDb by rememberUpdatedState(activeItemFromDb)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(screen.Name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Canvas Layout Editor — ${itemsList.size} item(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to list")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .pointerInput(screen.id) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            // Short click/tap: Select item (show grips & blinking border)
                            val clickedItem = currentItemsList.findLast { item ->
                                val upperLeftX = item.OffsetX
                                val upperLeftY = item.OffsetY
                                val lowerRightX = item.OffsetX + item.Width
                                val lowerRightY = item.OffsetY + item.Height

                                tapOffset.x in upperLeftX..lowerRightX && tapOffset.y in upperLeftY..lowerRightY
                            }

                            if (clickedItem != null) {
                                activeItemId = clickedItem.id
                            } else {
                                activeItemId = null
                            }
                        },
                        onLongPress = { touchOffset ->
                            // Long-press: Bring up the Item Info Popup Dialog Box!
                            val longPressedItem = currentItemsList.findLast { item ->
                                val upperLeftX = item.OffsetX
                                val upperLeftY = item.OffsetY
                                val lowerRightX = item.OffsetX + item.Width
                                val lowerRightY = item.OffsetY + item.Height

                                touchOffset.x in upperLeftX..lowerRightX && touchOffset.y in upperLeftY..lowerRightY
                            }

                            if (longPressedItem != null) {
                                activeItemId = longPressedItem.id
                                debugItemForDialog = longPressedItem
                            }
                        }
                    )
                }
                .pointerInput(screen.id) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            hasDraggedSinceTouch = false
                            val active = draggingRectOverride ?: currentActiveItemFromDb

                            if (active != null) {
                                val left = active.OffsetX
                                val top = active.OffsetY
                                val right = active.OffsetX + active.Width
                                val bottom = active.OffsetY + active.Height
                                val gripRadius = 32.dp.toPx()

                                val tlGrip = Offset(left, top)
                                val trGrip = Offset(right, top)
                                val blGrip = Offset(left, bottom)
                                val brGrip = Offset(right, bottom)

                                when {
                                    (startOffset - tlGrip).getDistance() <= gripRadius -> {
                                        activeGripIndex = 0
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = startOffset
                                        return@detectDragGestures
                                    }
                                    (startOffset - trGrip).getDistance() <= gripRadius -> {
                                        activeGripIndex = 1
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = startOffset
                                        return@detectDragGestures
                                    }
                                    (startOffset - blGrip).getDistance() <= gripRadius -> {
                                        activeGripIndex = 2
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = startOffset
                                        return@detectDragGestures
                                    }
                                    (startOffset - brGrip).getDistance() <= gripRadius -> {
                                        activeGripIndex = 3
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = startOffset
                                        return@detectDragGestures
                                    }
                                }

                                // Check if touch inside active rectangle body (Moving mode)
                                if (startOffset.x in left..right && startOffset.y in top..bottom) {
                                    isMovingActiveItem = true
                                    activeItemStartRect = active.copy()
                                    draggingRectOverride = active.copy()
                                    touchOffsetInRect = startOffset - Offset(left, top)
                                    dragStartPoint = startOffset
                                    return@detectDragGestures
                                }
                            }

                            // Touch outside active rectangle -> check if touch inside any existing item
                            val clickedExisting = currentItemsList.findLast { item ->
                                val l = item.OffsetX
                                val t = item.OffsetY
                                val r = item.OffsetX + item.Width
                                val b = item.OffsetY + item.Height
                                startOffset.x in l..r && startOffset.y in t..b
                            }

                            if (clickedExisting != null) {
                                activeItemId = clickedExisting.id
                                isMovingActiveItem = true
                                activeItemStartRect = clickedExisting.copy()
                                draggingRectOverride = clickedExisting.copy()
                                touchOffsetInRect = startOffset - Offset(clickedExisting.OffsetX, clickedExisting.OffsetY)
                                dragStartPoint = startOffset
                            } else {
                                // Touch outside all items -> Start creating new selection rectangle
                                activeItemId = null
                                draggingRectOverride = null
                                isDrawingNewRect = true
                                dragStartPoint = startOffset
                                dragCurrentPoint = startOffset
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            hasDraggedSinceTouch = true
                            val pos = change.position
                            val startRect = activeItemStartRect

                            if (activeGripIndex >= 0 && startRect != null) {
                                // Direct finger-to-corner tracking for grips
                                val sL = startRect.OffsetX
                                val sT = startRect.OffsetY
                                val sR = startRect.OffsetX + startRect.Width
                                val sB = startRect.OffsetY + startRect.Height

                                var nL = sL
                                var nT = sT
                                var nW = startRect.Width
                                var nH = startRect.Height

                                when (activeGripIndex) {
                                    0 -> { // Top-Left Grip
                                        nL = minOf(pos.x, sR - 10f).coerceAtLeast(0f)
                                        nT = minOf(pos.y, sB - 10f).coerceAtLeast(0f)
                                        nW = sR - nL
                                        nH = sB - nT
                                    }
                                    1 -> { // Top-Right Grip
                                        val nR = maxOf(pos.x, sL + 10f)
                                        nT = minOf(pos.y, sB - 10f).coerceAtLeast(0f)
                                        nW = nR - sL
                                        nH = sB - nT
                                        nL = sL
                                    }
                                    2 -> { // Bottom-Left Grip
                                        nL = minOf(pos.x, sR - 10f).coerceAtLeast(0f)
                                        val nB = maxOf(pos.y, sT + 10f)
                                        nW = sR - nL
                                        nH = nB - sT
                                        nT = sT
                                    }
                                    3 -> { // Bottom-Right Grip
                                        val nR = maxOf(pos.x, sL + 10f)
                                        val nB = maxOf(pos.y, sT + 10f)
                                        nW = nR - sL
                                        nH = nB - sT
                                        nL = sL
                                        nT = sT
                                    }
                                }

                                draggingRectOverride = startRect.copy(
                                    OffsetX = nL,
                                    OffsetY = nT,
                                    Width = nW,
                                    Height = nH
                                )
                            } else if (isMovingActiveItem && startRect != null) {
                                // Direct touch tracking for moving active rectangle
                                val newL = (pos.x - touchOffsetInRect.x).coerceAtLeast(0f)
                                val newT = (pos.y - touchOffsetInRect.y).coerceAtLeast(0f)

                                draggingRectOverride = startRect.copy(
                                    OffsetX = newL,
                                    OffsetY = newT
                                )
                            } else if (isDrawingNewRect) {
                                dragCurrentPoint = pos
                            }
                        },
                        onDragEnd = {
                            val start = dragStartPoint
                            val current = dragCurrentPoint
                            val dragged = draggingRectOverride

                            if (hasDraggedSinceTouch) {
                                // User dragged during gesture -> update or insert item
                                if (dragged != null && (isMovingActiveItem || activeGripIndex >= 0)) {
                                    viewModel.updateGraphicsScreenItem(dragged)
                                } else if (isDrawingNewRect && start != null && current != null) {
                                    val left = minOf(start.x, current.x).coerceAtLeast(0f)
                                    val top = minOf(start.y, current.y).coerceAtLeast(0f)
                                    val width = abs(current.x - start.x)
                                    val height = abs(current.y - start.y)

                                    if (width >= 10f && height >= 10f) {
                                        val newRect = GraphicsScreenItems(
                                            parentScreenId = screen.id,
                                            DisplayType = "Rectangle",
                                            OffsetX = left,
                                            OffsetY = top,
                                            Width = width,
                                            Height = height
                                        )
                                        viewModel.insertGraphicsScreenItem(newRect) { newId ->
                                            activeItemId = newId
                                        }
                                    }
                                }
                            }

                            // Reset gesture states
                            draggingRectOverride = null
                            isDrawingNewRect = false
                            isMovingActiveItem = false
                            activeGripIndex = -1
                            dragStartPoint = null
                            dragCurrentPoint = null
                            activeItemStartRect = null
                        },
                        onDragCancel = {
                            draggingRectOverride = null
                            isDrawingNewRect = false
                            isMovingActiveItem = false
                            activeGripIndex = -1
                            dragStartPoint = null
                            dragCurrentPoint = null
                            activeItemStartRect = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val activeId = displayActiveItem?.id

                // 1. Draw Inactive Selection Rectangles
                itemsList.filter { it.id != activeId }.forEach { item ->
                    val rectOffset = Offset(item.OffsetX, item.OffsetY)
                    val rectSize = Size(item.Width, item.Height)

                    drawRect(
                        color = Color(0xFF2196F3).copy(alpha = 0.15f),
                        topLeft = rectOffset,
                        size = rectSize
                    )
                    drawRect(
                        color = Color(0xFF2196F3),
                        topLeft = rectOffset,
                        size = rectSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 2. Draw In-Progress Creation Rectangle
                if (isDrawingNewRect && dragStartPoint != null && dragCurrentPoint != null) {
                    val s = dragStartPoint!!
                    val c = dragCurrentPoint!!
                    val l = minOf(s.x, c.x)
                    val t = minOf(s.y, c.y)
                    val w = abs(c.x - s.x)
                    val h = abs(c.y - s.y)

                    val rectOffset = Offset(l, t)
                    val rectSize = Size(w, h)

                    drawRect(
                        color = Color(0xFFFF9800).copy(alpha = 0.2f),
                        topLeft = rectOffset,
                        size = rectSize
                    )
                    drawRect(
                        color = Color(0xFFFF9800),
                        topLeft = rectOffset,
                        size = rectSize,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                }

                // 3. Draw Active Selection Rectangle with Blinking Highlighted Border and Corner Grips
                displayActiveItem?.let { item ->
                    val rectOffset = Offset(item.OffsetX, item.OffsetY)
                    val rectSize = Size(item.Width, item.Height)

                    // Highlighted Blinking Fill & Border
                    drawRect(
                        color = Color(0xFFFF3D00).copy(alpha = 0.12f * alphaAnim),
                        topLeft = rectOffset,
                        size = rectSize
                    )
                    drawRect(
                        color = Color(0xFFFF3D00).copy(alpha = alphaAnim),
                        topLeft = rectOffset,
                        size = rectSize,
                        style = Stroke(width = 3.5.dp.toPx())
                    )

                    // Corner Grips (TL, TR, BL, BR)
                    val left = item.OffsetX
                    val top = item.OffsetY
                    val right = item.OffsetX + item.Width
                    val bottom = item.OffsetY + item.Height
                    val gripRadius = 8.dp.toPx()

                    val grips = listOf(
                        Offset(left, top),       // Top-Left
                        Offset(right, top),      // Top-Right
                        Offset(left, bottom),    // Bottom-Left
                        Offset(right, bottom)    // Bottom-Right
                    )

                    grips.forEach { gripOffset ->
                        drawCircle(
                            color = Color.White,
                            radius = gripRadius + 3.dp.toPx(),
                            center = gripOffset
                        )
                        drawCircle(
                            color = Color(0xFFFF3D00),
                            radius = gripRadius,
                            center = gripOffset
                        )
                    }
                }
            }
        }
    }

    // Debug Popup Dialog Box (When clicking inside active rectangle)
    debugItemForDialog?.let { dialogItem ->
        AlertDialog(
            onDismissRequest = { debugItemForDialog = null },
            title = { Text("Selection Rectangle Debug Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Type: ${dialogItem.DisplayType}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Item ID: #${dialogItem.id}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Position (X, Y):", fontWeight = FontWeight.SemiBold)
                        Text("(${dialogItem.OffsetX.toInt()} px, ${dialogItem.OffsetY.toInt()} px)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Size (W x H):", fontWeight = FontWeight.SemiBold)
                        Text("${dialogItem.Width.toInt()} x ${dialogItem.Height.toInt()} px", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGraphicsScreenItem(dialogItem)
                        if (activeItemId == dialogItem.id) activeItemId = null
                        debugItemForDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Rectangle")
                }
            },
            dismissButton = {
                TextButton(onClick = { debugItemForDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}
