package org.example.employeeattendenceapp.Navigation

import com.arkivanov.decompose.ComponentContext

class SignupComponent(
    componentContext: ComponentContext,
    val onNavigateBack: () -> Unit,
    val onNavigateToLogin: () -> Unit
) : ComponentContext by componentContext 