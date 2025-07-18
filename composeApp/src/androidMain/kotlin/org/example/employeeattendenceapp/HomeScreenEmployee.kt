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
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFF4B89DC), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "J", // Initial or avatar
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Welcome, John",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tuesday, 12 March",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                    // Logout
                    IconButton(
                        onClick = {
                            signOut()
                            clearUserRole(context)
                            if (context is Activity) {
                                val intent = Intent(context, context::class.java)
                                context.finish()
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFFF6F8FB), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Log Out",
                            tint = Color(0xFF4B89DC)
                        )
                    }
                }
            }

            // Location Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Current Location",
                        style = MaterialTheme.typography.titleLarge, // larger font
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Latitude: 37.7749° N",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium, // larger font
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Longitude: 122.4194° W",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium, // larger font
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Image(
                        painter = painterResource(id = R.drawable.map),
                        contentDescription = "Location Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Mark Attendance Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            try {
                                attendanceState.markAttendance()
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
                        enabled = markAttendanceEnabled,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Mark Attendance", color = Color.White)
                    }
                    AnimatedVisibility(
                        visible = withinZoneVisible,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .height(64.dp), // Increased height for the box
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F6D6))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp), // Increased padding for more space
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.check_circle),
                                    contentDescription = "Within Zone",
                                    tint = Color(0xFF38761D),
                                    modifier = Modifier.size(36.dp) // Increased icon size
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "You are within allowed location zone",
                                    color = Color(0xFF38761D),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            // Today's Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .height(200.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "Today's Stats",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start) // Align to top left
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Check-in time", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                        Text(text = "08:45 AM", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Working hours", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                        Text(text = "5h 15m", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Status", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                        Text(text = statusText, color = Color(0xFF4B89DC), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
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

