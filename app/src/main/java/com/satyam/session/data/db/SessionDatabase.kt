package com.satyam.session.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.satyam.session.data.model.ActivityBucket
import com.satyam.session.data.model.Session

@Database(
    entities = [ActivityBucket::class, Session::class],
    version = 1,
    exportSchema = false
)
abstract class SessionDatabase : RoomDatabase() {

    abstract fun activityBucketDao(): ActivityBucketDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: SessionDatabase? = null

        fun getDatabase(context: Context): SessionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SessionDatabase::class.java,
                    "session_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
