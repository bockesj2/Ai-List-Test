package com.example.ailisttest.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ailisttest.data.local.BitTags
import com.example.ailisttest.data.local.DataTypes
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import kotlinx.coroutines.launch

fun getPickerDataTypeBadgeColor(dataType: DataTypes?): Color? {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagNamePickerDialog(
    hierarchy: List<NodeWithPacketsAndTags>,
    dataTypes: List<DataTypes> = emptyList(),
    onTagsSelected: (selectedNames: List<String>, createNewGroup: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedTagPaths = remember { mutableStateListOf<String>() }
    var createNewGroup by remember { mutableStateOf(false) }
    val expandedNodes = remember { mutableStateMapOf<Long, Boolean>() }
    val expandedPackets = remember { mutableStateMapOf<Long, Boolean>() }
    val expandedTags = remember { mutableStateMapOf<Long, Boolean>() }

    fun toggleSelection(path: String) {
        if (selectedTagPaths.contains(path)) {
            selectedTagPaths.remove(path)
        } else {
            selectedTagPaths.add(path)
        }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Sell,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Tag Name Picker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                // Scrollable Hierarchical Tree List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        hierarchy.forEach { nodeWithPackets ->
                            val node = nodeWithPackets.node
                            item(key = "picker_node_${node.id}") {
                                PickerTreeRow(
                                    text = "${node.name} (${node.ipAddress})",
                                    level = 0,
                                    isExpanded = expandedNodes[node.id] ?: true,
                                    isSelected = selectedTagPaths.contains(node.name),
                                    hasChildren = nodeWithPackets.packetsWithTags.isNotEmpty(),
                                    leadingIcon = Icons.Rounded.NetworkCheck,
                                    onToggleExpand = { expandedNodes[node.id] = !(expandedNodes[node.id] ?: true) },
                                    onSelect = { toggleSelection(node.name) }
                                )
                            }

                            if (expandedNodes[node.id] != false) {
                                nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                                    val packet = packetWithTags.packet
                                    val packetDataType = packet.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
                                        ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) || it.dataType.equals("INT", ignoreCase = true) }
                                        ?: DataTypes(id = 0, description = "Data Register Short", shortName = "DS", dataType = "INT", bytes = 2, defaultModbusAddress = 400001L, isZeroBasedAddressing = false, hasBits = true)
                                    val packetShortName = packetDataType.shortName.ifEmpty { packetDataType.description }
                                    val packetBadgeText = "$packetShortName${packet.offset}"
                                    val badgeColor = getPickerDataTypeBadgeColor(packetDataType) ?: Color(0xFF81C784)

                                    item(key = "picker_packet_${packet.id}") {
                                        PickerTreeRow(
                                            text = packet.name,
                                            level = 1,
                                            isExpanded = expandedPackets[packet.id] ?: false,
                                            isSelected = selectedTagPaths.contains(packet.name),
                                            hasChildren = packetWithTags.tagsWithBitTags.isNotEmpty(),
                                            leadingIcon = Icons.Rounded.AccountTree,
                                            badgeText = packetBadgeText,
                                            badgeColor = badgeColor,
                                            onToggleExpand = { expandedPackets[packet.id] = !(expandedPackets[packet.id] ?: false) },
                                            onSelect = { toggleSelection(packet.name) }
                                        )
                                    }

                                    if (expandedPackets[packet.id] == true) {
                                        packetWithTags.tagsWithBitTags.forEach { tagWithBitTags ->
                                            val tag = tagWithBitTags.tag
                                            val tagDataType = dataTypes.find { it.id == tag.dataTypeId } ?: packetDataType
                                            val typeHasBits = tagDataType.hasBits
                                            val numBits = tagDataType.bytes * 8

                                            val bitTagsList = if (!typeHasBits || numBits <= 0) {
                                                emptyList()
                                            } else if (tagWithBitTags.bitTags.isNotEmpty()) {
                                                tagWithBitTags.bitTags
                                            } else {
                                                (0 until numBits).map { bitIndex ->
                                                    BitTags(
                                                        id = (tag.id * 100 + bitIndex),
                                                        name = "${tag.name}-$bitIndex",
                                                        parentTagId = tag.id,
                                                        bitIndex = bitIndex
                                                    )
                                                }
                                            }

                                            val hasBitTags = bitTagsList.isNotEmpty()
                                            val tagShortName = tagDataType.shortName.ifEmpty { tagDataType.description }
                                            val tagBadgeText = "$tagShortName${tag.offset}"
                                            val tagBadgeColor = getPickerDataTypeBadgeColor(tagDataType) ?: Color(0xFF81C784)

                                            item(key = "picker_tag_${tag.id}") {
                                                PickerTreeRow(
                                                    text = tag.name,
                                                    level = 2,
                                                    isExpanded = expandedTags[tag.id] ?: false,
                                                    isSelected = selectedTagPaths.contains(tag.name),
                                                    hasChildren = hasBitTags,
                                                    badgeText = tagBadgeText,
                                                    badgeColor = tagBadgeColor,
                                                    onToggleExpand = { expandedTags[tag.id] = !(expandedTags[tag.id] ?: false) },
                                                    onSelect = { toggleSelection(tag.name) }
                                                )
                                            }

                                            if (expandedTags[tag.id] == true && hasBitTags) {
                                                items(bitTagsList, key = { "picker_bitTag_${it.parentTagId}_${it.bitIndex}" }) { bitTag ->
                                                    PickerTreeRow(
                                                        text = bitTag.name,
                                                        level = 3,
                                                        isExpanded = false,
                                                        isSelected = selectedTagPaths.contains(bitTag.name),
                                                        hasChildren = false,
                                                        badgeText = "${tagShortName}${tag.offset}:${bitTag.bitIndex}",
                                                        badgeColor = Color(0xFF00BCD4),
                                                        onToggleExpand = {},
                                                        onSelect = { toggleSelection(bitTag.name) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (canScrollDown) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp),
                            onClick = {
                                scope.launch {
                                    val total = listState.layoutInfo.totalItemsCount
                                    val nextIndex = (listState.firstVisibleItemIndex + 4).coerceAtMost(total - 1)
                                    if (nextIndex >= 0) {
                                        listState.animateScrollToItem(nextIndex)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "More content below",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selected Items Section at Bottom
                Text(
                    text = "Selected Items (${selectedTagPaths.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                ) {
                    if (selectedTagPaths.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "No items selected. Tap items above to select.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxWidth()
                        ) {
                            items(selectedTagPaths) { itemPath ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = itemPath,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { selectedTagPaths.remove(itemPath) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Create New Group Checkbox Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { createNewGroup = !createNewGroup }
                        .padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = createNewGroup,
                        onCheckedChange = { createNewGroup = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Put all items into a new group",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onTagsSelected(selectedTagPaths.toList(), createNewGroup)
                    onDismiss()
                },
                enabled = selectedTagPaths.isNotEmpty()
            ) {
                Text(if (selectedTagPaths.isEmpty()) "Select" else "Select (${selectedTagPaths.size})")
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
fun PickerTreeRow(
    text: String,
    level: Int,
    isExpanded: Boolean,
    isSelected: Boolean,
    hasChildren: Boolean,
    leadingIcon: ImageVector? = null,
    badgeText: String? = null,
    badgeColor: Color? = null,
    offsetText: String? = null,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit
) {
    val startIndent = (level * 16).dp
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(4.dp))
            .clickable(onClick = onSelect)
            .padding(start = startIndent, top = 4.dp, bottom = 4.dp, end = 8.dp)
    ) {
        if (hasChildren) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(18.dp)
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

        Text(
            text = text,
            style = if (level >= 2) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
