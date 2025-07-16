package org.example.employeeattendenceapp.Auth

import com.google.firebase.auth.FirebaseAuth

actual fun signUpWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
){
    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError(task.exception?.localizedMessage ?: "Sign-up failed")
            }
        }
}
