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
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.gms.location.LocationServices
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import androidx.compose.runtime.DisposableEffect
import android.location.Location
import android.location.LocationManager
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import java.time.LocalTime
import androidx.compose.ui.draw.alpha
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

@OptIn(ExperimentalPermissionsApi::class)
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
    val checkInTime by attendanceState.checkInTime.collectAsState(initial = null)
    val attendanceStatus by attendanceState.attendanceStatus.collectAsState(initial = "Absent")
    val attendanceMarkedTime by attendanceState.attendanceMarkedTime.collectAsState(initial = null)
    val workingHours by attendanceState.workingHours.collectAsState(initial = "0h 0m 0s")

    // Location state
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }

    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Request permission if not granted
    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    // Only fetch location if permission is granted
    if (locationPermissionState.status.isGranted) {
        val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
        val locationRequest = remember {
            LocationRequest.create().apply {
                interval = 1000 // 1 second
                fastestInterval = 500 // 0.5 seconds
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }

        // Show last known location immediately if available
        LaunchedEffect(Unit) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    locationError = null
                }
            }.addOnFailureListener {
                // Don't set error here, wait for real-time update
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
            fusedLocationClient.requestLocationUpdates(locationRequest, callback, null)
            onDispose {
                fusedLocationClient.removeLocationUpdates(callback)
            }
        }
    } else {
        locationError = "Location permission not granted."
    }

    // Helper to determine if we have a valid location
    val hasLocation = latitude != null && longitude != null && locationError == null

    // If permission is revoked or location is unavailable, clear values
    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (!locationPermissionState.status.isGranted) {
            latitude = null
            longitude = null
            locationError = "Location permission not granted."
        }
    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.registerReceiver(receiver, filter)
        } else {
            context.registerReceiver(receiver, filter)
        }
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
            locationError = null
        }
    }

    // Clear location if internet is off (since location services might depend on network)
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

    // Add this inside your composable:
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1000L) // update every second
        }
    }
    val officeStartTime = LocalTime.of(9, 0)
    val officeEndTime = LocalTime.of(18, 0)
    val isOfficeTime = now.isAfter(officeStartTime.minusNanos(1)) && now.isBefore(officeEndTime.plusNanos(1))

    // Office location (from user):
    val officeLat = 29.275748
    val officeLon = 79.545030

    // Helper to calculate distance between two lat/lon points (in meters)
    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    // State: is user in office zone?
    val isInOfficeZone = latitude != null && longitude != null &&
        distanceBetween(latitude!!, longitude!!, officeLat, officeLon) <= 20

    // Show loading spinner if user is near the office zone boundary (within 20m but not in 10m zone)
    val isNearOfficeZone = latitude != null && longitude != null &&
        distanceBetween(latitude!!, longitude!!, officeLat, officeLon) in 10.0..20.0

    // State for attendance button warning
    var showZoneWarning by remember { mutableStateOf(false) }

    var isRefreshing by remember { mutableStateOf(false) }
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Track the last day when attendance was marked
    var lastAttendanceDay by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(now) {
        val today = LocalDate.now()
        if (today != lastAttendanceDay) {
            // Reset attendance state for new day
            attendanceState.resetForNewDay()
            lastAttendanceDay = today
        }
    }

    // Remove markAttendance logic for status
    // Real-time status update based on location
    val isWithin20m = latitude != null && longitude != null &&
        distanceBetween(latitude!!, longitude!!, officeLat, officeLon) <= 20

    // Real-time status update based on location, office hours, and internet connectivity
    LaunchedEffect(isWithin20m, isOfficeTime, now, internetConnected) {
        // Update working hours every time location or time changes
        attendanceState.updateWorkingHours(now, isInOfficeZone)
        
        if (!internetConnected) {
            // No internet connection - set status to --
            attendanceState.setStatusDash()
        } else if (attendanceState.isAttendanceMarkedToday()) {
            // If attendance is already marked today, set attendance status to Present but keep status as Active or --
            attendanceState.setStatusPresent()
            // Status text remains Active or -- based on location
            if (isWithin20m) {
                attendanceState.setStatusActive()
            } else {
                attendanceState.setStatusDash()
            }
        } else if (!isOfficeTime) {
            // Outside office hours, set attendance to Absent and status to --
            attendanceState.setStatusAbsent()
            attendanceState.setStatusDash()
        } else if (isWithin20m) {
            // Within office zone during office hours
            attendanceState.setStatusActive()
        } else {
            // Outside office zone during office hours
            attendanceState.setStatusDash()
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
        userName,
        formattedDate,
        formattedDay,
        latitude,
        longitude,
        checkInTime,
        workingHours,
        attendanceStatus,
        statusText
    ) {
        if (uid.isNotEmpty()) {
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

                    // Request a single high-accuracy location update
                    val locationRequest = LocationRequest.create().apply {
                        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        numUpdates = 1
                        interval = 0
                    }

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
                            // Remove updates after receiving the location
                            fusedLocationClient.removeLocationUpdates(this)
                        }
                    }

                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
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
                            // Place here so both avatar and text can use it
                            val userEmail = FirebaseAuth.getInstance().currentUser?.email
                            val userName = userEmail?.substringBefore("@") ?: "Employee"
                            val initial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "E"
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
                                    text = initial, // Initial from name
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = MaterialTheme.typography.headlineMedium.fontSize
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Welcome, $userName",
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
                        when {
                            !internetConnected -> {
                                Text(
                                    text = "No internet connection",
                                    color = Color.Red,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            !locationServicesEnabled -> {
                                Text(
                                    text = "Location services disabled",
                                    color = Color.Red,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            locationError != null -> {
                                Text(
                                    text = locationError!!,
                                    color = Color.Red,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            hasLocation -> {
                                val color = if (isInOfficeZone) Color.Gray else Color.Red
                                Text(
                                    text = "Latitude: ${latitude?.let { String.format("%.6f", it) }}",
                                    color = color,
                                    style = MaterialTheme.typography.titleMedium, // larger font
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Longitude: ${longitude?.let { String.format("%.6f", it) }}",
                                    color = color,
                                    style = MaterialTheme.typography.titleMedium, // larger font
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "Waiting for location...",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
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
                                if (!internetConnected) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("No internet connection. Please check your network.")
                                    }
                                } else if (!isOfficeTime) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Not an office time")
                                    }
                                } else if (!isInOfficeZone) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("You can't mark attendance. You are not in office.")
                                    }
                                } else {
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
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isOfficeTime) 1f else 0.5f), // Faded when not office time
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isInOfficeZone && internetConnected) Color(0xFF4B89DC) else Color(0xFFBDBDBD)
                            ),
                            enabled = markAttendanceEnabled && isOfficeTime && !attendanceState.isAttendanceMarkedToday() && internetConnected, // Only enabled during office hours, if not already marked, and internet is connected
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
                                text = if (attendanceState.isAttendanceMarkedToday()) "Attendance Marked" else "Mark Attendance", 
                                color = Color.White
                            )
                        }
                        if (showZoneWarning) {
                            LaunchedEffect(showZoneWarning) {
                                snackbarHostState.showSnackbar("You can't mark attendance. You are not in office.")
                                showZoneWarning = false
                            }
                        }
                        AnimatedVisibility(
                            visible = withinZoneVisible && isInOfficeZone && locationServicesEnabled && internetConnected,
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
                        .height(240.dp),
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
                            Text(
                                text = checkInTime ?: "Not marked", 
                                fontWeight = FontWeight.Medium, 
                                style = MaterialTheme.typography.titleMedium,
                                color = if (checkInTime != null) Color(0xFF4B89DC) else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Working hours", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = workingHours, 
                                fontWeight = FontWeight.Medium, 
                                style = MaterialTheme.typography.titleMedium,
                                color = if (workingHours != "0h 0m 0s") Color(0xFF4B89DC) else Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Attendance", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = attendanceStatus, 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.titleMedium,
                                color = if (attendanceStatus == "Present") Color(0xFF4B89DC) else Color.Red
                            )
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

