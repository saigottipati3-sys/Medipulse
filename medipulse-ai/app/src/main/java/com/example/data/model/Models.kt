package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UrgencyLevel(val rawValue: String, val displayName: String) {
    SELF_CARE("self_care", "Self Care"),
    SEE_DOCTOR_SOON("see_doctor_soon", "See Doctor Soon"),
    URGENT_CARE("urgent_care", "Urgent Care"),
    EMERGENCY("emergency", "Emergency");

    companion object {
        fun fromString(value: String): UrgencyLevel {
            return when (value.trim().lowercase()) {
                "self_care", "self care", "selfcare" -> SELF_CARE
                "see_doctor_soon", "see doctor soon", "seedoctorsoon" -> SEE_DOCTOR_SOON
                "urgent_care", "urgent care", "urgentcare" -> URGENT_CARE
                "emergency" -> EMERGENCY
                else -> {
                    if (value.contains("emergency", ignoreCase = true)) EMERGENCY
                    else if (value.contains("urgent", ignoreCase = true)) URGENT_CARE
                    else if (value.contains("soon", ignoreCase = true)) SEE_DOCTOR_SOON
                    else SELF_CARE
                }
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class TriageResponse(
    @Json(name = "follow_up_question") val followUpQuestion: String = "",
    @Json(name = "urgency_level") val urgencyLevel: String = "see_doctor_soon",
    @Json(name = "recommended_specialist") val recommendedSpecialist: String = "General Physician",
    @Json(name = "self_care_tip") val selfCareTip: String = "",
    @Json(name = "trigger_emergency_flow") val triggerEmergencyFlow: Boolean = false,
    @Json(name = "summary_for_doctor") val summaryForDoctor: String = "",
    @Json(name = "disclaimer") val disclaimer: String = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
)

@JsonClass(generateAdapter = true)
data class WellnessSummaryResponse(
    @Json(name = "wellness_context_summary") val wellnessContextSummary: String = ""
)

@JsonClass(generateAdapter = true)
data class EmergencyMessageResponse(
    @Json(name = "emergency_sms_text") val emergencySmsText: String = ""
)

data class Hospital(
    val id: String,
    val name: String,
    val type: String, // "General Hospital", "Urgent Care Center", "Cardiology Specialty", "Emergency Trauma Center"
    val specialistTypes: List<String>,
    val address: String,
    val distanceMiles: Double,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val isOpen24Hours: Boolean,
    val erWaitTimeMinutes: Int?, // e.g. 15 mins for ER
    val phoneNumber: String,
    val availableSlots: List<String>
)
