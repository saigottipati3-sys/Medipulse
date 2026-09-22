package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.UrgencyBadge
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SeeDoctorAmber
import com.example.ui.theme.SelfCareGreen
import com.example.ui.theme.UrgentCareOrange
import com.example.viewmodel.MediPulseViewModel
import com.example.viewmodel.NavTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDoctorScreen(
    viewModel: MediPulseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val symptoms by viewModel.symptomsText.collectAsState()
    val duration by viewModel.durationText.collectAsState()
    val severity by viewModel.severitySlider.collectAsState()
    val wellnessContext by viewModel.wellnessContextText.collectAsState()
    val isTriaging by viewModel.isTriaging.collectAsState()
    val triageResult by viewModel.currentTriageResult.collectAsState()
    val wellnessSummary by viewModel.wellnessSummary.collectAsState()

    var followUpAnswer by remember { mutableStateOf("") }

    val severityColor = when (severity.toInt()) {
        in 1..3 -> SelfCareGreen
        in 4..5 -> SeeDoctorAmber
        in 6..7 -> UrgentCareOrange
        else -> EmergencyRed
    }

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
                                .background(PrimaryBlue, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROLE 1 : CLINICAL TRIAGE ENGINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "AI Doctor Symptom Triage",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Preset Test Benchmarks
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Quick Test Presets (1-Tap Benchmark)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Benchmark Test 1: Exactly the prompt case!
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmergencyRedContainer)
                                .clickable { viewModel.loadPreset(1) }
                                .padding(8.dp)
                                .testTag("preset_chest_emergency"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chest Tightness (Prompt Case: Sev 8)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmergencyRed
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.loadPreset(2) }
                                .padding(8.dp)
                                .testTag("preset_migraine"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Migraine & Aura (Sev 6)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.loadPreset(3) }
                                .padding(8.dp)
                                .testTag("preset_allergies"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Seasonal Allergies (Sev 3)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { viewModel.loadPreset(4) }
                                .padding(8.dp)
                                .testTag("preset_ankle"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ankle Sprain (Sev 7)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Input Form Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Symptoms Input
                    OutlinedTextField(
                        value = symptoms,
                        onValueChange = { viewModel.symptomsText.value = it },
                        label = { Text("What symptoms are you experiencing?") },
                        placeholder = { Text("e.g. chest feels tight and I'm short of breath") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("symptoms_input"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Duration Input
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { viewModel.durationText.value = it },
                        label = { Text("Duration / When did it start?") },
                        placeholder = { Text("e.g. started 20 minutes ago, or 2 days") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("duration_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Severity Slider (1-10)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Self-Rated Severity (1 - 10)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(severityColor)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${severity.toInt()} / 10",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Slider(
                        value = severity,
                        onValueChange = { viewModel.severitySlider.value = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = severityColor,
                            activeTrackColor = severityColor
                        ),
                        modifier = Modifier.testTag("severity_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wellness Context (Role 2 integration)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wellness Context (from Role 2)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Attach Tracker Data",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                viewModel.wellnessContextText.value =
                                    if (wellnessSummary.isNotBlank()) wellnessSummary
                                    else "Sleep averaged 4.2 hrs over last 3 days, high reported stress"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = wellnessContext,
                        onValueChange = { viewModel.wellnessContextText.value = it },
                        placeholder = { Text("Recent sleep, stress, activity context...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wellness_context_input"),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Triage Button
                    Button(
                        onClick = {
                            viewModel.executeTriage()
                        },
                        enabled = !isTriaging && symptoms.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_triage_button")
                    ) {
                        if (isTriaging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Triaging with Gemini AI...")
                        } else {
                            Icon(Icons.Default.MedicalServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assess & Triage Symptoms", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Triage Result Section
        item {
            triageResult?.let { result ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Urgent Emergency Alert Banner if trigger_emergency_flow is true
                    if (result.triggerEmergencyFlow) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = EmergencyRedContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("emergency_trigger_alert_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Emergency Triggered",
                                        tint = EmergencyRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "EMERGENCY FLOW TRIGGERED",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmergencyRed,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = "Your symptoms indicate potential acute emergency risk. Do not drive yourself. Contact emergency services or use the SOS button below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7F1D1D),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        viewModel.selectTab(NavTab.EMERGENCY)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("go_to_sos_button")
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Emergency SOS & Dispatch Alert", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Main Assessment Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("triage_assessment_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Urgency Badge & Specialist Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TRIAGE CLASSIFICATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = result.recommendedSpecialist,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                UrgencyBadge(urgencyLevelString = result.urgencyLevel)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Follow-up question (if present)
                            if (result.followUpQuestion.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.HelpOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "AI DOCTOR FOLLOW-UP QUESTION",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.followUpQuestion,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = followUpAnswer,
                                                onValueChange = { followUpAnswer = it },
                                                placeholder = { Text("Your answer (e.g. Yes, radiates to left shoulder)") },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                                singleLine = true
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.executeTriage(
                                                        symptoms = "${symptoms}. Clarification: ${result.followUpQuestion} Answer: $followUpAnswer",
                                                        duration = duration,
                                                        severity = severity.toInt(),
                                                        context = wellnessContext
                                                    )
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Update")
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Self-care tip (if self_care or present)
                            if (result.selfCareTip.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .padding(12.dp)
                                ) {
                                    Row {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SelfCareGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Safe Self-Care Guidance",
                                                fontWeight = FontWeight.Bold,
                                                color = SelfCareGreen,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = result.selfCareTip,
                                                fontSize = 13.sp,
                                                color = Color(0xFF14532D)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Summary for Doctor
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CLINICAL SUMMARY FOR REAL DOCTOR",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.8.sp
                                        )
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("Doctor Summary", result.summaryForDoctor))
                                                Toast.makeText(context, "Doctor summary copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copy summary",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = result.summaryForDoctor,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Locate on Map & Book Appointment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.selectTab(NavTab.HOSPITALS)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Find Clinics", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        viewModel.selectTab(NavTab.HOSPITALS)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Book Doctor", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Disclaimer
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = result.disclaimer,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
