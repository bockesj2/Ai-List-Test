package com.example.ailisttest.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "Screens")
data class Screens(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo var Name: String = "",
    @ColumnInfo var Type: Int = TYPE_LIST,
    @ColumnInfo(name = "backgroundColorHex") var backgroundColorHex: String = "#FAFAFA",
    @ColumnInfo(name = "backgroundImage") var backgroundImage: String = "",
    @ColumnInfo(name = "backgroundType") var backgroundType: String = "Color",
    @ColumnInfo(name = "canvasWidth") var canvasWidth: Float = 0f,
    @ColumnInfo(name = "canvasHeight") var canvasHeight: Float = 0f
) {
    companion object {
        const val TYPE_LIST = 1
        const val TYPE_GRAPHICS = 2
        const val TYPE_GRAPHICS_GROUP = 3
    }
}

@Entity(
    tableName = "CustomGroup",
    foreignKeys = [
        ForeignKey(
            entity = Screens::class,
            parentColumns = ["id"],
            childColumns = ["parentScreenId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomGroup(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "parentScreenId") var parentScreenId: Long = 0,
    @ColumnInfo var groupName: String = "New Group",
    @ColumnInfo var colorHex: String = "#F5F5F5",
    @ColumnInfo var ScreenIndex: Int = 0
)

@Entity(
    tableName = "HeaderItem",
    foreignKeys = [
        ForeignKey(
            entity = Screens::class,
            parentColumns = ["id"],
            childColumns = ["parentScreenId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HeaderItem(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "parentScreenId") var parentScreenId: Long = 0,
    @ColumnInfo var title: String = "Header Text",
    @ColumnInfo var colorHex: String = "#2196F3",
    @ColumnInfo var ScreenIndex: Int = 0
)

@Entity(
    tableName = "ListScreenItems",
    foreignKeys = [
        ForeignKey(
            entity = Screens::class,
            parentColumns = ["id"],
            childColumns = ["parentScreenId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ListScreenItems(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "parentScreenId") var parentScreenId: Long = 0,
    @ColumnInfo(name = "parentCustomGroupId") var parentCustomGroupId: Long? = null,
    @ColumnInfo(name = "parentTagId") var parentTagId: Long? = null,
    @ColumnInfo(name = "DisplayTypeId") var DisplayType: String = "",
    @ColumnInfo var Type: Int = 0,
    @ColumnInfo var ScreenIndex: Int = 0,
    @ColumnInfo(name = "isReadOnly") var isReadOnly: Boolean = true,
    @ColumnInfo(name = "isTwoTouch") var isTwoTouch: Boolean = true,
    @ColumnInfo(name = "ShowBits") var isShowBits: Boolean = false
)

data class ListScreenItemWithTag(
    @Embedded val item: ListScreenItems,
    @Relation(
        parentColumn = "parentTagId",
        entityColumn = "id"
    )
    val tag: TagEntity?
)

data class ScreenWithListItems(
    @Embedded val screen: Screens,
    @Relation(
        entity = ListScreenItems::class,
        parentColumn = "id",
        entityColumn = "parentScreenId"
    )
    val items: List<ListScreenItemWithTag>
)

@Entity(
    tableName = "GraphicsScreenItems",
    foreignKeys = [
        ForeignKey(
            entity = Screens::class,
            parentColumns = ["id"],
            childColumns = ["parentScreenId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GraphicsScreenItems(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "parentScreenId") var parentScreenId: Long = 0,
    @ColumnInfo(name = "parentCustomGroupId") var parentCustomGroupId: Long? = null,
    @ColumnInfo(name = "parentTagId") var parentTagId: Long? = null,
    @ColumnInfo(name = "DisplayTypeId") var DisplayType: String = "",
    @ColumnInfo var Type: Int = 0,
    @ColumnInfo var OffsetX: Float = 0f,
    @ColumnInfo var OffsetY: Float = 0f,
    @ColumnInfo var Width: Float = 0f,
    @ColumnInfo var Height: Float = 0f,
    @ColumnInfo(name = "isReadOnly") var isReadOnly: Boolean = true,
    @ColumnInfo(name = "isTwoTouch") var isTwoTouch: Boolean = true,
    @ColumnInfo(name = "ShowBits") var isShowBits: Boolean = false,
    @ColumnInfo(name = "ShowTagName") var isShowTagName: Boolean = true,
    @ColumnInfo(name = "configStr") var configStr: String = ""
)

data class GraphicsScreenItemWithTag(
    @Embedded val item: GraphicsScreenItems,
    @Relation(
        parentColumn = "parentTagId",
        entityColumn = "id"
    )
    val tag: TagEntity?
)

data class ScreenWithGraphicsItems(
    @Embedded val screen: Screens,
    @Relation(
        entity = GraphicsScreenItems::class,
        parentColumn = "id",
        entityColumn = "parentScreenId"
    )
    val items: List<GraphicsScreenItemWithTag>
)
