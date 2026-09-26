package com.example.ailisttest.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ailisttest.data.local.NodeWithPacketsAndTags
import com.example.ailisttest.data.local.PacketWithTags
import com.example.ailisttest.ui.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(viewModel: MainViewModel, onOpenDrawer: () -> Unit) {
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Status") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(hierarchy) { nodeWithPackets ->
                    NodeDisplayItem(nodeWithPackets)
                }
            }

            AnimatedVisibility(
                visible = canScrollDown,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
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
                            .padding(8.dp)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NodeDisplayItem(nodeWithPackets: NodeWithPacketsAndTags) {
    var expanded by remember { mutableStateOf(true) }

    Column {
        ListItem(
            headlineContent = { Text(nodeWithPackets.node.name, fontWeight = FontWeight.Bold) },
            supportingContent = { Text(nodeWithPackets.node.ipAddress) },
            leadingContent = { Icon(Icons.Rounded.NetworkCheck, contentDescription = null) },
            trailingContent = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (expanded) {
            nodeWithPackets.packetsWithTags.forEach { packetWithTags ->
                PacketDisplayItem(packetWithTags)
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
fun PacketDisplayItem(packetWithTags: PacketWithTags) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 32.dp)) {
        ListItem(
            headlineContent = { Text(packetWithTags.packet.name) },
            supportingContent = { Text("Period: ${packetWithTags.packet.period}ms") },
            trailingContent = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
        )

        if (expanded) {
            packetWithTags.tags.forEach { tag ->
                ListItem(
                    headlineContent = { Text(tag.name) },
                    supportingContent = { Text(tag.description) },
                    trailingContent = {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = tag.storedValue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    leadingContent = { Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}
