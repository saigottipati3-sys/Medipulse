package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hospitalName: String,
    val doctorName: String,
    val specialty: String,
    val date: String,
    val timeSlot: String,
    val reason: String,
    val status: String = "Confirmed", // Confirmed, Completed, Cancelled
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String,
    val phoneNumber: String,
    val isPrimary: Boolean = false
)

@Entity(tableName = "wellness_logs")
data class WellnessLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val dayName: String,
    val mood: String, // Calm, Good, Stressed, Fatigued, Anxious
    val sleepHours: Double,
    val steps: Int,
    val screenTimeHours: Double,
    val heartRateBpm: Int = 72,
    val stressScore: Int = 3, // 1-10
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "triage_records")
data class TriageRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symptoms: String,
    val duration: String,
    val severity: Int,
    val wellnessContext: String,
    val followUpQuestion: String = "",
    val urgencyLevel: String, // self_care, see_doctor_soon, urgent_care, emergency
    val recommendedSpecialist: String,
    val selfCareTip: String,
    val summaryForDoctor: String,
    val triggerEmergencyFlow: Boolean,
    val disclaimer: String,
    val createdAt: Long = System.currentTimeMillis()
)
