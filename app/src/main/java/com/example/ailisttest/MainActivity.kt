package com.example.ailisttest

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.ailisttest.data.*
import com.example.ailisttest.data.local.Screens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.ailisttest.ui.MainViewModel
import com.example.ailisttest.ui.TagNamePickerDialog
import com.example.ailisttest.ui.navigation.AppNavKey
import com.example.ailisttest.ui.screens.ConfigureScreen
import com.example.ailisttest.ui.screens.DataTypesConfigScreen
import com.example.ailisttest.ui.screens.DebugAnimationScreen
import com.example.ailisttest.ui.screens.DisplayScreen
import com.example.ailisttest.ui.screens.DynamicGraphicsScreen
import com.example.ailisttest.ui.screens.DynamicListScreen
import com.example.ailisttest.ui.screens.GraphicsGroupsConfigScreen
import com.example.ailisttest.ui.screens.DataTypesConfigScreen
import com.example.ailisttest.ui.screens.GraphicsScreensConfigScreen
import com.example.ailisttest.ui.screens.InternalTagsConfigScreen
import com.example.ailisttest.ui.screens.ListScreensConfigScreen
import com.example.ailisttest.ui.screens.MainScreen
import com.example.ailisttest.ui.screens.ModbusByteOrderConfigScreen
import com.example.ailisttest.ui.screens.PollingStatusScreen
import com.example.ailisttest.ui.screens.getFileNameFromUri
import com.example.ailisttest.ui.theme.AiListTestTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        GraphicsImageManager.ensureGraphicsDirectoryExists(applicationContext)
        setContent {
            AiListTestTheme {
                val backStack = rememberNavBackStack(AppNavKey.TagsInternal)
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                val mainViewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)

                val expandedDrawerItems = remember {
                    mutableStateMapOf(
                        "Configure" to true,
                        "ConfigureTags" to true,
                        "Screens" to true
                    )
                }
                var selectedTestItem by remember { mutableStateOf<String?>(null) }
                var showTagPickerDialog by remember { mutableStateOf(false) }
                var showRecreateDbDialog by remember { mutableStateOf(false) }
                var databaseDialogAction by remember { mutableStateOf<String?>(null) }
                var backupResultState by remember { mutableStateOf<BackupResult?>(null) }
                var selectedRestoreMode by remember { mutableStateOf(RestoreMode.OVERWRITE) }
                var showRestoreFilePickerDialog by remember { mutableStateOf(false) }
                var isRestoringDatabase by remember { mutableStateOf(false) }
                var availableBackupFiles by remember { mutableStateOf<List<BackupFileInfo>>(emptyList()) }
                var selectedBackupFile by remember { mutableStateOf<BackupFileInfo?>(null) }

                var restoreResultState by remember { mutableStateOf<RestoreResult?>(null) }
                var activeConflictsQueue by remember { mutableStateOf<List<RestoreConflict>>(emptyList()) }
                var activeConflictIndex by remember { mutableIntStateOf(0) }
                var showConflictDialog by remember { mutableStateOf(false) }

                val openRestoreDocumentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        val fileName = getFileNameFromUri(context, uri)
                        val displayPath = uri.path ?: uri.toString()
                        val pickedInfo = BackupFileInfo(
                            file = if (uri.path != null) File(uri.path!!) else null,
                            name = fileName,
                            path = displayPath,
                            sizeText = "Available",
                            dateText = "System Selected",
                            uri = uri
                        )

                        val existing = availableBackupFiles.find { it.path == pickedInfo.path || (it.uri != null && it.uri == uri) }
                        if (existing != null) {
                            selectedBackupFile = existing
                        } else {
                            availableBackupFiles = listOf(pickedInfo) + availableBackupFiles
                            selectedBackupFile = pickedInfo
                        }
                    }
                }
                val isConfigureMode by mainViewModel.isConfigureMode.collectAsStateWithLifecycle()
                val hierarchy by mainViewModel.hierarchy.collectAsStateWithLifecycle()
                val dataTypes by mainViewModel.dataTypes.collectAsStateWithLifecycle()
                val allScreens by mainViewModel.allScreens.collectAsStateWithLifecycle()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            val drawerScrollState = rememberScrollState()
                            val canDrawerScrollDown by remember {
                                derivedStateOf { drawerScrollState.canScrollForward }
                            }

                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(drawerScrollState)
                                ) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "AI List Test",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Configure",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Switch(
                                            checked = isConfigureMode,
                                            onCheckedChange = { enabled ->
                                                mainViewModel.setConfigureMode(enabled)
                                                if (!enabled && (backStack.lastOrNull() is AppNavKey.Configure || backStack.lastOrNull() is AppNavKey.ListScreens || backStack.lastOrNull() is AppNavKey.GraphicsScreens)) {
                                                    backStack.add(AppNavKey.Main)
                                                    while (backStack.size > 1) {
                                                        backStack.removeAt(0)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                // Dashboard Multi-level Menu with Dynamic Screens
                                val dashboardExpanded = expandedDrawerItems["Dashboard"] ?: true
                                DrawerTreeItemRow(
                                    title = "Dashboard",
                                    level = 0,
                                    isExpanded = dashboardExpanded,
                                    isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.Main,
                                    hasChildren = true,
                                    onToggleExpand = {
                                        expandedDrawerItems["Dashboard"] = !(expandedDrawerItems["Dashboard"] ?: true)
                                    },
                                    onClick = {
                                        selectedTestItem = null
                                        scope.launch { drawerState.close() }
                                        if (backStack.lastOrNull() !is AppNavKey.Main) {
                                            backStack.add(AppNavKey.Main)
                                            while (backStack.size > 1) {
                                                backStack.removeAt(0)
                                            }
                                        }
                                    },
                                    icon = Icons.Rounded.Home
                                )

                                AnimatedVisibility(
                                    visible = dashboardExpanded,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column {
                                        // Screens Sub-menu under Dashboard (Level 1)
                                        val dashboardScreensExpanded = expandedDrawerItems["DashboardScreens"] ?: true
                                        DrawerTreeItemRow(
                                            title = "Screens",
                                            level = 1,
                                            isExpanded = dashboardScreensExpanded,
                                            isSelected = false,
                                            hasChildren = allScreens.isNotEmpty(),
                                            onToggleExpand = {
                                                expandedDrawerItems["DashboardScreens"] = !(expandedDrawerItems["DashboardScreens"] ?: true)
                                            },
                                            onClick = {
                                                expandedDrawerItems["DashboardScreens"] = !(expandedDrawerItems["DashboardScreens"] ?: true)
                                            },
                                            icon = Icons.Rounded.Monitor
                                        )

                                        AnimatedVisibility(
                                            visible = dashboardScreensExpanded,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column {
                                                val dashboardScreens = remember(allScreens) {
                                                    allScreens.filter { it.Type == Screens.TYPE_LIST || it.Type == Screens.TYPE_GRAPHICS }
                                                }

                                                if (dashboardScreens.isEmpty()) {
                                                    DrawerTreeItemRow(
                                                        title = "(No Screens Configured)",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = false,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.ListScreens) {
                                                                backStack.add(AppNavKey.ListScreens)
                                                            }
                                                        },
                                                        icon = Icons.AutoMirrored.Rounded.ViewList
                                                    )
                                                } else {
                                                    dashboardScreens.forEach { screen ->
                                                        val isGraphics = screen.Type == Screens.TYPE_GRAPHICS
                                                        val screenIcon = if (isGraphics) Icons.Rounded.Dashboard else Icons.AutoMirrored.Rounded.ViewList
                                                        val isScreenSelected = selectedTestItem == null && (
                                                            if (isGraphics) (backStack.lastOrNull() as? AppNavKey.DynamicGraphicsScreen)?.screenId == screen.id
                                                            else (backStack.lastOrNull() as? AppNavKey.DynamicListScreen)?.screenId == screen.id
                                                        )

                                                        DrawerTreeItemRow(
                                                            title = screen.Name,
                                                            level = 2,
                                                            isExpanded = false,
                                                            isSelected = isScreenSelected,
                                                            hasChildren = false,
                                                            onToggleExpand = {},
                                                            onClick = {
                                                                selectedTestItem = null
                                                                scope.launch { drawerState.close() }
                                                                if (isGraphics) {
                                                                    if ((backStack.lastOrNull() as? AppNavKey.DynamicGraphicsScreen)?.screenId != screen.id) {
                                                                        backStack.add(AppNavKey.DynamicGraphicsScreen(screen.id))
                                                                    }
                                                                } else {
                                                                    if ((backStack.lastOrNull() as? AppNavKey.DynamicListScreen)?.screenId != screen.id) {
                                                                        backStack.add(AppNavKey.DynamicListScreen(screen.id))
                                                                    }
                                                                }
                                                            },
                                                            icon = screenIcon
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                NavigationDrawerItem(
                                    label = { Text("Status Display") },
                                    selected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.Display,
                                    onClick = {
                                        selectedTestItem = null
                                        scope.launch { drawerState.close() }
                                        if (backStack.lastOrNull() !is AppNavKey.Display) {
                                            backStack.add(AppNavKey.Display)
                                        }
                                    },
                                    icon = { Icon(Icons.AutoMirrored.Rounded.ViewList, contentDescription = null) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                NavigationDrawerItem(
                                    label = { Text("Polling status") },
                                    selected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.PollingStatus,
                                    onClick = {
                                        selectedTestItem = null
                                        scope.launch { drawerState.close() }
                                        if (backStack.lastOrNull() !is AppNavKey.PollingStatus) {
                                            backStack.add(AppNavKey.PollingStatus)
                                        }
                                    },
                                    icon = { Icon(Icons.Rounded.Sensors, contentDescription = null) },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )

                                if (isConfigureMode) {
                                    // Multi-level Configure Menu
                                    val configureExpanded = expandedDrawerItems["Configure"] ?: false
                                    DrawerTreeItemRow(
                                        title = "Configure",
                                        level = 0,
                                        isExpanded = configureExpanded,
                                        isSelected = false,
                                        hasChildren = true,
                                        onToggleExpand = {
                                            expandedDrawerItems["Configure"] = !(expandedDrawerItems["Configure"] ?: false)
                                        },
                                        onClick = {
                                            expandedDrawerItems["Configure"] = !(expandedDrawerItems["Configure"] ?: false)
                                        },
                                        icon = Icons.Rounded.Settings
                                    )

                                    AnimatedVisibility(
                                        visible = configureExpanded,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        Column {
                                            // Tags Sub-menu (Level 1)
                                            val configureTagsExpanded = expandedDrawerItems["ConfigureTags"] ?: true
                                            DrawerTreeItemRow(
                                                title = "Tags",
                                                level = 1,
                                                isExpanded = configureTagsExpanded,
                                                isSelected = false,
                                                hasChildren = true,
                                                onToggleExpand = {
                                                    expandedDrawerItems["ConfigureTags"] = !(expandedDrawerItems["ConfigureTags"] ?: true)
                                                },
                                                onClick = {
                                                    expandedDrawerItems["ConfigureTags"] = !(expandedDrawerItems["ConfigureTags"] ?: true)
                                                },
                                                icon = Icons.Rounded.Sell
                                            )

                                            AnimatedVisibility(
                                                visible = configureTagsExpanded,
                                                enter = expandVertically(),
                                                exit = shrinkVertically()
                                            ) {
                                                Column {
                                                    // Configure / Tags / PLC
                                                    DrawerTreeItemRow(
                                                        title = "PLC",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = selectedTestItem == null && (backStack.lastOrNull() is AppNavKey.Configure || backStack.lastOrNull() is AppNavKey.TagsPlc),
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.Configure) {
                                                                backStack.add(AppNavKey.Configure)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.Dns
                                                    )

                                                    // Configure / Tags / Internal
                                                    DrawerTreeItemRow(
                                                        title = "Internal",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.TagsInternal,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.TagsInternal) {
                                                                backStack.add(AppNavKey.TagsInternal)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.Storage
                                                    )
                                                }
                                            }

                                            // Modbus Byte Order Sub-item
                                            DrawerTreeItemRow(
                                                title = "Modbus Byte Order",
                                                level = 1,
                                                isExpanded = false,
                                                isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.ModbusByteOrderConfig,
                                                hasChildren = false,
                                                onToggleExpand = {},
                                                onClick = {
                                                    selectedTestItem = null
                                                    scope.launch { drawerState.close() }
                                                    if (backStack.lastOrNull() !is AppNavKey.ModbusByteOrderConfig) {
                                                        backStack.add(AppNavKey.ModbusByteOrderConfig)
                                                    }
                                                },
                                                icon = Icons.Rounded.SwapHoriz
                                            )

                                            // Define Data Types Sub-item
                                            DrawerTreeItemRow(
                                                title = "Define Data Types",
                                                level = 1,
                                                isExpanded = false,
                                                isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.DataTypesConfig,
                                                hasChildren = false,
                                                onToggleExpand = {},
                                                onClick = {
                                                    selectedTestItem = null
                                                    scope.launch { drawerState.close() }
                                                    if (backStack.lastOrNull() !is AppNavKey.DataTypesConfig) {
                                                        backStack.add(AppNavKey.DataTypesConfig)
                                                    }
                                                },
                                                icon = Icons.Rounded.Category
                                            )

                                            // Screens Sub-menu (Level 1 Parent)
                                            val screensExpanded = expandedDrawerItems["Screens"] ?: false
                                            DrawerTreeItemRow(
                                                title = "Screens",
                                                level = 1,
                                                isExpanded = screensExpanded,
                                                isSelected = false,
                                                hasChildren = true,
                                                onToggleExpand = {
                                                    expandedDrawerItems["Screens"] = !(expandedDrawerItems["Screens"] ?: false)
                                                },
                                                onClick = {
                                                    expandedDrawerItems["Screens"] = !(expandedDrawerItems["Screens"] ?: false)
                                                },
                                                icon = Icons.Rounded.Monitor
                                            )

                                            AnimatedVisibility(
                                                visible = screensExpanded,
                                                enter = expandVertically(),
                                                exit = shrinkVertically()
                                            ) {
                                                Column {
                                                    // List Screens Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "List Screens",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.ListScreens,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.ListScreens) {
                                                                backStack.add(AppNavKey.ListScreens)
                                                            }
                                                        },
                                                        icon = Icons.AutoMirrored.Rounded.ViewList
                                                    )

                                                    // Graphics Screens Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "Graphics Screens",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.GraphicsScreens,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.GraphicsScreens) {
                                                                backStack.add(AppNavKey.GraphicsScreens)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.Dashboard
                                                    )

                                                    // Groups Library Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "Groups Library",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.GraphicsGroups,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            selectedTestItem = null
                                                            scope.launch { drawerState.close() }
                                                            if (backStack.lastOrNull() !is AppNavKey.GraphicsGroups) {
                                                                backStack.add(AppNavKey.GraphicsGroups)
                                                            }
                                                        },
                                                        icon = Icons.Rounded.Folder
                                                    )
                                                }
                                            }

                                            // Database Sub-menu (Level 1 Parent)
                                            val databaseExpanded = expandedDrawerItems["Database"] ?: false
                                            DrawerTreeItemRow(
                                                title = "Database",
                                                level = 1,
                                                isExpanded = databaseExpanded,
                                                isSelected = false,
                                                hasChildren = true,
                                                onToggleExpand = {
                                                    expandedDrawerItems["Database"] = !(expandedDrawerItems["Database"] ?: false)
                                                },
                                                onClick = {
                                                    expandedDrawerItems["Database"] = !(expandedDrawerItems["Database"] ?: false)
                                                },
                                                icon = Icons.Rounded.Storage
                                            )

                                            AnimatedVisibility(
                                                visible = databaseExpanded,
                                                enter = expandVertically(),
                                                exit = shrinkVertically()
                                            ) {
                                                Column {
                                                    // Backup Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "Backup",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = false,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            scope.launch { drawerState.close() }
                                                            databaseDialogAction = "Backup"
                                                        },
                                                        icon = Icons.Rounded.Backup
                                                    )

                                                    // Restore Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "Restore",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = false,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            scope.launch { drawerState.close() }
                                                            availableBackupFiles = mainViewModel.getAvailableBackupFiles(context)
                                                            selectedBackupFile = availableBackupFiles.firstOrNull()
                                                            selectedRestoreMode = RestoreMode.OVERWRITE
                                                            showRestoreFilePickerDialog = true
                                                        },
                                                        icon = Icons.Rounded.Restore
                                                    )

                                                    // Erase Sub-item
                                                    DrawerTreeItemRow(
                                                        title = "Erase",
                                                        level = 2,
                                                        isExpanded = false,
                                                        isSelected = false,
                                                        hasChildren = false,
                                                        onToggleExpand = {},
                                                        onClick = {
                                                            scope.launch { drawerState.close() }
                                                            databaseDialogAction = "Erase"
                                                        },
                                                        icon = Icons.Rounded.DeleteForever
                                                    )
                                                }
                                            }

                                            // Debugging Menu Item (Level 1)
                                            DrawerTreeItemRow(
                                                title = "Debugging",
                                                level = 1,
                                                isExpanded = false,
                                                isSelected = selectedTestItem == null && backStack.lastOrNull() is AppNavKey.Debugging,
                                                hasChildren = false,
                                                onToggleExpand = {},
                                                onClick = {
                                                    selectedTestItem = null
                                                    scope.launch { drawerState.close() }
                                                    if (backStack.lastOrNull() !is AppNavKey.Debugging) {
                                                        backStack.add(AppNavKey.Debugging)
                                                    }
                                                },
                                                icon = Icons.Rounded.BugReport
                                            )
                                        }
                                    }
                                }
                            }

                            if (canDrawerScrollDown) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 12.dp),
                                    onClick = {
                                        scope.launch {
                                            drawerScrollState.animateScrollTo(
                                                (drawerScrollState.value + 300).coerceAtMost(drawerScrollState.maxValue)
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "More menu items below",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                ) {
                    NavDisplay(
                        backStack = backStack,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                        },
                        entryProvider = entryProvider {
                            entry<AppNavKey.Main> {
                                MainScreen(
                                    viewModel = mainViewModel,
                                    onNavigateToConfigure = dropUnlessResumed {
                                        backStack.add(AppNavKey.Configure)
                                    },
                                    onNavigateToDisplay = dropUnlessResumed {
                                        backStack.add(AppNavKey.Display)
                                    },
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.Configure> {
                                ConfigureScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.DataTypesConfig> {
                                DataTypesConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.ModbusByteOrderConfig> {
                                ModbusByteOrderConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.TagsPlc> {
                                ConfigureScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.TagsInternal> {
                                InternalTagsConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.Display> {
                                DisplayScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.ListScreens> {
                                ListScreensConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.GraphicsScreens> {
                                GraphicsScreensConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.GraphicsGroups> {
                                GraphicsGroupsConfigScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.PollingStatus> {
                                PollingStatusScreen(
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.Debugging> {
                                DebugAnimationScreen(
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.DynamicListScreen> { key ->
                                DynamicListScreen(
                                    screenId = key.screenId,
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            entry<AppNavKey.DynamicGraphicsScreen> { key ->
                                DynamicGraphicsScreen(
                                    screenId = key.screenId,
                                    viewModel = mainViewModel,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onNavigateToScreen = { targetScreen ->
                                        if ((backStack.lastOrNull() as? AppNavKey.DynamicGraphicsScreen)?.screenId != targetScreen.id) {
                                            backStack.add(AppNavKey.DynamicGraphicsScreen(targetScreen.id))
                                        }
                                    }
                                )
                            }
                        }
                    )
                }

                if (showTagPickerDialog) {
                    val internalTags by mainViewModel.internalTags.collectAsStateWithLifecycle()
                    TagNamePickerDialog(
                        hierarchy = hierarchy,
                        dataTypes = dataTypes,
                        internalTags = internalTags,
                        onTagsSelected = { selectedNames, createNewGroup ->
                            Toast.makeText(context, "Selected ${selectedNames.size} item(s) (New Group: $createNewGroup)", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { showTagPickerDialog = false }
                    )
                }

                if (showRecreateDbDialog) {
                    AlertDialog(
                        onDismissRequest = { showRecreateDbDialog = false },
                        title = { Text("Create Database") },
                        text = { Text("Are you sure you want to erase the current database and re-create it with default tags? All current custom screens and data will be reset.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showRecreateDbDialog = false
                                    mainViewModel.recreateDatabaseWithDefaults { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Create Database", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRecreateDbDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Database Backup & Restore Dialogs
                if (databaseDialogAction == "Backup") {
                    AlertDialog(
                        onDismissRequest = { databaseDialogAction = null },
                        title = { Text("Backup Database") },
                        text = { Text("Are you sure that you want to Backup the Database") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    databaseDialogAction = null
                                    mainViewModel.performBackup(context) { result ->
                                        backupResultState = result
                                    }
                                }
                            ) {
                                Text("Proceed")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { databaseDialogAction = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                backupResultState?.let { result ->
                    AlertDialog(
                        onDismissRequest = { backupResultState = null },
                        title = { Text(if (result.isSuccess) "Database Saved Successfully" else "Database Save Failed") },
                        text = {
                            if (result.isSuccess) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("The database has been saved successfully to public storage!")
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("File Name: ${result.fileName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Saved Path:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                            Text(result.filePath, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Total Records Saved: ${result.totalRecords}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            } else {
                                Text("Database save failed.\n\nError: ${result.errorMessage ?: "Unknown error"}")
                            }
                        },
                        confirmButton = {
                            Button(onClick = { backupResultState = null }) {
                                Text("OK")
                            }
                        }
                    )
                }



                if (showRestoreFilePickerDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isRestoringDatabase) showRestoreFilePickerDialog = false
                        },
                        title = { Text("Select File to Restore") },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isRestoringDatabase) {
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
                                                text = "Crunching data and restoring database, please wait...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        openRestoreDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    },
                                    enabled = !isRestoringDatabase,
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

                                val chosen = selectedBackupFile
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
                                            text = "No file selected yet.\nTap 'Browse System Files...' to select a backup JSON file.",
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
                                    val chosen = selectedBackupFile
                                    if (chosen != null) {
                                        isRestoringDatabase = true
                                        if (chosen.uri != null) {
                                            mainViewModel.performRestoreFromUri(context, chosen.uri, selectedRestoreMode) { result ->
                                                isRestoringDatabase = false
                                                showRestoreFilePickerDialog = false
                                                restoreResultState = result
                                                if (result.conflicts.isNotEmpty() && result.mode == RestoreMode.APPEND) {
                                                    activeConflictsQueue = result.conflicts
                                                    activeConflictIndex = 0
                                                    showConflictDialog = true
                                                }
                                            }
                                        } else if (chosen.file != null && chosen.file.exists()) {
                                            mainViewModel.performRestore(chosen.file, selectedRestoreMode) { result ->
                                                isRestoringDatabase = false
                                                showRestoreFilePickerDialog = false
                                                restoreResultState = result
                                                if (result.conflicts.isNotEmpty() && result.mode == RestoreMode.APPEND) {
                                                    activeConflictsQueue = result.conflicts
                                                    activeConflictIndex = 0
                                                    showConflictDialog = true
                                                }
                                            }
                                        } else if (chosen.path.isNotEmpty()) {
                                            val f = File(chosen.path)
                                            mainViewModel.performRestore(f, selectedRestoreMode) { result ->
                                                isRestoringDatabase = false
                                                showRestoreFilePickerDialog = false
                                                restoreResultState = result
                                                if (result.conflicts.isNotEmpty() && result.mode == RestoreMode.APPEND) {
                                                    activeConflictsQueue = result.conflicts
                                                    activeConflictIndex = 0
                                                    showConflictDialog = true
                                                }
                                            }
                                        } else {
                                            isRestoringDatabase = false
                                            Toast.makeText(context, "Selected backup file does not exist.", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "Please select a backup file.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isRestoringDatabase && selectedBackupFile != null
                            ) {
                                if (isRestoringDatabase) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Text("Restoring...")
                                    }
                                } else {
                                    Text("Restore Selected File")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRestoreFilePickerDialog = false },
                                enabled = !isRestoringDatabase
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showConflictDialog && activeConflictIndex < activeConflictsQueue.size) {
                    val currentConflict = activeConflictsQueue[activeConflictIndex]
                    AlertDialog(
                        onDismissRequest = { /* Halt process until acknowledged */ },
                        title = { Text("Restore Conflict Encountered") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Item '${currentConflict.entityName}' (${currentConflict.entityType}) cannot be added to the database.")
                                Text("Reason: ${currentConflict.reason}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        confirmButton = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = {
                                        // Acknowledge All - close conflict dialogs and proceed to final statistics
                                        showConflictDialog = false
                                    }
                                ) {
                                    Text("Acknowledge All")
                                }
                                Button(
                                    onClick = {
                                        // Acknowledge single conflict
                                        if (activeConflictIndex + 1 < activeConflictsQueue.size) {
                                            activeConflictIndex++
                                        } else {
                                            showConflictDialog = false
                                        }
                                    }
                                ) {
                                    Text("Acknowledge")
                                }
                            }
                        }
                    )
                }

                restoreResultState?.let { result ->
                    if (!showConflictDialog) {
                        AlertDialog(
                            onDismissRequest = { restoreResultState = null },
                            title = { Text(if (result.isSuccess) "Database Restore Completed" else "Database Restore Failed") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (result.isSuccess) {
                                        Text("Restore Operation Completed Successfully!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("File Name: ${result.fileName}")
                                        Text("Path: ${result.filePath}")
                                        Text("File Size: ${result.fileSizeText}")
                                        Text("Total Records in File: ${result.totalRecordsInFile}")
                                        Text("Successfully Restored: ${result.restoredCount}", fontWeight = FontWeight.Bold)
                                        Text("Skipped / Conflicts: ${result.skippedCount}")
                                        Text("Mode Used: ${if (result.mode == RestoreMode.OVERWRITE) "Overwrite" else "Append"}")
                                    } else {
                                        Text("Database restore failed.\n\nError: ${result.errorMessage ?: "Unknown error"}")
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { restoreResultState = null }) {
                                    Text("OK")
                                }
                            }
                        )
                    }
                }

                if (databaseDialogAction == "Erase") {
                    AlertDialog(
                        onDismissRequest = { databaseDialogAction = null },
                        title = { Text("Erase Database") },
                        text = { Text("Are you sure that you want to Erase the Database") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    databaseDialogAction = null
                                    mainViewModel.recreateDatabaseWithDefaults { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("Erase Database", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { databaseDialogAction = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerTreeItemRow(
    title: String,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val startPadding = (level * 16).dp
    val textStyle = when (level) {
        0 -> MaterialTheme.typography.titleMedium
        1 -> MaterialTheme.typography.bodyMedium
        else -> MaterialTheme.typography.bodySmall
    }

    NavigationDrawerItem(
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = textStyle,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                if (hasChildren) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        selected = isSelected,
        onClick = {
            if (hasChildren) {
                onToggleExpand()
            }
            onClick()
        },
        icon = icon?.let { { Icon(it, contentDescription = null) } },
        modifier = Modifier
            .padding(start = startPadding)
            .padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
