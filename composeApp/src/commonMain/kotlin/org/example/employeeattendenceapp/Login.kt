package org.example.employeeattendenceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import employeeattendanceapp.composeapp.generated.resources.Res
import employeeattendanceapp.composeapp.generated.resources.logo
import org.example.employeeattendenceapp.Navigation.LoginComponent
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.platform.LocalFocusManager
import kotlinx.coroutines.delay
import org.example.employeeattendenceapp.Auth.signInWithEmailPassword
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(component: LoginComponent) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isLoading by remember { mutableStateOf(false) }
    // No need for isSuccess state
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Back arrow in top left corner
        IconButton(
            onClick = { component.onNavigateBack() },
            modifier = Modifier
                .padding(start = 16.dp, top = 25.dp)
                .align(Alignment.TopStart)
                .zIndex(1f) // Ensure it's on top of other content
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(), // Add keyboard avoidance
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Change to spacedBy instead of SpaceBetween
        ) {
            // Logo and Company Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo), // Replace with your logo resource
                    contentDescription = "Company Logo",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "BPG Renewables Pvt. Ltd.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Login",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // Login Form
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Email
                Text(
                    text = "Email",
                    style = MaterialTheme.typography.bodyLarge
                )
                var emailValue by remember { mutableStateOf(TextFieldValue("")) }
                OutlinedTextField(
                    value = emailValue,
                    onValueChange = { emailValue = it },
                    placeholder = { Text("Enter your email") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                // Password
                Text(
                    text = "Password",
                    style = MaterialTheme.typography.bodyLarge
                )
                var passwordValue by remember { mutableStateOf(TextFieldValue("")) }
                OutlinedTextField(
                    value = passwordValue,
                    onValueChange = { passwordValue = it },
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Forgot Password
                TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(Alignment.End)) {
                    Text("Forgot password?", color = Color(0xFF4285F4))
                }

                // Login Button
                Button(
                    onClick = {
                        isLoading = true
                        signInWithEmailPassword(
                            email = emailValue.text,
                            password = passwordValue.text,
                            onSuccess = {
                                coroutineScope.launch {
                                    focusManager.clearFocus()
                                    isLoading = false
                                    component.onNavigateToHome()
                                }
                            },
                            onError = { message ->
                                coroutineScope.launch {
                                    focusManager.clearFocus()
                                    isLoading = false
                                    val lowerMsg = message.lowercase()
                                    if ("no user record" in lowerMsg || "no user" in lowerMsg || "does not exist" in lowerMsg) {
                                        snackbarHostState.showSnackbar("Email doesn't exist")
                                    } else if ("password is invalid" in lowerMsg || "auth credential is incorrect" in lowerMsg) {
                                        snackbarHostState.showSnackbar("Invalid Password")
                                    } else {
                                        snackbarHostState.showSnackbar(message)
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading
                ) {
                    Text("Log In", color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Don't have an account?
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Don't have an account? ")

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "Sign Up",
                        color = Color(0xFF4285F4),
                        modifier = Modifier.clickable { component.onNavigateToSignup() }
                    )
                }
            }

            // Footer
            Spacer(modifier = Modifier.weight(1f)) // Add flexible space
            Text(
                text = "© 2023 BPG Renewables. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
        // SnackbarHost overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        // Loading indicator overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.8f),
                    shadowElevation = 8.dp
                ) {
                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}