package com.example.ailisttest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PacketDao {
    @Query("SELECT * FROM packets WHERE parentNodeId = :nodeId")
    suspend fun getPacketsForNodeSync(nodeId: Long): List<PacketEntity>

    @Query("SELECT * FROM packets WHERE id = :id")
    suspend fun getPacketByIdSync(id: Long): PacketEntity?

    @Query("SELECT * FROM packets")
    suspend fun getAllPacketsSync(): List<PacketEntity>

    @Query("SELECT * FROM packets WHERE parentNodeId = :nodeId")
    fun getPacketsForNode(nodeId: Long): Flow<List<PacketEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPacket(packet: PacketEntity): Long

    @Update
    suspend fun updatePacket(packet: PacketEntity)

    @Delete
    suspend fun deletePacket(packet: PacketEntity)

    @Query("DELETE FROM packets")
    suspend fun deleteAllPackets()
}
