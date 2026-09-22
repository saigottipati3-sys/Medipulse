package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EcgPulseWaveform
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SelfCareGreen
import com.example.ui.theme.UrgentCareOrange
import com.example.viewmodel.MediPulseViewModel
import com.example.viewmodel.NavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    viewModel: MediPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recentLogs by viewModel.recentWellnessLogs.collectAsState()
    val wellnessSummary by viewModel.wellnessSummary.collectAsState()
    val isBuildingContext by viewModel.isBuildingContext.collectAsState()
    val liveHeartRate by viewModel.sensorManager.liveHeartRate.collectAsState()
    val liveRespiratoryRate by viewModel.sensorManager.liveRespiratoryRate.collectAsState()
    val stepCount by viewModel.sensorManager.stepCount.collectAsState()

    var showLogDialog by remember { mutableStateOf(false) }

    // Log input states
    var selectedMood by remember { mutableStateOf("Calm") }
    var inputSleep by remember { mutableStateOf(7.0f) }
    var inputScreenTime by remember { mutableStateOf(4.5f) }
    var inputStress by remember { mutableStateOf(3f) }

    val moodOptions = listOf("Calm", "Good", "Fatigued", "Stressed", "Anxious")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SecondaryTeal, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROLE 2 : WELLNESS-TO-TRIAGE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Wellness & Vitals Tracker",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showLogDialog = !showLogDialog },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live Biometric Waveform
        item {
            EcgPulseWaveform(
                heartRateBpm = liveHeartRate,
                respiratoryRateRpm = liveRespiratoryRate,
                modifier = Modifier.testTag("wellness_waveform")
            )
        }

        // Role 2 Clinical Context Sentence Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("role_2_summary_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ROLE 2 : 7-DAY CLINICAL SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.buildWellnessContext() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(14.dp)
                    ) {
                        if (isBuildingContext) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing 7-day vitals via Gemini...", fontSize = 13.sp)
                            }
                        } else {
                            Text(
                                text = if (wellnessSummary.isNotBlank()) "\"$wellnessSummary\"" else "\"Sleep averaged 4.2 hrs over last 3 days, high reported stress\"",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Button to feed directly into Role 1 AI Doctor
                    Button(
                        onClick = {
                            val contextToSend = if (wellnessSummary.isNotBlank()) wellnessSummary else "Sleep averaged 4.2 hrs over last 3 days, high reported stress"
                            viewModel.wellnessContextText.value = contextToSend
                            viewModel.selectTab(NavTab.AI_DOCTOR)
                            Toast.makeText(context, "Wellness context loaded into AI Doctor Triage!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("use_as_context_button")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use as Context in AI Doctor Triage (Role 1)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Daily Logging Form (Collapsible)
        if (showLogDialog) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Record Today's Health Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mood selector
                        Text("Today's Mental State / Mood", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            moodOptions.forEach { mood ->
                                val isSelected = selectedMood == mood
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedMood = mood }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mood,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sleep slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sleep Duration", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${String.format("%.1f", inputSleep)} hrs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Slider(
                            value = inputSleep,
                            onValueChange = { inputSleep = it },
                            valueRange = 3f..12f,
                            steps = 17
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Screen Time slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Screen Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${String.format("%.1f", inputScreenTime)} hrs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UrgentCareOrange)
                        }
                        Slider(
                            value = inputScreenTime,
                            onValueChange = { inputScreenTime = it },
                            valueRange = 1f..14f,
                            steps = 12
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stress slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stress Level (1 - 10)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${inputStress.toInt()} / 10", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (inputStress >= 7) EmergencyRed else SelfCareGreen)
                        }
                        Slider(
                            value = inputStress,
                            onValueChange = { inputStress = it },
                            valueRange = 1f..10f,
                            steps = 8
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                viewModel.logDailyWellness(
                                    mood = selectedMood,
                                    sleepHours = inputSleep.toDouble(),
                                    steps = stepCount,
                                    screenTime = inputScreenTime.toDouble(),
                                    stressScore = inputStress.toInt()
                                )
                                showLogDialog = false
                                Toast.makeText(context, "Logged! Context re-summarized with Gemini.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save & Re-Summarize Context", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 7-Day History Section
        item {
            Text(
                text = "7-Day Wellness Log History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(recentLogs) { log ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = log.dayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val moodBg = when (log.mood) {
                                "Stressed", "Anxious" -> Color(0xFFFEE2E2)
                                "Fatigued" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFDCFCE7)
                            }
                            val moodColor = when (log.mood) {
                                "Stressed", "Anxious" -> EmergencyRed
                                "Fatigued" -> Color(0xFFB45309)
                                else -> SelfCareGreen
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(moodBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(log.mood, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = moodColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${log.sleepHours}h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${log.steps}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = UrgentCareOrange, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("${log.screenTimeHours}h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Stress Indicator
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Stress: ${log.stressScore}/10",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.stressScore >= 7) EmergencyRed else if (log.stressScore >= 5) UrgentCareOrange else SelfCareGreen
                        )
                        Text(
                            text = "${log.heartRateBpm} BPM",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
