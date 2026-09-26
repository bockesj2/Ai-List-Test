package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ListScreenItemsDao {
    @Query("SELECT * FROM ListScreenItems WHERE parentScreenId = :screenId ORDER BY ScreenIndex ASC")
    fun getItemsForScreen(screenId: Long): Flow<List<ListScreenItems>>

    @Query("SELECT * FROM ListScreenItems WHERE parentScreenId = :screenId ORDER BY ScreenIndex ASC")
    suspend fun getItemsForScreenSync(screenId: Long): List<ListScreenItems>

    @Transaction
    @Query("SELECT * FROM ListScreenItems WHERE parentScreenId = :screenId ORDER BY ScreenIndex ASC")
    fun getItemsWithTagForScreen(screenId: Long): Flow<List<ListScreenItemWithTag>>

    @Query("SELECT * FROM ListScreenItems WHERE id = :id")
    suspend fun getItemById(id: Long): ListScreenItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ListScreenItems): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ListScreenItems>)

    @Update
    suspend fun updateItem(item: ListScreenItems)

    @Query("SELECT * FROM ListScreenItems")
    suspend fun getAllItemsSync(): List<ListScreenItems>

    @Delete
    suspend fun deleteItem(item: ListScreenItems)

    @Query("DELETE FROM ListScreenItems")
    suspend fun deleteAllListScreenItems()
}
