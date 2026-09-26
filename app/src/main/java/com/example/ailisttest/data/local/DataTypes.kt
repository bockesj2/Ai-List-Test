package com.example.ailisttest.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "PlcDataTypes")
data class PlcDataTypes(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "Description")
    val description: String = "",
    @ColumnInfo(name = "shortName")
    val shortName: String = "",
    @ColumnInfo(name = "dataType")
    val dataType: String = "INT",
    @ColumnInfo(name = "bytes")
    val bytes: Int = 2,
    @ColumnInfo(name = "defaultModbusAddress")
    val defaultModbusAddress: Long = 0,
    @ColumnInfo(name = "isZeroBasedAddressing")
    val isZeroBasedAddressing: Boolean = false,
    @ColumnInfo(name = "hasBits")
    val hasBits: Boolean = true
)

typealias DataTypes = PlcDataTypes
typealias DataTypeEntity = PlcDataTypes
