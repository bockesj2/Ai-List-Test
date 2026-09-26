package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HeaderItemDao {
    @Query("SELECT * FROM HeaderItem WHERE parentScreenId = :screenId ORDER BY ScreenIndex ASC")
    fun getHeadersForScreen(screenId: Long): Flow<List<HeaderItem>>

    @Query("SELECT * FROM HeaderItem WHERE id = :id")
    suspend fun getHeaderByIdSync(id: Long): HeaderItem?

    @Query("SELECT * FROM HeaderItem")
    suspend fun getAllHeadersSync(): List<HeaderItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(header: HeaderItem): Long

    @Update
    suspend fun updateHeader(header: HeaderItem)

    @Delete
    suspend fun deleteHeader(header: HeaderItem)

    @Query("DELETE FROM HeaderItem")
    suspend fun deleteAllHeaderItems()
}
