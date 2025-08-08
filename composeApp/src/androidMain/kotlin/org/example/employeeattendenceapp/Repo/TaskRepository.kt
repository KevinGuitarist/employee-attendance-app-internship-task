package org.example.employeeattendenceapp.Repo

import android.util.Log
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.example.employeeattendenceapp.data.model.Task
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val tasksRef: DatabaseReference by lazy {
        database.getReference("tasks")
    }

    suspend fun createTask(task: Task): Result<Unit> {
        return try {
            tasksRef.child(task.id).setValue(task).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            val updates = hashMapOf<String, Any>(
                "title" to task.title,
                "description" to task.description,
                "dueDate" to task.dueDate,
                "status" to task.status,
                "employeeResponse" to task.employeeResponse,
                "lastUpdated" to task.lastUpdated
            )
            tasksRef.child(task.id).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getTasksForEmployee(employeeId: String): Flow<List<Task>> = callbackFlow {
        // Convert email prefix to match the case used in database
        val dbEmployeeId = employeeId.replaceFirstChar { it.uppercase() } // "employee1" -> "Employee1"

        val query = tasksRef.orderByChild("employeeId").equalTo(dbEmployeeId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tasks = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                trySend(tasks)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { tasksRef.removeEventListener(listener) }
    }

    fun getTasksForAdmin(adminId: String): Flow<List<Task>> = callbackFlow {
        val listener = tasksRef.orderByChild("adminId").equalTo(adminId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val tasks = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                    trySend(tasks)
                }

                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            })

        awaitClose { tasksRef.removeEventListener(listener) }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            tasksRef.child(taskId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}