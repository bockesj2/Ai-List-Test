package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BitTagDao {
    @Query("SELECT * FROM BitTags WHERE parentTagtId = :tagId ORDER BY bitIndex ASC")
    fun getBitTagsForTag(tagId: Long): Flow<List<BitTags>>

    @Query("SELECT * FROM BitTags WHERE parentTagtId = :tagId ORDER BY bitIndex ASC")
    suspend fun getBitTagsForTagSync(tagId: Long): List<BitTags>

    @Query("SELECT * FROM BitTags WHERE Name = :name LIMIT 1")
    suspend fun getBitTagByNameSync(name: String): BitTags?

    @Query("SELECT * FROM BitTags")
    suspend fun getAllBitTagsSync(): List<BitTags>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBitTag(bitTag: BitTags): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBitTags(bitTags: List<BitTags>)

    @Update
    suspend fun updateBitTag(bitTag: BitTags)

    @Delete
    suspend fun deleteBitTag(bitTag: BitTags)

    @Query("DELETE FROM BitTags")
    suspend fun deleteAllBitTags()
}
