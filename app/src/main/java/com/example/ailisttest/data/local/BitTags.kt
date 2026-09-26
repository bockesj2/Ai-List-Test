package com.example.ailisttest.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "BitTags",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTagtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentTagtId"])
    ]
)
data class BitTags(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "Name")
    val name: String = "",
    @ColumnInfo(name = "parentTagtId")
    val parentTagId: Long = 0,
    @ColumnInfo(name = "bitIndex")
    val bitIndex: Int = 0
)

typealias BitTagEntity = BitTags
