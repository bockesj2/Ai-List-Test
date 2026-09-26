package com.example.ailisttest.data.modbus

import com.example.ailisttest.data.local.PlcDataTypes
import java.lang.Float
import java.util.Locale

enum class ModbusByteOrder(val label: String, val description: String) {
    ABCD("ABCD (1234) — Big-Endian", "Big-Endian order (ABCD)"),
    CDAB("CDAB (3412) — Word-Swap", "Word-Swap order (CDAB)"),
    BADC("BADC (2143) — Byte-Swap", "Byte-Swap order (BADC)"),
    DCBA("DCBA (4321) — Little-Endian", "Little-Endian order (DCBA)");

    companion object {
        val ALL_LABELS: List<String> = entries.map { it.label }

        fun fromLabel(label: String?): ModbusByteOrder {
            if (label.isNullOrBlank()) return CDAB
            val clean = label.trim().lowercase(Locale.US)
            return entries.find {
                it.label.equals(clean, ignoreCase = true) ||
                it.name.equals(clean, ignoreCase = true) ||
                clean.contains(it.name.lowercase(Locale.US)) ||
                (clean.contains("flip words") && it == CDAB) ||
                (clean.contains("no change") && it == ABCD)
            } ?: CDAB
        }
    }
}

object ModbusByteOrderTransformer {

    private data class Tuple4(val b0: Int, val b1: Int, val b2: Int, val b3: Int)

    /**
     * Transforms raw 16-bit Modbus registers (IntArray) according to the specified ModbusByteOrder
     * into Big-Endian (ABCD) format for the application.
     */
    fun transformRegisters(
        registers: IntArray,
        byteOrder: ModbusByteOrder,
        regsPerTag: Int = 2
    ): IntArray {
        if (registers.isEmpty() || byteOrder == ModbusByteOrder.ABCD) {
            return registers.copyOf()
        }

        val result = registers.copyOf()

        if (regsPerTag >= 2) {
            // 32-bit block for 2-register tags (4 bytes: b0, b1, b2, b3)
            var i = 0
            while (i + 1 < result.size) {
                val w0 = result[i] and 0xFFFF
                val w1 = result[i + 1] and 0xFFFF

                val b0 = (w0 ushr 8) and 0xFF
                val b1 = w0 and 0xFF
                val b2 = (w1 ushr 8) and 0xFF
                val b3 = w1 and 0xFF

                val (nb0, nb1, nb2, nb3) = when (byteOrder) {
                    ModbusByteOrder.ABCD -> Tuple4(b0, b1, b2, b3)
                    ModbusByteOrder.CDAB -> Tuple4(b2, b3, b0, b1)
                    ModbusByteOrder.BADC -> Tuple4(b1, b0, b3, b2)
                    ModbusByteOrder.DCBA -> Tuple4(b3, b2, b1, b0)
                }

                result[i] = ((nb0 and 0xFF) shl 8) or (nb1 and 0xFF)
                result[i + 1] = ((nb2 and 0xFF) shl 8) or (nb3 and 0xFF)
                i += 2
            }
        } else {
            // 16-bit block for 1-register tags (2 bytes: b0, b1)
            for (i in result.indices) {
                val w0 = result[i] and 0xFFFF
                val b0 = (w0 ushr 8) and 0xFF
                val b1 = w0 and 0xFF

                val (nb0, nb1) = when (byteOrder) {
                    ModbusByteOrder.ABCD, ModbusByteOrder.CDAB -> Pair(b0, b1)
                    ModbusByteOrder.BADC, ModbusByteOrder.DCBA -> Pair(b1, b0)
                }

                result[i] = ((nb0 and 0xFF) shl 8) or (nb1 and 0xFF)
            }
        }

        return result
    }

    /**
     * Parses value string from transformed registers array using PlcDataTypes entity definition.
     */
    fun parseValueFromRegisters(
        transformedRegisters: IntArray,
        offsetIndex: Int,
        dataTypeObj: PlcDataTypes?,
        typeNameFallback: String = "INT"
    ): String {
        if (transformedRegisters.isEmpty() || offsetIndex < 0 || offsetIndex >= transformedRegisters.size) {
            return "0"
        }

        val dataTypeKind = dataTypeObj?.dataType?.uppercase(Locale.US)
            ?: when {
                typeNameFallback.contains("FLOAT", ignoreCase = true) || typeNameFallback.contains("DF", ignoreCase = true) -> "FLOAT"
                typeNameFallback.contains("HEX", ignoreCase = true) || typeNameFallback.contains("DH", ignoreCase = true) || typeNameFallback.contains("UINT", ignoreCase = true) -> "UINT"
                else -> "INT"
            }

        val bytes = dataTypeObj?.bytes ?: if (dataTypeKind == "FLOAT" || typeNameFallback.contains("2") || typeNameFallback.contains("DD")) 4 else 2
        val regsPerTag = (bytes / 2).coerceAtLeast(1)

        if (regsPerTag == 1) {
            val reg16 = transformedRegisters[offsetIndex] and 0xFFFF
            return when (dataTypeKind) {
                "UINT" -> reg16.toString()
                "INT" -> reg16.toShort().toString()
                else -> reg16.toShort().toString()
            }
        } else {
            if (offsetIndex + 1 < transformedRegisters.size) {
                val w0 = transformedRegisters[offsetIndex] and 0xFFFF
                val w1 = transformedRegisters[offsetIndex + 1] and 0xFFFF
                val combined32 = (w0.toLong() shl 16) or w1.toLong()

                return when (dataTypeKind) {
                    "FLOAT" -> {
                        val f = Float.intBitsToFloat(combined32.toInt())
                        if (f.isNaN() || f.isInfinite()) "0.0" else String.format(Locale.US, "%.2f", f)
                    }
                    "UINT" -> (combined32 and 0xFFFFFFFFL).toString()
                    "INT" -> combined32.toInt().toString()
                    else -> combined32.toInt().toString()
                }
            } else {
                return (transformedRegisters[offsetIndex] and 0xFFFF).toString()
            }
        }
    }

    fun parseValueFromRegisters(
        transformedRegisters: IntArray,
        offsetIndex: Int,
        typeName: String,
        regsPerTag: Int
    ): String {
        return parseValueFromRegisters(transformedRegisters, offsetIndex, null, typeName)
    }
}
