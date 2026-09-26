package com.example.ailisttest.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.PacketWithTags
import com.example.ailisttest.data.modbus.PacketPollingStats
import com.example.ailisttest.ui.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SelectedPollingTarget(
    val nodeWithPackets: NodeWithPacketsAndTags,
    val packetWithTags: PacketWithTags
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun PollingStatusScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val isPollingEnabled by viewModel.isPollingEnabled.collectAsStateWithLifecycle()
    val statsMap by viewModel.packetPollingStatsMap.collectAsStateWithLifecycle()

    var selectedTarget by remember { mutableStateOf<SelectedPollingTarget?>(null) }
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    val scope = rememberCoroutineScope()

    BackHandler(enabled = navigator.currentDestination?.pane == ListDetailPaneScaffoldRole.Detail) {
        scope.launch { navigator.navigateBack() }
    }

    val baseDirective = navigator.scaffoldDirective
    val customDirective = remember(baseDirective) {
        baseDirective.copy(defaultPanePreferredWidth = 450.dp)
    }

    Scaffold { innerPadding ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(innerPadding),
            directive = customDirective,
            value = navigator.scaffoldValue,
            listPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    Column {
                        TopAppBar(
                            title = { Text("Polling Status") },
                            navigationIcon = {
                                IconButton(onClick = onOpenDrawer) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                }
                            }
                        )

                        // Top Polling Master Toggle
                        ElevatedCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Autorenew,
                                    contentDescription = null,
                                    tint = if (isPollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .padding(end = 12.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modbus Background Polling",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isPollingEnabled) "Status: ACTIVE (Repetitive Read ON)" else "Status: OFF (Polling Disabled)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isPollingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPollingEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setPollingEnabled(enabled)
                                    }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        PollingNodeTreeList(
                            hierarchy = hierarchy,
                            statsMap = statsMap,
                            selectedTarget = selectedTarget,
                            onPacketSelected = { nodeWithPackets, packetWithTags ->
                                selectedTarget = SelectedPollingTarget(nodeWithPackets, packetWithTags)
                                scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail) }
                            }
                        )
                    }
                }
            },
            detailPane = {
                AnimatedPane(modifier = Modifier.fillMaxSize()) {
                    selectedTarget?.let { target ->
                        val packetStats = statsMap[target.packetWithTags.packet.id]
                        PacketPollingDetailPane(
                            target = target,
                            stats = packetStats,
                            isPollingEnabled = isPollingEnabled,
                            onBack = { scope.launch { navigator.navigateBack() } }
                        )
                    } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a packet from the list to view polling statistics")
                    }
                }
            }
        )
    }
}

@Composable
fun PollingNodeTreeList(
    hierarchy: List<NodeWithPacketsAndTags>,
    statsMap: Map<Long, PacketPollingStats>,
    selectedTarget: SelectedPollingTarget?,
    onPacketSelected: (NodeWithPacketsAndTags, PacketWithTags) -> Unit
) {
    val expandedNodes = remember { mutableStateMapOf<Long, Boolean>() }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        hierarchy.forEach { nodeWithPackets ->
            val node = nodeWithPackets.node
            val isNodeExpanded = expandedNodes[node.id] ?: true

            // Calculate overall alarm bell color for Node
            val nodePackets = nodeWithPackets.packetsWithTags
            val hasCommFailure = nodePackets.any { p ->
                val stats = statsMap[p.packet.id]
                stats != null && !stats.commStatus && stats.totalPackets > 0
            }
            val alarmBellColor = if (hasCommFailure) Color(0xFFF44336) else Color(0xFF4CAF50)

            item(key = "polling_node_${node.id}") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedNodes[node.id] = !(expandedNodes[node.id] ?: true) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = { expandedNodes[node.id] = !(expandedNodes[node.id] ?: true) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isNodeExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Node Alarm Bell Indicator Icon
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = "Alarm Indicator",
                        tint = alarmBellColor,
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(22.dp)
                    )

                    Icon(
                        imageVector = Icons.Rounded.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = node.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = node.ipAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isNodeExpanded) {
                items(nodePackets, key = { "polling_packet_${it.packet.id}" }) { packetWithTags ->
                    val packet = packetWithTags.packet
                    val stats = statsMap[packet.id]
                    val isSelected = selectedTarget?.packetWithTags?.packet?.id == packet.id
                    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg, RoundedCornerShape(4.dp))
                            .clickable { onPacketSelected(nodeWithPackets, packetWithTags) }
                            .padding(start = 48.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountTree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packet.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "Period: ${packet.period}ms | Size: ${packet.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Comm Status Badge
                        val (statusText, statusBg, statusFg) = when {
                            stats == null || stats.totalPackets == 0L -> Triple("IDLE", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
                            stats.commStatus -> Triple("COMM OK", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            else -> Triple("FAIL", Color(0xFFFFEBEE), Color(0xFFC62828))
                        }

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
            }
        }
    }
}

@Composable
fun PacketPollingDetailPane(
    target: SelectedPollingTarget,
    stats: PacketPollingStats?,
    isPollingEnabled: Boolean,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

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
                text = "Packet Polling Statistics",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                    )
                    Text(
                        text = target.packetWithTags.packet.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    val (statusText, statusBg, statusFg) = when {
                        !isPollingEnabled -> Triple("POLLING OFF", MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
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

                HorizontalDivider()

                Text("Node: ${target.nodeWithPackets.node.name} (${target.nodeWithPackets.node.ipAddress})", fontWeight = FontWeight.SemiBold)
                Text("Description: ${target.packetWithTags.packet.description}")

                HorizontalDivider()

                Text("Polling Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Packets Polled:", fontWeight = FontWeight.SemiBold)
                    Text("${stats?.totalPackets ?: 0}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("% Success (Last 20 Polls):", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${stats?.successRatePercentage ?: 0}%",
                        fontWeight = FontWeight.Bold,
                        color = if ((stats?.successRatePercentage ?: 0) >= 90) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Last Poll Time:", fontWeight = FontWeight.SemiBold)
                    Text(if (stats != null && stats.lastReadTimestamp > 0) dateFormat.format(Date(stats.lastReadTimestamp)) else "-")
                }

                if (!stats?.lastErrorMessage.isNullOrEmpty()) {
                    Text(
                        text = "Last Error: ${stats?.lastErrorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                Text("Modbus Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Poll Interval Period: ${target.packetWithTags.packet.period} ms")
                Text("Start Offset Address: ${target.packetWithTags.packet.offset}")
                Text("Size: ${target.packetWithTags.packet.size} registers")
                Text("Slave Unit ID: ${target.packetWithTags.packet.slaveNode}")
            }
        }
    }
}
