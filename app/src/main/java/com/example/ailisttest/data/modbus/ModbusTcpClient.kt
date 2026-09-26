package com.example.ailisttest.data.modbus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

data class ModbusReadResult(
    val registers: IntArray?,
    val requestBytes: ByteArray,
    val responseBytes: ByteArray?,
    val isSuccess: Boolean,
    val errorMessage: String?
)

data class ModbusWriteResult(
    val requestBytes: ByteArray,
    val responseBytes: ByteArray?,
    val isSuccess: Boolean,
    val errorMessage: String?
)

fun ByteArray.toHexString(): String = joinToString(" ") { String.format("%02X", it) }

class ModbusTcpClient(
    val ipAddress: String,
    val port: Int = 502,
    val timeoutMs: Int = 3000
) {
    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private val transactionIdCounter = AtomicInteger(1)

    val isConnected: Boolean
        get() = socket?.isConnected == true && socket?.isClosed == false

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            newSocket.soTimeout = timeoutMs
            input = DataInputStream(newSocket.inputStream)
            output = DataOutputStream(newSocket.outputStream)
            socket = newSocket
            true
        } catch (_: Exception) {
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            input?.close()
            output?.close()
            socket?.close()
        } catch (_: Exception) {
        } finally {
            input = null
            output = null
            socket = null
        }
    }

    /**
     * FC 03: Read Holding Registers with Detailed Debug Info
     */
    suspend fun readHoldingRegistersDetailed(
        slaveNode: Int,
        startAddress: Int,
        quantity: Int
    ): ModbusReadResult = withContext(Dispatchers.IO) {
        val transactionId = transactionIdCounter.getAndIncrement() and 0xFFFF
        val pduLength = 6 // 1 UnitID + 1 FC + 2 Address + 2 Quantity

        val reqBytes = ByteArray(12)
        reqBytes[0] = (transactionId ushr 8).toByte()
        reqBytes[1] = (transactionId and 0xFF).toByte()
        reqBytes[2] = 0x00
        reqBytes[3] = 0x00
        reqBytes[4] = (pduLength ushr 8).toByte()
        reqBytes[5] = (pduLength and 0xFF).toByte()
        reqBytes[6] = slaveNode.toByte()
        reqBytes[7] = 0x03 // FC 03
        reqBytes[8] = (startAddress ushr 8).toByte()
        reqBytes[9] = (startAddress and 0xFF).toByte()
        reqBytes[10] = (quantity ushr 8).toByte()
        reqBytes[11] = (quantity and 0xFF).toByte()

        if (!isConnected) {
            if (!connect()) {
                return@withContext ModbusReadResult(
                    registers = null,
                    requestBytes = reqBytes,
                    responseBytes = null,
                    isSuccess = false,
                    errorMessage = "Failed TCP socket connection to $ipAddress:$port"
                )
            }
        }
        val outStream = output
        val inStream = input
        if (outStream == null || inStream == null) {
            return@withContext ModbusReadResult(
                registers = null,
                requestBytes = reqBytes,
                responseBytes = null,
                isSuccess = false,
                errorMessage = "Socket Streams are null"
            )
        }

        try {
            outStream.write(reqBytes)
            outStream.flush()

            val headerBytes = ByteArray(7)
            inStream.readFully(headerBytes)

            val respLength = ((headerBytes[4].toInt() and 0xFF) shl 8) or (headerBytes[5].toInt() and 0xFF)
            val pduRemainingLength = (respLength - 1).coerceAtLeast(0)
            val pduBody = ByteArray(pduRemainingLength)
            inStream.readFully(pduBody)

            val fullRespBytes = headerBytes + pduBody

            if (pduRemainingLength < 1) {
                return@withContext ModbusReadResult(
                    registers = null,
                    requestBytes = reqBytes,
                    responseBytes = fullRespBytes,
                    isSuccess = false,
                    errorMessage = "Truncated Modbus PDU Response"
                )
            }

            val fc = pduBody[0].toInt() and 0xFF
            if (fc != 0x03) {
                val errCode = if (pduRemainingLength >= 2) (pduBody[1].toInt() and 0xFF).toString() else "Unknown"
                return@withContext ModbusReadResult(
                    registers = null,
                    requestBytes = reqBytes,
                    responseBytes = fullRespBytes,
                    isSuccess = false,
                    errorMessage = "Modbus Exception FC 0x${Integer.toHexString(fc)} (Code $errCode)"
                )
            }

            val registers = IntArray(quantity)
            for (i in 0 until quantity) {
                val offset = 2 + i * 2
                if (offset + 1 < pduBody.size) {
                    val high = pduBody[offset].toInt() and 0xFF
                    val low = pduBody[offset + 1].toInt() and 0xFF
                    registers[i] = (high shl 8) or low
                }
            }

            ModbusReadResult(
                registers = registers,
                requestBytes = reqBytes,
                responseBytes = fullRespBytes,
                isSuccess = true,
                errorMessage = null
            )
        } catch (e: Exception) {
            disconnect()
            ModbusReadResult(
                registers = null,
                requestBytes = reqBytes,
                responseBytes = null,
                isSuccess = false,
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }

    /**
     * FC 16 (0x10): Write Multiple Registers with Detailed Debug Info
     */
    suspend fun writeMultipleRegistersDetailed(
        slaveNode: Int,
        startAddress: Int,
        values: IntArray
    ): ModbusWriteResult = withContext(Dispatchers.IO) {
        val transactionId = transactionIdCounter.getAndIncrement() and 0xFFFF
        val quantity = values.size
        val byteCount = quantity * 2
        val pduLength = 7 + byteCount

        val reqBytes = ByteArray(7 + 6 + byteCount)
        reqBytes[0] = (transactionId ushr 8).toByte()
        reqBytes[1] = (transactionId and 0xFF).toByte()
        reqBytes[2] = 0x00
        reqBytes[3] = 0x00
        reqBytes[4] = (pduLength ushr 8).toByte()
        reqBytes[5] = (pduLength and 0xFF).toByte()
        reqBytes[6] = slaveNode.toByte()

        reqBytes[7] = 0x10 // FC 16
        reqBytes[8] = (startAddress ushr 8).toByte()
        reqBytes[9] = (startAddress and 0xFF).toByte()
        reqBytes[10] = (quantity ushr 8).toByte()
        reqBytes[11] = (quantity and 0xFF).toByte()
        reqBytes[12] = byteCount.toByte()

        for (i in 0 until quantity) {
            val v = values[i]
            reqBytes[13 + i * 2] = (v ushr 8).toByte()
            reqBytes[13 + i * 2 + 1] = (v and 0xFF).toByte()
        }

        if (!isConnected) {
            if (!connect()) {
                return@withContext ModbusWriteResult(
                    requestBytes = reqBytes,
                    responseBytes = null,
                    isSuccess = false,
                    errorMessage = "Failed TCP socket connection to $ipAddress:$port"
                )
            }
        }
        val outStream = output
        val inStream = input
        if (outStream == null || inStream == null) {
            return@withContext ModbusWriteResult(
                requestBytes = reqBytes,
                responseBytes = null,
                isSuccess = false,
                errorMessage = "Socket Streams are null"
            )
        }

        try {
            outStream.write(reqBytes)
            outStream.flush()

            val headerBytes = ByteArray(7)
            inStream.readFully(headerBytes)

            val respLength = ((headerBytes[4].toInt() and 0xFF) shl 8) or (headerBytes[5].toInt() and 0xFF)
            val pduRemainingLength = (respLength - 1).coerceAtLeast(0)
            val pduBody = ByteArray(pduRemainingLength)
            inStream.readFully(pduBody)

            val fullRespBytes = headerBytes + pduBody

            if (pduRemainingLength < 1) {
                return@withContext ModbusWriteResult(
                    requestBytes = reqBytes,
                    responseBytes = fullRespBytes,
                    isSuccess = false,
                    errorMessage = "Truncated Modbus PDU Response"
                )
            }

            val fc = pduBody[0].toInt() and 0xFF
            val isSuccess = fc == 0x10

            ModbusWriteResult(
                requestBytes = reqBytes,
                responseBytes = fullRespBytes,
                isSuccess = isSuccess,
                errorMessage = if (isSuccess) null else "Modbus Exception FC 0x${Integer.toHexString(fc)}"
            )
        } catch (e: Exception) {
            disconnect()
            ModbusWriteResult(
                requestBytes = reqBytes,
                responseBytes = null,
                isSuccess = false,
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            )
        }
    }
}
