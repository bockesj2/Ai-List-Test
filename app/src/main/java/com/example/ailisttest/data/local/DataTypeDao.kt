package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DataTypeDao {
    @Query("SELECT * FROM PlcDataTypes")
    fun getAllDataTypes(): Flow<List<PlcDataTypes>>

    @Query("SELECT * FROM PlcDataTypes")
    suspend fun getAllDataTypesList(): List<PlcDataTypes>

    @Query("SELECT * FROM PlcDataTypes WHERE id = :id")
    suspend fun getDataTypeById(id: Long): PlcDataTypes?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataType(dataType: PlcDataTypes): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataTypes: List<PlcDataTypes>)

    @Update
    suspend fun updateDataType(dataType: PlcDataTypes)

    @Query("DELETE FROM PlcDataTypes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM PlcDataTypes")
    suspend fun deleteAllDataTypes()
}

typealias PlcDataTypeDao = DataTypeDao
