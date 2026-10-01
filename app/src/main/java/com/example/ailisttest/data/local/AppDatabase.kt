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
        GraphicsScreenItems::class,
        TagListItems::class
    ],
    version = 35,
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
    abstract fun graphicsScreenItemsDao(): GraphicsScreenItemsDao
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
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
