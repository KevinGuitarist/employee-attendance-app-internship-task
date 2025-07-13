package org.example.employeeattendenceapp.Navigation

import com.arkivanov.decompose.ComponentContext

// shared/src/commonMain/kotlin/components/LoginComponent.kt
class LoginComponent(
    componentContext: ComponentContext,
    val role: String
) : ComponentContext by componentContext