package com.example.ailisttest.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.ailisttest.data.local.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CsvTagRecord(
    val address: String,
    val dataTypeStr: String,
    val nickname: String,
    val initialValue: String,
    val retentive: String,
    val comment: String
)

data class ImportResult(
    val isSuccess: Boolean,
    val nodeName: String,
    val totalTagsImported: Int,
    val packetsCreated: Int,
    val errorMessage: String? = null
)

data class ExportResult(
    val isSuccess: Boolean,
    val filePath: String,
    val fileName: String,
    val totalTagsExported: Int,
    val errorMessage: String? = null
)

data class CsvFileInfo(
    val file: File?,
    val name: String,
    val dirPath: String,
    val path: String,
    val sizeText: String,
    val dateText: String,
    val isSample: Boolean = false,
    val sampleText: String? = null,
    val uri: Uri? = null
)

object TagCsvImporterExporter {

    fun getScannedDirectories(context: Context): List<String> {
        val appName = "AiListTest"
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return listOfNotNull(
            documentsDir?.absolutePath,
            File(documentsDir, appName).absolutePath,
            downloadsDir?.absolutePath,
            File(downloadsDir, appName).absolutePath
        ).distinct()
    }

    fun getAvailableCsvFiles(context: Context): List<CsvFileInfo> {
        val appName = "AiListTest"
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val docAppSubDir = File(documentsDir, appName)
        val downloadAppSubDir = File(downloadsDir, appName)

        val foundList = mutableListOf<CsvFileInfo>()
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
                CsvFileInfo(
                    file = file,
                    name = fileName,
                    dirPath = file.parent ?: "Public Directory",
                    path = path,
                    sizeText = sizeText,
                    dateText = dateText,
                    isSample = false
                )
            )
        }

        // 1. Explicit known file paths
        val explicitPaths = listOf(
            File(docAppSubDir, "test1.csv").absolutePath,
            File(documentsDir, "test1.csv").absolutePath,
            "/storage/emulated/0/Documents/AiListTest/test1.csv",
            "/storage/emulated/0/Documents/test1.csv",
            "/storage/emulated/0/Download/AiListTest/test1.csv",
            "/storage/emulated/0/Download/test1.csv"
        )
        explicitPaths.forEach { addPathEntry(it) }

        // 2. Scan directories
        fun scanDir(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            try {
                val list = dir.listFiles() ?: return
                for (file in list) {
                    if (file.isFile) {
                        addPathEntry(file.absolutePath, file.name)
                    }
                }
            } catch (_: Exception) {}
        }

        scanDir(docAppSubDir)
        scanDir(documentsDir)
        scanDir(downloadAppSubDir)
        scanDir(downloadsDir)

        // 3. MediaStore query fallback
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
                        if (!path.isNullOrEmpty() && (path.endsWith(".csv", ignoreCase = true) || path.contains("test1", ignoreCase = true))) {
                            addPathEntry(path, displayName)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return foundList.sortedByDescending { it.path.contains("test1", ignoreCase = true) }
    }

    val SAMPLE_CLICK_CSV_TEXT = """
Address,Data Type,Nickname,Initial Value,Retentive,Address Comment
X001,BIT,"Pos1PE_BoardInPlace",0,No,""
X002,BIT,"Pos2PE_StartLatchPos",0,No,""
X003,BIT,"Pos3PE_BoardOnOutfeed",0,No,""
X021,BIT,"Axis1_LmtPos",0,No,"Axis1_Blk"
X022,BIT,"Axis1_LmtNeg",0,No,"Axis1_Brn"
X023,BIT,"Axis2_LmtPos",0,No,"Axis1_R"
X024,BIT,"Axis2_LmtNeg",0,No,""
X026,BIT,"Axis2DriverError",0,No,""
Y001,BIT,"Axis1_Pulse",0,No,""
Y002,BIT,"Axis1_Dir",0,No,""
Y003,BIT,"Axis2_Pulse",0,No,""
Y004,BIT,"Axis2_Dir",0,No,""
Y005,BIT,"Axis1_DisableDriver",0,No,""
Y006,BIT,"Axis2_DisableDriver",0,No,""
Y021,BIT,"sol1_BoardClamp",0,No,""
Y022,BIT,"sol2_LowerSaw",0,No,""
Y101,BIT,"Saw Power",0,No,""
C1,BIT,"Axis1MoveBusy",0,No,""
C2,BIT,"Axis1MoveComplete",0,No,""
C3,BIT,"Axis1MoveSucess",0,No,""
C4,BIT,"Axis1MoveError",0,No,""
DS1,INT,"Axis1MoveErrorCode",0,No,""
DS2,INT,"Axis1HomeErrorCode",0,No,""
DS3,INT,"Axis1CalibState",0,No,""
DS4,INT,"Axis1MoveErrorCode2",0,No,""
DS5,INT,"Axis1SelectedMoveCommand",0,No,""
DS6,INT,"Axis1GotoState",0,No,""
DS7,INT,"Axis1MoveMode",0,No,""
DS8,INT,"Axis1MoveModeHMI",0,No,""
DS101,INT,"Axis2MoveErrorCode",0,No,""
DS102,INT,"Axis2HomeErrorCode",0,No,""
DS103,INT,"Axis2CalibState",0,No,""
DS104,INT,"Axis2MoveErrorCode2",0,No,""
DS105,INT,"Axis2SelectedMoveCommand",0,No,""
DS106,INT,"Axis2GotoState",0,No,""
DS107,INT,"Axis2MoveMode",0,No,""
DS108,INT,"Axis2MoveModeHMI",0,No,""
DS120,INT,"AxisAutoCutCycleStep",0,Yes,""
DD1,INT2,"Axis1_CurrPos",0,Yes,""
DD2,INT2,"Axis1_CurrVel",0,Yes,""
DD3,INT2,"Axis1MoveTargetPos",0,Yes,""
DD4,INT2,"Axis1MoveTargetVelocity",0,Yes,""
DD5,INT2,"Axis1MoveAcceleration",0,Yes,""
DD6,INT2,"Axis1MoveDeceleration",0,Yes,""
DD7,INT2,"Axis1TargetPosPrev",0,Yes,""
DD10,INT2,"Axis1HomeInitialVelocity",0,Yes,""
DD101,INT2,"Axis2_CurrPos",0,Yes,""
DD102,INT2,"Axis2_CurrVel",0,Yes,""
DD103,INT2,"Axis2MoveTargetPos",0,Yes,""
DD104,INT2,"Axis2MoveTargetVelocity",0,Yes,""
DD105,INT2,"Axis2MoveAcceleration",0,Yes,""
DD106,INT2,"Axis2MoveDeceleration",0,Yes,""
DD107,INT2,"Axis2TargetPosPrev",0,Yes,""
DD110,INT2,"Axis2HomeInitialVelocity",0,Yes,""
DD111,INT2,"Axis2HomeAcceleration",0,Yes,""
DD112,INT2,"Axis2HomeDeceleration",0,Yes,""
DD113,INT2,"Axis2Sw1Capture",0,Yes,""
DD114,INT2,"Axis2Sw1CapturewithOffst",0,Yes,""
DD115,INT2,"Axis2PosOffset",0,Yes,""
DD116,INT2,"Axis2ExtentsPos",0,Yes,""
DD117,INT2,"Axis2CalibPosLL",0,Yes,""
DD118,INT2,"Axis2CalibPosExtents",0,Yes,""
DD119,INT2,"Axis2CalibOffset",0,Yes,""
DF1,FLOAT,"Axis1Slope",0,Yes,""
DF2,FLOAT,"Axis1ActPosEng",0,Yes,""
DF3,FLOAT,"Axis1ReqBoardLengthEngr",0,Yes,""
DF4,FLOAT,"Axis1CalibLlEngrUnits",0,Yes,""
DF5,FLOAT,"Axis1CalibUlEngrUnits",0,Yes,""
DF6,FLOAT,"Axis1DesBoardLength",0,Yes,""
DF7,FLOAT,"Axis1DesPosPrevEng",0,Yes,""
DF101,FLOAT,"Axis2Slope",0,Yes,""
DF102,FLOAT,"Axis2ActPosEng",0,Yes,""
DF104,FLOAT,"Axis2CalibLlEngrUnits",0,Yes,""
DF105,FLOAT,"Axis2CalibUlEngrUnits",0,Yes,""
DF106,FLOAT,"Axis2DesPosEngHMI",0,Yes,""
DF107,FLOAT,"Axis2DesPosPrevEng",0,Yes,""
""".trimIndent()

    fun readUriContent(context: Context, uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes()
        } ?: throw IOException("Unable to open stream for $uri")

        if (bytes.isEmpty()) return ""

        return when {
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> {
                String(bytes, Charsets.UTF_16BE).removePrefix("\uFEFF")
            }
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> {
                String(bytes, Charsets.UTF_16LE).removePrefix("\uFEFF")
            }
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> {
                String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
            }
            else -> {
                try {
                    String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
                } catch (_: Exception) {
                    String(bytes, Charsets.ISO_8859_1).removePrefix("\uFEFF")
                }
            }
        }
    }

    fun getMediaStoreUriForFile(context: Context, file: File): Uri? {
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val uri = MediaStore.Files.getContentUri("external")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.MediaColumns.DATA} = ?",
                arrayOf(file.absolutePath),
                null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIndex = c.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idIndex != -1) {
                        val id = c.getLong(idIndex)
                        return ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    }
                }
            }
        } catch (_: Exception) {}

        // Fallback search by displayName
        try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val uri = MediaStore.Files.getContentUri("external")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(file.name),
                null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idIndex = c.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idIndex != -1) {
                        val id = c.getLong(idIndex)
                        return ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun readFileContent(context: Context, file: File): String {
        var inputStream: InputStream? = null

        // 1. Try MediaStore content:// URI
        val mediaUri = getMediaStoreUriForFile(context, file)
        if (mediaUri != null) {
            try {
                inputStream = context.contentResolver.openInputStream(mediaUri)
            } catch (_: Exception) {}
        }

        // 2. Try Uri.fromFile
        if (inputStream == null) {
            try {
                inputStream = context.contentResolver.openInputStream(Uri.fromFile(file))
            } catch (_: Exception) {}
        }

        // 3. Try direct File Stream
        if (inputStream == null) {
            try {
                inputStream = file.inputStream()
            } catch (_: Exception) {}
        }

        if (inputStream == null) {
            throw IOException("Permission denied for file '${file.name}'. Please use the 'Browse System Files...' button in the dialog to select the file.")
        }

        val bytes = inputStream.use { it.readBytes() }
        if (bytes.isEmpty()) return ""

        return when {
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> {
                String(bytes, Charsets.UTF_16BE).removePrefix("\uFEFF")
            }
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> {
                String(bytes, Charsets.UTF_16LE).removePrefix("\uFEFF")
            }
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() -> {
                String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
            }
            else -> {
                try {
                    String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
                } catch (_: Exception) {
                    String(bytes, Charsets.ISO_8859_1).removePrefix("\uFEFF")
                }
            }
        }
    }

    fun parseCsvContent(csvText: String): List<CsvTagRecord> {
        val records = mutableListOf<CsvTagRecord>()
        val cleanText = csvText.removePrefix("\uFEFF").replace("\uFEFF", "")
        val lines = cleanText.lines()
        if (lines.isEmpty()) return records

        for (line in lines) {
            val trimmed = line.trim().removePrefix("\uFEFF")
            val lower = trimmed.lowercase()
            if (trimmed.isEmpty() || lower.startsWith("address") || lower.startsWith("addr")) continue

            val parts = parseCsvLine(trimmed)
            if (parts.size >= 3) {
                val address = parts[0].trim().removePrefix("\uFEFF")
                val dataTypeStr = parts[1].trim()
                val nickname = parts[2].trim()
                val initialValue = if (parts.size > 3) parts[3].trim() else "0"
                val retentive = if (parts.size > 4) parts[4].trim() else "No"
                val comment = if (parts.size > 5) parts[5].trim() else ""

                if (nickname.isNotEmpty() || address.isNotEmpty()) {
                    records.add(
                        CsvTagRecord(
                            address = address,
                            dataTypeStr = dataTypeStr,
                            nickname = nickname.ifEmpty { address },
                            initialValue = initialValue,
                            retentive = retentive,
                            comment = comment
                        )
                    )
                }
            }
        }
        return records
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            when (ch) {
                '"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        sb.append(ch)
                    } else {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }

    fun parseAddressOffset(address: String): Int? {
        val digits = address.filter { it.isDigit() }
        return digits.toIntOrNull()
    }

    fun resolveDataTypeFromAddress(address: String, dataTypes: List<DataTypes>): DataTypes? {
        val cleanAddr = address.trim().uppercase()
        val targetShortName = when {
            cleanAddr.startsWith("CTD") -> "CTD"
            cleanAddr.startsWith("XD") -> "XD"
            cleanAddr.startsWith("YD") -> "YD"
            cleanAddr.startsWith("TD") -> "TD"
            cleanAddr.startsWith("SD") -> "SD"
            cleanAddr.startsWith("DD") -> "DD"
            cleanAddr.startsWith("DF") -> "DF"
            cleanAddr.startsWith("DH") -> "DH"
            cleanAddr.startsWith("DS") || cleanAddr.startsWith("C") || cleanAddr.startsWith("X") || cleanAddr.startsWith("Y") -> "DS"
            else -> {
                val prefixLetters = cleanAddr.takeWhile { !it.isDigit() }
                if (prefixLetters.isEmpty()) "DS" else null
            }
        } ?: return null

        return dataTypes.find { dt ->
            dt.shortName.equals(targetShortName, ignoreCase = true)
        } ?: dataTypes.find { dt ->
            when (targetShortName) {
                "CTD" -> dt.shortName.equals("CTD", ignoreCase = true) || dt.description.contains("Counter", ignoreCase = true)
                "XD" -> dt.shortName.equals("XD", ignoreCase = true) || dt.description.contains("Input", ignoreCase = true)
                "YD" -> dt.shortName.equals("YD", ignoreCase = true) || dt.description.contains("Output", ignoreCase = true)
                "TD" -> dt.shortName.equals("TD", ignoreCase = true) || dt.description.contains("Timer", ignoreCase = true)
                "SD" -> dt.shortName.equals("SD", ignoreCase = true) || dt.description.contains("System", ignoreCase = true)
                "DD" -> dt.shortName.equals("DD", ignoreCase = true) || dt.description.contains("Double", ignoreCase = true)
                "DF" -> dt.shortName.equals("DF", ignoreCase = true) || dt.description.contains("Float", ignoreCase = true)
                "DH" -> dt.shortName.equals("DH", ignoreCase = true) || dt.description.contains("Hex", ignoreCase = true)
                "DS" -> dt.shortName.equals("DS", ignoreCase = true) || dt.description.contains("Short", ignoreCase = true)
                else -> false
            }
        } ?: dataTypes.find { dt ->
            dt.shortName.equals("DS", ignoreCase = true) || dt.description.contains("Short", ignoreCase = true)
        } ?: dataTypes.firstOrNull()
    }

    suspend fun importCsvRecordsToNode(
        records: List<CsvTagRecord>,
        node: NodeEntity,
        repository: MainRepository
    ): ImportResult {
        if (records.isEmpty()) {
            return ImportResult(
                isSuccess = false,
                nodeName = node.name,
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = "No valid tag records found in CSV."
            )
        }

        try {
            repository.ensureDefaultDataTypes()
            val dataTypes = repository.getAllDataTypesList()
            if (dataTypes.isEmpty()) {
                return ImportResult(
                    isSuccess = false,
                    nodeName = node.name,
                    totalTagsImported = 0,
                    packetsCreated = 0,
                    errorMessage = "Database DataTypes empty."
                )
            }

            var totalTagsUpdated = 0
            var totalTagsInserted = 0
            var packetsCreated = 0

            for (rec in records) {
                val offsetFromAddr = parseAddressOffset(rec.address) ?: continue

                // Resolve target DataType strictly from the Address column prefix (e.g. "DS101" -> "DS", "DD100" -> "DD")
                val targetDataType = resolveDataTypeFromAddress(rec.address, dataTypes) ?: continue

                var matchedTag: TagEntity? = null

                // Search through existing packets in this node
                val currentPackets = repository.getPacketsForNodeSync(node.id)
                for (packet in currentPackets) {
                    val packetDt = packet.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
                    val isSameDataType = (packetDt?.id == targetDataType.id) ||
                            (packetDt?.shortName?.equals(targetDataType.shortName, ignoreCase = true) == true) ||
                            (packetDt?.description?.equals(targetDataType.description, ignoreCase = true) == true)

                    if (isSameDataType) {
                        val tagsInPacket = repository.getTagsForPacketSync(packet.id)
                        val found = tagsInPacket.find { tag ->
                            val calcOffset = packet.offset + tag.offset - 1
                            calcOffset == offsetFromAddr
                        }
                        if (found != null) {
                            matchedTag = found
                            break
                        }
                    }
                }

                val targetTag = matchedTag ?: repository.getTagByDataTypeAndOffsetSync(targetDataType.id, offsetFromAddr)

                if (targetTag != null) {
                    val updatedTag = targetTag.copy(
                        name = rec.nickname,
                        storedValue = rec.initialValue.ifEmpty { targetTag.storedValue },
                        description = if (rec.comment.isNotBlank()) "${rec.address} - ${rec.comment}" else targetTag.description
                    )
                    repository.updateTag(updatedTag)
                    totalTagsUpdated++

                    if (targetDataType.hasBits) {
                        val bitTags = repository.getBitTagsForTagSync(targetTag.id)
                        bitTags.forEach { bitTag ->
                            val defaultName1 = "${targetTag.name}-${bitTag.bitIndex}"
                            val defaultName2 = "Tag #${targetTag.id}-${bitTag.bitIndex}"
                            val isUnedited = bitTag.name == defaultName1 ||
                                    bitTag.name == defaultName2 ||
                                    (bitTag.name.endsWith("-${bitTag.bitIndex}") && bitTag.name.substringBeforeLast("-") == targetTag.name)

                            if (isUnedited) {
                                val newBitName = "${rec.nickname}-${bitTag.bitIndex}"
                                if (bitTag.name != newBitName) {
                                    repository.updateBitTag(bitTag.copy(name = newBitName))
                                }
                            }
                        }
                    }
                }
            }

            return ImportResult(
                isSuccess = true,
                nodeName = node.name,
                totalTagsImported = totalTagsUpdated + totalTagsInserted,
                packetsCreated = packetsCreated
            )
        } catch (e: Exception) {
            return ImportResult(
                isSuccess = false,
                nodeName = node.name,
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = e.localizedMessage ?: "Failed to import tags."
            )
        }
    }

    suspend fun importCsvRecordsToPacket(
        records: List<CsvTagRecord>,
        packet: PacketEntity,
        repository: MainRepository
    ): ImportResult {
        if (records.isEmpty()) {
            return ImportResult(
                isSuccess = false,
                nodeName = packet.name,
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = "No valid tag records found in CSV."
            )
        }

        try {
            val dataTypes = repository.getAllDataTypesList()
            val packetDataType = repository.resolveDataTypeForPacket(packet, dataTypes)

            val packetDtKind = packetDataType.dataType.uppercase()
            val packetDtShort = packetDataType.shortName.uppercase()

            val existingTags = repository.getTagsForPacketSync(packet.id)
            var totalTagsUpdated = 0
            var totalTagsInserted = 0

            for (rec in records) {
                val dtStr = rec.dataTypeStr.trim().uppercase()
                val addressUpper = rec.address.trim().uppercase()

                // Ensure the CSV record's DataType matches the packet's DataType
                val isMatchingDataType = when {
                    (packetDtKind == "FLOAT" || packetDtShort == "DF") -> {
                        (dtStr == "FLOAT" || dtStr == "DF" || addressUpper.startsWith("DF"))
                    }
                    (packetDtShort == "DD") -> {
                        (dtStr == "INT2" || dtStr == "DD" || addressUpper.startsWith("DD"))
                    }
                    (packetDtKind == "UINT" || packetDtShort == "DH") -> {
                        (dtStr == "HEX" || dtStr == "DH" || dtStr == "UINT" || addressUpper.startsWith("DH"))
                    }
                    (packetDtKind == "INT" || packetDtShort == "DS") -> {
                        (dtStr == "INT" || dtStr == "DS" || dtStr == "BIT" || addressUpper.startsWith("DS") || addressUpper.startsWith("C") || addressUpper.startsWith("X") || addressUpper.startsWith("Y")) &&
                                !dtStr.contains("INT2") && !addressUpper.startsWith("DD") && !addressUpper.startsWith("DF") && !addressUpper.startsWith("DH")
                    }
                    else -> false
                }

                if (!isMatchingDataType) continue

                val offsetFromAddr = parseAddressOffset(rec.address) ?: continue

                // Find corresponding tag by offset
                val matchingTag = existingTags.find { tag ->
                    tag.offset == offsetFromAddr || (packet.offset + tag.offset - 1) == offsetFromAddr
                }

                val actualTagId: Long = if (matchingTag != null) {
                    val updatedTag = matchingTag.copy(
                        name = rec.nickname,
                        storedValue = rec.initialValue.ifEmpty { matchingTag.storedValue },
                        description = if (rec.comment.isNotBlank()) rec.comment else matchingTag.description
                    )
                    repository.updateTag(updatedTag)
                    totalTagsUpdated++
                    matchingTag.id
                } else {
                    val existingInDb = repository.getTagByDataTypeAndOffsetSync(packetDataType.id, offsetFromAddr)
                    if (existingInDb != null) {
                        val updatedTag = existingInDb.copy(
                            name = rec.nickname,
                            storedValue = rec.initialValue.ifEmpty { existingInDb.storedValue },
                            description = if (rec.comment.isNotBlank()) rec.comment else existingInDb.description
                        )
                        repository.updateTag(updatedTag)
                        totalTagsUpdated++
                        existingInDb.id
                    } else {
                        val newTagId = repository.insertTag(
                            TagEntity(
                                name = rec.nickname,
                                description = rec.comment,
                                dataTypeId = packetDataType.id,
                                storedValue = rec.initialValue,
                                offset = offsetFromAddr
                            )
                        )
                        if (newTagId > 0) {
                            totalTagsInserted++
                        }
                        newTagId
                    }
                }

                if (packetDataType.hasBits && actualTagId > 0) {
                    val bitTags = repository.getBitTagsForTagSync(actualTagId)
                    if (bitTags.isNotEmpty()) {
                        bitTags.forEach { bitTag ->
                            val newBitName = "${rec.nickname}-${bitTag.bitIndex}"
                            if (bitTag.name != newBitName) {
                                repository.updateBitTag(bitTag.copy(name = newBitName))
                            }
                        }
                    } else {
                        val numBits = packetDataType.bytes * 8
                        val bitTagsList = (0 until numBits).map { bIdx ->
                            BitTags(
                                name = "${rec.nickname}-$bIdx",
                                parentTagId = actualTagId,
                                bitIndex = bIdx
                            )
                        }
                        repository.insertAllBitTags(bitTagsList)
                    }
                }
            }

            val finalTagCount = existingTags.size + totalTagsInserted
            if (finalTagCount > packet.size) {
                repository.updatePacket(packet.copy(size = finalTagCount))
            }

            return ImportResult(
                isSuccess = true,
                nodeName = packet.name,
                totalTagsImported = totalTagsUpdated + totalTagsInserted,
                packetsCreated = 0
            )
        } catch (e: Exception) {
            return ImportResult(
                isSuccess = false,
                nodeName = packet.name,
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = e.localizedMessage ?: "Failed to import tags into packet."
            )
        }
    }

    suspend fun exportTagsForPacket(
        packet: PacketEntity,
        repository: MainRepository
    ): ExportResult {
        return try {
            val dataTypes = repository.getAllDataTypesList()
            val packetDt = packet.type?.let { typeId -> dataTypes.find { it.id == typeId.toLong() } }
            val tags = repository.getTagsForPacketSync(packet.id)

            val sb = StringBuilder()
            sb.append("Address,Data Type,Nickname,Initial Value,Retentive,Address Comment\r\n")

            var exportedCount = 0
            tags.forEach { tag ->
                val tagDt = dataTypes.find { it.id == tag.dataTypeId } ?: packetDt
                val dtShortName = tagDt?.shortName?.ifEmpty { "DS" } ?: "DS"
                val dtName = tagDt?.dataType?.ifEmpty { "INT" } ?: "INT"

                val addr = "$dtShortName${tag.offset}"
                val nick = tag.name
                val initVal = tag.storedValue.ifEmpty { "0" }
                val comment = tag.description

                sb.append("$addr,$dtName,\"$nick\",$initVal,No,\"$comment\"\r\n")
                exportedCount++
            }

            val appName = "AiListTest"
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appSubDir = File(publicDir, appName)
            if (!appSubDir.exists()) {
                appSubDir.mkdirs()
            }

            val sanitizedPacketName = packet.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "$sanitizedPacketName.csv"
            val exportFile = File(appSubDir, fileName)

            FileOutputStream(exportFile).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            ExportResult(
                isSuccess = true,
                filePath = exportFile.absolutePath,
                fileName = fileName,
                totalTagsExported = exportedCount
            )
        } catch (e: Exception) {
            ExportResult(
                isSuccess = false,
                filePath = "",
                fileName = "",
                totalTagsExported = 0,
                errorMessage = e.localizedMessage ?: "Failed to export tags."
            )
        }
    }

    suspend fun exportTagsForNode(
        node: NodeEntity,
        repository: MainRepository
    ): ExportResult {
        return try {
            val packets = repository.getPacketsForNodeSync(node.id)
            val dataTypes = repository.getAllDataTypesList()

            val sb = StringBuilder()
            sb.append("Address,Data Type,Nickname,Initial Value,Retentive,Address Comment\r\n")

            var exportedCount = 0

            packets.forEach { packet ->
                val packetDt = repository.resolveDataTypeForPacket(packet, dataTypes)
                val tags = repository.getTagsForPacketSync(packet.id)

                tags.forEach { tag ->
                    val tagDt = dataTypes.find { it.id == tag.dataTypeId } ?: packetDt
                    val dtShortName = tagDt.shortName.ifEmpty { tagDt.description }
                    val dtName = tagDt.dataType.ifEmpty { "INT" }

                    val addr = "$dtShortName${tag.offset}"
                    val nick = tag.name
                    val initVal = tag.storedValue.ifEmpty { "0" }
                    val comment = tag.description

                    sb.append("$addr,$dtName,\"$nick\",$initVal,No,\"$comment\"\r\n")
                    exportedCount++
                }
            }

            val appName = "AiListTest"
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appSubDir = File(publicDir, appName)
            if (!appSubDir.exists()) {
                appSubDir.mkdirs()
            }

            val sanitizedNodeName = node.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val fileName = "${sanitizedNodeName}_Tags_Export.csv"
            val exportFile = File(appSubDir, fileName)

            FileOutputStream(exportFile).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            ExportResult(
                isSuccess = true,
                filePath = exportFile.absolutePath,
                fileName = fileName,
                totalTagsExported = exportedCount
            )
        } catch (e: Exception) {
            ExportResult(
                isSuccess = false,
                filePath = "",
                fileName = "",
                totalTagsExported = 0,
                errorMessage = e.localizedMessage ?: "Failed to export tags."
            )
        }
    }

    suspend fun importAllTags(
        records: List<CsvTagRecord>,
        repository: MainRepository,
        format: String = "Click Plus"
    ): ImportResult {
        if (records.isEmpty()) {
            return ImportResult(
                isSuccess = false,
                nodeName = "Existing Packets",
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = "No valid tag records found in CSV."
            )
        }

        try {
            repository.ensureDefaultDataTypes()
            val dataTypes = repository.getAllDataTypesList()
            if (dataTypes.isEmpty()) {
                return ImportResult(
                    isSuccess = false,
                    nodeName = "Existing Packets",
                    totalTagsImported = 0,
                    packetsCreated = 0,
                    errorMessage = "Database DataTypes empty."
                )
            }

            val allPackets = repository.getAllPacketsList()
            val isRawDataFormat = format.equals("Raw Data", ignoreCase = true)
            var totalTagsUpdated = 0

            for (rec in records) {
                val rawAddr = rec.address.trim()
                val isBitRecord = rec.dataTypeStr.trim().equals("BIT", ignoreCase = true) || rawAddr.contains("-") || rawAddr.contains(".")

                if (isBitRecord) {
                    val delimiter = if (rawAddr.contains("-")) "-" else "."
                    val baseAddrPart = rawAddr.substringBefore(delimiter).trim()
                    val bitIndexPart = rawAddr.substringAfter(delimiter).toIntOrNull() ?: continue

                    val offsetFromAddr = parseAddressOffset(baseAddrPart) ?: continue
                    val targetDataType = resolveDataTypeFromAddress(baseAddrPart, dataTypes) ?: continue

                    var parentTag: TagEntity? = null
                    for (packet in allPackets) {
                        val packetDt = repository.resolveDataTypeForPacket(packet, dataTypes)
                        if (packetDt.shortName.equals(targetDataType.shortName, ignoreCase = true)) {
                            val tagsInPacket = repository.getTagsForPacketSync(packet.id)
                            val isZeroBased = packetDt.isZeroBasedAddressing
                            val found = tagsInPacket.find { tag ->
                                val calcOffset = if (isZeroBased) (packet.offset + tag.offset) else (packet.offset + tag.offset - 1)
                                calcOffset == offsetFromAddr
                            }
                            if (found != null) {
                                parentTag = found
                                break
                            }
                        }
                    }

                    val targetParentTag = parentTag ?: repository.getTagByDataTypeAndOffsetSync(targetDataType.id, offsetFromAddr)

                    if (targetParentTag != null) {
                        val bitTags = repository.getBitTagsForTagSync(targetParentTag.id)
                        val matchingBitTag = bitTags.find { it.bitIndex == bitIndexPart }

                        if (matchingBitTag != null) {
                            if (rec.nickname.isNotBlank() && matchingBitTag.name != rec.nickname) {
                                repository.updateBitTag(matchingBitTag.copy(name = rec.nickname))
                                totalTagsUpdated++
                            }
                        }
                    }
                } else {
                    val offsetFromAddr = parseAddressOffset(rawAddr) ?: continue
                    val targetDataType = resolveDataTypeFromAddress(rawAddr, dataTypes) ?: continue

                    var matchedTag: TagEntity? = null

                    for (packet in allPackets) {
                        val packetDt = repository.resolveDataTypeForPacket(packet, dataTypes)
                        val isSameDataType = packetDt.shortName.equals(targetDataType.shortName, ignoreCase = true)

                        if (isSameDataType) {
                            val tagsInPacket = repository.getTagsForPacketSync(packet.id)
                            val isZeroBased = packetDt.isZeroBasedAddressing
                            val found = tagsInPacket.find { tag ->
                                val calcOffset = if (isZeroBased) (packet.offset + tag.offset) else (packet.offset + tag.offset - 1)
                                calcOffset == offsetFromAddr
                            }
                            if (found != null) {
                                matchedTag = found
                                break
                            }
                        }
                    }

                    val targetTag = matchedTag ?: repository.getTagByDataTypeAndOffsetSync(targetDataType.id, offsetFromAddr)

                    if (targetTag != null) {
                        val commentText = if (rec.comment.isNotBlank()) rec.comment else targetTag.description
                        val newTagTitle = if (rec.nickname.isNotBlank() && rec.nickname != rec.address) {
                            rec.nickname
                        } else if (targetTag.name.isNotBlank() && !targetTag.name.startsWith("Tag #") && targetTag.name != rec.address) {
                            targetTag.name
                        } else {
                            rec.nickname
                        }

                        val updatedTag = targetTag.copy(
                            name = newTagTitle,
                            description = commentText,
                            storedValue = rec.initialValue.ifEmpty { targetTag.storedValue }
                        )
                        repository.updateTag(updatedTag)
                        totalTagsUpdated++

                        if (!isRawDataFormat && targetDataType.hasBits) {
                            val bitTags = repository.getBitTagsForTagSync(targetTag.id)
                            bitTags.forEach { bitTag ->
                                val defaultName1 = "${targetTag.name}-${bitTag.bitIndex}"
                                val defaultName2 = "Tag #${targetTag.id}-${bitTag.bitIndex}"
                                val isUnedited = bitTag.name == defaultName1 ||
                                        bitTag.name == defaultName2 ||
                                        (bitTag.name.endsWith("-${bitTag.bitIndex}") && bitTag.name.substringBeforeLast("-") == targetTag.name)

                                if (isUnedited) {
                                    val newBitName = "$newTagTitle-${bitTag.bitIndex}"
                                    if (bitTag.name != newBitName) {
                                        repository.updateBitTag(bitTag.copy(name = newBitName))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            repository.refreshHierarchy()

            return ImportResult(
                isSuccess = true,
                nodeName = "Existing Packets",
                totalTagsImported = totalTagsUpdated,
                packetsCreated = 0
            )
        } catch (e: Exception) {
            return ImportResult(
                isSuccess = false,
                nodeName = "Existing Packets",
                totalTagsImported = 0,
                packetsCreated = 0,
                errorMessage = e.localizedMessage ?: "Failed to import tags."
            )
        }
    }

    suspend fun exportAllTags(
        repository: MainRepository,
        format: String = "Click Plus"
    ): ExportResult {
        return try {
            val dataTypes = repository.getAllDataTypesList()
            val allTags = repository.getAllTagsList()
            val allPackets = repository.getAllPacketsList()

            val sb = StringBuilder()
            sb.append("Address,Data Type,Nickname,Initial Value,Retentive,Address Comment\r\n")

            val isRawDataFormat = format.equals("Raw Data", ignoreCase = true)
            val exportedCombinations = mutableSetOf<String>()
            var exportedCount = 0

            allTags.forEach { tag ->
                val tagDt = dataTypes.find { it.id == tag.dataTypeId }
                    ?: dataTypes.find { it.shortName.equals("DS", ignoreCase = true) }
                    ?: dataTypes.firstOrNull()

                val dtShortName = tagDt?.shortName?.ifEmpty { tagDt?.description } ?: "DS"
                val dtName = tagDt?.dataType?.ifEmpty { "INT" } ?: "INT"

                val parentPacket = allPackets.find { p ->
                    repository.getTagsForPacketSync(p.id).any { t -> t.id == tag.id }
                }

                val calcOffset = if (parentPacket != null) {
                    if (tagDt?.isZeroBasedAddressing == true) (parentPacket.offset + tag.offset)
                    else (parentPacket.offset + tag.offset - 1)
                } else tag.offset

                val comboKey = "${dtShortName.uppercase()}_$calcOffset"

                if (isRawDataFormat || !exportedCombinations.contains(comboKey)) {
                    exportedCombinations.add(comboKey)

                    val addr = "$dtShortName$calcOffset"
                    val nick = tag.name
                    val initVal = tag.storedValue.ifEmpty { "0" }
                    val comment = tag.description

                    sb.append("$addr,$dtName,\"$nick\",$initVal,No,\"$comment\"\r\n")
                    exportedCount++

                    if (isRawDataFormat && tagDt?.hasBits == true) {
                        val bitTags = repository.getBitTagsForTagSync(tag.id)
                        bitTags.forEach { bitTag ->
                            val bitAddr = "$addr-${bitTag.bitIndex}"
                            val bitNick = bitTag.name
                            sb.append("$bitAddr,BIT,\"$bitNick\",0,No,\"\"\r\n")
                            exportedCount++
                        }
                    }
                }
            }

            val appName = "AiListTest"
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appSubDir = File(publicDir, appName)
            if (!appSubDir.exists()) {
                appSubDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val filePrefix = if (isRawDataFormat) "Raw_Data_Export" else "Tags_Export"
            val fileName = "${filePrefix}_$timestamp.csv"
            val exportFile = File(appSubDir, fileName)

            FileOutputStream(exportFile).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            ExportResult(
                isSuccess = true,
                filePath = exportFile.absolutePath,
                fileName = fileName,
                totalTagsExported = exportedCount
            )
        } catch (e: Exception) {
            ExportResult(
                isSuccess = false,
                filePath = "",
                fileName = "",
                totalTagsExported = 0,
                errorMessage = e.localizedMessage ?: "Failed to export tags."
            )
        }
    }
}
