package org.example.employeeattendenceapp

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.employeeattendenceapp.Auth.clearUserRole
import org.example.employeeattendenceapp.Auth.signOut
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.auth.FirebaseAuth

@Composable
actual fun HomeScreenAdmin(justLoggedIn: Boolean) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser

    // Extract name from authenticated user's email
    val adminName = remember(currentUser) {
        currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Admin"
    }

    if (justLoggedIn) {
        LaunchedEffect(Unit) {
            delay(300)
            snackbarHostState.showSnackbar("Logged in successfully!")
        }
    }

    val presentCount = 42
    val absentCount = 5

    val recentAttendance = listOf(
        Triple("John Davis", "Checked in at 8:45 AM", "Present"),
        Triple("Sarah Miller", "Checked in at 9:02 AM", "Absent"),
        Triple("Robert Johnson", "Checked in at 8:30 AM", "Present"),
        Triple("Amanda Wilson", "Not checked in", "Absent")
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initials = adminName.take(1).uppercase()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4B89DC)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome, $adminName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Manage employee attendance and records",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = {
                    signOut()
                    clearUserRole(context)
                    if (context is Activity) {
                        val intent = Intent(context, context::class.java)
                        context.finish()
                        context.startActivity(intent)
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFF4B89DC)
                    )
                }
            }

            // Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Attendance Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Today", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B89DC))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$presentCount", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Present", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFECACA))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$absentCount", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Absent", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    }
                }
            }

            // Recent Attendance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Recent Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("View All", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B89DC))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recentAttendance.forEach { (name, time, status) ->
                    val initials = name.split(" ").map { it.first().uppercase() }.joinToString("").take(2)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4B89DC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Text(
                                text = status,
                                color = if (status == "Present") Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Quick Actions
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B89DC))
                ) {
                    Text("Export Report")
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B89DC))
                ) {
                    Text("Mark Attendance")
                }
            }

            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text("Monthly Report", color = Color.Black)
            }
        }
    }
}
