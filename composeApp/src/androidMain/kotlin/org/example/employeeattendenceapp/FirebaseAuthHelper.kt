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

actual fun signInWithEmailPassword(
    email: String,
    password: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError(task.exception?.localizedMessage ?: "Login failed")
            }
        }
}

actual fun isUserLoggedIn(): Boolean {
    return FirebaseAuth.getInstance().currentUser != null
}

actual fun signOut() {
    FirebaseAuth.getInstance().signOut()
}
