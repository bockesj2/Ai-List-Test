package com.example.ailisttest.data.modbus

data class PacketPollingStats(
    val packetId: Long,
    val packetName: String,
    val commStatus: Boolean = false,
    val totalPackets: Long = 0L,
    val recentSuccesses: List<Boolean> = emptyList(),
    val lastReadTimestamp: Long = 0L,
    val lastErrorMessage: String? = null
) {
    val successRatePercentage: Int
        get() {
            if (recentSuccesses.isEmpty()) return 0
            val successfulCount = recentSuccesses.count { it }
            return (successfulCount * 100) / recentSuccesses.size
        }

    val consecutiveFailures: Int
        get() {
            var count = 0
            for (i in recentSuccesses.indices.reversed()) {
                if (!recentSuccesses[i]) count++ else break
            }
            return count
        }
}
