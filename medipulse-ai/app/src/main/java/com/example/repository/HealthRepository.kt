package com.example.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.TriageRecordEntity
import com.example.data.local.entity.WellnessLogEntity
import com.example.data.model.Hospital
import com.example.data.model.TriageResponse
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HealthRepository(private val db: AppDatabase) {

    val upcomingAppointments: Flow<List<AppointmentEntity>> =
        db.appointmentDao().getUpcomingAppointments()

    val allAppointments: Flow<List<AppointmentEntity>> =
        db.appointmentDao().getAllAppointments()

    val emergencyContacts: Flow<List<EmergencyContactEntity>> =
        db.emergencyContactDao().getAllContacts()

    val recentWellnessLogs: Flow<List<WellnessLogEntity>> =
        db.wellnessLogDao().getRecentLogs()

    val latestTriageRecord: Flow<TriageRecordEntity?> =
        db.triageRecordDao().getLatestTriageRecord()

    suspend fun seedInitialDataIfEmpty() {
        if (db.emergencyContactDao().getCount() == 0) {
            db.emergencyContactDao().insertContacts(
                listOf(
                    EmergencyContactEntity(
                        name = "Sarah Miller",
                        relationship = "Spouse / Next of Kin",
                        phoneNumber = "+1 (555) 019-2834",
                        isPrimary = true
                    ),
                    EmergencyContactEntity(
                        name = "Dr. Robert Vance",
                        relationship = "Primary Care Physician",
                        phoneNumber = "+1 (555) 014-9821",
                        isPrimary = false
                    ),
                    EmergencyContactEntity(
                        name = "Emergency Services (Local Dispatch)",
                        relationship = "EMS Rapid Response",
                        phoneNumber = "911",
                        isPrimary = false
                    )
                )
            )
        }

        if (db.wellnessLogDao().getCount() == 0) {
            // Seed a realistic 7-day pattern showing the exact prompt's background:
            // "Sleep averaged 4.2 hrs over last 3 days, high reported stress"
            db.wellnessLogDao().insertLogs(
                listOf(
                    WellnessLogEntity(
                        date = "Today",
                        dayName = "Today",
                        mood = "Stressed",
                        sleepHours = 4.0,
                        steps = 3840,
                        screenTimeHours = 7.5,
                        heartRateBpm = 86,
                        stressScore = 8
                    ),
                    WellnessLogEntity(
                        date = "Yesterday",
                        dayName = "Sun",
                        mood = "Fatigued",
                        sleepHours = 4.2,
                        steps = 4210,
                        screenTimeHours = 6.8,
                        heartRateBpm = 82,
                        stressScore = 8
                    ),
                    WellnessLogEntity(
                        date = "2 days ago",
                        dayName = "Sat",
                        mood = "Anxious",
                        sleepHours = 4.4,
                        steps = 5120,
                        screenTimeHours = 8.1,
                        heartRateBpm = 79,
                        stressScore = 7
                    ),
                    WellnessLogEntity(
                        date = "3 days ago",
                        dayName = "Fri",
                        mood = "Tired",
                        sleepHours = 5.5,
                        steps = 6800,
                        screenTimeHours = 6.0,
                        heartRateBpm = 74,
                        stressScore = 6
                    ),
                    WellnessLogEntity(
                        date = "4 days ago",
                        dayName = "Thu",
                        mood = "Good",
                        sleepHours = 6.8,
                        steps = 8400,
                        screenTimeHours = 4.5,
                        heartRateBpm = 70,
                        stressScore = 4
                    ),
                    WellnessLogEntity(
                        date = "5 days ago",
                        dayName = "Wed",
                        mood = "Calm",
                        sleepHours = 7.1,
                        steps = 9100,
                        screenTimeHours = 4.2,
                        heartRateBpm = 68,
                        stressScore = 3
                    ),
                    WellnessLogEntity(
                        date = "6 days ago",
                        dayName = "Tue",
                        mood = "Energetic",
                        sleepHours = 7.4,
                        steps = 10240,
                        screenTimeHours = 3.9,
                        heartRateBpm = 66,
                        stressScore = 2
                    )
                )
            )
        }
    }

    // Role 1: AI Doctor Triage
    suspend fun triageSymptoms(
        symptoms: String,
        duration: String,
        severity: Int,
        wellnessContext: String?
    ): TriageResponse {
        val result = GeminiClient.triageSymptoms(symptoms, duration, severity, wellnessContext)
        // Store in local history
        db.triageRecordDao().insertRecord(
            TriageRecordEntity(
                symptoms = symptoms,
                duration = duration,
                severity = severity,
                wellnessContext = wellnessContext ?: "",
                followUpQuestion = result.followUpQuestion,
                urgencyLevel = result.urgencyLevel,
                recommendedSpecialist = result.recommendedSpecialist,
                selfCareTip = result.selfCareTip,
                summaryForDoctor = result.summaryForDoctor,
                triggerEmergencyFlow = result.triggerEmergencyFlow,
                disclaimer = result.disclaimer
            )
        )
        return result
    }

    // Role 2: Wellness Context Builder
    suspend fun buildWellnessContext(logs: List<WellnessLogEntity>): String {
        return GeminiClient.summarizeWellness(logs)
    }

    // Role 3: Emergency SMS Generator
    suspend fun generateEmergencySms(
        userName: String,
        triageSummary: String,
        currentLocation: String
    ): String {
        return GeminiClient.generateEmergencyMessage(userName, triageSummary, currentLocation)
    }

    // Appointments
    suspend fun bookAppointment(
        hospitalName: String,
        doctorName: String,
        specialty: String,
        date: String,
        timeSlot: String,
        reason: String
    ): Long {
        return db.appointmentDao().insertAppointment(
            AppointmentEntity(
                hospitalName = hospitalName,
                doctorName = doctorName,
                specialty = specialty,
                date = date,
                timeSlot = timeSlot,
                reason = reason,
                status = "Confirmed"
            )
        )
    }

    suspend fun cancelAppointment(id: Long) {
        db.appointmentDao().updateStatus(id, "Cancelled")
    }

    suspend fun addEmergencyContact(name: String, relationship: String, phone: String, isPrimary: Boolean) {
        db.emergencyContactDao().insertContact(
            EmergencyContactEntity(
                name = name,
                relationship = relationship,
                phoneNumber = phone,
                isPrimary = isPrimary
            )
        )
    }

    suspend fun deleteEmergencyContact(id: Long) {
        db.emergencyContactDao().deleteContact(id)
    }

    suspend fun logDailyWellness(
        mood: String,
        sleepHours: Double,
        steps: Int,
        screenTime: Double,
        stressScore: Int
    ) {
        db.wellnessLogDao().insertLog(
            WellnessLogEntity(
                date = "Today",
                dayName = "Today",
                mood = mood,
                sleepHours = sleepHours,
                steps = steps,
                screenTimeHours = screenTime,
                heartRateBpm = 72 + (stressScore * 2),
                stressScore = stressScore
            )
        )
    }

    // Hospitals directory
    fun getHospitals(): List<Hospital> {
        return listOf(
            Hospital(
                id = "h1",
                name = "St. Jude Emergency & Level 1 Trauma Center",
                type = "Emergency Hospital",
                specialistTypes = listOf("Emergency Room", "Cardiologist", "Trauma Surgeon", "Neurologist"),
                address = "1200 Pine Street, Medical District",
                distanceMiles = 0.8,
                latitude = 37.7892,
                longitude = -122.4101,
                rating = 4.9,
                isOpen24Hours = true,
                erWaitTimeMinutes = 8,
                phoneNumber = "+1 (555) 911-3000",
                availableSlots = listOf("Immediate Walk-in", "Today 1:00 PM", "Today 2:30 PM", "Tomorrow 9:00 AM")
            ),
            Hospital(
                id = "h2",
                name = "Metro Heart & Vascular Institute",
                type = "Specialty Cardiac Care",
                specialistTypes = listOf("Cardiologist", "Pulmonologist", "General Physician"),
                address = "450 Sutter Healthcare Blvd, Suite 800",
                distanceMiles = 1.4,
                latitude = 37.7898,
                longitude = -122.4082,
                rating = 4.8,
                isOpen24Hours = false,
                erWaitTimeMinutes = null,
                phoneNumber = "+1 (555) 832-7200",
                availableSlots = listOf("Today 2:15 PM", "Today 4:00 PM", "Tomorrow 10:30 AM", "Tomorrow 2:00 PM")
            ),
            Hospital(
                id = "h3",
                name = "Downtown Express Urgent Care",
                type = "Urgent Care Clinic",
                specialistTypes = listOf("Urgent Care", "General Physician", "Orthopedist"),
                address = "870 Market St, Financial Center",
                distanceMiles = 0.5,
                latitude = 37.7845,
                longitude = -122.4070,
                rating = 4.7,
                isOpen24Hours = false,
                erWaitTimeMinutes = 15,
                phoneNumber = "+1 (555) 431-8900",
                availableSlots = listOf("Walk-ins Welcome", "Today 1:30 PM", "Today 3:45 PM", "Tomorrow 8:30 AM")
            ),
            Hospital(
                id = "h4",
                name = "Beacon Comprehensive Primary Care",
                type = "Outpatient Clinic",
                specialistTypes = listOf("General Physician", "Dermatologist", "Pediatrician"),
                address = "600 Harrison St, Suite 210",
                distanceMiles = 1.1,
                latitude = 37.7850,
                longitude = -122.3980,
                rating = 4.9,
                isOpen24Hours = false,
                erWaitTimeMinutes = null,
                phoneNumber = "+1 (555) 762-1100",
                availableSlots = listOf("Tomorrow 9:30 AM", "Tomorrow 11:15 AM", "Tomorrow 3:00 PM", "Thu 10:00 AM")
            ),
            Hospital(
                id = "h5",
                name = "Bay Area Neurological & Spine Hospital",
                type = "Neurological Center",
                specialistTypes = listOf("Neurologist", "Orthopedist", "Emergency Room"),
                address = "2100 Webster St, Upper Campus",
                distanceMiles = 2.3,
                latitude = 37.7910,
                longitude = -122.4330,
                rating = 4.8,
                isOpen24Hours = true,
                erWaitTimeMinutes = 18,
                phoneNumber = "+1 (555) 923-4500",
                availableSlots = listOf("Today 4:30 PM", "Tomorrow 11:00 AM", "Thu 1:30 PM")
            )
        )
    }
}
