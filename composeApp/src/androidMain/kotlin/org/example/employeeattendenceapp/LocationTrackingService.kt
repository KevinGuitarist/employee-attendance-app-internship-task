package org.example.employeeattendenceapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "LocationTrackingChannel"
        const val NOTIFICATION_ID = 123

        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        requestLocationUpdates()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Employee Attendance")
            .setContentText("Tracking your location for attendance")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    updateFirebase(it)
                }
            }
        }
    }

    // In LocationTrackingService.kt
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000 // 1 second interval
        ).apply {
            setMinUpdateIntervalMillis(500) // Minimum 0.5s
            setMaxUpdateDelayMillis(1500)  // Maximum 1.5s delay
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateFirebase(location: Location) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val userName = userEmail?.substringBefore("@") ?: "Employee"
        val currentDate = LocalDate.now()
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        val formattedDate = currentDate.format(dateFormatter)
        val formattedDay = currentDate.format(dayFormatter)
        val currentTime = LocalTime.now().format(timeFormatter)

        // Calculate status based on office time and location
        val officeLat = 29.275748
        val officeLon = 79.545030
        val isInOfficeZone = distanceBetween(
            location.latitude,
            location.longitude,
            officeLat,
            officeLon
        ) <= 100

        val officeStartTime = LocalTime.of(9, 0)
        val officeEndTime = LocalTime.of(18, 0)
        val now = LocalTime.now()
        val isOfficeTime = now.isAfter(officeStartTime.minusNanos(1)) &&
                now.isBefore(officeEndTime.plusNanos(1))

        val status = when {
            !isOfficeTime -> "--"
            isInOfficeZone -> "Active"
            else -> "--"
        }

        FirebaseDatabase.getInstance().getReference("attendance").child(uid).setValue(
            mapOf(
                "name" to userName,
                "date" to formattedDate,
                "day" to formattedDay,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "checkInTime" to "Background Update", // Placeholder
                "workingHours" to "Background Update", // Placeholder
                "attendance" to "Background Update", // Placeholder
                "status" to status
            )
        )
    }

    private fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}