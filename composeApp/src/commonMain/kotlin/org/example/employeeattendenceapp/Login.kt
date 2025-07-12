package org.example.employeeattendenceapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import employeeattendanceapp.composeapp.generated.resources.Res
import employeeattendanceapp.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun LoginScreen( ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        // Logo and Company Name
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.logo), // Replace with your logo resource
                contentDescription = "Company Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "BPG Renewables Pvt. Ltd.",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Login Form
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Login",
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
                shape = RoundedCornerShape(8.dp)
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
                visualTransformation = PasswordVisualTransformation()
            )

            // Forgot Password
            TextButton(onClick = { /*TODO*/ }, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?", color = Color(0xFF4285F4))
            }

            // Login Button
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Log In", color = Color.White)
            }

            // Don't have an account?
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? ")
                TextButton(onClick = { /*TODO*/ }) {
                    Text("Sign Up", color = Color(0xFF4285F4))
                }
            }
        }

        // Footer
        Text(
            text = "© 2023 BPG Renewables. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
