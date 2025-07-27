package org.example.employeeattendenceapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.Composable // only if supported by JetBrains Compose Multiplatform
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Duration

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

    // New: Accumulated working duration and last zone entry time
    private var totalWorkingDuration: Duration = Duration.ZERO
    private var lastZoneEntryTime: LocalTime? = null
    private var wasInOfficeZone: Boolean = false

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
        // Reset accumulators
        totalWorkingDuration = Duration.ZERO
        lastZoneEntryTime = null
        wasInOfficeZone = false
    }

    fun updateWorkingHours(currentTime: LocalTime, isInOfficeZone: Boolean) {
        val checkIn = _checkInTimeStamp.value
        if (checkIn != null) {
            val officeStartTime = LocalTime.of(9, 0)
            val officeEndTime = LocalTime.of(18, 0)
            val now = currentTime
            val boundedNow = if (now.isAfter(officeEndTime)) officeEndTime else if (now.isBefore(officeStartTime)) officeStartTime else now

            // Detect transition: out-of-zone -> in-zone
            if (isInOfficeZone && !wasInOfficeZone) {
                // Just entered zone, start timing from now
                lastZoneEntryTime = boundedNow
            }
            // Detect transition: in-zone -> out-of-zone
            if (!isInOfficeZone && wasInOfficeZone) {
                // Just left zone, accumulate time
                if (lastZoneEntryTime != null) {
                    val entry = lastZoneEntryTime!!
                    if (!entry.isBefore(officeStartTime) && !entry.isAfter(officeEndTime)) {
                        val duration = Duration.between(entry, boundedNow)
                        if (!duration.isNegative && !duration.isZero) {
                            totalWorkingDuration = totalWorkingDuration.plus(duration)
                        }
                    }
                }
                lastZoneEntryTime = null
            }
            // Display logic
            val displayDuration = if (isInOfficeZone && lastZoneEntryTime != null) {
                val duration = Duration.between(lastZoneEntryTime, boundedNow)
                if (!duration.isNegative && !duration.isZero) {
                    totalWorkingDuration.plus(duration)
                } else {
                    totalWorkingDuration
                }
            } else {
                totalWorkingDuration
            }
            val hours = displayDuration.toHours()
            val minutes = displayDuration.toMinutesPart()
            val seconds = displayDuration.toSecondsPart()
            _workingHours.value = "${hours}h ${minutes}m ${seconds}s"
            wasInOfficeZone = isInOfficeZone
        }
    }

    fun resetZoneVisibility() {
        _withinZoneVisible.value = true
    }

    fun setStatusActive() {
        // Remove the attendance check - we want to show Active status whenever in zone
        _statusText.value = "Active"
    }

    fun setStatusDash() {
        // Remove the attendance check - we want to show -- status whenever out of zone
        _statusText.value = "--"
    }

    fun setStatusAbsent() {
        // Only set to Absent if attendance hasn't been marked today
        if (_lastAttendanceDate.value != LocalDate.now()) {
            _attendanceStatus.value = "Absent"
        }
    }

    fun setStatusPresent() {
        _attendanceStatus.value = "Present"
        // Don't change statusText here - keep it as Active or --
    }

    fun isAttendanceMarkedToday(): Boolean {
        return _lastAttendanceDate.value == LocalDate.now()
    }

    fun resetForNewDay() {
        _checkInTime.value = null
        _checkInTimeStamp.value = null
        _attendanceStatus.value = "Absent"
        _attendanceMarkedTime.value = null
        _workingHours.value = "0h 0m 0s"
        _statusText.value = "--"
        _markAttendanceEnabled.value = true
        _withinZoneVisible.value = true
        _lastAttendanceDate.value = null
        totalWorkingDuration = Duration.ZERO
        lastZoneEntryTime = null
        wasInOfficeZone = false
    }
}

@Composable
expect fun HomeScreenEmployee(justLoggedIn: Boolean = false)

