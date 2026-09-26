package com.example.ailisttest.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "packets",
    foreignKeys = [
        ForeignKey(
            entity = NodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentNodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlcDataTypes::class,
            parentColumns = ["id"],
            childColumns = ["type"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["parentNodeId"]),
        Index(value = ["parentNodeId", "name"], unique = true),
        Index(value = ["type"])
    ]
)
data class PacketEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentNodeId: Long,
    val name: String,
    val description: String,
    val type: Int? = 0,
    val period: Int = 1000,
    val config: String,
    val offset: Int = 1,
    val slaveNode: Int = 1,
    val pollData: Boolean = true,
    val size: Int = 1
)
