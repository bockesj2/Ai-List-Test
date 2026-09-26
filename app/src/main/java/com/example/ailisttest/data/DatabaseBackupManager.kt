package com.example.ailisttest.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.ailisttest.data.local.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

enum class RestoreMode {
    OVERWRITE,
    APPEND
}

data class BackupResult(
    val isSuccess: Boolean,
    val filePath: String,
    val fileName: String,
    val fileSizeText: String,
    val totalRecords: Int,
    val errorMessage: String? = null
)

data class BackupFileInfo(
    val file: File?,
    val name: String,
    val path: String,
    val sizeText: String,
    val dateText: String,
    val uri: Uri? = null
)

data class RestoreConflict(
    val entityType: String,
    val entityName: String,
    val reason: String
)

data class RestoreResult(
    val isSuccess: Boolean,
    val fileName: String,
    val filePath: String,
    val fileSizeText: String,
    val totalRecordsInFile: Int,
    val restoredCount: Int,
    val skippedCount: Int,
    val mode: RestoreMode,
    val conflicts: List<RestoreConflict>,
    val errorMessage: String? = null
)

object DatabaseBackupManager {

    suspend fun performBackup(context: Context, repository: MainRepository): BackupResult {
        return try {
            val nodes = repository.getAllNodesList()
            val packets = repository.getAllPacketsList()
            val tags = repository.getAllTagsList()
            val dataTypes = repository.getAllDataTypesList()
            val screens = repository.getAllScreensList()
            val listItems = repository.getAllListScreenItemsSync()

            val jsonRoot = JSONObject()
            jsonRoot.put("backupVersion", 1)
            jsonRoot.put("appVersion", "1.0")
            jsonRoot.put("databaseVersion", 22)
            jsonRoot.put("createdTimestamp", System.currentTimeMillis())

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            jsonRoot.put("createdDate", sdf.format(Date()))
            jsonRoot.put("plcPreset", repository.getPlcPresetSync())
            jsonRoot.put("byteOrder", repository.getByteOrderSync())

            // Serialize Nodes
            val nodesArray = JSONArray()
            nodes.forEach { node ->
                val obj = JSONObject()
                obj.put("id", node.id)
                obj.put("name", node.name)
                obj.put("ipAddress", node.ipAddress)
                nodesArray.put(obj)
            }
            jsonRoot.put("nodes", nodesArray)

            // Serialize Packets
            val packetsArray = JSONArray()
            packets.forEach { packet ->
                val obj = JSONObject()
                obj.put("id", packet.id)
                obj.put("parentNodeId", packet.parentNodeId)
                obj.put("name", packet.name)
                obj.put("description", packet.description)
                obj.put("type", packet.type ?: JSONObject.NULL)
                obj.put("period", packet.period)
                obj.put("config", packet.config)
                obj.put("offset", packet.offset)
                obj.put("slaveNode", packet.slaveNode)
                obj.put("pollData", packet.pollData)
                obj.put("size", packet.size)
                packetsArray.put(obj)
            }
            jsonRoot.put("packets", packetsArray)

            // Serialize Tags
            val tagsArray = JSONArray()
            tags.forEach { tag ->
                val obj = JSONObject()
                obj.put("id", tag.id)
                obj.put("name", tag.name)
                obj.put("description", tag.description)
                obj.put("dataTypeId", tag.dataTypeId)
                obj.put("storedValue", tag.storedValue)
                obj.put("offset", tag.offset)
                tagsArray.put(obj)
            }
            jsonRoot.put("tags", tagsArray)

            // Serialize DataTypes
            val dataTypesArray = JSONArray()
            dataTypes.forEach { dt ->
                val obj = JSONObject()
                obj.put("id", dt.id)
                obj.put("description", dt.description)
                obj.put("shortName", dt.shortName)
                obj.put("dataType", dt.dataType)
                obj.put("bytes", dt.bytes)
                obj.put("defaultModbusAddress", dt.defaultModbusAddress)
                obj.put("isZeroBasedAddressing", dt.isZeroBasedAddressing)
                obj.put("hasBits", dt.hasBits)
                dataTypesArray.put(obj)
            }
            jsonRoot.put("plcDataTypes", dataTypesArray)

            // Serialize Screens
            val screensArray = JSONArray()
            screens.forEach { screen ->
                val obj = JSONObject()
                obj.put("id", screen.id)
                obj.put("name", screen.Name)
                obj.put("type", screen.Type)
                screensArray.put(obj)
            }
            jsonRoot.put("screens", screensArray)

            // Serialize ListScreenItems
            val listItemsArray = JSONArray()
            listItems.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("parentScreenId", item.parentScreenId)
                obj.put("parentCustomGroupId", item.parentCustomGroupId ?: JSONObject.NULL)
                obj.put("parentTagId", item.parentTagId ?: JSONObject.NULL)
                obj.put("displayType", item.DisplayType)
                obj.put("type", item.Type)
                obj.put("screenIndex", item.ScreenIndex)
                obj.put("isReadOnly", item.isReadOnly)
                obj.put("isTwoTouch", item.isTwoTouch)
                obj.put("isShowBits", item.isShowBits)
                listItemsArray.put(obj)
            }
            jsonRoot.put("listScreenItems", listItemsArray)

            val totalRecords = nodes.size + packets.size + tags.size + screens.size + listItems.size

            // Public Storage Location (Documents folder with App Name subfolder)
            val appName = "AiListTest"
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appSubDir = File(publicDir, appName)
            if (!appSubDir.exists()) {
                appSubDir.mkdirs()
            }

            val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AiListTest_Backup_$fileTimestamp.json"
            val backupFile = File(appSubDir, fileName)

            FileOutputStream(backupFile).use { out ->
                out.write(jsonRoot.toString(2).toByteArray(Charsets.UTF_8))
            }

            val fileSizeKb = backupFile.length() / 1024.0
            val sizeText = String.format(Locale.getDefault(), "%.1f KB", fileSizeKb)

            BackupResult(
                isSuccess = true,
                filePath = backupFile.absolutePath,
                fileName = fileName,
                fileSizeText = sizeText,
                totalRecords = totalRecords
            )
        } catch (e: Exception) {
            BackupResult(
                isSuccess = false,
                filePath = "",
                fileName = "",
                fileSizeText = "0 KB",
                totalRecords = 0,
                errorMessage = e.localizedMessage ?: "Failed to write backup file."
            )
        }
    }

    fun getAvailableBackupFiles(context: Context): List<BackupFileInfo> {
        val appName = "AiListTest"
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val docAppSubDir = File(documentsDir, appName)
        val downloadAppSubDir = File(downloadsDir, appName)

        val foundList = mutableListOf<BackupFileInfo>()
        val foundPaths = mutableSetOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun addPathEntry(path: String, name: String? = null) {
            if (path.isBlank() || foundPaths.contains(path)) return
            foundPaths.add(path)
            val file = File(path)
            val fileName = name ?: file.name
            val sizeText = try {
                if (file.exists() && file.isFile) String.format(Locale.getDefault(), "%.1f KB", file.length() / 1024.0) else "Available"
            } catch (_: Exception) { "Available" }
            val dateText = try {
                if (file.exists() && file.isFile) sdf.format(Date(file.lastModified())) else "Public File"
            } catch (_: Exception) { "Public File" }

            foundList.add(
                BackupFileInfo(
                    file = file,
                    name = fileName,
                    path = path,
                    sizeText = sizeText,
                    dateText = dateText
                )
            )
        }

        fun scanDir(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            try {
                val list = dir.listFiles() ?: return
                for (file in list) {
                    if (file.isFile && (file.name.contains("Backup", ignoreCase = true) || file.name.endsWith(".json", ignoreCase = true))) {
                        addPathEntry(file.absolutePath, file.name)
                    }
                }
            } catch (_: Exception) {}
        }

        scanDir(docAppSubDir)
        scanDir(documentsDir)
        scanDir(downloadAppSubDir)
        scanDir(downloadsDir)

        // MediaStore query fallback
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.SIZE
            )
            val uri = MediaStore.Files.getContentUri("external")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )

            cursor?.use { c ->
                val dataIndex = c.getColumnIndex(MediaStore.MediaColumns.DATA)
                val nameIndex = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                while (c.moveToNext()) {
                    if (dataIndex != -1) {
                        val path = c.getString(dataIndex)
                        val displayName = if (nameIndex != -1) c.getString(nameIndex) else null
                        if (!path.isNullOrEmpty() && (path.contains("Backup", ignoreCase = true) || path.endsWith(".json", ignoreCase = true))) {
                            addPathEntry(path, displayName)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return foundList.sortedByDescending { it.path }
    }

    suspend fun executeRestoreFromUri(
        context: Context,
        uri: Uri,
        mode: RestoreMode,
        repository: MainRepository
    ): RestoreResult {
        return try {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IOException("Unable to open stream for $uri")
            val jsonRoot = JSONObject(jsonText)
            executeRestoreFromJson(jsonRoot, mode, repository)
        } catch (e: Exception) {
            RestoreResult(
                isSuccess = false,
                fileName = "Selected System File",
                filePath = uri.toString(),
                fileSizeText = "Available",
                totalRecordsInFile = 0,
                restoredCount = 0,
                skippedCount = 0,
                mode = mode,
                conflicts = emptyList(),
                errorMessage = "Failed to parse restore file: ${e.localizedMessage}"
            )
        }
    }

    fun parseBackupJson(file: File): JSONObject? {
        return try {
            val jsonText = FileInputStream(file).use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            }
            JSONObject(jsonText)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun executeRestore(
        file: File,
        mode: RestoreMode,
        repository: MainRepository
    ): RestoreResult {
        return try {
            val jsonRoot = parseBackupJson(file)
                ?: return RestoreResult(
                    isSuccess = false,
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSizeText = "0 KB",
                    totalRecordsInFile = 0,
                    restoredCount = 0,
                    skippedCount = 0,
                    mode = mode,
                    conflicts = emptyList(),
                    errorMessage = "Failed to parse backup JSON file."
                )

            val kb = file.length() / 1024.0
            val sizeText = String.format(Locale.getDefault(), "%.1f KB", kb)

            val res = executeRestoreFromJson(jsonRoot, mode, repository)
            res.copy(fileName = file.name, filePath = file.absolutePath, fileSizeText = sizeText)
        } catch (e: Exception) {
            RestoreResult(
                isSuccess = false,
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeText = "0 KB",
                totalRecordsInFile = 0,
                restoredCount = 0,
                skippedCount = 0,
                mode = mode,
                conflicts = emptyList(),
                errorMessage = e.localizedMessage ?: "Failed to restore database."
            )
        }
    }

    suspend fun executeRestoreFromJson(
        jsonRoot: JSONObject,
        mode: RestoreMode,
        repository: MainRepository
    ): RestoreResult {
        return try {
            val nodesArray = jsonRoot.optJSONArray("nodes") ?: JSONArray()
            val packetsArray = jsonRoot.optJSONArray("packets") ?: JSONArray()
            val tagsArray = jsonRoot.optJSONArray("tags") ?: JSONArray()
            val screensArray = jsonRoot.optJSONArray("screens") ?: JSONArray()
            val listItemsArray = jsonRoot.optJSONArray("listScreenItems") ?: JSONArray()

            val totalRecordsInFile = nodesArray.length() + packetsArray.length() + tagsArray.length() + screensArray.length() + listItemsArray.length()

            val detectedConflicts = mutableListOf<RestoreConflict>()
            var restoredCount = 0
            var skippedCount = 0

            if (mode == RestoreMode.OVERWRITE) {
                repository.recreateDatabaseWithDefaults()
            }

            val localDataTypes = repository.getAllDataTypesList()
            val dataTypeIdMap = mutableMapOf<Long, Long>()

            val dataTypesArray = jsonRoot.optJSONArray("plcDataTypes") ?: jsonRoot.optJSONArray("dataTypes") ?: JSONArray()
            for (i in 0 until dataTypesArray.length()) {
                val obj = dataTypesArray.getJSONObject(i)
                val oldDtId = obj.optLong("id", -1L)
                val dtDesc = obj.optString("description", obj.optString("name", "Data Register"))
                val dtShort = obj.optString("shortName", "DS")

                val match = localDataTypes.find { it.description.equals(dtDesc, ignoreCase = true) || it.shortName.equals(dtShort, ignoreCase = true) }
                    ?: localDataTypes.firstOrNull()

                if (match != null && oldDtId >= 0) {
                    dataTypeIdMap[oldDtId] = match.id
                }
            }

            val existingNodes = repository.getAllNodesList()
            val existingPackets = repository.getAllPacketsList()
            val existingScreens = repository.getAllScreensList()

            val nodeIdMap = mutableMapOf<Long, Long>()
            val packetIdMap = mutableMapOf<Long, Long>()
            val tagIdMap = mutableMapOf<Long, Long>()
            val screenIdMap = mutableMapOf<Long, Long>()

            // 1. Restore Nodes
            for (i in 0 until nodesArray.length()) {
                val obj = nodesArray.getJSONObject(i)
                val oldId = obj.optLong("id", -1L)
                val name = obj.optString("name", "Node").trim()
                val ip = obj.optString("ipAddress", "127.0.0.1")

                val existing = existingNodes.find { it.name.trim().equals(name, ignoreCase = true) }

                if (mode == RestoreMode.APPEND && existing != null) {
                    val conflict = RestoreConflict("Node", name, "Node name '$name' already exists in the database.")
                    detectedConflicts.add(conflict)
                    skippedCount++
                    if (oldId >= 0) nodeIdMap[oldId] = existing.id
                } else {
                    val newId = repository.insertNode(NodeEntity(name = name, ipAddress = ip))
                    if (oldId >= 0) nodeIdMap[oldId] = newId
                    restoredCount++
                }
            }

            // 2. Restore Packets
            for (i in 0 until packetsArray.length()) {
                val obj = packetsArray.getJSONObject(i)
                val oldId = obj.optLong("id", -1L)
                val oldParentNodeId = obj.optLong("parentNodeId", -1L)
                val newParentNodeId = nodeIdMap[oldParentNodeId] ?: oldParentNodeId
                val name = obj.optString("name", "Packet").trim()

                val existing = existingPackets.find { it.parentNodeId == newParentNodeId && it.name.trim().equals(name, ignoreCase = true) }

                if (mode == RestoreMode.APPEND && existing != null) {
                    val conflict = RestoreConflict("Packet", name, "Packet '$name' already exists under node.")
                    detectedConflicts.add(conflict)
                    skippedCount++
                    if (oldId >= 0) packetIdMap[oldId] = existing.id
                } else {
                    val newPacket = PacketEntity(
                        parentNodeId = newParentNodeId,
                        name = name,
                        description = obj.optString("description", ""),
                        type = if (obj.isNull("type")) null else obj.getInt("type"),
                        period = obj.optInt("period", 1000),
                        config = obj.optString("config", ""),
                        offset = obj.optInt("offset", 1),
                        slaveNode = obj.optInt("slaveNode", 1),
                        pollData = obj.optBoolean("pollData", true),
                        size = obj.optInt("size", 1)
                    )
                    val newId = repository.insertPacket(newPacket)
                    if (oldId >= 0) packetIdMap[oldId] = newId
                    restoredCount++
                }
            }

            // 3. Restore Tags
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                val oldId = obj.optLong("id", -1L)
                val name = obj.optString("name", "Tag").trim()
                val oldDtId = obj.optLong("dataTypeId", 0L)
                val resolvedDataTypeId = dataTypeIdMap[oldDtId] ?: localDataTypes.find { it.id == oldDtId }?.id ?: localDataTypes.firstOrNull()?.id ?: 1L
                val tagOffset = obj.optInt("offset", obj.optInt("packetOffset", 1))

                val existingTag = repository.getTagByDataTypeAndOffsetSync(resolvedDataTypeId, tagOffset)

                if (existingTag != null) {
                    val updatedTag = existingTag.copy(
                        name = name,
                        description = obj.optString("description", ""),
                        storedValue = obj.optString("storedValue", "0")
                    )
                    repository.updateTag(updatedTag)
                    if (oldId >= 0) tagIdMap[oldId] = existingTag.id
                    restoredCount++
                } else {
                    val newTag = TagEntity(
                        name = name,
                        description = obj.optString("description", ""),
                        dataTypeId = resolvedDataTypeId,
                        storedValue = obj.optString("storedValue", "0"),
                        offset = tagOffset
                    )
                    val newId = repository.insertTag(newTag)
                    if (newId > 0 && oldId >= 0) {
                        tagIdMap[oldId] = newId
                        restoredCount++
                    }
                }
            }

            // 4. Restore Screens
            for (i in 0 until screensArray.length()) {
                val obj = screensArray.getJSONObject(i)
                val oldId = obj.optLong("id", -1L)
                val name = obj.optString("name", obj.optString("Name", "Screen")).trim()

                val existing = existingScreens.find { it.Name.trim().equals(name, ignoreCase = true) }

                if (mode == RestoreMode.APPEND && existing != null) {
                    val conflict = RestoreConflict("Screen", name, "Screen name '$name' already exists.")
                    detectedConflicts.add(conflict)
                    skippedCount++
                    if (oldId >= 0) screenIdMap[oldId] = existing.id
                } else {
                    val newId = repository.insertScreen(Screens(Name = name, Type = obj.optInt("type", obj.optInt("Type", 0))))
                    if (oldId >= 0) screenIdMap[oldId] = newId
                    restoredCount++
                }
            }

            // 5. Restore ListScreenItems
            for (i in 0 until listItemsArray.length()) {
                val obj = listItemsArray.getJSONObject(i)
                val oldScreenId = obj.optLong("parentScreenId", -1L)
                val newScreenId = screenIdMap[oldScreenId] ?: oldScreenId
                val oldTagId = if (obj.isNull("parentTagId")) null else obj.optLong("parentTagId", -1L)
                val newTagId = if (oldTagId != null && oldTagId >= 0) tagIdMap[oldTagId] ?: oldTagId else null

                if (newScreenId >= 0) {
                    repository.insertListScreenItem(
                        ListScreenItems(
                            parentScreenId = newScreenId,
                            parentCustomGroupId = if (obj.isNull("parentCustomGroupId")) null else obj.optLong("parentCustomGroupId", 0L),
                            parentTagId = newTagId,
                            DisplayType = obj.optString("displayType", obj.optString("DisplayType", "")),
                            Type = obj.optInt("type", obj.optInt("Type", 0)),
                            ScreenIndex = obj.optInt("screenIndex", obj.optInt("ScreenIndex", 1)),
                            isReadOnly = obj.optBoolean("isReadOnly", true),
                            isTwoTouch = obj.optBoolean("isTwoTouch", true),
                            isShowBits = obj.optBoolean("isShowBits", obj.optBoolean("ShowBits", false))
                        )
                    )
                    restoredCount++
                }
            }

            repository.cleanupOrphanListScreenItems()
            repository.refreshHierarchy()

            val restoredPlcPreset = jsonRoot.optString("plcPreset", "Click Plus PLC")
            val restoredByteOrder = jsonRoot.optString("byteOrder", "flip words")
            repository.setPlcPreset(restoredPlcPreset)
            repository.setByteOrder(restoredByteOrder)

            RestoreResult(
                isSuccess = true,
                fileName = "Restored Backup",
                filePath = "Public Storage Area",
                fileSizeText = "Available",
                totalRecordsInFile = totalRecordsInFile,
                restoredCount = restoredCount,
                skippedCount = skippedCount,
                mode = mode,
                conflicts = detectedConflicts
            )
        } catch (e: Exception) {
            RestoreResult(
                isSuccess = false,
                fileName = "Backup File",
                filePath = "Public Storage Area",
                fileSizeText = "Available",
                totalRecordsInFile = 0,
                restoredCount = 0,
                skippedCount = 0,
                mode = mode,
                conflicts = emptyList(),
                errorMessage = "Failed to restore database: ${e.localizedMessage}"
            )
        }
    }
}
