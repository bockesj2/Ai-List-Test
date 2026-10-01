package com.example.ailisttest.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ailisttest.ui.components.ScrollMoreDownIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val PRESET_SWATCHES = listOf(
    "#F5F5F5", "#E3F2FD", "#E8F5E9", "#F3E5F5",
    "#FFF3E0", "#FFEBEE", "#E0F7FA", "#FFFDE7",
    "#2196F3", "#4CAF50", "#9C27B0", "#FF9800",
    "#F44336", "#00BCD4", "#607D8B", "#3F51B5"
)

fun Color.toHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupColorPickerDialog(
    initialColorHex: String = "#F5F5F5",
    onColorSelected: (colorHex: String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColor = remember(initialColorHex) { parseHexColor(initialColorHex) }

    // Convert initial color to HSV
    val hsv = remember(initialColor) {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(
            android.graphics.Color.rgb(
                (initialColor.red * 255).toInt(),
                (initialColor.green * 255).toInt(),
                (initialColor.blue * 255).toInt()
            ),
            hsvArray
        )
        hsvArray
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    val currentColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    }

    val dialogScrollState = rememberScrollState()
    val canScrollDialogDown by remember {
        derivedStateOf { dialogScrollState.canScrollForward }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Standard Color Picker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(dialogScrollState)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Live Color Preview Card & Hex Display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(currentColor)
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected Color",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentColor.toHex(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 2D Saturation / Value Gradient Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    ) {
                        val pureHueColor = remember(hue) { Color.hsv(hue, 1f, 1f) }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(hue) {
                                    detectTapGestures { offset ->
                                        saturation = (offset.x / size.width).coerceIn(0f, 1f)
                                        value = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                                    }
                                }
                                .pointerInput(hue) {
                                    detectDragGestures { change, _ ->
                                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                                        value = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                                    }
                                }
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height

                            // Horizontal white to pure hue
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.White, pureHueColor)
                                )
                            )

                            // Vertical transparent to black overlay
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black)
                                )
                            )

                            // Draw Selector Thumb Circle
                            val thumbX = saturation * canvasWidth
                            val thumbY = (1f - value) * canvasHeight

                            drawCircle(
                                color = Color.White,
                                radius = 12f,
                                center = Offset(thumbX, thumbY),
                                style = Stroke(width = 4f)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 14f,
                                center = Offset(thumbX, thumbY),
                                style = Stroke(width = 2f)
                            )
                        }
                    }

                    // Rainbow Hue Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Hue Spectrum",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        ) {
                            val rainbowColors = remember {
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            }

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, _ ->
                                            hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                                        }
                                    }
                            ) {
                                drawRect(
                                    brush = Brush.horizontalGradient(colors = rainbowColors)
                                )

                                val thumbX = (hue / 360f) * size.width
                                drawCircle(
                                    color = Color.White,
                                    radius = 12f,
                                    center = Offset(thumbX, size.height / 2f)
                                )
                                drawCircle(
                                    color = Color.Black,
                                    radius = 14f,
                                    center = Offset(thumbX, size.height / 2f),
                                    style = Stroke(width = 2.5f)
                                )
                            }
                        }
                    }

                    // Preset Palette Swatches
                    Text(
                        text = "Preset Swatches",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PRESET_SWATCHES) { swatchHex ->
                            val swatchColor = parseHexColor(swatchHex)
                            val isSelected = currentColor.toHex().equals(swatchHex, ignoreCase = true)

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(swatchColor, CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        val swatchHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(
                                            android.graphics.Color.rgb(
                                                (swatchColor.red * 255).toInt(),
                                                (swatchColor.green * 255).toInt(),
                                                (swatchColor.blue * 255).toInt()
                                            ),
                                            swatchHsv
                                        )
                                        hue = swatchHsv[0]
                                        saturation = swatchHsv[1]
                                        value = swatchHsv[2]
                                    }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                ScrollMoreDownIndicator(
                    canScrollMore = canScrollDialogDown,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorSelected(currentColor.toHex())
                    onDismiss()
                }
            ) {
                Text("Apply Color")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
