package org.example.employeeattendenceapp.Auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

actual fun signUpWithEmailPassword(
    email: String,
    password: String,
    role: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
){
    FirebaseAuth.getInstance()
        .createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
                    dbRef.child("role").setValue(role)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.localizedMessage ?: "Failed to save role") }
                } else {
                    onError("Failed to get user UID")
                }
            } else {
                onError(task.exception?.localizedMessage ?: "Sign-up failed")
            }
        }
}

actual fun signInWithEmailPassword(
    email: String,
    password: String,
    expectedRole: String,
    onSuccess: () -> Unit,
    onRoleMismatch: () -> Unit,
    onError: (String) -> Unit
) {
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("role")
                    dbRef.get().addOnSuccessListener { dataSnapshot ->
                        val storedRole = dataSnapshot.getValue(String::class.java)
                        if (storedRole == expectedRole) {
                            onSuccess()
                        } else {
                            onRoleMismatch()
                        }
                    }.addOnFailureListener { e ->
                        onError(e.localizedMessage ?: "Failed to fetch role")
                    }
                } else {
                    onError("Failed to get user UID")
                }
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
