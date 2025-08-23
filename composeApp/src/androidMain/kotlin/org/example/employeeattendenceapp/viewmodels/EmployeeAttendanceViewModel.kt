package org.example.employeeattendenceapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class EmployeeAttendanceViewModel @Inject constructor() : ViewModel() {

    private val _attendanceMarked = MutableStateFlow(false)
    private val _attendanceMarkedTime = MutableStateFlow<LocalTime?>(null)
    private val _statusText = MutableStateFlow("Active")
    private val _withinZoneVisible = MutableStateFlow(true)
    private val _checkInTime = MutableStateFlow<LocalTime?>(null)
    private val _attendanceStatus = MutableStateFlow("Absent")
    private val _workingHours = MutableStateFlow("0h 0m 0s")
    private val _lastAttendanceDay = MutableStateFlow(LocalDate.now())

    val attendanceMarked: StateFlow<Boolean> = _attendanceMarked.asStateFlow()
    val attendanceMarkedTime: StateFlow<LocalTime?> = _attendanceMarkedTime.asStateFlow()
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    val withinZoneVisible: StateFlow<Boolean> = _withinZoneVisible.asStateFlow()
    val checkInTime: StateFlow<LocalTime?> = _checkInTime.asStateFlow()
    val attendanceStatus: StateFlow<String> = _attendanceStatus.asStateFlow()
    val workingHours: StateFlow<String> = _workingHours.asStateFlow()
    val lastAttendanceDay: StateFlow<LocalDate> = _lastAttendanceDay.asStateFlow()

    fun markAttendance() {
        val currentTime = LocalTime.now()
        _checkInTime.value = currentTime
        _attendanceMarked.value = true
        _attendanceMarkedTime.value = currentTime
        _attendanceStatus.value = "Present"
        _lastAttendanceDay.value = LocalDate.now()

        // Initialize working hours immediately
        _workingHours.value = "0h 0m 0s"
    }

    fun resetForNewDay() {
        _checkInTime.value = null
        _attendanceMarked.value = false
        _attendanceMarkedTime.value = null
        _attendanceStatus.value = "Absent"
        _workingHours.value = "0h 0m 0s"
        _statusText.value = "--"
        _withinZoneVisible.value = false
    }

    fun isAttendanceMarkedToday(): Boolean {
        return _attendanceMarked.value &&
                _lastAttendanceDay.value == LocalDate.now()
    }

    fun updateWorkingHours(currentTime: LocalTime, isInOfficeZone: Boolean) {
        _workingHours.value = calculateWorkingHours(currentTime, isInOfficeZone)
    }

    private fun calculateWorkingHours(currentTime: LocalTime, isInOfficeZone: Boolean): String {
        val checkIn = _checkInTime.value
        if (checkIn == null || !_attendanceMarked.value) {
            return "0h 0m 0s"
        }

        // Ensure current time is after check-in time to avoid negative duration
        if (currentTime.isBefore(checkIn)) {
            return "0h 0m 0s"
        }

        val duration = java.time.Duration.between(checkIn, currentTime)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        return "${hours}h ${minutes}m ${seconds}s"
    }

    fun setStatusActive() { _statusText.value = "Active" }
    fun setStatusPresent() { _statusText.value = "Present" }
    fun setStatusAbsent() { _statusText.value = "Absent" }
    fun setStatusDash() { _statusText.value = "--" }
    fun resetZoneVisibility() { _withinZoneVisible.value = false }
    fun setLocationEnabled(enabled: Boolean) { /* Handle location state */ }
    fun setInternetConnected(connected: Boolean) { /* Handle internet state */ }
    fun setWorkingHours(hours: String) {
        _workingHours.value = hours
    }
}