package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppointmentDao
import com.example.data.local.dao.EmergencyContactDao
import com.example.data.local.dao.TriageRecordDao
import com.example.data.local.dao.WellnessLogDao
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.TriageRecordEntity
import com.example.data.local.entity.WellnessLogEntity

@Database(
    entities = [
        AppointmentEntity::class,
        EmergencyContactEntity::class,
        WellnessLogEntity::class,
        TriageRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun wellnessLogDao(): WellnessLogDao
    abstract fun triageRecordDao(): TriageRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medipulse_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
