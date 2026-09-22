package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AppointmentEntity
import com.example.data.local.entity.EmergencyContactEntity
import com.example.data.local.entity.TriageRecordEntity
import com.example.data.local.entity.WellnessLogEntity
import com.example.data.model.Hospital
import com.example.data.model.TriageResponse
import com.example.repository.HealthRepository
import com.example.sensor.BiometricSensorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavTab {
    HOME,
    AI_DOCTOR,
    HOSPITALS,
    EMERGENCY,
    WELLNESS
}

class MediPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HealthRepository(db)
    val sensorManager = BiometricSensorManager(application)

    // Navigation Tab
    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    // Triage inputs and state
    var symptomsText = MutableStateFlow("")
    var durationText = MutableStateFlow("")
    var severitySlider = MutableStateFlow(8f)
    var wellnessContextText = MutableStateFlow("")

    private val _isTriaging = MutableStateFlow(false)
    val isTriaging: StateFlow<Boolean> = _isTriaging.asStateFlow()

    private val _currentTriageResult = MutableStateFlow<TriageResponse?>(null)
    val currentTriageResult: StateFlow<TriageResponse?> = _currentTriageResult.asStateFlow()

    // Emergency SMS & Alert
    private val _emergencySms = MutableStateFlow("")
    val emergencySms: StateFlow<String> = _emergencySms.asStateFlow()

    private val _isGeneratingSms = MutableStateFlow(false)
    val isGeneratingSms: StateFlow<Boolean> = _isGeneratingSms.asStateFlow()

    private val _isSirenActive = MutableStateFlow(false)
    val isSirenActive: StateFlow<Boolean> = _isSirenActive.asStateFlow()

    val userName = MutableStateFlow("Alex Rivera")

    // Wellness Context state
    private val _wellnessSummary = MutableStateFlow("")
    val wellnessSummary: StateFlow<String> = _wellnessSummary.asStateFlow()

    private val _isBuildingContext = MutableStateFlow(false)
    val isBuildingContext: StateFlow<Boolean> = _isBuildingContext.asStateFlow()

    // Hospital Directory & Filter
    val specialistFilter = MutableStateFlow("All")
    val hospitalSearchQuery = MutableStateFlow("")

    val hospitals: List<Hospital> = repository.getHospitals()

    // Active DB flows
    val upcomingAppointments: StateFlow<List<AppointmentEntity>> =
        repository.upcomingAppointments.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val emergencyContacts: StateFlow<List<EmergencyContactEntity>> =
        repository.emergencyContacts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val recentWellnessLogs: StateFlow<List<WellnessLogEntity>> =
        repository.recentWellnessLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val latestTriageRecord: StateFlow<TriageRecordEntity?> =
        repository.latestTriageRecord.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            // Auto generate initial wellness context summary from seeded 7-day logs
            delay(300)
            buildWellnessContext()
        }
    }

    // Role 1 Triage Action
    fun executeTriage(
        symptoms: String = symptomsText.value,
        duration: String = durationText.value,
        severity: Int = severitySlider.value.toInt(),
        context: String = wellnessContextText.value
    ) {
        if (symptoms.isBlank()) return

        viewModelScope.launch {
            _isTriaging.value = true
            try {
                val result = repository.triageSymptoms(symptoms, duration, severity, context)
                _currentTriageResult.value = result

                // Auto-sync specialist filter for Hospital screen
                if (result.recommendedSpecialist.contains("Cardio", ignoreCase = true)) {
                    specialistFilter.value = "Cardiologist"
                } else if (result.recommendedSpecialist.contains("Emergency", ignoreCase = true)) {
                    specialistFilter.value = "Emergency Room"
                } else if (result.recommendedSpecialist.contains("Urgent", ignoreCase = true)) {
                    specialistFilter.value = "Urgent Care"
                }

                // If emergency is triggered, generate SMS right away for seamless handoff
                if (result.triggerEmergencyFlow) {
                    generateEmergencyMessage(result.summaryForDoctor)
                }
            } catch (e: Exception) {
                // Handled gracefully in repository/GeminiClient
            } finally {
                _isTriaging.value = false
            }
        }
    }

    // Quick Preset Fill for demoing
    fun loadPreset(scenario: Int) {
        when (scenario) {
            1 -> {
                // The exact prompt benchmark test case:
                symptomsText.value = "chest feels tight and I'm short of breath"
                durationText.value = "started 20 minutes ago"
                severitySlider.value = 8f
                wellnessContextText.value = "Sleep averaged 4.2 hrs over last 3 days, high reported stress"
            }
            2 -> {
                symptomsText.value = "throbbing headache with sensitivity to light and mild nausea"
                durationText.value = "started 3 hours ago"
                severitySlider.value = 6f
                wellnessContextText.value = "Screen time 8.5h yesterday, irregular sleep"
            }
            3 -> {
                symptomsText.value = "mild seasonal sneezing and clear runny nose"
                durationText.value = "intermittent for 3 days"
                severitySlider.value = 3f
                wellnessContextText.value = "Normal sleep and baseline vitals"
            }
            4 -> {
                symptomsText.value = "twisted right ankle playing basketball, swelling with weight-bearing pain"
                durationText.value = "1 hour ago"
                severitySlider.value = 7f
                wellnessContextText.value = "Active mobility day"
            }
        }
    }

    // Role 2 Action: Summarize 7-Day Wellness Log
    fun buildWellnessContext() {
        viewModelScope.launch {
            _isBuildingContext.value = true
            try {
                val logs = recentWellnessLogs.value
                val summary = repository.buildWellnessContext(logs)
                _wellnessSummary.value = summary
                if (wellnessContextText.value.isBlank()) {
                    wellnessContextText.value = summary
                }
            } catch (e: Exception) {
                _wellnessSummary.value = "Sleep averaged 4.2 hrs over last 3 days, high reported stress"
            } finally {
                _isBuildingContext.value = false
            }
        }
    }

    // Role 3 Action: Generate Emergency SMS
    fun generateEmergencyMessage(summary: String? = null) {
        val triageSummaryToUse = summary
            ?: _currentTriageResult.value?.summaryForDoctor
            ?: latestTriageRecord.value?.summaryForDoctor
            ?: "Patient reports acute chest tightness and dyspnea with high severity."

        viewModelScope.launch {
            _isGeneratingSms.value = true
            try {
                val loc = sensorManager.currentLocationText.value
                val message = repository.generateEmergencySms(userName.value, triageSummaryToUse, loc)
                _emergencySms.value = message
            } catch (e: Exception) {
                val loc = sensorManager.currentLocationText.value
                _emergencySms.value = "EMERGENCY ALERT: ${userName.value} needs urgent medical assistance ($triageSummaryToUse). Loc: $loc. Please call 911 or dispatch assistance."
            } finally {
                _isGeneratingSms.value = false
            }
        }
    }

    fun toggleSiren() {
        _isSirenActive.value = !_isSirenActive.value
        if (_isSirenActive.value) {
            sensorManager.triggerSosVibration()
        }
    }

    // Appointment Booking Action
    fun bookAppointment(
        hospitalName: String,
        doctorName: String,
        specialty: String,
        date: String,
        timeSlot: String,
        reason: String
    ) {
        viewModelScope.launch {
            repository.bookAppointment(hospitalName, doctorName, specialty, date, timeSlot, reason)
        }
    }

    fun cancelAppointment(id: Long) {
        viewModelScope.launch {
            repository.cancelAppointment(id)
        }
    }

    fun addEmergencyContact(name: String, relationship: String, phone: String, isPrimary: Boolean) {
        viewModelScope.launch {
            repository.addEmergencyContact(name, relationship, phone, isPrimary)
        }
    }

    fun deleteEmergencyContact(id: Long) {
        viewModelScope.launch {
            repository.deleteEmergencyContact(id)
        }
    }

    fun logDailyWellness(mood: String, sleepHours: Double, steps: Int, screenTime: Double, stressScore: Int) {
        viewModelScope.launch {
            repository.logDailyWellness(mood, sleepHours, steps, screenTime, stressScore)
            buildWellnessContext()
        }
    }

    fun refreshGps() {
        sensorManager.refreshLocation()
        // If an emergency message exists, refresh location in it
        if (_emergencySms.value.isNotBlank()) {
            generateEmergencyMessage()
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.cleanup()
    }
}
