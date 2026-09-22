package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EcgCyan
import com.example.ui.theme.EcgGreen
import com.example.ui.theme.EmergencyRed
import kotlin.math.sin

@Composable
fun EcgPulseWaveform(
    heartRateBpm: Int,
    respiratoryRateRpm: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "ecgSweep")
    val sweepProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF070D18))
            .padding(14.dp)
    ) {
        // Vitals HUD Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x33EF4444), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Heart Rate",
                        tint = EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "HEART RATE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$heartRateBpm",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BPM",
                            fontSize = 11.sp,
                            color = EcgCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Respiratory Rate HUD
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x3306B6D4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = "Respiratory Rate",
                        tint = EcgCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RESPIRATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$respiratoryRateRpm",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BR/MIN",
                            fontSize = 11.sp,
                            color = EcgGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Dual-Trace Biometric Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
        ) {
            val width = size.width
            val height = size.height
            val midY = height * 0.45f
            val respMidY = height * 0.78f

            // Background subtle grid lines
            val gridSpacing = 20.dp.toPx()
            var x = 0f
            while (x < width) {
                drawLine(
                    color = Color(0x1A00F0FF),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 0.8f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < height) {
                drawLine(
                    color = Color(0x1A00F0FF),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 0.8f
                )
                y += gridSpacing
            }

            // ECG Heart Trace Path
            val ecgPath = Path()
            val points = 120
            val dx = width / points

            for (i in 0..points) {
                val px = i * dx
                // Create classic P-Q-R-S-T wave repetition
                val cyclePos = (px / 120f) % 1f
                val ecgWave = when {
                    cyclePos in 0.15f..0.22f -> sin((cyclePos - 0.15f) / 0.07f * Math.PI.toFloat()) * 5f // P wave
                    cyclePos in 0.28f..0.31f -> -sin((cyclePos - 0.28f) / 0.03f * Math.PI.toFloat()) * 6f // Q dip
                    cyclePos in 0.31f..0.36f -> sin((cyclePos - 0.31f) / 0.05f * Math.PI.toFloat()) * 26f // R peak
                    cyclePos in 0.36f..0.40f -> -sin((cyclePos - 0.36f) / 0.04f * Math.PI.toFloat()) * 9f // S dip
                    cyclePos in 0.48f..0.62f -> sin((cyclePos - 0.48f) / 0.14f * Math.PI.toFloat()) * 8f // T wave
                    else -> 0f
                }
                val py = midY - ecgWave

                if (i == 0) {
                    ecgPath.moveTo(px, py)
                } else {
                    ecgPath.lineTo(px, py)
                }
            }

            drawPath(
                path = ecgPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x3300F0FF), EcgCyan, Color(0x3300F0FF))
                ),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Respiratory Pattern Path (Gentle Sinusoidal Wave)
            val respPath = Path()
            for (i in 0..points) {
                val px = i * dx
                val respWave = sin((px / width * 3.5f * Math.PI.toFloat()) + (sweepProgress * 6.28f)) * 10f
                val py = respMidY + respWave
                if (i == 0) respPath.moveTo(px, py) else respPath.lineTo(px, py)
            }

            drawPath(
                path = respPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x2210B981), EcgGreen, Color(0x2210B981))
                ),
                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
            )

            // Glowing sweep scan line
            val sweepX = sweepProgress * width
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, EcgCyan, Color.Transparent)
                ),
                start = Offset(sweepX, 0f),
                end = Offset(sweepX, height),
                strokeWidth = 3f
            )

            // Sweep head point
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(sweepX, midY)
            )
        }
    }
}
