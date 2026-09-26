package com.example.ailisttest.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    foreignKeys = [
        ForeignKey(
            entity = PlcDataTypes::class,
            parentColumns = ["id"],
            childColumns = ["dataTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dataTypeId"]),
        Index(value = ["dataTypeId", "Offset"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val dataTypeId: Long,
    val storedValue: String,
    @ColumnInfo(name = "Offset")
    val offset: Int
)
