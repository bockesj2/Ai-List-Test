package com.example.ailisttest.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class TagWithBitTags(
    @Embedded val tag: TagEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentTagtId"
    )
    val bitTags: List<BitTags>
)

data class PacketWithTags(
    val packet: PacketEntity,
    val tagsWithBitTags: List<TagWithBitTags>
) {
    val tags: List<TagEntity>
        get() = tagsWithBitTags.map { it.tag }
}

data class NodeWithPackets(
    @Embedded val node: NodeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "parentNodeId"
    )
    val packets: List<PacketEntity>
)

data class NodeWithPacketsAndTags(
    val node: NodeEntity,
    val packetsWithTags: List<PacketWithTags>
)
