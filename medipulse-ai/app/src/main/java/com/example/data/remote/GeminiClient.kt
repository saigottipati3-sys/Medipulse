package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.entity.WellnessLogEntity
import com.example.data.model.EmergencyMessageResponse
import com.example.data.model.TriageResponse
import com.example.data.model.WellnessSummaryResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "MediPulseGemini"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are "MediPulse AI", an AI health assistant inside a hackathon MVP Android app. You have THREE roles depending on the input type. Always respond in the exact JSON format specified for that role. Never provide a medical diagnosis — you triage and guide, a licensed doctor diagnoses.

=====================================================
ROLE 1: AI DOCTOR - SYMPTOM TRIAGE
=====================================================
INPUT: 
- symptoms: free text describing what the user feels
- duration: how long they've had it
- severity_self_rated: 1-10
- wellness_context (optional): recent sleep/stress/activity data from the app's wellness tracker

TASK:
1. Ask ONE relevant follow-up question if critical info is missing (e.g., "Is the pain in your chest or elsewhere?"). Otherwise proceed to assessment.
2. Classify urgency into exactly one of: "self_care", "see_doctor_soon", "urgent_care", "emergency"
3. Recommend the type of specialist/facility needed (e.g., "General Physician", "Cardiologist", "Emergency Room")
4. Give ONE simple, safe self-care tip if urgency is "self_care" only
5. If urgency is "emergency", set trigger_emergency_flow to true

OUTPUT (JSON):
{
  "follow_up_question": "" (empty if not needed),
  "urgency_level": "self_care | see_doctor_soon | urgent_care | emergency",
  "recommended_specialist": "...",
  "self_care_tip": "...",
  "trigger_emergency_flow": true/false,
  "summary_for_doctor": "1-2 sentence summary a real doctor could quickly read to understand the case, written in clinical-lite language",
  "disclaimer": "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
}

=====================================================
ROLE 2: WELLNESS-TO-TRIAGE CONTEXT BUILDER
=====================================================
INPUT: 7-day wellness log (mood, sleep_hours, steps, screen_time_hours)

TASK: Summarize into ONE short clinical-lite sentence that can be passed as "wellness_context" into ROLE 1. Focus only on patterns relevant to physical/mental health risk (e.g., poor sleep trend, high stress signals).

OUTPUT (JSON):
{
  "wellness_context_summary": "..."
}

=====================================================
ROLE 3: EMERGENCY MESSAGE GENERATOR
=====================================================
INPUT:
- user_name
- last_ai_triage_summary
- current_location (lat/long or address string)

TASK: Generate a short, clear SMS-length message to send to emergency contacts. Must include: what's happening (from triage summary), that this is urgent, and the location. Keep it under 300 characters. Calm, clear, factual tone — no panic language.

OUTPUT (JSON):
{
  "emergency_sms_text": "..."
}

=====================================================
GENERAL RULES FOR ALL ROLES:
- Never diagnose a specific disease/condition by name with certainty
- Never recommend specific medications or dosages
- Always err toward caution — if unsure between two urgency levels, pick the higher one
- Keep all language simple (8th-grade reading level), warm, and clear
- If symptoms described suggest self-harm or suicidal ideation, IMMEDIATELY set urgency_level to "emergency", trigger_emergency_flow to true, and in self_care_tip field instead output: "Please reach out now to a crisis helpline or trusted person — you don't have to go through this alone."
=====================================================
"""

    suspend fun triageSymptoms(
        symptoms: String,
        duration: String,
        severity: Int,
        wellnessContext: String? = null
    ): TriageResponse = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("[ROLE: 1]")
            appendLine("symptoms: \"$symptoms\"")
            appendLine("duration: \"$duration\"")
            appendLine("severity_self_rated: $severity")
            if (!wellnessContext.isNullOrBlank()) {
                appendLine("wellness_context: \"$wellnessContext\"")
            }
        }

        try {
            val rawJson = executeGeminiRequest(prompt)
            if (rawJson != null) {
                parseTriageResponse(rawJson)
            } else {
                fallbackTriage(symptoms, duration, severity, wellnessContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini call failed, utilizing clinical fallback engine: ${e.message}")
            fallbackTriage(symptoms, duration, severity, wellnessContext)
        }
    }

    suspend fun summarizeWellness(logs: List<WellnessLogEntity>): String = withContext(Dispatchers.IO) {
        if (logs.isEmpty()) return@withContext "No recent wellness logs available."

        val logDescriptions = logs.take(7).joinToString("; ") {
            "${it.dayName}: mood=${it.mood}, sleep=${it.sleepHours}h, steps=${it.steps}, screen=${it.screenTimeHours}h, HR=${it.heartRateBpm}bpm, stress=${it.stressScore}/10"
        }

        val prompt = buildString {
            appendLine("[ROLE: 2]")
            appendLine("7-day wellness log: $logDescriptions")
        }

        try {
            val rawJson = executeGeminiRequest(prompt)
            if (rawJson != null) {
                val json = JSONObject(extractJsonSubstring(rawJson))
                json.optString("wellness_context_summary")
            } else {
                fallbackWellnessSummary(logs)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini wellness summary fallback: ${e.message}")
            fallbackWellnessSummary(logs)
        }
    }

    suspend fun generateEmergencyMessage(
        userName: String,
        lastTriageSummary: String,
        currentLocation: String
    ): String = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("[ROLE: 3]")
            appendLine("user_name: \"$userName\"")
            appendLine("last_ai_triage_summary: \"$lastTriageSummary\"")
            appendLine("current_location: \"$currentLocation\"")
        }

        try {
            val rawJson = executeGeminiRequest(prompt)
            if (rawJson != null) {
                val json = JSONObject(extractJsonSubstring(rawJson))
                val msg = json.optString("emergency_sms_text")
                if (msg.isNotBlank()) msg else fallbackEmergencySms(userName, lastTriageSummary, currentLocation)
            } else {
                fallbackEmergencySms(userName, lastTriageSummary, currentLocation)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini emergency msg fallback: ${e.message}")
            fallbackEmergencySms(userName, lastTriageSummary, currentLocation)
        }
    }

    private fun executeGeminiRequest(userPrompt: String): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.i(TAG, "No active Gemini API key configured in BuildConfig, using high-fidelity offline engine.")
            return null
        }

        val url = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", SYSTEM_PROMPT))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error code: ${response.code} message: ${response.message}")
                return null
            }
            val bodyString = response.body?.string() ?: return null
            val root = JSONObject(bodyString)
            val candidates = root.optJSONArray("candidates") ?: return null
            val firstCandidate = candidates.optJSONObject(0) ?: return null
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            return parts.optJSONObject(0)?.optString("text")
        }
    }

    private fun extractJsonSubstring(text: String): String {
        val clean = text.trim()
        val firstBrace = clean.indexOf('{')
        val lastBrace = clean.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return clean.substring(firstBrace, lastBrace + 1)
        }
        return clean
    }

    private fun parseTriageResponse(rawJson: String): TriageResponse {
        val cleanJson = extractJsonSubstring(rawJson)
        val json = JSONObject(cleanJson)
        return TriageResponse(
            followUpQuestion = json.optString("follow_up_question", ""),
            urgencyLevel = json.optString("urgency_level", "see_doctor_soon"),
            recommendedSpecialist = json.optString("recommended_specialist", "General Physician"),
            selfCareTip = json.optString("self_care_tip", ""),
            triggerEmergencyFlow = json.optBoolean("trigger_emergency_flow", false),
            summaryForDoctor = json.optString("summary_for_doctor", "Patient reported symptoms under triage review."),
            disclaimer = json.optString("disclaimer", "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor.")
        )
    }

    // High-fidelity Clinical Rules Fallback Engine
    private fun fallbackTriage(
        symptoms: String,
        duration: String,
        severity: Int,
        wellnessContext: String?
    ): TriageResponse {
        val s = symptoms.lowercase()
        val isSelfHarm = s.contains("suicide") || s.contains("kill myself") || s.contains("self-harm") || s.contains("end it all")
        if (isSelfHarm) {
            return TriageResponse(
                followUpQuestion = "",
                urgencyLevel = "emergency",
                recommendedSpecialist = "Crisis Hotline / Emergency Facility",
                selfCareTip = "Please reach out now to a crisis helpline or trusted person — you don't have to go through this alone.",
                triggerEmergencyFlow = true,
                summaryForDoctor = "Patient expresses acute mental distress and thoughts of self-harm. Immediate crisis intervention requested.",
                disclaimer = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
            )
        }

        val isCardiacOrRespiratory = (s.contains("chest") && (s.contains("tight") || s.contains("pain") || s.contains("pressure"))) ||
                s.contains("short of breath") || s.contains("shortness of breath") || s.contains("difficulty breathing") ||
                s.contains("heart racing") && severity >= 7

        val isSevereNeuro = (s.contains("slurred speech") || s.contains("facial droop") || s.contains("numbness") || s.contains("worst headache of life"))

        if (isCardiacOrRespiratory || isSevereNeuro || severity >= 9) {
            val wellnessAddition = if (!wellnessContext.isNullOrBlank()) " Note: $wellnessContext." else ""
            return TriageResponse(
                followUpQuestion = if (!s.contains("arm") && !s.contains("jaw") && s.contains("chest")) "Does the chest sensation radiate to your left arm, neck, or jaw?" else "",
                urgencyLevel = "emergency",
                recommendedSpecialist = "Emergency Room / Cardiologist",
                selfCareTip = "",
                triggerEmergencyFlow = true,
                summaryForDoctor = "Patient reports acute chest tightness and dyspnea lasting $duration with severity $severity/10.$wellnessAddition High risk of acute cardiopulmonary event.",
                disclaimer = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
            )
        }

        if (severity in 6..8 || s.contains("fever") && severity >= 6 || s.contains("sprain") || s.contains("infection") || s.contains("vomiting")) {
            return TriageResponse(
                followUpQuestion = if (s.contains("headache")) "Are you also experiencing sensitivity to light or nausea?" else "",
                urgencyLevel = "urgent_care",
                recommendedSpecialist = if (s.contains("bone") || s.contains("ankle") || s.contains("wrist")) "Orthopedist / Urgent Care" else "Urgent Care Clinic",
                selfCareTip = "",
                triggerEmergencyFlow = false,
                summaryForDoctor = "Patient reports ${symptoms.take(60)} for $duration (severity $severity/10). Needs prompt clinical evaluation.",
                disclaimer = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
            )
        }

        if (severity in 4..5 || s.contains("cough") || s.contains("rash") || s.contains("back pain") || s.contains("stomach")) {
            return TriageResponse(
                followUpQuestion = "",
                urgencyLevel = "see_doctor_soon",
                recommendedSpecialist = if (s.contains("skin") || s.contains("rash")) "Dermatologist" else "General Physician",
                selfCareTip = "",
                triggerEmergencyFlow = false,
                summaryForDoctor = "Patient experiencing ${symptoms.take(60)} ongoing for $duration with moderate discomfort (severity $severity/10).",
                disclaimer = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
            )
        }

        // Mild / Self Care
        return TriageResponse(
            followUpQuestion = "",
            urgencyLevel = "self_care",
            recommendedSpecialist = "General Physician (Routine)",
            selfCareTip = "Rest in a well-ventilated room, stay hydrated with room-temperature fluids, and monitor symptoms over the next 24 hours.",
            triggerEmergencyFlow = false,
            summaryForDoctor = "Patient reports mild symptoms ($symptoms) over $duration (severity $severity/10) consistent with low-acuity discomfort.",
            disclaimer = "This is an AI triage suggestion, not a medical diagnosis. Please consult a licensed doctor."
        )
    }

    private fun fallbackWellnessSummary(logs: List<WellnessLogEntity>): String {
        val avgSleep = logs.map { it.sleepHours }.average()
        val avgSteps = logs.map { it.steps }.average().toInt()
        val highStressDays = logs.count { it.stressScore >= 7 || it.mood.equals("Stressed", ignoreCase = true) }

        return when {
            avgSleep < 5.5 && highStressDays >= 2 ->
                "Sleep averaged ${String.format("%.1f", avgSleep)} hrs over last ${logs.size} days, high reported stress"
            avgSleep < 6.0 ->
                "Sleep deficit noted with average ${String.format("%.1f", avgSleep)} hours per night over the past week."
            highStressDays >= 3 ->
                "Elevated chronic stress signals reported across multiple consecutive days."
            avgSteps < 3000 ->
                "Low physical activity trend with less than $avgSteps steps daily and irregular rest patterns."
            else ->
                "Wellness metrics stable with ${String.format("%.1f", avgSleep)} hrs average sleep and moderate activity."
        }
    }

    private fun fallbackEmergencySms(userName: String, triageSummary: String, location: String): String {
        val cleanSummary = if (triageSummary.isNotBlank()) triageSummary.take(130) else "experiencing sudden severe symptoms"
        val name = if (userName.isNotBlank()) userName else "I"
        val loc = if (location.isNotBlank()) "Loc: $location." else ""
        val text = "EMERGENCY ALERT: $name needs urgent medical assistance ($cleanSummary). $loc Please send help or check in immediately."
        return if (text.length > 295) text.take(292) + "..." else text
    }
}
