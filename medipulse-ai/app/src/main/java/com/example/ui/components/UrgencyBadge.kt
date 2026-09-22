package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UrgencyLevel
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.EmergencyRedContainer
import com.example.ui.theme.SeeDoctorAmber
import com.example.ui.theme.SeeDoctorContainer
import com.example.ui.theme.SelfCareContainer
import com.example.ui.theme.SelfCareGreen
import com.example.ui.theme.UrgentCareContainer
import com.example.ui.theme.UrgentCareOrange

@Composable
fun UrgencyBadge(
    urgencyLevelString: String,
    modifier: Modifier = Modifier
) {
    val level = UrgencyLevel.fromString(urgencyLevelString)

    val (bgColor, textColor, borderColor, icon) = when (level) {
        UrgencyLevel.EMERGENCY -> Quad(
            EmergencyRed,
            Color.White,
            Color(0xFFFFB4AB),
            Icons.Default.Warning
        )
        UrgencyLevel.URGENT_CARE -> Quad(
            UrgentCareOrange,
            Color.White,
            Color(0xFFFFCCAA),
            Icons.Default.LocalHospital
        )
        UrgencyLevel.SEE_DOCTOR_SOON -> Quad(
            SeeDoctorAmber,
            Color.White,
            Color(0xFFFFE082),
            Icons.Default.CalendarMonth
        )
        UrgencyLevel.SELF_CARE -> Quad(
            SelfCareGreen,
            Color.White,
            Color(0xFFA7F3D0),
            Icons.Default.CheckCircle
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (level == UrgencyLevel.EMERGENCY) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .scale(pulseScale)
            .background(bgColor, RoundedCornerShape(24.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("urgency_badge_${level.rawValue}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = level.displayName,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = level.displayName.uppercase(),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
