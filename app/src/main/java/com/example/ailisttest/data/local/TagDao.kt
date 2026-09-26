package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE dataTypeId = :dataTypeId AND `Offset` = :offset LIMIT 1")
    suspend fun getTagByDataTypeAndOffsetSync(dataTypeId: Long, offset: Int): TagEntity?

    @Query("SELECT * FROM tags WHERE dataTypeId = :dataTypeId AND `Offset` >= :startOffset AND `Offset` < :endOffset ORDER BY `Offset` ASC")
    suspend fun getTagsForDataTypeAndOffsetRangeSync(dataTypeId: Long, startOffset: Int, endOffset: Int): List<TagEntity>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagByIdSync(id: Long): TagEntity?

    @Query("SELECT * FROM tags ORDER BY dataTypeId ASC, `Offset` ASC")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByNameSync(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
