package com.example.ui.components.map

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HealthcareCategory

@Composable
fun CategoryFilterBar(
    selectedCategory: HealthcareCategory,
    onCategorySelected: (HealthcareCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HealthcareCategory.values().forEach { category ->
            val isSelected = selectedCategory == category
            val icon = getCategoryIcon(category)

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color(category.hexColor)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 2.dp),
                modifier = Modifier.testTag("filter_chip_${category.id}")
            )
        }
    }
}

fun getCategoryIcon(category: HealthcareCategory): ImageVector {
    return when (category) {
        HealthcareCategory.ALL -> Icons.Default.MedicalServices
        HealthcareCategory.HOSPITAL -> Icons.Default.LocalHospital
        HealthcareCategory.EMERGENCY_DEPT -> Icons.Default.Emergency
        HealthcareCategory.CLINIC -> Icons.Default.Healing
        HealthcareCategory.DOCTOR -> Icons.Default.Person
        HealthcareCategory.PHARMACY -> Icons.Default.LocalPharmacy
        HealthcareCategory.DIAGNOSTIC_LAB -> Icons.Default.Science
        HealthcareCategory.AMBULANCE_SERVICE -> Icons.Default.Emergency
        HealthcareCategory.BLOOD_BANK -> Icons.Default.WaterDrop
        HealthcareCategory.MEDICAL_STORE -> Icons.Default.Medication
    }
}
