package org.example.employeeattendenceapp.Auth

expect fun signUpWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)
