package com.example.ailisttest.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GraphicsFileInfo(
    val file: File?,
    val name: String,
    val path: String,
    val sizeText: String,
    val dateText: String,
    val uri: Uri? = null
)

data class GraphicsImageConfig(
    val fileName: String = "",
    val offsetXTagId: Long? = null,
    val offsetYTagId: Long? = null,
    val rotationTagId: Long? = null,
    val isAnimated: Boolean = false,
    val animMode: String = "Continuous",
    val animTagId: Long? = null,
    val isVisibilityEnabled: Boolean = false,
    val visibilityTagId: Long? = null,
    val visibilityBitIndex: Int? = null,
    val isButtonTransparent: Boolean = false
) {
    fun toJson(): String {
        return try {
            val json = JSONObject()
            json.put("file", fileName)
            offsetXTagId?.let { json.put("oxTag", it) }
            offsetYTagId?.let { json.put("oyTag", it) }
            rotationTagId?.let { json.put("rotTag", it) }
            if (isAnimated) {
                json.put("anim", true)
                json.put("animMode", animMode)
                animTagId?.let { json.put("animTag", it) }
            }
            if (isVisibilityEnabled) {
                json.put("visEnabled", true)
                visibilityTagId?.let { json.put("visTag", it) }
                visibilityBitIndex?.let { json.put("visBit", it) }
            }
            if (isButtonTransparent) {
                json.put("btnTrans", true)
            }
            json.toString()
        } catch (e: Exception) {
            fileName
        }
    }

    companion object {
        fun fromString(raw: String): GraphicsImageConfig {
            if (raw.isBlank()) return GraphicsImageConfig()
            if (raw.startsWith("{")) {
                return try {
                    val json = JSONObject(raw)
                    GraphicsImageConfig(
                        fileName = json.optString("file", ""),
                        offsetXTagId = if (json.has("oxTag")) json.getLong("oxTag") else null,
                        offsetYTagId = if (json.has("oyTag")) json.getLong("oyTag") else null,
                        rotationTagId = if (json.has("rotTag")) json.getLong("rotTag") else null,
                        isAnimated = json.optBoolean("anim", false),
                        animMode = json.optString("animMode", "Continuous"),
                        animTagId = if (json.has("animTag")) json.getLong("animTag") else null,
                        isVisibilityEnabled = json.optBoolean("visEnabled", false),
                        visibilityTagId = if (json.has("visTag")) json.getLong("visTag") else null,
                        visibilityBitIndex = if (json.has("visBit")) json.getInt("visBit") else null,
                        isButtonTransparent = json.optBoolean("btnTrans", false)
                    )
                } catch (e: Exception) {
                    GraphicsImageConfig(fileName = raw)
                }
            }
            return GraphicsImageConfig(fileName = raw)
        }
    }
}

object GraphicsImageManager {
    const val APP_NAME = "AIListTest"

    fun getAllGraphicsDirectories(context: Context): List<File> {
        val list = mutableListOf<File>()

        try {
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val f = File(docDir, "$APP_NAME/graphics")
            if (!f.exists()) f.mkdirs()
            list.add(f)
        } catch (_: Exception) {}

        try {
            val extDir = context.getExternalFilesDir(null)
            if (extDir != null) {
                val f = File(extDir, "graphics")
                if (!f.exists()) f.mkdirs()
                list.add(f)
            }
        } catch (_: Exception) {}

        try {
            val intDir = File(context.filesDir, "graphics")
            if (!intDir.exists()) intDir.mkdirs()
            list.add(intDir)
        } catch (_: Exception) {}

        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val f1 = File(dlDir, "$APP_NAME/graphics")
            if (f1.exists()) list.add(f1)
            if (dlDir.exists()) list.add(dlDir)
        } catch (_: Exception) {}

        return list.distinctBy { it.absolutePath }
    }

    fun getGraphicsDirectory(context: Context): File {
        val primaryDir = context.getExternalFilesDir("Graphics")
            ?: File(context.filesDir, "Graphics")
        if (!primaryDir.exists()) {
            primaryDir.mkdirs()
        }
        return primaryDir
    }

    fun ensureAppDirectoriesExist(context: Context) {
        val graphicsDir = getGraphicsDirectory(context)
        val databaseDir = context.getExternalFilesDir("Database")
            ?: File(context.filesDir, "Database")
        if (!databaseDir.exists()) {
            databaseDir.mkdirs()
        }

        try {
            val publicDir = getPublicGraphicsDirectory()
            if (publicDir != null && !publicDir.exists()) {
                publicDir.mkdirs()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        syncPublicGraphicsFiles(context)
    }

    fun ensureGraphicsDirectoryExists(context: Context): File {
        ensureAppDirectoriesExist(context)
        return getGraphicsDirectory(context)
    }

    fun getPublicGraphicsDirectory(): File? {
        return try {
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val f = File(docDir, "$APP_NAME/graphics")
            if (!f.exists()) f.mkdirs()
            f
        } catch (_: Exception) { null }
    }

    fun syncPublicGraphicsFiles(context: Context) {
        val primaryDir = getGraphicsDirectory(context)
        val candidateDirs = mutableListOf<File>()

        getPublicGraphicsDirectory()?.let { candidateDirs.add(it) }

        try {
            val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val f1 = File(dlDir, "$APP_NAME/graphics")
            if (f1.exists()) candidateDirs.add(f1)
            if (dlDir.exists()) candidateDirs.add(dlDir)
        } catch (_: Exception) {}

        candidateDirs.forEach { publicDir ->
            if (publicDir.exists() && publicDir.isDirectory) {
                val files = publicDir.listFiles() ?: emptyArray()
                for (file in files) {
                    if (file.isFile && isImageFile(file.name) && file.length() > 0) {
                        val destFile = File(primaryDir, file.name)
                        if (!destFile.exists() || destFile.length() != file.length()) {
                            try {
                                file.copyTo(destFile, overwrite = true)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    fun getAvailableGraphicsFiles(context: Context): List<GraphicsFileInfo> {
        syncPublicGraphicsFiles(context)
        val primaryDir = getGraphicsDirectory(context)
        val foundList = mutableListOf<GraphicsFileInfo>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        if (primaryDir.exists() && primaryDir.isDirectory) {
            val list = primaryDir.listFiles() ?: emptyArray()
            for (file in list) {
                if (file.isFile && isImageFile(file.name) && file.length() > 0) {
                    val sizeText = try {
                        String.format(Locale.getDefault(), "%.1f KB", file.length() / 1024.0)
                    } catch (_: Exception) { "Available" }
                    val dateText = try {
                        sdf.format(Date(file.lastModified()))
                    } catch (_: Exception) { "File" }

                    foundList.add(
                        GraphicsFileInfo(
                            file = file,
                            name = file.name,
                            path = file.absolutePath,
                            sizeText = sizeText,
                            dateText = dateText
                        )
                    )
                }
            }
        }
        return foundList.sortedByDescending { it.file?.lastModified() ?: 0L }
    }

    fun findGraphicsFile(context: Context, filename: String): File? {
        if (filename.isBlank()) return null
        syncPublicGraphicsFiles(context)
        val primaryDir = getGraphicsDirectory(context)

        // 1. Direct match in primary internal storage
        val directFile = File(primaryDir, filename)
        if (directFile.exists() && directFile.isFile && directFile.length() > 0) return directFile

        // 2. Clean name match
        val cleanName = sanitizeFilename(filename)
        val cleanFile = File(primaryDir, cleanName)
        if (cleanFile.exists() && cleanFile.isFile && cleanFile.length() > 0) return cleanFile

        // 3. Substring / Case-insensitive match in primary internal storage
        val baseName = filename.substringBeforeLast('.').lowercase(Locale.getDefault())
        val files = primaryDir.listFiles() ?: emptyArray()

        return files.find {
            it.isFile && it.length() > 0 && it.name.substringBeforeLast('.').lowercase(Locale.getDefault()) == baseName
        } ?: files.find {
            it.isFile && it.length() > 0 && it.name.lowercase(Locale.getDefault()).contains(baseName)
        }
    }

    fun isImageFile(name: String): Boolean {
        val lower = name.lowercase(Locale.getDefault())
        return lower.endsWith(".gif") ||
               lower.endsWith(".png") ||
               lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".webp") ||
               lower.endsWith(".bmp")
    }

    fun importImageFromUri(context: Context, uri: Uri, preferredName: String? = null): String? {
        return try {
            val graphicsDir = getGraphicsDirectory(context)

            // 1. If URI path points directly inside graphicsDir, return filename directly
            val uriPath = uri.path
            if (uriPath != null) {
                val candidate = File(uriPath)
                if (candidate.exists() && candidate.isFile && candidate.parentFile?.absolutePath == graphicsDir.absolutePath) {
                    return candidate.name
                }
            }

            // 2. Extract real display filename from ContentResolver or URI
            var displayName: String? = null
            try {
                if (uri.scheme == "content") {
                    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) displayName = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}

            val rawName = displayName?.ifBlank { null }
                ?: uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null }
                ?: preferredName?.ifBlank { null }
                ?: "image.gif"

            // 3. CHECK IF FILE ALREADY EXISTS IN GRAPHICS DIR (RAW, SANITIZED, OR BASE MATCH) -> NEVER CREATE COPIES!
            val rawFile = File(graphicsDir, rawName)
            if (rawFile.exists() && rawFile.isFile && rawFile.length() > 0) {
                return rawFile.name
            }

            val sanitizedName = sanitizeFilename(rawName)
            val destFile = File(graphicsDir, sanitizedName)
            if (destFile.exists() && destFile.isFile && destFile.length() > 0) {
                return destFile.name
            }

            val targetBase = rawName.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.getDefault())
            val existingFiles = graphicsDir.listFiles() ?: emptyArray()
            val existingMatch = existingFiles.find { file ->
                file.isFile && file.length() > 0 &&
                file.name.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.getDefault()) == targetBase
            }
            if (existingMatch != null) {
                return existingMatch.name
            }

            // 4. Copy external file to graphics directory once
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile, false).use { output ->
                    inputStream.copyTo(output)
                }
            }

            val internalDir = File(context.filesDir, "graphics")
            if (!internalDir.exists()) internalDir.mkdirs()
            val internalFile = File(internalDir, sanitizedName)
            if (destFile.exists()) {
                destFile.copyTo(internalFile, overwrite = true)
            }

            destFile.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportItemImage(
        context: Context,
        tagName: String,
        widthPx: Int,
        heightPx: Int,
        sourceFilename: String?
    ): File? {
        return try {
            val graphicsDir = getGraphicsDirectory(context)
            val w = widthPx.coerceAtLeast(10)
            val h = heightPx.coerceAtLeast(10)

            val cleanTagName = tagName.ifBlank { "GraphicsItem" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val sourceFile = if (!sourceFilename.isNullOrBlank()) File(graphicsDir, sourceFilename) else null

            val isGif = sourceFilename?.lowercase(Locale.getDefault())?.endsWith(".gif") == true
            val ext = if (isGif) "gif" else "png"
            val exportFileName = "${cleanTagName}_${w}x${h}.$ext"
            val destFile = File(graphicsDir, exportFileName)

            if (sourceFile != null && sourceFile.exists() && sourceFile.isFile) {
                if (isGif) {
                    sourceFile.copyTo(destFile, overwrite = true)
                    return destFile
                } else {
                    val original = BitmapFactory.decodeFile(sourceFile.absolutePath)
                    if (original != null) {
                        val scaled = Bitmap.createScaledBitmap(original, w, h, true)
                        FileOutputStream(destFile, false).use { out ->
                            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        return destFile
                    }
                }
            }

            val blankBitmap = createBlankBitmap(w, h)
            FileOutputStream(destFile, false).use { out ->
                blankBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateUniqueFilename(context: Context, originalName: String): String {
        val graphicsDir = getGraphicsDirectory(context)
        val ext = originalName.substringAfterLast('.', "")
        val baseName = if (ext.isNotEmpty()) originalName.substringBeforeLast('.') else originalName
        val cleanExt = if (ext.isNotBlank()) ".$ext" else ".png"

        var count = 1
        var candidate = "${baseName}_$count$cleanExt"
        while (File(graphicsDir, candidate).exists()) {
            count++
            candidate = "${baseName}_$count$cleanExt"
        }
        return candidate
    }

    fun copyUriToGraphicsFile(context: Context, uri: Uri, targetFilename: String): String? {
        return try {
            val graphicsDir = getGraphicsDirectory(context)
            val destFile = File(graphicsDir, targetFilename)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile, false).use { output ->
                    inputStream.copyTo(output)
                }
            }
            destFile.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getCoilModel(context: Context, model: Any?): Any? {
        if (model == null) return null
        return when (model) {
            is Uri -> {
                if (model.scheme == "file") {
                    val path = model.path
                    if (path != null) {
                        val file = File(path)
                        if (file.exists() && file.isFile && file.length() > 0) {
                            try { file.readBytes() } catch (_: Exception) { model }
                        } else model
                    } else model
                } else model
            }
            is File -> {
                if (model.exists() && model.isFile && model.length() > 0) {
                    try { model.readBytes() } catch (_: Exception) { model }
                } else model
            }
            else -> model
        }
    }

    private fun createBlankBitmap(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        return bitmap
    }

    private fun sanitizeFilename(name: String): String {
        val extension = name.substringAfterLast('.', "")
        val baseName = if (extension.isNotEmpty()) name.substringBeforeLast('.') else name

        val cleanBase = baseName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").trim('_')
        val cleanExt = extension.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]"), "")

        return if (cleanExt.isNotBlank() && isImageFile("test.$cleanExt")) {
            "${cleanBase.ifBlank { "image" }}.$cleanExt"
        } else {
            "${cleanBase.ifBlank { "image" }}.png"
        }
    }
}
