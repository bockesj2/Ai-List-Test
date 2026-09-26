package com.example.ailisttest.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "TagListItems",
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parentTagId"]),
        Index(value = ["parentTagId", "number"], unique = true)
    ]
)
data class TagListItems(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "parentTagId")
    val parentTagId: Long = 0,
    @ColumnInfo(name = "number")
    val number: Int = 0,
    @ColumnInfo(name = "label")
    val label: String = ""
)
