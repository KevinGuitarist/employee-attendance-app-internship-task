package org.example.employeeattendenceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.Intent
import androidx.compose.ui.draw.clip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import org.example.employeeattendenceapp.Auth.signOut
import androidx.compose.runtime.collectAsState
import org.example.employeeattendenceapp.Auth.clearUserRole

@Composable
actual fun HomeScreenEmployee(justLoggedIn: Boolean) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Use business logic state from commonMain
    val attendanceState = remember { EmployeeAttendanceState() }
    val statusText by attendanceState.statusText.collectAsState(initial = "Active")
    val markAttendanceEnabled by attendanceState.markAttendanceEnabled.collectAsState(initial = true)
    val withinZoneVisible by attendanceState.withinZoneVisible.collectAsState(initial = true)

    // Show snackbar if just logged in
    if (justLoggedIn) {
        LaunchedEffect(justLoggedIn) {
            delay(300)
            snackbarHostState.showSnackbar("Logged in successfully!")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logout icon at top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    signOut()
                    clearUserRole(context)
                    // Restart activity to reset navigation
                    if (context is Activity) {
                        val intent = Intent(context, context::class.java)
                        context.finish()
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out"
                )
            }
        }

        // Welcome message
        Text(
            text = "Welcome, John",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        // Date
        Text(
            text = "Tuesday, 12 March",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Current Location Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "Latitude: 37.7749° N")
                Text(text = "Longitude: 122.4194° W")
                // Map image (ensure resource exists in androidMain/res/drawable)
                Image(
                    painter = painterResource(id = R.drawable.map),
                    contentDescription = "Location Map",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Mark Attendance Button
        Button(
            onClick = {
                try {
                    attendanceState.markAttendance()
                    // Hide the "within zone" text briefly with animation
                    coroutineScope.launch {
                        delay(3000)
                        attendanceState.resetZoneVisibility()
                    }
                } catch (e: Exception) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Attendance failed: ${e.localizedMessage}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B89DC)),
            enabled = markAttendanceEnabled
        ) {
            Text(text = "Mark Attendance", color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Within Allowed Zone Indicator with animation
        AnimatedVisibility(
            visible = withinZoneVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F6D6))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Check icon (ensure resource exists in androidMain/res/drawable)
                    Icon(
                        painter = painterResource(id = R.drawable.check_circle),
                        contentDescription = "Within Zone",
                        tint = Color(0xFF38761D)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "You are within allowed location zone", color = Color(0xFF38761D))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Today's Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Today's Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Check-in time")
                    Text(text = "08:45 AM")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Working hours")
                    Text(text = "5h 15m")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Status")
                    Text(text = statusText, color = Color(0xFF4B89DC))
                }
            }
        }
        // Snackbar host at the bottom of the screen
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

