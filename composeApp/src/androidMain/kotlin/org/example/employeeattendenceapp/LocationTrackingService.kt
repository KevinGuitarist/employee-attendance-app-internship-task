package org.example.employeeattendenceapp

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
                if (hasRequiredPermissions(context)) {
                    context.startForegroundService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackingService::class.java)
            context.stopService(intent)
        }

        private fun hasRequiredPermissions(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationCallback()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (hasRequiredPermissions()) {
            startForegroundServiceWithLocation()
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startForegroundServiceWithLocation() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        requestLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks employee location for attendance"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Employee Attendance")
            .setContentText("Tracking your location for attendance")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).apply {
            setMinUpdateIntervalMillis(500)
            setMaxUpdateDelayMillis(1500)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopSelf()
        }
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

        val locationStatus = if (isInOfficeZone) "In Office" else "Not in Office"

        FirebaseDatabase.getInstance().getReference("attendance/$formattedDate/$uid").setValue(
            mapOf(
                "name" to userName,
                "date" to formattedDate,
                "day" to formattedDay,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "checkInTime" to "Background Update",
                "workingHours" to "Background Update",
                "attendance" to "Background Update",
                "location" to locationStatus
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
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            // Ignore if updates were never requested
        }
    }
}