package org.example.employeeattendenceapp

import androidx.compose.runtime.*
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import org.example.employeeattendenceapp.DashboardSection
import org.example.employeeattendenceapp.Navigation.DashboardComponent
import org.example.employeeattendenceapp.Navigation.RootComponent
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val root = remember {
        RootComponent(
            componentContext = DefaultComponentContext(
                lifecycle = LifecycleRegistry()
            )
        )
    }

    Children(root.stack) { child ->
        when (val instance = child.instance) {
            is RootComponent.Child.Dashboard ->
                DashboardSection(component = instance.component)
            is RootComponent.Child.Login ->
                LoginScreen(component = instance.component)
            // Add other screens here as you expand
        }
    }
}