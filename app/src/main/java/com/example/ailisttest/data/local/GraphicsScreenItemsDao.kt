package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphicsScreenItemsDao {
    @Query("SELECT * FROM GraphicsScreenItems WHERE parentScreenId = :screenId")
    fun getItemsForScreen(screenId: Long): Flow<List<GraphicsScreenItems>>

    @Query("SELECT * FROM GraphicsScreenItems WHERE parentScreenId = :screenId")
    suspend fun getItemsForScreenSync(screenId: Long): List<GraphicsScreenItems>

    @Transaction
    @Query("SELECT * FROM GraphicsScreenItems WHERE parentScreenId = :screenId")
    fun getItemsWithTagForScreen(screenId: Long): Flow<List<GraphicsScreenItemWithTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GraphicsScreenItems): Long

    @Update
    suspend fun updateItem(item: GraphicsScreenItems)

    @Delete
    suspend fun deleteItem(item: GraphicsScreenItems)

    @Query("DELETE FROM GraphicsScreenItems WHERE parentScreenId = :screenId")
    suspend fun deleteItemsForScreen(screenId: Long)
}
