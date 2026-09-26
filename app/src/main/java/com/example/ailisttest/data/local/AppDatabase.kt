package com.example.ailisttest.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        NodeEntity::class,
        PacketEntity::class,
        TagEntity::class,
        PlcDataTypes::class,
        BitTags::class,
        Screens::class,
        CustomGroup::class,
        HeaderItem::class,
        ListScreenItems::class,
        TagListItems::class
    ],
    version = 30,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun nodeDao(): NodeDao
    abstract fun packetDao(): PacketDao
    abstract fun tagDao(): TagDao
    abstract fun dataTypeDao(): DataTypeDao
    abstract fun bitTagDao(): BitTagDao
    abstract fun screensDao(): ScreensDao
    abstract fun customGroupDao(): CustomGroupDao
    abstract fun headerItemDao(): HeaderItemDao
    abstract fun listScreenItemsDao(): ListScreenItemsDao
    abstract fun tagListItemDao(): TagListItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // populateInitialData(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // populateInitialData(db)
                    }

                    /*
                    private fun populateInitialData(db: SupportSQLiteDatabase) {
                        try {
                            db.execSQL("PRAGMA foreign_keys=OFF")

                            db.execSQL("""
                                INSERT OR IGNORE INTO DataTypes (id, Name, shortName, bytes, defaultModbusAddress, isSigned, hasBits)
                                VALUES
                                (0, 'INT', 'DS', 2, 400001, 0, 1),
                                (1, 'INT2', 'DD', 4, 416385, 0, 1),
                                (2, 'HEX', 'DH', 2, 424577, 0, 1),
                                (3, 'FLOAT', 'DF', 4, 428673, 0, 0)
                            """.trimIndent())
                            db.execSQL("UPDATE DataTypes SET shortName = 'DS' WHERE Name = 'INT'")
                            db.execSQL("UPDATE DataTypes SET hasBits = 1 WHERE Name != 'FLOAT'")
                            db.execSQL("UPDATE DataTypes SET hasBits = 0 WHERE Name = 'FLOAT'")

                            db.execSQL("""
                                INSERT OR IGNORE INTO nodes (id, name, ipAddress) VALUES
                                (1, 'PLC Node 1', '192.168.1.100'),
                                (2, 'Sensor Node 2', '192.168.1.101')
                            """.trimIndent())

                            db.execSQL("""
                                INSERT OR IGNORE INTO packets (id, parentNodeId, name, description, type, period, config, offset, slaveNode, pollData, size) VALUES
                                (1, 1, 'Status Packet', 'System status and health metrics', 0, 1000, 'Modbus TCP', 1, 1, 1, 10),
                                (2, 1, 'Control Packet', 'Control commands and outputs', 0, 500, 'Modbus TCP', 10, 1, 0, 8),
                                (3, 2, 'Telemetry Packet', 'Environmental telemetry data', 3, 2000, 'Modbus TCP', 1, 2, 1, 16)
                            """.trimIndent())

                            db.execSQL("""
                                INSERT OR IGNORE INTO tags (id, name, description, parentPacketId, packetPosIndex, storedValue, packetOffset) VALUES
                                (1, 'Status Packet-1', 'Main status metric', 1, 1, '24', 1),
                                (2, 'Status Packet-2', 'System health metric', 1, 2, '101.3', 2),
                                (3, 'Control Packet-1', 'Pump ON/OFF control state', 2, 1, '1', 1),
                                (4, 'Telemetry Packet-1', 'Relative humidity (%)', 3, 1, '45', 1),
                                (5, 'Telemetry Packet-2', 'Supply voltage (V)', 3, 2, '12.0', 2)
                            """.trimIndent())

                            // Seed bitTags for tags 1, 2, 3 (each has INT data type = 2 bytes * 8 = 16 bits)
                            val sampleTags = listOf(
                                Pair(1L, "Status Packet-1"),
                                Pair(2L, "Status Packet-2"),
                                Pair(3L, "Control Packet-1")
                            )
                            for ((tagId, tagName) in sampleTags) {
                                for (i in 0 until 16) {
                                    db.execSQL("INSERT OR IGNORE INTO BitTags (Name, parentTagtId, bitIndex) VALUES ('$tagName-$i', $tagId, $i)")
                                }
                            }
                        } finally {
                            db.execSQL("PRAGMA foreign_keys=ON")
                        }
                    }
                    */
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
