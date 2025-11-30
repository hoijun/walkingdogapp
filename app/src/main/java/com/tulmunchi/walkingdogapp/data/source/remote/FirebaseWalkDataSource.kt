package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Firebase DataSource for Walk operations
 */
interface FirebaseWalkDataSource {
    suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit>
    suspend fun getWalkHistory(uid: String, dogName: String): Result<List<WalkRecordDto>>
    suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>>
}

class FirebaseWalkDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : FirebaseWalkDataSource {

    override suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit> {
        return try {
            database.getReference("Users").child(uid)
                .child("dog").child(dogName)
                .child("walkDates").child(record.day)
                .push()
                .setValue(record)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWalkHistory(uid: String, dogName: String): Result<List<WalkRecordDto>> = suspendCoroutine { cont ->
        database.getReference("Users").child(uid)
            .child("dog").child(dogName).child("walkDates")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val records = mutableListOf<WalkRecordDto>()
                    if (snapshot.exists()) {
                        for (dateSnapshot in snapshot.children) {
                            for (recordSnapshot in dateSnapshot.children) {
                                recordSnapshot.getValue(WalkRecordDto::class.java)?.let {
                                    records.add(it)
                                }
                            }
                        }
                    }
                    cont.resume(Result.success(records))
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
    }

    override suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>> = suspendCoroutine { cont ->
        database.getReference("Users").child(uid).child("dog")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historyMap = mutableMapOf<String, List<WalkRecordDto>>()

                    if (snapshot.exists()) {
                        for (dogSnapshot in snapshot.children) {
                            val dogName = dogSnapshot.child("name").getValue(String::class.java) ?: continue
                            val records = mutableListOf<WalkRecordDto>()

                            val walkDatesSnapshot = dogSnapshot.child("walkDates")
                            for (dateSnapshot in walkDatesSnapshot.children) {
                                for (recordSnapshot in dateSnapshot.children) {
                                    recordSnapshot.getValue(WalkRecordDto::class.java)?.let {
                                        records.add(it)
                                    }
                                }
                            }

                            historyMap[dogName] = records
                        }
                    }

                    cont.resume(Result.success(historyMap))
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
    }
}
