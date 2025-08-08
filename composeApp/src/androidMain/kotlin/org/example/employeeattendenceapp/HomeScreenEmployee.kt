package org.example.employeeattendenceapp

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import android.provider.Settings
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.employeeattendenceapp.Auth.clearUserRole
import org.example.employeeattendenceapp.Auth.signOut
import org.example.employeeattendenceapp.ui.employee.TaskEmployeeViewModel
import org.example.employeeattendenceapp.ui.employee.components.EmployeeTaskView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun HomeScreenEmployee(justLoggedIn: Boolean) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Request location permission
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    // State to control permission UI
    var showLocationSettingsDialog by remember { mutableStateOf(false) }

    val taskViewModel: TaskEmployeeViewModel = hiltViewModel()
    val employeeId = FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@")?.lowercase() ?: ""
    LaunchedEffect(Unit) {
        taskViewModel.loadTasksForEmployee(employeeId)
    }

    if (showLocationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showLocationSettingsDialog = false },
            title = { Text("Enable Location Services") },
            text = { Text("Location services are required for attendance tracking. Please enable location services.") },
            confirmButton = {
                Button(onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    showLocationSettingsDialog = false
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Request foreground location first, then background if needed
    val foregroundLocationPermission = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    val foregroundServicePermission = rememberPermissionState(
        Manifest.permission.FOREGROUND_SERVICE_LOCATION
    )

    // State to control permission UI
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Function to check location services and start tracking
    fun checkLocationServicesAndStart(context: Context) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationSettingsDialog = true
        } else {
            LocationTrackingService.startService(context)
        }
    }

    // Check permissions when composable launches
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            showPermissionRationale = true
        } else {
            checkLocationServicesAndStart(context)
        }
    }

    // Handle permission results
    LaunchedEffect(locationPermissionState.status) {
        when {
            locationPermissionState.status.isGranted -> {
                checkLocationServicesAndStart(context)
                showPermissionRationale = false
            }
            locationPermissionState.status is PermissionStatus.Denied -> {
                showPermissionRationale = true
            }
        }
    }

    // Show permission rationale UI if needed
    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onRequestPermission = {
                locationPermissionState.launchPermissionRequest()
            },
            onDismiss = {
                showPermissionRationale = false
            }
        )
        return
    }

    // Check permissions when composable launches
    LaunchedEffect(Unit) {
        if (!foregroundLocationPermission.status.isGranted) {
            showPermissionRationale = true
        }
    }

    // Handle permission results
    LaunchedEffect(foregroundLocationPermission.status) {
        when {
            foregroundLocationPermission.status.isGranted -> {
                // Got foreground location, now check foreground service permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !foregroundServicePermission.status.isGranted) {
                    foregroundServicePermission.launchPermissionRequest()
                } else {
                    // All required permissions granted
                    LocationTrackingService.startService(context)
                    showPermissionRationale = false
                }
            }
            foregroundLocationPermission.status is PermissionStatus.Denied -> {
                showPermissionRationale = true
            }
        }
    }

    // Show permission rationale UI if needed
    if (showPermissionRationale) {
        PermissionRationaleDialog(
            onRequestPermission = {
                // Request foreground location first
                foregroundLocationPermission.launchPermissionRequest()
            },
            onDismiss = {
                // Optional: handle user dismissing without granting
                showPermissionRationale = false
            }
        )
        return
    }

    // Use business logic state from commonMain
    val attendanceState = remember { EmployeeAttendanceState() }
    val statusText by attendanceState.statusText.collectAsState(initial = "Active")
    val withinZoneVisible by attendanceState.withinZoneVisible.collectAsState(initial = true)
    val checkInTime by attendanceState.checkInTime.collectAsState(initial = null)
    val attendanceStatus by attendanceState.attendanceStatus.collectAsState(initial = "Absent")
    val workingHours by attendanceState.workingHours.collectAsState(initial = "0h 0m 0s")

    // Location state
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    // Only fetch location if permission is granted
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateIntervalMillis(500)
        }.build()
    }

    // Show last known location immediately if available
    LaunchedEffect(Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    locationError = null
                }
            }.addOnFailureListener {
                locationError = "Failed to get location"
            }
        } catch (e: SecurityException) {
            locationError = "Location permission not granted"
        }
    }

    // Start real-time location updates
    DisposableEffect(Unit) {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    locationError = null
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            locationError = "Location permission not granted"
        }

        onDispose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }

    // Helper to determine if we have a valid location
    val hasLocation = latitude != null && longitude != null && locationError == null

    // State to track if location services are enabled
    var locationServicesEnabled by remember { mutableStateOf(true) }

    // State to track if internet connectivity is available
    var internetConnected by remember { mutableStateOf(true) }

    // Helper to check if location services are enabled
    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    // Helper to check if internet is connected
    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    // Listen for location services changes
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                locationServicesEnabled = isLocationEnabled(context!!)
            }
        }
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(receiver, filter)

        // Set initial state
        locationServicesEnabled = isLocationEnabled(context)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Listen for internet connectivity changes
    DisposableEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                internetConnected = true
            }

            override fun onLost(network: Network) {
                internetConnected = false
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                internetConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Set initial state
        internetConnected = isInternetConnected(context)

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }

    // Clear location if services are off
    LaunchedEffect(locationServicesEnabled) {
        if (!locationServicesEnabled) {
            latitude = null
            longitude = null
            locationError = "Location services disabled"
        }
    }

    // Clear location if internet is off
    LaunchedEffect(internetConnected) {
        if (!internetConnected) {
            latitude = null
            longitude = null
            locationError = "No internet connection"
        }
    }

    // Show snackbar if just logged in
    if (justLoggedIn) {
        LaunchedEffect(justLoggedIn) {
            delay(300)
            snackbarHostState.showSnackbar("Logged in successfully!")
        }
    }

    // Current time tracking
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000L)
        }
    }

    // Office hours and location
    val officeStartTime = LocalTime.of(9, 0)
    val officeEndTime = LocalTime.of(18, 0)
    val isOfficeTime = now.isAfter(officeStartTime.minusNanos(1)) && now.isBefore(officeEndTime.plusNanos(1))
    val officeLat = 29.275748
    val officeLon = 79.545030

    // Helper to calculate distance between two lat/lon points
    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    // State: is user in office zone?
    val isInOfficeZone = hasLocation && distanceBetween(latitude!!, longitude!!, officeLat, officeLon) <= 100

    // Show loading spinner if user is near the office zone boundary
    val isNearOfficeZone = hasLocation && distanceBetween(latitude!!, longitude!!, officeLat, officeLon) in 10.0..20.0

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // Track the last day when attendance was marked
    var lastAttendanceDay by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(now) {
        val today = LocalDate.now()
        if (today != lastAttendanceDay) {
            attendanceState.resetForNewDay()
            lastAttendanceDay = today
        }
    }

    // Update working hours
    LaunchedEffect(now, isInOfficeZone) {
        attendanceState.updateWorkingHours(now, isInOfficeZone)
    }

    // Real-time status update
    LaunchedEffect(isInOfficeZone, isOfficeTime, internetConnected, locationServicesEnabled) {
        when {
            !internetConnected -> attendanceState.setStatusDash()
            !locationServicesEnabled -> attendanceState.setStatusDash()
            attendanceState.isAttendanceMarkedToday() -> {
                attendanceState.setStatusPresent()
                if (isInOfficeZone) attendanceState.setStatusActive() else attendanceState.setStatusDash()
            }
            !isOfficeTime -> {
                attendanceState.setStatusAbsent()
                attendanceState.setStatusDash()
            }
            isInOfficeZone -> attendanceState.setStatusActive()
            else -> attendanceState.setStatusDash()
        }
    }

    // Real-time database sync for attendance
    val userEmail = FirebaseAuth.getInstance().currentUser?.email
    val userName = userEmail?.substringBefore("@") ?: "Employee"
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val currentDate = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val formattedDate = currentDate.format(dateFormatter)
    val formattedDay = currentDate.format(dayFormatter)

    LaunchedEffect(
        userName, formattedDate, formattedDay, latitude, longitude,
        checkInTime, workingHours, attendanceStatus, statusText,
        isInOfficeZone, locationServicesEnabled, internetConnected
    ) {
        if (uid.isNotEmpty() && internetConnected) {
            org.example.employeeattendenceapp.Auth.updateEmployeeAttendance(
                uid = uid,
                name = userName,
                date = formattedDate,
                day = formattedDay,
                latitude = latitude,
                longitude = longitude,
                checkInTime = checkInTime ?: "Not Marked",
                workingHours = workingHours,
                attendance = attendanceStatus,
                status = statusText
            )
        }
    }

    // UI Components
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
    ) {
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    val startTime = System.currentTimeMillis()
                    attendanceState.resetZoneVisibility()

                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                        .setMaxUpdates(1)
                        .build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val loc = result.lastLocation
                            if (loc != null) {
                                latitude = loc.latitude
                                longitude = loc.longitude
                                locationError = null
                            }
                            val elapsed = System.currentTimeMillis() - startTime
                            val remaining = 1000 - elapsed
                            coroutineScope.launch {
                                if (remaining > 0) delay(remaining)
                                isRefreshing = false
                            }
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }

                    try {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                    } catch (e: SecurityException) {
                        locationError = "Location permission not granted"
                        isRefreshing = false
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Section
                HeaderSection(
                    adminName = userName,
                    onLogout = {
                        LocationTrackingService.stopService(context)
                        signOut()
                        clearUserRole(context)
                        if (context is Activity) {
                            val intent = Intent(context, context::class.java)
                            context.finish()
                            context.startActivity(intent)
                        }
                    }
                )

                // Location Card
                LocationCard(
                    internetConnected = internetConnected,
                    locationServicesEnabled = locationServicesEnabled,
                    locationError = locationError,
                    hasLocation = hasLocation,
                    latitude = latitude,
                    longitude = longitude,
                    isInOfficeZone = isInOfficeZone
                )

                // Mark Attendance Card
                MarkAttendanceCard(
                    internetConnected = internetConnected,
                    isOfficeTime = isOfficeTime,
                    isInOfficeZone = isInOfficeZone,
                    isNearOfficeZone = isNearOfficeZone,
                    isAttendanceMarkedToday = attendanceState.isAttendanceMarkedToday(),
                    attendanceMarkedTime = attendanceState.attendanceMarkedTime.value,
                    onMarkAttendance = {
                        try {
                            attendanceState.markAttendance()
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Marked at ${attendanceState.attendanceMarkedTime.value}")
                                delay(3000)
                                attendanceState.resetZoneVisibility()
                            }
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Attendance failed: ${e.localizedMessage}")
                            }
                        }
                    },
                    onSignOff = {
                        coroutineScope.launch {
                            try {
                                if (uid.isEmpty()) {
                                    snackbarHostState.showSnackbar("Error: User not authenticated")
                                    return@launch
                                }

                                // Save to daily_records first
                                val saveResult = try {
                                    org.example.employeeattendenceapp.Auth.saveDailyRecord(
                                        uid = uid,
                                        name = userName,
                                        date = formattedDate,
                                        day = formattedDay,
                                        checkInTime = checkInTime ?: "Not Marked",
                                        workingHours = workingHours,
                                        attendance = attendanceStatus,
                                        status = statusText,
                                        onSuccess = {
                                            // Don't reset here - we'll do it after both operations complete
                                        },
                                        onError = { error ->
                                            launch {
                                                snackbarHostState.showSnackbar("Daily record error: $error")
                                            }
                                        }
                                    )
                                    true
                                } catch (e: Exception) {
                                    false
                                }

                                if (saveResult) {
                                    // Then update attendance (without callbacks)
                                    org.example.employeeattendenceapp.Auth.updateEmployeeAttendance(
                                        uid = uid,
                                        name = userName,
                                        date = formattedDate,
                                        day = formattedDay,
                                        latitude = latitude,
                                        longitude = longitude,
                                        checkInTime = "Not Marked", // Reset check-in time
                                        workingHours = "0h 0m 0s", // Reset working hours
                                        attendance = "Absent",      // Reset attendance status
                                        status = "--"              // Reset current status
                                    )

                                    // Now reset the local state
                                    attendanceState.resetForNewDay()
                                    snackbarHostState.showSnackbar("Signed off successfully! Data saved.")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Sign-off failed: ${e.localizedMessage}")
                                Log.e("SignOff", "Error during sign-off", e)
                            }
                        } },
                    withinZoneVisible = withinZoneVisible && isInOfficeZone && locationServicesEnabled && internetConnected
                )

                // Today's Stats Card
                TodaysStatsCard(
                    checkInTime = checkInTime,
                    workingHours = workingHours,
                    attendanceStatus = attendanceStatus,
                    statusText = statusText
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Your Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        EmployeeTaskView(
                            viewModel = taskViewModel,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Snackbar host
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@Composable
private fun HeaderSection(
    adminName: String,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 24.dp)
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
                val initial = adminName.firstOrNull()?.uppercaseChar()?.toString() ?: "E"
                val currentDate = LocalDate.now()
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
                val formattedDate = currentDate.format(dateFormatter)

                // Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF4B89DC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Welcome, $adminName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            // Logout
            IconButton(
                onClick = onLogout,
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
}

@Composable
private fun LocationCard(
    internetConnected: Boolean,
    locationServicesEnabled: Boolean,
    locationError: String?,
    hasLocation: Boolean,
    latitude: Double?,
    longitude: Double?,
    isInOfficeZone: Boolean
) {
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            when {
                !internetConnected -> LocationStatusText("No internet connection", Color.Red)
                !locationServicesEnabled -> LocationStatusText("Location services disabled", Color.Red)
                locationError != null -> LocationStatusText(locationError!!, Color.Red)
                hasLocation -> {
                    val color = if (isInOfficeZone) Color.Gray else Color.Red
                    Column {
                        LocationStatusText("Latitude: ${latitude?.let { String.format("%.6f", it) }}", color)
                        Spacer(modifier = Modifier.height(8.dp))
                        LocationStatusText("Longitude: ${longitude?.let { String.format("%.6f", it) }}", color)
                    }
                }
                else -> LocationStatusText("Waiting for location...", Color.Gray)
            }
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
}

@Composable
private fun LocationStatusText(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun MarkAttendanceCard(
    internetConnected: Boolean,
    isOfficeTime: Boolean,
    isInOfficeZone: Boolean,
    isNearOfficeZone: Boolean,
    isAttendanceMarkedToday: Boolean,
    attendanceMarkedTime: String?,
    onMarkAttendance: () -> Unit,
    onSignOff: () -> Unit,
    withinZoneVisible: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        var isSignedOff by remember { mutableStateOf(false) }

        Column {
            // Mark Attendance Button
            Button(
                onClick = {
                    if (!internetConnected) {
                        // Show snackbar handled in parent
                    } else if (!isOfficeTime) {
                        // Show snackbar handled in parent
                    } else if (!isInOfficeZone) {
                        // Show snackbar handled in parent
                    } else {
                        onMarkAttendance()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (isOfficeTime && !isSignedOff) 1f else 0.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInOfficeZone && internetConnected) Color(0xFF4B89DC) else Color(0xFFBDBDBD)
                ),
                enabled = !isSignedOff &&
                        isOfficeTime &&
                        !isAttendanceMarkedToday &&
                        internetConnected,
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isNearOfficeZone) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isAttendanceMarkedToday) "Attendance Marked" else "Mark Attendance",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Signing Off Button
            Button(
                onClick = onSignOff,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd32f2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Signing Off", color = Color.White)
            }

            // Zone Visibility Banner
            AnimatedVisibility(
                visible = withinZoneVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .height(64.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2F6D6))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.check_circle),
                            contentDescription = "Within Zone",
                            tint = Color(0xFF38761D),
                            modifier = Modifier.size(36.dp)
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
}

@Composable
private fun TodaysStatsCard(
    checkInTime: String?,
    workingHours: String,
    attendanceStatus: String,
    statusText: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .height(240.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Today's Stats",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(28.dp))
            StatRow("Check-in time", checkInTime ?: "Not marked", checkInTime != null)
            Spacer(modifier = Modifier.height(6.dp))
            StatRow("Working hours", workingHours, workingHours != "0h 0m 0s")
            Spacer(modifier = Modifier.height(6.dp))
            StatRow("Attendance", attendanceStatus, attendanceStatus == "Present", isAttendance = true)
            Spacer(modifier = Modifier.height(6.dp))
            StatRow("Status", statusText, true)
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    isActive: Boolean,
    isAttendance: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.Gray,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = value,
            fontWeight = if (isAttendance) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium,
            color = when {
                isAttendance && value == "Present" -> Color(0xFF4B89DC)
                isAttendance && value == "Absent" -> Color.Red
                isActive -> Color(0xFF4B89DC)
                else -> Color.Gray
            }
        )
    }
}

@Composable
private fun PermissionRationaleDialog(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Location Permission Required") },
        text = { Text("Location permissions are required for attendance tracking") },
        confirmButton = {
            Button(onClick = {
                onRequestPermission()
            }) {
                Text("Grant Permission")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}