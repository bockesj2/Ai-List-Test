package com.example.ailisttest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.ailisttest.data.GraphicsFileInfo
import com.example.ailisttest.data.GraphicsImageConfig
import com.example.ailisttest.data.GraphicsImageManager
import com.example.ailisttest.ui.GroupColorPickerDialog
import java.io.File
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.CustomGroup
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.data.local.GraphicsScreenItemWithTag
import com.example.ailisttest.data.local.GraphicsScreenItems
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.ScreenWithGraphicsItems
import com.example.ailisttest.data.local.Screens
import com.example.ailisttest.data.local.TagEntity
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.TagNamePickerDialog
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
    var initialSelectedItemId by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<SelectedGraphicsListItem?>(null) }
    var itemForInfoDialog by remember { mutableStateOf<Pair<GraphicsScreenItemWithTag, Screens>?>(null) }

    BackHandler(enabled = editingBlankCanvasScreen != null) {
        editingBlankCanvasScreen = null
        initialSelectedItemId = null
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
            initialSelectedItemId = initialSelectedItemId,
            viewModel = viewModel,
            onBack = {
                editingBlankCanvasScreen = null
                initialSelectedItemId = null
            }
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
                                    onSelectCanvasItem = { screen, itemWithTag ->
                                        selectedItem = SelectedGraphicsListItem.Item(itemWithTag, screen)
                                        initialSelectedItemId = itemWithTag.item.id
                                        editingBlankCanvasScreen = screen
                                    },
                                    onEditCanvas = { screen ->
                                        initialSelectedItemId = null
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
    onSelectCanvasItem: (Screens, GraphicsScreenItemWithTag) -> Unit,
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
                            onSelectCanvasItem = {
                                onSelectCanvasItem(screen, itemWithTag)
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
    onSelectCanvasItem: () -> Unit,
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

            // Prominent "Select" Button on the Sub-Item Row
            FilledTonalButton(
                onClick = onSelectCanvasItem,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Select on Canvas",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Select", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

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
    initialSelectedItemId: Long? = null,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val graphicsScreensWithItems by viewModel.graphicsScreensWithItems.collectAsStateWithLifecycle()
    val allScreens by viewModel.allScreens.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val dataTypes by viewModel.dataTypes.collectAsStateWithLifecycle()
    val screenWithItems = remember(graphicsScreensWithItems, screen.id) {
        graphicsScreensWithItems.find { it.screen.id == screen.id }
    }
    val itemsList = remember(screenWithItems) {
        screenWithItems?.items?.map { it.item } ?: emptyList()
    }

    // Pan & Zoom Scale States
    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isPanMode by remember { mutableStateOf(false) } // False = Edit/Draw Mode, True = Pan Mode

    var activeItemId by remember(screen.id, initialSelectedItemId) { mutableStateOf<Long?>(initialSelectedItemId) }
    var debugItemForDialog by remember { mutableStateOf<GraphicsScreenItems?>(null) }

    // Transient in-memory rectangle during active dragging for instant 60/120 FPS responsiveness
    var draggingRectOverride by remember { mutableStateOf<GraphicsScreenItems?>(null) }

    // In-progress creation gesture states
    var isDrawingNewRect by remember { mutableStateOf(false) }
    var dragStartPoint by remember { mutableStateOf<Offset?>(null) }
    var dragCurrentPoint by remember { mutableStateOf<Offset?>(null) }

    // Active item moving/resizing drag gesture states
    var isMovingActiveItem by remember { mutableStateOf(false) }
    var isPanningCanvas by remember { mutableStateOf(false) }
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

    var showScreenConfigDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val activeScreen = screenWithItems?.screen ?: screen

    val screenBgFile = remember(activeScreen.backgroundImage) {
        if (activeScreen.backgroundImage.isNotBlank()) {
            File(GraphicsImageManager.getGraphicsDirectory(context), activeScreen.backgroundImage)
        } else null
    }
    val screenBgBitmap = remember(screenBgFile?.absolutePath, screenBgFile?.lastModified()) {
        if (screenBgFile != null && screenBgFile.exists() && screenBgFile.isFile) {
            try {
                BitmapFactory.decodeFile(screenBgFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    val parsedScreenBgColor = remember(activeScreen.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(activeScreen.backgroundColorHex.ifBlank { "#FAFAFA" }))
        } catch (_: Exception) {
            Color(0xFFFAFAFA)
        }
    }

    // Helper to map Screen Touch Offset -> Canvas Model Coordinates
    fun toModelOffset(screenOffset: Offset): Offset {
        return Offset(
            x = (screenOffset.x - panOffset.x) / scale,
            y = (screenOffset.y - panOffset.y) / scale
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeScreen.Name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Canvas Layout Editor — ${itemsList.size} item(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to list")
                    }
                },
                actions = {
                    // Configure Screen Background Button
                    OutlinedButton(
                        onClick = { showScreenConfigDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Configure Screen", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Configure", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Zoom Level Indicator & Reset View Button
                    FilledTonalButton(
                        onClick = {
                            scale = 1f
                            panOffset = Offset.Zero
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Rounded.ZoomIn, contentDescription = "Zoom", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Mode Toggle (Pan Mode vs Edit Mode)
                    IconButton(
                        onClick = { isPanMode = !isPanMode }
                    ) {
                        Icon(
                            imageVector = if (isPanMode) Icons.Rounded.PanTool else Icons.Rounded.CropSquare,
                            contentDescription = if (isPanMode) "Pan Mode Active" else "Select/Draw Mode Active",
                            tint = if (isPanMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

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
                .background(parsedScreenBgColor)
                // 1. Pinch-To-Zoom & Two-Finger Pan Gesture Detector
                .pointerInput(screen.id) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (scale * zoom).coerceIn(0.2f, 5.0f)
                        panOffset = (panOffset + pan - centroid) * (newScale / oldScale) + centroid
                        scale = newScale
                    }
                }
                // 2. Click, Double-Click & Long-Press Tap Gesture Detector (in Model Space)
                .pointerInput(screen.id, panOffset, scale) {
                    detectTapGestures(
                        onTap = { screenTapOffset ->
                            val modelTap = toModelOffset(screenTapOffset)

                            // Short click/tap: Select item or bring up configuration dialog if already selected
                            val clickedItem = currentItemsList.findLast { item ->
                                val upperLeftX = item.OffsetX
                                val upperLeftY = item.OffsetY
                                val lowerRightX = item.OffsetX + item.Width
                                val lowerRightY = item.OffsetY + item.Height

                                modelTap.x in upperLeftX..lowerRightX && modelTap.y in upperLeftY..lowerRightY
                            }

                            if (clickedItem != null) {
                                if (activeItemId == clickedItem.id) {
                                    // Single click on an already selected item -> Bring up Item Configuration dialog
                                    debugItemForDialog = clickedItem
                                } else {
                                    // Single click on an unselected item -> Select item (show grips & blinking border)
                                    activeItemId = clickedItem.id
                                }
                            } else {
                                activeItemId = null
                            }
                        },
                        onDoubleTap = { screenTouchOffset ->
                            val modelTouch = toModelOffset(screenTouchOffset)

                            // Double-click: Bring up the Item Configuration Dialog Box!
                            val doubleTappedItem = currentItemsList.findLast { item ->
                                val upperLeftX = item.OffsetX
                                val upperLeftY = item.OffsetY
                                val lowerRightX = item.OffsetX + item.Width
                                val lowerRightY = item.OffsetY + item.Height

                                modelTouch.x in upperLeftX..lowerRightX && modelTouch.y in upperLeftY..lowerRightY
                            }

                            if (doubleTappedItem != null) {
                                activeItemId = doubleTappedItem.id
                                debugItemForDialog = doubleTappedItem
                            }
                        },
                        onLongPress = { screenTouchOffset ->
                            val modelTouch = toModelOffset(screenTouchOffset)

                            // Long-press: Bring up the Item Configuration Dialog Box!
                            val longPressedItem = currentItemsList.findLast { item ->
                                val upperLeftX = item.OffsetX
                                val upperLeftY = item.OffsetY
                                val lowerRightX = item.OffsetX + item.Width
                                val lowerRightY = item.OffsetY + item.Height

                                modelTouch.x in upperLeftX..lowerRightX && modelTouch.y in upperLeftY..lowerRightY
                            }

                            if (longPressedItem != null) {
                                activeItemId = longPressedItem.id
                                debugItemForDialog = longPressedItem
                            }
                        }
                    )
                }
                // 3. Drag Gesture Detector (Resizing Grips, Moving Items, Drawing Rectangles & Background Pan)
                .pointerInput(screen.id, panOffset, scale, isPanMode) {
                    detectDragGestures(
                        onDragStart = { screenStartOffset ->
                            hasDraggedSinceTouch = false
                            val modelStart = toModelOffset(screenStartOffset)
                            val active = draggingRectOverride ?: currentActiveItemFromDb

                            if (!isPanMode && active != null) {
                                val left = active.OffsetX
                                val top = active.OffsetY
                                val right = active.OffsetX + active.Width
                                val bottom = active.OffsetY + active.Height
                                val gripRadiusInModel = 32.dp.toPx() / scale

                                val tlGrip = Offset(left, top)
                                val trGrip = Offset(right, top)
                                val blGrip = Offset(left, bottom)
                                val brGrip = Offset(right, bottom)

                                when {
                                    (modelStart - tlGrip).getDistance() <= gripRadiusInModel -> {
                                        activeGripIndex = 0
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = modelStart
                                        return@detectDragGestures
                                    }
                                    (modelStart - trGrip).getDistance() <= gripRadiusInModel -> {
                                        activeGripIndex = 1
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = modelStart
                                        return@detectDragGestures
                                    }
                                    (modelStart - blGrip).getDistance() <= gripRadiusInModel -> {
                                        activeGripIndex = 2
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = modelStart
                                        return@detectDragGestures
                                    }
                                    (modelStart - brGrip).getDistance() <= gripRadiusInModel -> {
                                        activeGripIndex = 3
                                        activeItemStartRect = active.copy()
                                        draggingRectOverride = active.copy()
                                        dragStartPoint = modelStart
                                        return@detectDragGestures
                                    }
                                }

                                // Check if touch inside active rectangle body (Moving mode)
                                if (modelStart.x in left..right && modelStart.y in top..bottom) {
                                    isMovingActiveItem = true
                                    activeItemStartRect = active.copy()
                                    draggingRectOverride = active.copy()
                                    touchOffsetInRect = modelStart - Offset(left, top)
                                    dragStartPoint = modelStart
                                    return@detectDragGestures
                                }
                            }

                            // Check if touch inside any existing item
                            val clickedExisting = if (!isPanMode) {
                                currentItemsList.findLast { item ->
                                    val l = item.OffsetX
                                    val t = item.OffsetY
                                    val r = item.OffsetX + item.Width
                                    val b = item.OffsetY + item.Height
                                    modelStart.x in l..r && modelStart.y in t..b
                                }
                            } else null

                            if (clickedExisting != null) {
                                activeItemId = clickedExisting.id
                                isMovingActiveItem = true
                                activeItemStartRect = clickedExisting.copy()
                                draggingRectOverride = clickedExisting.copy()
                                touchOffsetInRect = modelStart - Offset(clickedExisting.OffsetX, clickedExisting.OffsetY)
                                dragStartPoint = modelStart
                            } else if (isPanMode) {
                                isPanningCanvas = true
                            } else {
                                // Touch outside all items in Edit Mode -> Start creating new selection rectangle
                                activeItemId = null
                                draggingRectOverride = null
                                isDrawingNewRect = true
                                dragStartPoint = modelStart
                                dragCurrentPoint = modelStart
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            hasDraggedSinceTouch = true
                            val modelPos = toModelOffset(change.position)
                            val startRect = activeItemStartRect

                            if (activeGripIndex >= 0 && startRect != null) {
                                // Direct finger-to-corner tracking for grips in model space
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
                                        nL = minOf(modelPos.x, sR - 10f).coerceAtLeast(0f)
                                        nT = minOf(modelPos.y, sB - 10f).coerceAtLeast(0f)
                                        nW = sR - nL
                                        nH = sB - nT
                                    }
                                    1 -> { // Top-Right Grip
                                        val nR = maxOf(modelPos.x, sL + 10f)
                                        nT = minOf(modelPos.y, sB - 10f).coerceAtLeast(0f)
                                        nW = nR - sL
                                        nH = sB - nT
                                        nL = sL
                                    }
                                    2 -> { // Bottom-Left Grip
                                        nL = minOf(modelPos.x, sR - 10f).coerceAtLeast(0f)
                                        val nB = maxOf(modelPos.y, sT + 10f)
                                        nW = sR - nL
                                        nH = nB - sT
                                        nT = sT
                                    }
                                    3 -> { // Bottom-Right Grip
                                        val nR = maxOf(modelPos.x, sL + 10f)
                                        val nB = maxOf(modelPos.y, sT + 10f)
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
                                // Direct touch tracking for moving active rectangle in model space
                                val newL = (modelPos.x - touchOffsetInRect.x).coerceAtLeast(0f)
                                val newT = (modelPos.y - touchOffsetInRect.y).coerceAtLeast(0f)

                                draggingRectOverride = startRect.copy(
                                    OffsetX = newL,
                                    OffsetY = newT
                                )
                            } else if (isPanningCanvas) {
                                panOffset += dragAmount
                            } else if (isDrawingNewRect) {
                                dragCurrentPoint = modelPos
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
                                            val createdItem = newRect.copy(id = newId)
                                            activeItemId = newId
                                            debugItemForDialog = createdItem
                                        }
                                    }
                                }
                            }

                            // Reset gesture states
                            draggingRectOverride = null
                            isDrawingNewRect = false
                            isMovingActiveItem = false
                            isPanningCanvas = false
                            activeGripIndex = -1
                            dragStartPoint = null
                            dragCurrentPoint = null
                            activeItemStartRect = null
                        },
                        onDragCancel = {
                            draggingRectOverride = null
                            isDrawingNewRect = false
                            isMovingActiveItem = false
                            isPanningCanvas = false
                            activeGripIndex = -1
                            dragStartPoint = null
                            dragCurrentPoint = null
                            activeItemStartRect = null
                        }
                    )
                }
        ) {
            // Screen Background Layer (Always drawn FIRST at bottom layer, non-interactive)
            if (activeScreen.backgroundType == "Image" && screenBgBitmap != null) {
                Image(
                    bitmap = screenBgBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                withTransform({
                    translate(panOffset.x, panOffset.y)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    val activeId = displayActiveItem?.id

                    // 1. Draw Inactive Selection Rectangles (for plain rectangles only)
                    itemsList.filter { it.id != activeId && (it.DisplayType.isEmpty() || it.DisplayType == "Rectangle") }.forEach { item ->
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
                            style = Stroke(width = 2.dp.toPx() / scale)
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
                                width = 2.5.dp.toPx() / scale,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f / scale, 12f / scale), 0f)
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
                            style = Stroke(width = 3.5.dp.toPx() / scale)
                        )

                        // Corner Grips (TL, TR, BL, BR)
                        val left = item.OffsetX
                        val top = item.OffsetY
                        val right = item.OffsetX + item.Width
                        val bottom = item.OffsetY + item.Height
                        val gripRadius = 8.dp.toPx() / scale

                        val grips = listOf(
                            Offset(left, top),       // Top-Left
                            Offset(right, top),      // Top-Right
                            Offset(left, bottom),    // Bottom-Left
                            Offset(right, bottom)    // Bottom-Right
                        )

                        grips.forEach { gripOffset ->
                            drawCircle(
                                color = Color.White,
                                radius = gripRadius + (3.dp.toPx() / scale),
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

            // Overlay Fitted GUI Controls on Canvas for Configured Items
            val density = LocalDensity.current
            screenWithItems?.items?.forEach { itemWithTag ->
                val item = itemWithTag.item
                val displayItem = if (item.id == displayActiveItem?.id) (displayActiveItem ?: item) else item

                if (displayItem.DisplayType.isNotEmpty() && displayItem.DisplayType != "Rectangle") {
                    val widthDp = with(density) { displayItem.Width.toDp() }
                    val heightDp = with(density) { displayItem.Height.toDp() }

                    val imgConfig = GraphicsImageConfig.fromString(displayItem.configStr)
                    val oxTag = hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == imgConfig.offsetXTagId }?.tag
                    val oyTag = hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == imgConfig.offsetYTagId }?.tag
                    val rotTag = hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == imgConfig.rotationTagId }?.tag

                    val extraX = oxTag?.storedValue?.toFloatOrNull() ?: 0f
                    val extraY = oyTag?.storedValue?.toFloatOrNull() ?: 0f
                    val rotDeg = rotTag?.storedValue?.toFloatOrNull() ?: 0f

                    GraphicsItemWidget(
                        item = displayItem,
                        tag = itemWithTag.tag,
                        allScreens = allScreens,
                        hierarchy = hierarchy,
                        enabled = false,
                        onShowSnackbar = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = (displayItem.OffsetX + extraX) * scale + panOffset.x
                                translationY = (displayItem.OffsetY + extraY) * scale + panOffset.y
                                rotationZ = rotDeg
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                            .size(width = widthDp, height = heightDp)
                    )
                }
            }
        }
    }

    // Screen Config Dialog Box
    if (showScreenConfigDialog) {
        ScreenConfigDialog(
            screen = activeScreen,
            context = context,
            screenWidthPx = 1080,
            screenHeightPx = 1920,
            onDismiss = { showScreenConfigDialog = false },
            onSave = { updatedScreen ->
                viewModel.updateScreen(updatedScreen)
                showScreenConfigDialog = false
            }
        )
    }

    // Item Config Dialog Box
    debugItemForDialog?.let { dialogItem ->
        GraphicsItemConfigDialog(
            dialogItem = dialogItem,
            currentScreenId = activeScreen.id,
            allScreens = allScreens,
            hierarchy = hierarchy,
            dataTypes = dataTypes,
            viewModel = viewModel,
            onDismiss = { debugItemForDialog = null },
            onSave = { updatedItem ->
                viewModel.updateGraphicsScreenItem(updatedItem)
                debugItemForDialog = null
            },
            onDelete = { deletedItem ->
                viewModel.deleteGraphicsScreenItem(deletedItem)
                if (activeItemId == deletedItem.id) activeItemId = null
                debugItemForDialog = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphicsItemConfigDialog(
    dialogItem: GraphicsScreenItems,
    currentScreenId: Long,
    allScreens: List<Screens>,
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes>,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onSave: (GraphicsScreenItems) -> Unit,
    onDelete: (GraphicsScreenItems) -> Unit
) {
    var showTagPicker by remember { mutableStateOf(false) }

    // Initial Category: Data (default for new items), Graphics, or Other
    var selectedCategory by remember(dialogItem.id, dialogItem.parentTagId, dialogItem.DisplayType) {
        val cat = when {
            dialogItem.DisplayType == "Goto Page Button" || dialogItem.DisplayType == "TBD" -> "Other"
            dialogItem.DisplayType == "Image" -> "Graphics"
            else -> "Data"
        }
        mutableStateOf(cat)
    }

    var selectedTagId by remember(dialogItem.id, dialogItem.parentTagId) {
        mutableStateOf(dialogItem.parentTagId)
    }

    var selectedItemType by remember(dialogItem.id, dialogItem.Type) {
        mutableIntStateOf(dialogItem.Type)
    }

    val imageConfig = remember(dialogItem.id, dialogItem.configStr) {
        GraphicsImageConfig.fromString(dialogItem.configStr)
    }

    var selectedConfigStr by remember(dialogItem.id, dialogItem.configStr) {
        mutableStateOf(imageConfig.fileName)
    }

    var selectedOffsetXTagId by remember(dialogItem.id, dialogItem.configStr) {
        mutableStateOf(imageConfig.offsetXTagId)
    }

    var selectedOffsetYTagId by remember(dialogItem.id, dialogItem.configStr) {
        mutableStateOf(imageConfig.offsetYTagId)
    }

    var selectedRotationTagId by remember(dialogItem.id, dialogItem.configStr) {
        mutableStateOf(imageConfig.rotationTagId)
    }

    var activeTagPickingTarget by remember { mutableStateOf("itemTag") } // "itemTag", "offsetX", "offsetY", "rotation"

    val offsetXTag = remember(hierarchy, selectedOffsetXTagId) {
        if (selectedOffsetXTagId != null) {
            hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == selectedOffsetXTagId }?.tag
        } else null
    }

    val offsetYTag = remember(hierarchy, selectedOffsetYTagId) {
        if (selectedOffsetYTagId != null) {
            hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == selectedOffsetYTagId }?.tag
        } else null
    }

    val rotationTag = remember(hierarchy, selectedRotationTagId) {
        if (selectedRotationTagId != null) {
            hierarchy.flatMap { n -> n.packetsWithTags }.flatMap { p -> p.tagsWithBitTags }.find { t -> t.tag.id == selectedRotationTagId }?.tag
        } else null
    }

    // Available target screens for "Goto Page Button" (excluding current screen)
    val availableTargetScreens = remember(allScreens, currentScreenId) {
        allScreens.filter { it.id != currentScreenId }
    }

    // Currently selected target screen ID for "Goto Page Button" (stored in configStr)
    var selectedTargetScreenId by remember(dialogItem.id, dialogItem.configStr, availableTargetScreens) {
        val initialId = dialogItem.configStr.toLongOrNull() ?: availableTargetScreens.firstOrNull()?.id ?: 0L
        mutableLongStateOf(initialId)
    }

    // Other options list
    val otherOptions = listOf("TBD", "Goto Page Button")

    val isBitTag = selectedItemType in 1..1000
    val bitIndex = if (isBitTag) (selectedItemType - 1) else null

    val selectedTag = remember(hierarchy, selectedTagId) {
        if (selectedTagId != null) {
            hierarchy.flatMap { node -> node.packetsWithTags }
                .flatMap { p -> p.tagsWithBitTags }
                .find { t -> t.tag.id == selectedTagId }?.tag
        } else null
    }

    val displayTagTitle = remember(selectedTag, isBitTag, bitIndex) {
        when {
            selectedTag != null && isBitTag && bitIndex != null -> "${selectedTag.name}-$bitIndex"
            selectedTag != null -> selectedTag.name
            else -> "(Click to select tag...)"
        }
    }

    // Packet data type for options lookup
    val packetDataType = remember(hierarchy, dataTypes, selectedTag) {
        if (selectedTag != null) {
            hierarchy.flatMap { node -> node.packetsWithTags }
                .find { p -> p.tagsWithBitTags.any { t -> t.tag.id == selectedTag.id } }
                ?.packet?.type?.let { typeId -> dataTypes.find { dt -> dt.id == typeId.toLong() } }
                ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
        } else {
            dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
        }
    }

    val dataTypeShortName = if (isBitTag) "B" else packetDataType.shortName.ifEmpty { packetDataType.description }
    val displayOptions = remember(dataTypeShortName, packetDataType) {
        DisplayTypes.getOptionsForDataType(dataTypeShortName)
    }

    var selectedDisplayType by remember(dialogItem.id, dialogItem.DisplayType, displayOptions, selectedCategory) {
        val initial = when (selectedCategory) {
            "Other" -> if (otherOptions.contains(dialogItem.DisplayType)) dialogItem.DisplayType else "Goto Page Button"
            "Graphics" -> "Image"
            else -> if (displayOptions.contains(dialogItem.DisplayType)) dialogItem.DisplayType else displayOptions.firstOrNull() ?: "Default (Numeric entry)"
        }
        mutableStateOf(initial)
    }

    var isShowTagName by remember(dialogItem.id, dialogItem.isShowTagName) {
        mutableStateOf(dialogItem.isShowTagName)
    }

    var showImagePickerDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val openItemImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val importedName = GraphicsImageManager.importImageFromUri(context, uri)
                if (!importedName.isNullOrEmpty()) {
                    selectedConfigStr = importedName
                    Toast.makeText(context, "Imported image '$importedName'", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error importing image: Could not copy file from system URI.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error importing image: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Item Configuration", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Group Box labeled "Display Type"
                OutlinedCard(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Display Type",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedCategory = "Data" }
                            ) {
                                RadioButton(
                                    selected = selectedCategory == "Data",
                                    onClick = { selectedCategory = "Data" }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedCategory = "Graphics" }
                            ) {
                                RadioButton(
                                    selected = selectedCategory == "Graphics",
                                    onClick = {
                                        selectedCategory = "Graphics"
                                        selectedDisplayType = "Image"
                                    }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Graphics", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedCategory = "Other" }
                            ) {
                                RadioButton(
                                    selected = selectedCategory == "Other",
                                    onClick = {
                                        selectedCategory = "Other"
                                        selectedDisplayType = "Goto Page Button"
                                    }
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Other", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selectedCategory == "Data") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Show Tag Name Checkbox Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isShowTagName = !isShowTagName }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isShowTagName,
                                    onCheckedChange = { isShowTagName = it }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Show tag name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Tag Selection Field
                            Text("Tag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTagPickingTarget = "itemTag"
                                        showTagPicker = true
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Sell,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayTagTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedTag != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTag != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Type Dropdown Listbox
                            Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    displayOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedDisplayType = option
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedCategory == "Other") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            var otherTypeExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = otherTypeExpanded,
                                onExpandedChange = { otherTypeExpanded = !otherTypeExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedDisplayType,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = otherTypeExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = otherTypeExpanded,
                                    onDismissRequest = { otherTypeExpanded = false }
                                ) {
                                    otherOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedDisplayType = option
                                                otherTypeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (selectedDisplayType == "Goto Page Button") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Target Screen", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                var screenDropdownExpanded by remember { mutableStateOf(false) }

                                val selectedTargetScreen = availableTargetScreens.find { it.id == selectedTargetScreenId }
                                val targetScreenTitle = if (selectedTargetScreen != null) {
                                    val typeText = if (selectedTargetScreen.Type == Screens.TYPE_GRAPHICS) "Graphics" else "List"
                                    "${selectedTargetScreen.Name} ($typeText)"
                                } else "(No target screen available)"

                                ExposedDropdownMenuBox(
                                    expanded = screenDropdownExpanded,
                                    onExpandedChange = { screenDropdownExpanded = !screenDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = targetScreenTitle,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = screenDropdownExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = screenDropdownExpanded,
                                        onDismissRequest = { screenDropdownExpanded = false }
                                    ) {
                                        availableTargetScreens.forEach { scr ->
                                            val typeBadge = if (scr.Type == Screens.TYPE_GRAPHICS) "Graphics" else "List"
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text(scr.Name, fontWeight = FontWeight.Bold)
                                                        Text("($typeBadge)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                },
                                                onClick = {
                                                    selectedTargetScreenId = scr.id
                                                    screenDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedCategory == "Graphics") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text("Graphics Image File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (selectedConfigStr.isNotBlank()) selectedConfigStr else "(No image selected)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedConfigStr.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedConfigStr.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            openItemImageLauncher.launch(arrayOf("image/*"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error launching system file picker: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import")
                                }

                                Button(
                                    onClick = {
                                        val tagNameForExport = displayTagTitle.ifBlank { "GraphicsItem_${dialogItem.id}" }
                                        val exportedFile = GraphicsImageManager.exportItemImage(
                                            context = context,
                                            tagName = tagNameForExport,
                                            widthPx = dialogItem.Width.toInt(),
                                            heightPx = dialogItem.Height.toInt(),
                                            sourceFilename = selectedConfigStr
                                        )
                                        if (exportedFile != null) {
                                            selectedConfigStr = exportedFile.name
                                            Toast.makeText(context, "Exported image '${exportedFile.name}' to /Documents/AIListTest/graphics/", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to export image.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 1. OffsetX Tag
                            Text("OffsetX Tag (pixels offset)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTagPickingTarget = "offsetX"
                                        showTagPicker = true
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = offsetXTag?.name ?: "None",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (offsetXTag != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (offsetXTag != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 2. OffsetY Tag
                            Text("OffsetY Tag (pixels offset)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTagPickingTarget = "offsetY"
                                        showTagPicker = true
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = offsetYTag?.name ?: "None",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (offsetYTag != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (offsetYTag != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // 3. Rotation(CW) Tag
                            Text("Rotation(CW) Tag (degrees)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTagPickingTarget = "rotation"
                                        showTagPicker = true
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Icon(Icons.Rounded.Sell, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rotationTag?.name ?: "None",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (rotationTag != null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (rotationTag != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                            }
                        }
                    }
                }

                // Coordinates Info Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Canvas Geometry",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Position (X, Y): (${dialogItem.OffsetX.toInt()} px, ${dialogItem.OffsetY.toInt()} px)", style = MaterialTheme.typography.bodySmall)
                        Text("Size (W x H): ${dialogItem.Width.toInt()} x ${dialogItem.Height.toInt()} px", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onDelete(dialogItem)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }

                Button(
                    onClick = {
                        val finalTagId = if (selectedCategory == "Data") selectedTagId else null
                        val finalDisplayType = when (selectedCategory) {
                            "Data" -> selectedDisplayType
                            "Other" -> selectedDisplayType
                            else -> "Image"
                        }
                        val finalConfigStr = when (selectedCategory) {
                            "Other" -> if (selectedDisplayType == "Goto Page Button") selectedTargetScreenId.toString() else ""
                            "Graphics" -> GraphicsImageConfig(
                                fileName = selectedConfigStr,
                                offsetXTagId = selectedOffsetXTagId,
                                offsetYTagId = selectedOffsetYTagId,
                                rotationTagId = selectedRotationTagId
                            ).toJson()
                            else -> ""
                        }

                        onSave(
                            dialogItem.copy(
                                parentTagId = finalTagId,
                                Type = if (selectedCategory == "Data") selectedItemType else 0,
                                DisplayType = finalDisplayType,
                                configStr = finalConfigStr,
                                isShowTagName = if (selectedCategory == "Data") isShowTagName else false
                            )
                        )
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Tag Name Picker Dialog when clicking any Tag field
    if (showTagPicker) {
        TagNamePickerDialog(
            hierarchy = hierarchy,
            dataTypes = dataTypes,
            allowGroupSelection = false,
            allowMultipleSelection = false,
            showCreateGroupCheckbox = false,
            allowBitSelection = activeTagPickingTarget == "itemTag",
            showNoneOption = activeTagPickingTarget != "itemTag",
            onSelectNone = {
                when (activeTagPickingTarget) {
                    "offsetX" -> selectedOffsetXTagId = null
                    "offsetY" -> selectedOffsetYTagId = null
                    "rotation" -> selectedRotationTagId = null
                    "itemTag" -> selectedTagId = null
                }
                showTagPicker = false
            },
            onTagsSelected = { selectedPaths, _ ->
                val chosenPath = selectedPaths.firstOrNull()
                if (!chosenPath.isNullOrEmpty()) {
                    val cleanName = chosenPath.substringAfterLast("/").trim()

                    // Check if selection is a bit tag (ends with -[0-9]+)
                    val bitMatchRegex = Regex("^(.+)-(\\d+)$")
                    val bitMatch = bitMatchRegex.find(cleanName)

                    if (bitMatch != null) {
                        val parentName = bitMatch.groupValues[1].trim()
                        val bitIdx = bitMatch.groupValues[2].toIntOrNull() ?: 0

                        val match = hierarchy.flatMap { node -> node.packetsWithTags }
                            .flatMap { p -> p.tagsWithBitTags }
                            .find { t -> t.tag.name == parentName || t.tag.name.endsWith(parentName) }?.tag

                        if (match != null) {
                            when (activeTagPickingTarget) {
                                "offsetX" -> selectedOffsetXTagId = match.id
                                "offsetY" -> selectedOffsetYTagId = match.id
                                "rotation" -> selectedRotationTagId = match.id
                                "itemTag" -> {
                                    selectedTagId = match.id
                                    selectedItemType = bitIdx + 1
                                }
                            }
                        }
                    } else {
                        val match = hierarchy.flatMap { node -> node.packetsWithTags }
                            .flatMap { p -> p.tagsWithBitTags }
                            .find { t -> t.tag.name == cleanName || chosenPath.endsWith(t.tag.name) }?.tag

                        if (match != null) {
                            when (activeTagPickingTarget) {
                                "offsetX" -> selectedOffsetXTagId = match.id
                                "offsetY" -> selectedOffsetYTagId = match.id
                                "rotation" -> selectedRotationTagId = match.id
                                "itemTag" -> {
                                    selectedTagId = match.id
                                    selectedItemType = 0
                                }
                            }
                        }
                    }
                }
                showTagPicker = false
            },
            onDismiss = { showTagPicker = false }
        )
    }

    // Image File Picker Dialog when clicking Import button
    if (showImagePickerDialog) {
        ImageFilePickerDialog(
            context = context,
            onImageSelected = { selectedFilename ->
                selectedConfigStr = selectedFilename
            },
            onDismiss = { showImagePickerDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageFilePickerDialog(
    context: Context,
    onImageSelected: (filename: String) -> Unit,
    onDismiss: () -> Unit
) {
    var availableFiles by remember { mutableStateOf(GraphicsImageManager.getAvailableGraphicsFiles(context)) }
    var selectedFile by remember { mutableStateOf<GraphicsFileInfo?>(availableFiles.firstOrNull()) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val importedName = GraphicsImageManager.importImageFromUri(context, uri)
            if (!importedName.isNullOrEmpty()) {
                availableFiles = GraphicsImageManager.getAvailableGraphicsFiles(context)
                selectedFile = availableFiles.find { it.name == importedName }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Graphics Image File", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Browse System Files...")
                }

                Text(
                    text = "Directory: /Documents/AIListTest/graphics/",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    if (availableFiles.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                            Text("No image files found in /Documents/AIListTest/graphics/.\nTap 'Browse System Files...' to select an image.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                            items(availableFiles) { fileInfo ->
                                val isSelected = selectedFile?.path == fileInfo.path
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(6.dp))
                                        .clickable { selectedFile = fileInfo }
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fileInfo.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${fileInfo.sizeText} • ${fileInfo.dateText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = selectedFile
                    if (chosen != null) {
                        onImageSelected(chosen.name)
                        onDismiss()
                    }
                },
                enabled = selectedFile != null
            ) {
                Text("Select Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AutoResizedText(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = 1
) {
    var resizedTextStyle by remember(text) {
        mutableStateOf(style.copy(fontSize = 100.sp, fontWeight = fontWeight, color = color))
    }
    var shouldDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        style = resizedTextStyle,
        softWrap = false,
        maxLines = maxLines,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.didOverflowWidth || result.didOverflowHeight) {
                if (resizedTextStyle.fontSize > 6.sp) {
                    resizedTextStyle = resizedTextStyle.copy(fontSize = resizedTextStyle.fontSize * 0.9f)
                } else {
                    shouldDraw = true
                }
            } else {
                shouldDraw = true
            }
        },
        modifier = modifier.drawWithContent {
            if (shouldDraw) {
                drawContent()
            }
        }
    )
}

@Composable
fun GraphicsItemWidget(
    item: GraphicsScreenItems,
    tag: TagEntity?,
    allScreens: List<Screens> = emptyList(),
    hierarchy: List<NodeWithPacketsAndTags> = emptyList(),
    enabled: Boolean = true,
    onNavigateToScreen: (Screens) -> Unit = {},
    onShowSnackbar: (String) -> Unit = {},
    onBitAction: (bitVal: Boolean?, isToggle: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val displayType = item.DisplayType
    val storedVal = tag?.storedValue ?: "0"
    val isBitTagItem = item.Type in 1..1000
    val bitIndex = if (isBitTagItem) (item.Type - 1) else null
    val parentTagVal = tag?.storedValue?.toLongOrNull() ?: 0L

    val isBitSet = if (isBitTagItem && bitIndex != null) {
        ((parentTagVal and (1L shl bitIndex)) != 0L)
    } else {
        storedVal.toDoubleOrNull()?.toInt() != 0 || storedVal.equals("1", ignoreCase = true) || storedVal.equals("true", ignoreCase = true)
    }

    val tagDisplayName = when {
        item.isShowTagName && tag != null && isBitTagItem && bitIndex != null -> "${tag.name}-$bitIndex"
        item.isShowTagName && tag != null -> tag.name
        else -> null
    }

    val imageConfig = remember(item.configStr) {
        GraphicsImageConfig.fromString(item.configStr)
    }

    val context = LocalContext.current
    val imageFile = remember(imageConfig.fileName) {
        if (imageConfig.fileName.isNotBlank()) {
            File(GraphicsImageManager.getGraphicsDirectory(context), imageConfig.fileName)
        } else null
    }
    val imageBitmap = remember(imageFile?.absolutePath, imageFile?.lastModified()) {
        if (imageFile != null && imageFile.exists() && imageFile.isFile) {
            try {
                BitmapFactory.decodeFile(imageFile.absolutePath)?.asImageBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
        ) {
            if (imageBitmap != null) {
                // Render fitted imported/exported image bitmap
                Image(
                    bitmap = imageBitmap,
                    contentDescription = item.configStr,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (displayType) {
                    "Goto Page Button" -> {
                        val targetScreenId = item.configStr.toLongOrNull() ?: 0L
                        val targetScreen = remember(allScreens, targetScreenId) {
                            allScreens.find { it.id == targetScreenId }
                        }
                        val targetScreenName = targetScreen?.Name ?: "Goto Screen"

                        Button(
                            onClick = {
                                if (enabled && targetScreen != null) {
                                    onNavigateToScreen(targetScreen)
                                } else {
                                    onShowSnackbar("Goto Screen: $targetScreenName")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AutoResizedText(
                                text = targetScreenName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "TBD" -> {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(2.dp)) {
                                AutoResizedText(
                                    text = "TBD",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    "On_Button" -> {
                        Button(
                            onClick = { if (enabled) onBitAction(true, false) },
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575),
                                disabledContainerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575)
                            ),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "Off_Button" -> {
                        Button(
                            onClick = { if (enabled) onBitAction(false, false) },
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFFE53935),
                                disabledContainerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFFE53935)
                            ),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "Toggle_Button" -> {
                        Button(
                            onClick = { if (enabled) onBitAction(null, true) },
                            enabled = enabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575),
                                disabledContainerColor = if (isBitSet) Color(0xFF4CAF50) else Color(0xFF757575)
                            ),
                            contentPadding = PaddingValues(2.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "Switch" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        ) {
                            Switch(
                                checked = isBitSet,
                                onCheckedChange = if (enabled) { checked -> onBitAction(checked, false) } else null,
                                enabled = enabled,
                                modifier = Modifier.scale(0.85f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = if (isBitSet) Color(0xFF2E7D32) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "CheckBox" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        ) {
                            Checkbox(
                                checked = isBitSet,
                                onCheckedChange = if (enabled) { { onBitAction(null, true) } } else null,
                                enabled = enabled
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = if (isBitSet) Color(0xFF2E7D32) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "Radio_Button" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(2.dp)
                        ) {
                            RadioButton(
                                selected = isBitSet,
                                onClick = if (enabled) { { onBitAction(null, true) } } else null,
                                enabled = enabled
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            AutoResizedText(
                                text = if (isBitSet) "ON" else "OFF",
                                color = if (isBitSet) Color(0xFF2E7D32) else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {
                        // Default Numeric or Tag Value — Show ONLY the value (not the tag name)
                        val textToShow = if (tag != null) tag.storedValue.ifEmpty { "0" } else (if (storedVal.isNotEmpty() && storedVal != "0") storedVal else displayType)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            ) {
                                AutoResizedText(
                                    text = textToShow,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Upper Left Corner Tag Name Label (like an outlined text box with a label)
            if (!tagDisplayName.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 3.dp, top = 1.dp)
                ) {
                    Text(
                        text = tagDisplayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenConfigDialog(
    screen: Screens,
    context: Context,
    screenWidthPx: Int,
    screenHeightPx: Int,
    onDismiss: () -> Unit,
    onSave: (Screens) -> Unit
) {
    var bgType by remember(screen.id, screen.backgroundType) {
        mutableStateOf(screen.backgroundType.ifBlank { "Color" })
    }
    var bgColorHex by remember(screen.id, screen.backgroundColorHex) {
        mutableStateOf(screen.backgroundColorHex.ifBlank { "#FAFAFA" })
    }
    var bgImageName by remember(screen.id, screen.backgroundImage) {
        mutableStateOf(screen.backgroundImage)
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }

    val openScreenBgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val importedName = GraphicsImageManager.importImageFromUri(context, uri)
                if (!importedName.isNullOrEmpty()) {
                    bgImageName = importedName
                    Toast.makeText(context, "Imported background image '$importedName'", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error importing background image: Could not copy file from system URI.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error importing background image: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Screen Background", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Background Type Selector Card
                OutlinedCard(
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Background Type",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { bgType = "Color" }
                            ) {
                                RadioButton(
                                    selected = bgType == "Color",
                                    onClick = { bgType = "Color" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { bgType = "Image" }
                            ) {
                                RadioButton(
                                    selected = bgType == "Image",
                                    onClick = { bgType = "Image" }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Image", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        if (bgType == "Color") {
                            Text("Background Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            val parsedColor = try { Color(android.graphics.Color.parseColor(bgColorHex)) } catch (_: Exception) { Color(0xFFFAFAFA) }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showColorPicker = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = parsedColor,
                                        border = BorderStroke(1.dp, Color.Gray),
                                        modifier = Modifier.size(24.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bgColorHex,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("Change", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Text("Background Image File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (bgImageName.isNotBlank()) bgImageName else "(No background image selected)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (bgImageName.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                                        color = if (bgImageName.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            openScreenBgLauncher.launch(arrayOf("image/*"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error launching system file picker: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import")
                                }

                                Button(
                                    onClick = {
                                        val exportName = screen.Name.ifBlank { "Screen_${screen.id}" }
                                        val exportedFile = GraphicsImageManager.exportItemImage(
                                            context = context,
                                            tagName = exportName,
                                            widthPx = screenWidthPx,
                                            heightPx = screenHeightPx,
                                            sourceFilename = bgImageName
                                        )
                                        if (exportedFile != null) {
                                            bgImageName = exportedFile.name
                                            Toast.makeText(context, "Exported background image '${exportedFile.name}' to /Documents/AIListTest/graphics/", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Failed to export background image.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Export")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        screen.copy(
                            backgroundType = bgType,
                            backgroundColorHex = bgColorHex,
                            backgroundImage = bgImageName
                        )
                    )
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showColorPicker) {
        GroupColorPickerDialog(
            initialColorHex = bgColorHex,
            onColorSelected = { newHex ->
                bgColorHex = newHex
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showImagePicker) {
        ImageFilePickerDialog(
            context = context,
            onImageSelected = { selectedFilename ->
                bgImageName = selectedFilename
                showImagePicker = false
            },
            onDismiss = { showImagePicker = false }
        )
    }
}
