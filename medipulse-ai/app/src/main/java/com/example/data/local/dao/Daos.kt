package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.TriageRecordEntity
import com.example.data.local.entity.WellnessLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query("SELECT * FROM appointments ORDER BY createdAt DESC")
    fun getAllAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE status = 'Confirmed' ORDER BY createdAt DESC")
    fun getUpcomingAppointments(): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity): Long

    @Update
    suspend fun updateAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteAppointment(id: Long)
}

@Dao
interface EmergencyContactDao {
    @Query("SELECT * FROM emergency_contacts ORDER BY isPrimary DESC, id ASC")
    fun getAllContacts(): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: EmergencyContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<EmergencyContactEntity>)

    @Query("DELETE FROM emergency_contacts WHERE id = :id")
    suspend fun deleteContact(id: Long)

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getCount(): Int
}

@Dao
interface WellnessLogDao {
    @Query("SELECT * FROM wellness_logs ORDER BY createdAt DESC LIMIT 7")
    fun getRecentLogs(): Flow<List<WellnessLogEntity>>

    @Query("SELECT * FROM wellness_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<WellnessLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WellnessLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<WellnessLogEntity>)

    @Query("SELECT COUNT(*) FROM wellness_logs")
    suspend fun getCount(): Int
}

@Dao
interface TriageRecordDao {
    @Query("SELECT * FROM triage_records ORDER BY createdAt DESC")
    fun getAllTriageRecords(): Flow<List<TriageRecordEntity>>

    @Query("SELECT * FROM triage_records ORDER BY createdAt DESC LIMIT 1")
    fun getLatestTriageRecord(): Flow<TriageRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: TriageRecordEntity): Long

    @Query("DELETE FROM triage_records")
    suspend fun clearHistory()
}
