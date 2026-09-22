package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.ui.screens.AiDoctorScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HospitalsScreen
import com.example.ui.screens.WellnessScreen
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.MediPulseTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MediPulseViewModel
import com.example.viewmodel.NavTab

class MainActivity : ComponentActivity() {

    private val viewModel: MediPulseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MediPulseTheme {
                MediPulseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MediPulseApp(viewModel: MediPulseViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()

    // Request Location permission gracefully
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshGps()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = Color.Transparent
            ) {
                NavigationBarItem(
                    selected = currentTab == NavTab.HOME,
                    onClick = { viewModel.selectTab(NavTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontWeight = if (currentTab == NavTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_home")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.AI_DOCTOR,
                    onClick = { viewModel.selectTab(NavTab.AI_DOCTOR) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "AI Doctor") },
                    label = { Text("AI Doctor", fontWeight = if (currentTab == NavTab.AI_DOCTOR) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_ai_doctor")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.HOSPITALS,
                    onClick = { viewModel.selectTab(NavTab.HOSPITALS) },
                    icon = { Icon(Icons.Default.LocalHospital, contentDescription = "Hospitals") },
                    label = { Text("Hospitals", fontWeight = if (currentTab == NavTab.HOSPITALS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_hospitals")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.EMERGENCY,
                    onClick = { viewModel.selectTab(NavTab.EMERGENCY) },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Emergency",
                            tint = if (currentTab == NavTab.EMERGENCY) EmergencyRed else Color.Unspecified
                        )
                    },
                    label = {
                        Text(
                            "SOS",
                            color = if (currentTab == NavTab.EMERGENCY) EmergencyRed else Color.Unspecified,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmergencyRed,
                        indicatorColor = EmergencyRed.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_item_emergency")
                )

                NavigationBarItem(
                    selected = currentTab == NavTab.WELLNESS,
                    onClick = { viewModel.selectTab(NavTab.WELLNESS) },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Wellness") },
                    label = { Text("Wellness", fontWeight = if (currentTab == NavTab.WELLNESS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("nav_item_wellness")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "screenTransition") { tab ->
                when (tab) {
                    NavTab.HOME -> HomeScreen(viewModel = viewModel)
                    NavTab.AI_DOCTOR -> AiDoctorScreen(viewModel = viewModel)
                    NavTab.HOSPITALS -> HospitalsScreen(viewModel = viewModel)
                    NavTab.EMERGENCY -> EmergencyScreen(viewModel = viewModel)
                    NavTab.WELLNESS -> WellnessScreen(viewModel = viewModel)
                }
            }
        }
    }
}

// Kept for backward compatibility with existing tests
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
