package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NodeDao {
    @Query("SELECT * FROM nodes")
    suspend fun getAllNodesList(): List<NodeEntity>

    @Query("SELECT * FROM nodes")
    fun getAllNodes(): Flow<List<NodeEntity>>

    @Query("SELECT * FROM nodes WHERE id = :id")
    suspend fun getNodeById(id: Long): NodeEntity?

    @Transaction
    @Query("SELECT * FROM nodes")
    fun getNodesWithPackets(): Flow<List<NodeWithPackets>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNode(node: NodeEntity): Long

    @Update
    suspend fun updateNode(node: NodeEntity)

    @Delete
    suspend fun deleteNode(node: NodeEntity)

    @Query("DELETE FROM nodes")
    suspend fun deleteAllNodes()
}
