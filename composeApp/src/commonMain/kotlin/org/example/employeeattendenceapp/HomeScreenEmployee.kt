package org.example.employeeattendenceapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.Composable // only if supported by JetBrains Compose Multiplatform


class EmployeeAttendanceState {
    private val _statusText = MutableStateFlow("Active")
    val statusText: StateFlow<String> = _statusText

    private val _markAttendanceEnabled = MutableStateFlow(true)
    val markAttendanceEnabled: StateFlow<Boolean> = _markAttendanceEnabled

    private val _withinZoneVisible = MutableStateFlow(true)
    val withinZoneVisible: StateFlow<Boolean> = _withinZoneVisible

    fun markAttendance() {
        _statusText.value = "Checked Out"
        _markAttendanceEnabled.value = false
        _withinZoneVisible.value = false
    }

    fun resetZoneVisibility() {
        _withinZoneVisible.value = true
    }
}

@Composable
expect fun HomeScreenEmployee(justLoggedIn: Boolean = false)

