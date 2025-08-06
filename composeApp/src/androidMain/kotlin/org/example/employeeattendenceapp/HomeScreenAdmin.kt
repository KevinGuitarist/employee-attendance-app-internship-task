package org.example.employeeattendenceapp

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
actual fun HomeScreenAdmin(justLoggedIn: Boolean) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val database = Firebase.database.reference
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var presentCount by remember { mutableStateOf(0) }
    var absentCount by remember { mutableStateOf(0) }
    var notMarkedCount by remember { mutableStateOf(0) }
    var totalEmployees by remember { mutableStateOf(0) }
    val todayDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var recentAttendanceList by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }


    // Extract name from authenticated user's email
    val adminName = remember(currentUser) {
        currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Admin"
    }

    // Fetch attendance data from Firebase
    LaunchedEffect(Unit) {
        val usersRef = database.child("users")
        val attendanceRef = database.child("attendance").child(todayDate)

        // Get total employees count (only those with role "employee")
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    var count = 0
                    val tempRecentAttendance = mutableListOf<Triple<String, String, String>>()

                    if (snapshot.exists()) {
                        snapshot.children.forEach { child ->
                            val role = child.child("role").getValue(String::class.java)
                            if (role == "employee") {
                                count++
                            }
                        }
                    }
                    totalEmployees = count
                } catch (e: Exception) {
                    Log.e("EmployeeCount", "Error counting employees", e)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Error counting employees: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("EmployeeCount", "Database error: ${error.message}")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Database error: ${error.message}")
                }
            }
        })

        // Get today's attendance and recent records
        attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val presentEmployees = mutableSetOf<String>()
                    val absentNotMarkedEmployees = mutableSetOf<String>()
                    val tempRecentAttendance = mutableListOf<Triple<String, String, String>>()

                    if (snapshot.exists()) {
                        snapshot.children.forEach { employeeSnapshot ->
                            val userId = employeeSnapshot.key ?: ""
                            val attendance = employeeSnapshot.child("attendance").getValue(String::class.java)
                            val checkInTime = employeeSnapshot.child("checkInTime").getValue(String::class.java) ?: ""
                            val status = employeeSnapshot.child("status").getValue(String::class.java) ?: ""

                            if (attendance == "Present") {
                                presentEmployees.add(userId)
                            } else {
                                absentNotMarkedEmployees.add(userId)
                            }

                            // Get user details for recent attendance
                            database.child("users").child(userId).get().addOnSuccessListener { userSnapshot ->
                                if (userSnapshot.exists()) {
                                    val email = userSnapshot.child("email").getValue(String::class.java) ?: ""
                                    val displayName = email.substringBefore("@")
                                        .replace(".", " ")
                                        .replaceFirstChar { it.uppercase() }

                                    val displayStatus = if (attendance == "Present") "Present" else "Absent/Not Marked"
                                    val displayTime = if (attendance == "Present") checkInTime else "Not checked in"

                                    tempRecentAttendance.add(Triple(
                                        displayName,
                                        displayTime,
                                        displayStatus
                                    ))

                                    recentAttendanceList = tempRecentAttendance.takeLast(4)
                                }
                            }
                        }
                    }

                    presentCount = presentEmployees.size
                    absentCount = absentNotMarkedEmployees.size
                    notMarkedCount = absentNotMarkedEmployees.size // Make equal to absentCount
                } catch (e: Exception) {
                    Log.e("Attendance", "Error processing attendance", e)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Error processing attendance: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Attendance", "Database error: ${error.message}")
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Database error: ${error.message}")
                }
            }
        })
    }

    if (justLoggedIn) {
        LaunchedEffect(Unit) {
            delay(300)
            snackbarHostState.showSnackbar("Logged in successfully!")
        }
    }

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
                Text("Attendance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Today", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4B89DC))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Present Card
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

                // Absent Card
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

                // Not Marked Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("$notMarkedCount", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Not Marked", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                    }
                }
            }

            // Total Employees Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Total Employees",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$totalEmployees",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B89DC)
                    )
                }
            }

            // Statistics Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AttendanceStatisticsBar(
                        presentCount = presentCount,
                        absentCount = absentCount,
                        totalEmployees = totalEmployees,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
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
                if (recentAttendanceList.isEmpty()) {
                    // Show nothing or a placeholder if no attendance records exist
                    Text(
                        "No attendance records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    recentAttendanceList.forEach { (name, time, status) ->
                        val initials = name.take(2).uppercase() // Simple initials from first 2 chars

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
                                    Text(
                                        if (status == "Absent") "Not checked in" else time,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
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
            }

            // Quick Actions
            Text("Quick Actions", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AttendanceStatisticsBar(
    presentCount: Int,
    absentCount: Int,
    totalEmployees: Int,
    modifier: Modifier = Modifier
) {
    // Animation for weights
    val total = maxOf(totalEmployees, 1)
    val presentWeight by animateFloatAsState(
        targetValue = maxOf(presentCount.toFloat() / total, 0.1f),
        animationSpec = tween(durationMillis = 500)
    )
    val absentWeight by animateFloatAsState(
        targetValue = maxOf(absentCount.toFloat() / total, 0.1f),
        animationSpec = tween(durationMillis = 500)
    )
    val remainingWeight by animateFloatAsState(
        targetValue = maxOf(1f - presentWeight - absentWeight, 0.1f),
        animationSpec = tween(durationMillis = 500)
    )

    // Animation for number changes
    val animatedPresentCount by animateIntAsState(
        targetValue = presentCount,
        animationSpec = tween(durationMillis = 500)
    )
    val animatedAbsentCount by animateIntAsState(
        targetValue = absentCount,
        animationSpec = tween(durationMillis = 500)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Animated labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedContent(
                targetState = animatedPresentCount,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                }
            ) { count ->
                Text(
                    text = "$count Present",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }

            AnimatedContent(
                targetState = animatedAbsentCount,
                transitionSpec = {
                    slideInVertically { height -> height } + fadeIn() with
                            slideOutVertically { height -> -height } + fadeOut()
                }
            ) { count ->
                Text(
                    text = "$count Absent",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Animated progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            // Present segment with animation
            Box(
                modifier = Modifier
                    .weight(presentWeight)
                    .fillMaxHeight()
                    .background(Color(0xFF4CAF50))
            )

            // Absent segment with animation
            Box(
                modifier = Modifier
                    .weight(absentWeight)
                    .fillMaxHeight()
                    .background(Color(0xFFF44336))
            )

            // Remaining segment with animation
            Box(
                modifier = Modifier
                    .weight(remainingWeight)
                    .fillMaxHeight()
                    .background(Color(0xFF9E9E9E))
            )
        }
    }
}