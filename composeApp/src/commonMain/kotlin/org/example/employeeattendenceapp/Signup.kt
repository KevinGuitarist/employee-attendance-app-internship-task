package org.example.employeeattendenceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import org.example.employeeattendenceapp.Navigation.SignupComponent
import org.jetbrains.compose.resources.painterResource

@Composable
fun SignUp(component: SignupComponent) {
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
                    text = "Sign Up",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            // Create Account Form
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.headlineSmall
                )

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
                    placeholder = { Text("Create a password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                // Password Requirements
                Text(
                    text = "Password must be at least 8 characters",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (passwordValue.text.length < 8 && passwordValue.text.isNotEmpty()) Color.Red else Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp) // Add padding to make it more visible
                )

                // Sign Up Button
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sign Up", color = Color.White)
                }

                // Already have an account?
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? ")

                    Spacer(modifier = Modifier.width(1.dp))

                    TextButton(onClick = { component.onNavigateToLogin() }) {
                        Text("Log in", color = Color(0xFF4285F4))
                    }
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
    }
}
