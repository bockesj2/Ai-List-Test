package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagListItemDao {
    @Query("SELECT * FROM TagListItems WHERE parentTagId = :tagId ORDER BY number ASC")
    fun getListItemsForTag(tagId: Long): Flow<List<TagListItems>>

    @Query("SELECT * FROM TagListItems WHERE parentTagId = :tagId ORDER BY number ASC")
    suspend fun getListItemsForTagSync(tagId: Long): List<TagListItems>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: TagListItems): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllItems(items: List<TagListItems>)

    @Update
    suspend fun updateItem(item: TagListItems)

    @Delete
    suspend fun deleteItem(item: TagListItems)

    @Query("DELETE FROM TagListItems WHERE parentTagId = :tagId")
    suspend fun deleteItemsForTag(tagId: Long)

    @Query("DELETE FROM TagListItems")
    suspend fun deleteAllTagListItems()
}
