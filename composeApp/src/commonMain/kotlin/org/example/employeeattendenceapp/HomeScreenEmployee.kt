package org.example.employeeattendenceapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.Composable // only if supported by JetBrains Compose Multiplatform
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class EmployeeAttendanceState {
    private val _statusText = MutableStateFlow("Active")
    val statusText: StateFlow<String> = _statusText

    private val _markAttendanceEnabled = MutableStateFlow(true)
    val markAttendanceEnabled: StateFlow<Boolean> = _markAttendanceEnabled

    private val _withinZoneVisible = MutableStateFlow(true)
    val withinZoneVisible: StateFlow<Boolean> = _withinZoneVisible

    // New properties for attendance tracking
    private val _checkInTime = MutableStateFlow<String?>(null)
    val checkInTime: StateFlow<String?> = _checkInTime

    private val _attendanceStatus = MutableStateFlow<String>("Absent")
    val attendanceStatus: StateFlow<String> = _attendanceStatus

    private val _lastAttendanceDate = MutableStateFlow<LocalDate?>(null)
    val lastAttendanceDate: StateFlow<LocalDate?> = _lastAttendanceDate

    private val _attendanceMarkedTime = MutableStateFlow<String?>(null)
    val attendanceMarkedTime: StateFlow<String?> = _attendanceMarkedTime

    // Working hours tracking
    private val _checkInTimeStamp = MutableStateFlow<LocalTime?>(null)
    val checkInTimeStamp: StateFlow<LocalTime?> = _checkInTimeStamp

    private val _workingHours = MutableStateFlow("0h 0m 0s")
    val workingHours: StateFlow<String> = _workingHours

    fun markAttendance() {
        val currentTime = LocalTime.now()
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        val formattedTime = currentTime.format(timeFormatter)
        
        _checkInTime.value = formattedTime
        _checkInTimeStamp.value = currentTime
        _attendanceStatus.value = "Present"
        _attendanceMarkedTime.value = formattedTime
        _markAttendanceEnabled.value = false
        _withinZoneVisible.value = false
        _lastAttendanceDate.value = LocalDate.now()
        
        // Keep statusText as Active or -- (don't change to Present)
    }

    fun updateWorkingHours(currentTime: LocalTime, isInOfficeZone: Boolean) {
        val checkIn = _checkInTimeStamp.value
        if (checkIn != null) {
            val officeStartTime = LocalTime.of(9, 0)
            val officeEndTime = LocalTime.of(18, 0)
            
            // Calculate working hours only during office time and when in office zone
            val startTime = if (checkIn.isBefore(officeStartTime)) officeStartTime else checkIn
            val endTime = if (currentTime.isAfter(officeEndTime)) officeEndTime else currentTime
            
            // Only calculate if current time is within or after office hours AND employee is in office zone
            if ((currentTime.isAfter(officeStartTime) || currentTime.equals(officeStartTime)) && isInOfficeZone) {
                val duration = java.time.Duration.between(startTime, endTime)
                val hours = duration.toHours()
                val minutes = duration.toMinutesPart()
                val seconds = duration.toSecondsPart()
                
                _workingHours.value = "${hours}h ${minutes}m ${seconds}s"
            } else {
                // If outside office zone, keep the last calculated time (don't reset to 0)
                // Only reset to 0 if it's before office hours
                if (currentTime.isBefore(officeStartTime)) {
                    _workingHours.value = "0h 0m 0s"
                }
                // If outside office zone during office hours, keep the last calculated time
            }
        }
    }

    fun resetForNewDay() {
        val today = LocalDate.now()
        if (_lastAttendanceDate.value != today) {
            _checkInTime.value = null
            _checkInTimeStamp.value = null
            _attendanceStatus.value = "Absent"
            _attendanceMarkedTime.value = null
            _workingHours.value = "0h 0m 0s"
            _markAttendanceEnabled.value = true
            _withinZoneVisible.value = true
            _lastAttendanceDate.value = null
        }
    }

    fun resetZoneVisibility() {
        _withinZoneVisible.value = true
    }

    fun setStatusActive() {
        // Only set to Active if attendance hasn't been marked today
        if (_lastAttendanceDate.value != LocalDate.now()) {
            _statusText.value = "Active"
        }
    }

    fun setStatusDash() {
        // Only set to -- if attendance hasn't been marked today
        if (_lastAttendanceDate.value != LocalDate.now()) {
            _statusText.value = "--"
        }
    }

    fun setStatusAbsent() {
        // Only set to Absent if attendance hasn't been marked today
        if (_lastAttendanceDate.value != LocalDate.now()) {
            _attendanceStatus.value = "Absent"
            // Don't change statusText here - keep it as Active or --
        }
    }

    fun setStatusPresent() {
        _attendanceStatus.value = "Present"
        // Don't change statusText here - keep it as Active or --
    }

    fun isAttendanceMarkedToday(): Boolean {
        return _lastAttendanceDate.value == LocalDate.now()
    }
}

@Composable
expect fun HomeScreenEmployee(justLoggedIn: Boolean = false)

