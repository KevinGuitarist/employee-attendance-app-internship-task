package org.example.employeeattendenceapp

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import org.example.employeeattendenceapp.Auth.signOut
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.content.Intent
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import org.example.employeeattendenceapp.Auth.clearUserRole

@Composable
actual fun HomeScreenAdmin(justLoggedIn: Boolean) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (justLoggedIn) {
        LaunchedEffect(Unit) {
            delay(300)
            snackbarHostState.showSnackbar("Logged in successfully!")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Logout icon at top right
        IconButton(
            onClick = {
                signOut()
                clearUserRole(context)
                // Restart activity to reset navigation
                if (context is Activity) {
                    val intent = Intent(context, context::class.java)
                    context.finish()
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Log Out"
            )
        }
        // Main content
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to Home Screen Admin!",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
        }
        // Place snackbar host at bottom
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

