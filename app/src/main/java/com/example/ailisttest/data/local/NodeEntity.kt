package com.example.ailisttest.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nodes",
    indices = [Index(value = ["name"], unique = true)]
)
data class NodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ipAddress: String = "192.168.0.10",
    val byteOrder: String = "CDAB (3412) — Word-Swap",
    val plcPreset: String = "Click Plus PLC"
)
