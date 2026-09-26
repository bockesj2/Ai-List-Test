package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomGroupDao {
    @Query("SELECT * FROM CustomGroup WHERE parentScreenId = :screenId ORDER BY ScreenIndex ASC")
    fun getGroupsForScreen(screenId: Long): Flow<List<CustomGroup>>

    @Query("SELECT * FROM CustomGroup WHERE id = :id")
    suspend fun getGroupByIdSync(id: Long): CustomGroup?

    @Query("SELECT * FROM CustomGroup")
    suspend fun getAllGroupsSync(): List<CustomGroup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CustomGroup): Long

    @Update
    suspend fun updateGroup(group: CustomGroup)

    @Delete
    suspend fun deleteGroup(group: CustomGroup)

    @Query("DELETE FROM CustomGroup")
    suspend fun deleteAllCustomGroups()
}
