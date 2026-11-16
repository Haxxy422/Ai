package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ActionExecutionStatus
import com.example.data.model.ActionType

class RoseTypeConverters {
    @TypeConverter
    fun fromStatus(status: ActionExecutionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): ActionExecutionStatus =
        runCatching { ActionExecutionStatus.valueOf(value) }
            .getOrDefault(ActionExecutionStatus.INFO)

    @TypeConverter
    fun fromActionType(action: ActionType?): String? = action?.name

    @TypeConverter
    fun toActionType(value: String?): ActionType? =
        value?.let { runCatching { ActionType.valueOf(it) }.getOrNull() }
}

@Database(
    entities = [
        ChatMessageEntity::class,
        CommandHistoryEntity::class,
        MemoryEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoseTypeConverters::class)
abstract class RoseDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun memoryDao(): MemoryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: RoseDatabase? = null

        fun getInstance(context: Context): RoseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoseDatabase::class.java,
                    "rose_assistant.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
