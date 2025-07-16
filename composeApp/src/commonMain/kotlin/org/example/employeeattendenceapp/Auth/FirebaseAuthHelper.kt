package org.example.employeeattendenceapp.Auth

expect fun signUpWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)

expect fun signInWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)

expect fun isUserLoggedIn(): Boolean

expect fun signOut()
