package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [IdeProjectEntity::class, IdeFileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class IdeDatabase : RoomDatabase() {
    abstract fun ideDao(): IdeDao

    companion object {
        @Volatile
        private var instance: IdeDatabase? = null

        fun getInstance(context: Context): IdeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    IdeDatabase::class.java,
                    "studiodroid_workspace.db",
                ).build().also { instance = it }
            }
        }
    }
}
