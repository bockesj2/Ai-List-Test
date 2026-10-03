package com.example.ailisttest.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.ailisttest.data.GraphicsImageManager
import com.example.ailisttest.data.local.Screens
import com.example.ailisttest.ui.MainViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicGraphicsScreen(
    screenId: Long,
    viewModel: MainViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToScreen: (Screens) -> Unit = {}
) {
    val graphicsScreensWithItems by viewModel.graphicsScreensWithItems.collectAsStateWithLifecycle()
    val graphicsGroupsWithItems by viewModel.graphicsGroupsWithItems.collectAsStateWithLifecycle()
    val allScreens by viewModel.allScreens.collectAsStateWithLifecycle()
    val hierarchy by viewModel.hierarchy.collectAsStateWithLifecycle()
    val screenWithItems = remember(graphicsScreensWithItems, graphicsGroupsWithItems, screenId) {
        graphicsScreensWithItems.find { it.screen.id == screenId }
            ?: graphicsGroupsWithItems.find { it.screen.id == screenId }
    }
    val currentScreen = screenWithItems?.screen
    val screenName = currentScreen?.Name ?: "Graphics Screen"
    val itemsList = remember(screenWithItems) {
        screenWithItems?.items ?: emptyList()
    }

    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(GifDecoder.Factory())
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                }
            }
            .build()
    }
    val screenBgFile = remember(currentScreen?.backgroundImage) {
        if (currentScreen != null && currentScreen.backgroundImage.isNotBlank()) {
            GraphicsImageManager.findGraphicsFile(context, currentScreen.backgroundImage)
        } else null
    }
    val screenBgUri = remember(screenBgFile?.absolutePath) {
        if (screenBgFile != null && screenBgFile.exists()) Uri.fromFile(screenBgFile) else null
    }

    val parsedScreenBgColor = remember(currentScreen?.backgroundColorHex, currentScreen?.backgroundType) {
        if (currentScreen?.backgroundType == "Transparent") {
            Color.Transparent
        } else {
            try {
                Color(android.graphics.Color.parseColor(currentScreen?.backgroundColorHex ?: "#FAFAFA"))
            } catch (_: Exception) {
                Color(0xFFFAFAFA)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenName) },
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
                .background(parsedScreenBgColor)
        ) {
            // Render Stretched Screen Background Image if configured
            if (currentScreen?.backgroundType == "Image" && screenBgFile != null && screenBgFile.exists()) {
                val screenCoilModel = GraphicsImageManager.getCoilModel(context, screenBgFile)
                AsyncImage(
                    model = screenCoilModel ?: screenBgUri,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (itemsList.isEmpty()) {
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
                            text = screenName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No configured graphics items on this screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val density = LocalDensity.current
                itemsList.forEach { itemWithTag ->
                    val item = itemWithTag.item
                    val tag = itemWithTag.tag

                    if (item.DisplayType.isNotEmpty() && item.DisplayType != "Rectangle") {
                        val widthDp = with(density) { item.Width.toDp() }
                        val heightDp = with(density) { item.Height.toDp() }

                        GraphicsItemWidget(
                            item = item,
                            tag = tag,
                            allScreens = allScreens,
                            hierarchy = hierarchy,
                            enabled = false,
                            onNavigateToScreen = onNavigateToScreen,
                            onBitAction = { bitVal, isToggle ->
                                if (tag != null) {
                                    if (item.Type in 1..1000) {
                                        val bitIdx = item.Type - 1
                                        val currParentVal = tag.storedValue.toLongOrNull() ?: 0L
                                        val newParentVal = if (isToggle) {
                                            currParentVal xor (1L shl bitIdx)
                                        } else if (bitVal != null) {
                                            if (bitVal) (currParentVal or (1L shl bitIdx)) else (currParentVal and (1L shl bitIdx).inv())
                                        } else currParentVal

                                        viewModel.updateTagValueAndWrite(tag, newParentVal.toString())
                                    } else {
                                        if (isToggle) {
                                            val currVal = tag.storedValue.toDoubleOrNull()?.toInt() ?: 0
                                            val newVal = if (currVal == 0) "1" else "0"
                                            viewModel.updateTagValueAndWrite(tag, newVal)
                                        } else if (bitVal != null) {
                                            val newVal = if (bitVal) "1" else "0"
                                            viewModel.updateTagValueAndWrite(tag, newVal)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = item.OffsetX
                                    translationY = item.OffsetY
                                }
                                .size(width = widthDp, height = heightDp)
                        )
                    }
                }
            }
        }
    }
}
