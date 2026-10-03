package com.example.ailisttest.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.ailisttest.data.GraphicsImageManager
import java.util.Locale

data class DebugImageMetadata(
    val name: String,
    val size: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val isAnimated: Boolean,
    val uriString: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugAnimationScreen(
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current

    // Look for clamp-none.gif in graphics directory as default initial file
    val initialFile = remember(context) {
        GraphicsImageManager.findGraphicsFile(context, "clamp-none.gif")
    }

    val initialUri = remember(initialFile) {
        if (initialFile != null && initialFile.exists()) {
            Uri.fromFile(initialFile)
        } else null
    }

    var selectedUri by remember(initialUri) { mutableStateOf<Uri?>(initialUri) }
    var metadata by remember(initialUri) {
        mutableStateOf<DebugImageMetadata?>(
            initialUri?.let { extractDebugMetadata(context, it) }
        )
    }
    var showBoundingBox by remember { mutableStateOf(true) }

    // Configure Coil ImageLoader with GifDecoder / ImageDecoderDecoder
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flag)
            } catch (_: Exception) {}

            selectedUri = uri
            metadata = extractDebugMetadata(context, uri)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Pets, contentDescription = "Turtle Icon", tint = MaterialTheme.colorScheme.primary)
                        Text("Visual Frame Debugger", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { showBoundingBox = !showBoundingBox }) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "Toggle Bounding Box",
                            tint = if (showBoundingBox) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Pick Document Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        launcher.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Open Image / GIF", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Bounding Box Toggle Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bounding Box Frame Debugger",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Green outline, corner reticles & dimension overlay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showBoundingBox,
                        onCheckedChange = { showBoundingBox = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00E676),
                            checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Visual Frame Debugger Viewer Card with Bounding Box
            ImageViewerBoundingBox(
                selectedUri = selectedUri,
                imageLoader = imageLoader,
                metadata = metadata,
                showBoundingBox = showBoundingBox
            )

            // Metadata & Animation Header UI
            if (metadata != null) {
                DebugMetadataHeaderCard(metadata = metadata!!)
            }
        }
    }
}

@Composable
fun ImageViewerBoundingBox(
    modifier: Modifier = Modifier,
    selectedUri: Uri?,
    imageLoader: ImageLoader,
    metadata: DebugImageMetadata?,
    showBoundingBox: Boolean
) {
    val debugGreen = Color(0xFF00E676)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri != null) {
                val coilModel = GraphicsImageManager.getCoilModel(LocalContext.current, selectedUri)
                AsyncImage(
                    model = coilModel ?: selectedUri,
                    imageLoader = imageLoader,
                    contentDescription = "Selected Image or Animation",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (showBoundingBox) 20.dp else 0.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "No file selected.\nTap above to pick GIF, WebP, PNG, or JPEG.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Green Bounding Box Overlay & Debugger Reticles
            if (showBoundingBox && selectedUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .drawBehind {
                            val strokeWidth = 2.5.dp.toPx()
                            val cornerLength = 28.dp.toPx()
                            val color = debugGreen

                            // Draw rectangle outline
                            drawRect(
                                color = color.copy(alpha = 0.85f),
                                style = Stroke(width = strokeWidth)
                            )

                            // Draw corner reticles / brackets
                            // Top-left
                            drawLine(color, Offset(0f, 0f), Offset(cornerLength, 0f), strokeWidth * 2f)
                            drawLine(color, Offset(0f, 0f), Offset(0f, cornerLength), strokeWidth * 2f)
                            // Top-right
                            drawLine(color, Offset(size.width, 0f), Offset(size.width - cornerLength, 0f), strokeWidth * 2f)
                            drawLine(color, Offset(size.width, 0f), Offset(size.width, cornerLength), strokeWidth * 2f)
                            // Bottom-left
                            drawLine(color, Offset(0f, size.height), Offset(cornerLength, size.height), strokeWidth * 2f)
                            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - cornerLength), strokeWidth * 2f)
                            // Bottom-right
                            drawLine(color, Offset(size.width, size.height), Offset(size.width - cornerLength, size.height), strokeWidth * 2f)
                            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - cornerLength), strokeWidth * 2f)
                        }
                ) {
                    // Top-Left Tag: Bounding Box Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        contentColor = debugGreen
                    ) {
                        Text(
                            text = "BOUNDING BOX",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Top-Right Tag: Dimensions
                    if (metadata != null && metadata.width > 0 && metadata.height > 0) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            contentColor = debugGreen
                        ) {
                            Text(
                                text = "${metadata.width} × ${metadata.height} px",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom-Left Tag: MIME Type / Animation Status
                    if (metadata != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            contentColor = debugGreen
                        ) {
                            Text(
                                text = if (metadata.isAnimated) "ANIMATED • ${metadata.mimeType.substringAfter('/')}" else metadata.mimeType,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom-Right Tag: Scaling Mode
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        contentColor = debugGreen
                    ) {
                        Text(
                            text = "ContentScale.Fit",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebugMetadataHeaderCard(metadata: DebugImageMetadata) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metadata.mimeType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (metadata.isAnimated) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (metadata.isAnimated) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (metadata.isAnimated) Icons.Rounded.PlayArrow else Icons.Rounded.Image,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (metadata.isAnimated) "ANIMATED" else "STATIC",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DebugMetadataDetailItem(
                    label = "File Size",
                    value = metadata.size,
                    modifier = Modifier.weight(1f)
                )
                DebugMetadataDetailItem(
                    label = "Dimensions",
                    value = if (metadata.width > 0 && metadata.height > 0) "${metadata.width} × ${metadata.height} px" else "Unknown",
                    modifier = Modifier.weight(1f)
                )
            }

            DebugMetadataDetailItem(
                label = "URI",
                value = metadata.uriString,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DebugMetadataDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun extractDebugMetadata(context: Context, uri: Uri, fallbackFile: File? = null): DebugImageMetadata {
    var name = "Unknown"
    var sizeStr = "Unknown size"
    var width = 0
    var height = 0
    var isAnimated = false
    val resolver = context.contentResolver
    var mimeType = try { resolver.getType(uri) ?: "image/*" } catch (_: Exception) { "image/*" }

    // 1. Query ContentResolver for DISPLAY_NAME and SIZE
    try {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                    name = cursor.getString(nameIndex) ?: "Unknown"
                }
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    val sizeBytes = cursor.getLong(sizeIndex)
                    if (sizeBytes > 0) sizeStr = formatDebugFileSize(sizeBytes)
                }
            }
        }
    } catch (_: Exception) {}

    // 2. Fallback to File if URI path or fallbackFile is available
    val targetFile = fallbackFile ?: (if (uri.scheme == "file" || uri.path != null) {
        val f = File(uri.path ?: "")
        if (f.exists() && f.isFile) f else null
    } else null)

    if (targetFile != null && targetFile.exists() && targetFile.isFile) {
        if (name == "Unknown" || name.isBlank()) name = targetFile.name
        if (sizeStr == "Unknown size" && targetFile.length() > 0) {
            sizeStr = formatDebugFileSize(targetFile.length())
        }
    }

    // 3. Fallback MimeType based on file extension
    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (mimeType == "image/*" || mimeType.isBlank()) {
        mimeType = when (ext) {
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "bmp" -> "image/bmp"
            else -> if (ext.isNotBlank()) "image/$ext" else "image/*"
        }
    }

    if (ext == "gif" || ext == "webp" || mimeType.contains("gif") || mimeType.contains("webp") || mimeType.contains("apng")) {
        isAnimated = true
    }

    // 4. Decode Width & Height via BitmapFactory
    try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
        if (options.outWidth > 0) width = options.outWidth
        if (options.outHeight > 0) height = options.outHeight
        if (!options.outMimeType.isNullOrBlank()) mimeType = options.outMimeType!!
    } catch (_: Exception) {}

    // 5. Fallback Width & Height decoding from File or Byte Array
    if ((width <= 0 || height <= 0) && targetFile != null && targetFile.exists()) {
        try {
            val bytes = targetFile.readBytes()
            if (bytes.isNotEmpty()) {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth > 0) width = options.outWidth
                if (options.outHeight > 0) height = options.outHeight

                if (width <= 0 || height <= 0) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        width = bitmap.width
                        height = bitmap.height
                        bitmap.recycle()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // 6. Fallback Width & Height decoding via ImageDecoder (Android 9+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            val source = if (targetFile != null && targetFile.exists()) {
                ImageDecoder.createSource(targetFile)
            } else {
                ImageDecoder.createSource(resolver, uri)
            }
            val drawable = ImageDecoder.decodeDrawable(source)
            if (drawable is AnimatedImageDrawable) {
                isAnimated = true
                if (width <= 0) width = drawable.intrinsicWidth
                if (height <= 0) height = drawable.intrinsicHeight
            } else if (drawable != null) {
                if (width <= 0) width = drawable.intrinsicWidth
                if (height <= 0) height = drawable.intrinsicHeight
            }
        } catch (_: Exception) {}
    }

    // Ensure fallback non-zero dimensions if file exists
    if (width <= 0) width = 300
    if (height <= 0) height = 300

    return DebugImageMetadata(
        name = if (name.isBlank()) "Image File" else name,
        size = if (sizeStr.isBlank()) "Available" else sizeStr,
        mimeType = mimeType,
        width = width,
        height = height,
        isAnimated = isAnimated,
        uriString = uri.toString()
    )
}

fun formatDebugFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}
