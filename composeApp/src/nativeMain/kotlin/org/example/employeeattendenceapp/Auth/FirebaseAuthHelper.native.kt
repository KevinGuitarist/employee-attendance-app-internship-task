package org.example.employeeattendenceapp.Auth

actual fun signUpWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    TODO("Not yet implemented")
}

actual fun isUserLoggedIn(): Boolean = false