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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.GraphicsFileInfo
import com.example.ailisttest.data.GraphicsImageConfig
import com.example.ailisttest.data.GraphicsImageManager
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.data.local.GraphicsScreenItemWithTag
import com.example.ailisttest.data.local.GraphicsScreenItems
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.ScreenWithGraphicsItems
import com.example.ailisttest.data.local.Screens
import com.example.ailisttest.data.local.TagEntity
import com.example.ailisttest.ui.GroupColorPickerDialog
import com.example.ailisttest.ui.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun GraphicsGroupsConfigScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val graphicsGroupsWithItems by viewModel.graphicsGroupsWithItems.collectAsStateWithLifecycle()
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
        // Blank Canvas Screen for Group
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
                                title = { Text("Configure Groups Library") },
                                navigationIcon = {
                                    IconButton(onClick = onOpenDrawer) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                    }
                                },
                                actions = {
                                    IconButton(
                                        onClick = {
                                            viewModel.addGraphicsGroup(onShowSnackbar = { msg ->
                                                scope.launch { snackbarHostState.showSnackbar(msg) }
                                            })
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = "Add Group to Library")
                                    }
                                }
                            )

                            if (graphicsGroupsWithItems.isEmpty()) {
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
                                            imageVector = Icons.Rounded.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No Groups in Library",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap the '+' button above to add a new Group to the Library.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                GraphicsScreensTreeList(
                                    graphicsScreensWithItems = graphicsGroupsWithItems,
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
                        if (selectedItem != null) {
                            GraphicsScreenDetailPane(
                                item = selectedItem!!,
                                graphicsScreensWithItems = graphicsGroupsWithItems,
                                viewModel = viewModel,
                                onUpdateScreen = { updated ->
                                    viewModel.updateScreen(updated)
                                },
                                onEditCanvas = { screen ->
                                    editingBlankCanvasScreen = screen
                                },
                                onDeleteItemWithSelection = { target ->
                                    if (target != null) itemToDelete = target
                                },
                                onBack = {
                                    scope.launch { navigator.navigateBack() }
                                },
                                onShowSnackbar = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select a Graphics Group or item to view details.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = when (target) {
                        is SelectedGraphicsListItem.Screen -> "Delete Graphics Group"
                        is SelectedGraphicsListItem.Item -> "Delete Item"
                        is SelectedGraphicsListItem.CustomGroupItem -> "Delete Item"
                    }
                )
            },
            text = {
                Text(
                    text = when (target) {
                        is SelectedGraphicsListItem.Screen -> "Are you sure you want to delete group '${target.screen.Name}' and all its items?"
                        is SelectedGraphicsListItem.Item -> "Are you sure you want to delete item #${target.itemWithTag.item.id}?"
                        is SelectedGraphicsListItem.CustomGroupItem -> "Are you sure you want to delete item #${target.itemWithTag.item.id}?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            is SelectedGraphicsListItem.Screen -> {
                                viewModel.deleteScreen(target.screen)
                                scope.launch { snackbarHostState.showSnackbar("Deleted group '${target.screen.Name}'.") }
                            }
                            is SelectedGraphicsListItem.Item -> {
                                viewModel.deleteGraphicsScreenItem(target.itemWithTag.item)
                                scope.launch { snackbarHostState.showSnackbar("Deleted item #${target.itemWithTag.item.id}.") }
                            }
                            is SelectedGraphicsListItem.CustomGroupItem -> {
                                viewModel.deleteGraphicsScreenItem(target.itemWithTag.item)
                                scope.launch { snackbarHostState.showSnackbar("Deleted item #${target.itemWithTag.item.id}.") }
                            }
                        }
                        if (selectedItem == target) {
                            selectedItem = null
                            scope.launch { navigator.navigateBack() }
                        }
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

    // Item Info Dialog
    itemForInfoDialog?.let { (itemWithTag, parentScreen) ->
        val graphicsItem = itemWithTag.item
        val tag = itemWithTag.tag

        AlertDialog(
            onDismissRequest = { itemForInfoDialog = null },
            title = { Text("Graphics Item Details", fontWeight = FontWeight.Bold) },
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
                                Text("Parent Group:", fontWeight = FontWeight.SemiBold)
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
