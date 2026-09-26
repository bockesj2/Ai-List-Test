package com.example.ailisttest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import com.example.ailisttest.data.local.NodeEntity
import com.example.ailisttest.data.modbus.ModbusByteOrder
import com.example.ailisttest.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModbusByteOrderConfigScreen(
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val allNodes = remember(hierarchy) { hierarchy.map { it.node } }
    var selectedNodeId by remember(allNodes) { mutableLongStateOf(allNodes.firstOrNull()?.id ?: 0L) }
    var nodeDropdownExpanded by remember { mutableStateOf(false) }

    val selectedNode = remember(selectedNodeId, allNodes) {
        allNodes.find { it.id == selectedNodeId } ?: allNodes.firstOrNull()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure / Modbus Byte Order") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val canScrollMore by remember { derivedStateOf { scrollState.canScrollForward } }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Per-Node Modbus Byte Order Configuration",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure the byte/word rearrangement algorithm for each PLC node. Select 'Click Plus PLC' for standard CDAB Word-Swap, or choose 'Custom' to select any algorithm.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (allNodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No PLC nodes configured yet. Create a node under Configuration / Tags.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Node Selection Dropdown Card
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Select Node",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            ExposedDropdownMenuBox(
                                expanded = nodeDropdownExpanded,
                                onExpandedChange = { nodeDropdownExpanded = !nodeDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedNode?.let { "${it.name} (${it.ipAddress})" } ?: "Select Node",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Node") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nodeDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = nodeDropdownExpanded,
                                    onDismissRequest = { nodeDropdownExpanded = false }
                                ) {
                                    allNodes.forEach { node ->
                                        DropdownMenuItem(
                                            text = { Text("${node.name} (${node.ipAddress})") },
                                            onClick = {
                                                selectedNodeId = node.id
                                                nodeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Cards for All Nodes
                    allNodes.forEach { node ->
                        NodeByteOrderCard(
                            node = node,
                            isSelected = node.id == selectedNode?.id,
                            onUpdateNode = { updatedNode ->
                                viewModel.updateNode(updatedNode)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Updated Modbus Byte Order for node '${updatedNode.name}'")
                                }
                            }
                        )
                    }
                }
            }

            ScrollMoreDownIndicator(
                canScrollMore = canScrollMore,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeByteOrderCard(
    node: NodeEntity,
    isSelected: Boolean,
    onUpdateNode: (NodeEntity) -> Unit
) {
    var plcPreset by remember(node.id, node.plcPreset) {
        mutableStateOf(node.plcPreset.ifEmpty { "Click Plus PLC" })
    }
    var byteOrder by remember(node.id, node.byteOrder) {
        mutableStateOf(node.byteOrder.ifEmpty { "CDAB (3412) — Word-Swap" })
    }

    var plcPresetDropdownExpanded by remember { mutableStateOf(false) }
    var byteOrderDropdownExpanded by remember { mutableStateOf(false) }

    val isCustomPreset = plcPreset.equals("Custom", ignoreCase = true)
    val plcPresets = listOf("Custom", "Click Plus PLC")

    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Node: ${node.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "IP Address: ${node.ipAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dropdown 1: PLC Preset ("Custom", "Click Plus PLC")
                ExposedDropdownMenuBox(
                    expanded = plcPresetDropdownExpanded,
                    onExpandedChange = { plcPresetDropdownExpanded = !plcPresetDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = plcPreset,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("PLC Preset") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = plcPresetDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = plcPresetDropdownExpanded,
                        onDismissRequest = { plcPresetDropdownExpanded = false }
                    ) {
                        plcPresets.forEach { presetOption ->
                            DropdownMenuItem(
                                text = { Text(presetOption) },
                                onClick = {
                                    plcPreset = presetOption
                                    val newAlgorithm = if (presetOption.equals("Custom", ignoreCase = true)) byteOrder else "CDAB (3412) — Word-Swap"
                                    byteOrder = newAlgorithm
                                    plcPresetDropdownExpanded = false
                                    onUpdateNode(node.copy(plcPreset = presetOption, byteOrder = newAlgorithm))
                                }
                            )
                        }
                    }
                }

                // Dropdown 2: Byte Order Algorithm (Editable if "Custom", Read-Only / Locked if non-custom)
                val isAlgorithmEditable = isCustomPreset

                ExposedDropdownMenuBox(
                    expanded = byteOrderDropdownExpanded && isAlgorithmEditable,
                    onExpandedChange = {
                        if (isAlgorithmEditable) {
                            byteOrderDropdownExpanded = !byteOrderDropdownExpanded
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = byteOrder,
                        onValueChange = {},
                        readOnly = true,
                        enabled = isAlgorithmEditable,
                        label = { Text(if (isAlgorithmEditable) "Algorithm" else "Algorithm (Locked)") },
                        trailingIcon = {
                            if (isAlgorithmEditable) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = byteOrderDropdownExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    if (isAlgorithmEditable) {
                        ExposedDropdownMenu(
                            expanded = byteOrderDropdownExpanded,
                            onDismissRequest = { byteOrderDropdownExpanded = false }
                        ) {
                            ModbusByteOrder.ALL_LABELS.forEach { orderLabel ->
                                DropdownMenuItem(
                                    text = { Text(orderLabel) },
                                    onClick = {
                                        byteOrder = orderLabel
                                        byteOrderDropdownExpanded = false
                                        onUpdateNode(node.copy(plcPreset = plcPreset, byteOrder = orderLabel))
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
