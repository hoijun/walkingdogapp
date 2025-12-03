package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.database.FirebaseDatabase
import com.tulmunchi.walkingdogapp.data.model.WalkRecordDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Firebase DataSource for Walk operations
 */
interface FirebaseWalkDataSource {
    suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit>
    suspend fun saveWalkRecords(uid: String, dogName: String, records: List<WalkRecordDto>): Result<Unit>
    suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>>
}

class FirebaseWalkDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase
) : FirebaseWalkDataSource {

    override suspend fun saveWalkRecord(uid: String, dogName: String, record: WalkRecordDto): Result<Unit> {
        return try {
            val key = "${record.day} ${record.startTime} ${record.endTime}"
            database.getReference("Users").child(uid)
                .child("dog").child(dogName)
                .child("walkdates").child(key)
                .setValue(record)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveWalkRecords(uid: String, dogName: String, records: List<WalkRecordDto>): Result<Unit> {
        return try {
            val walkDatesRef = database.getReference("Users").child(uid)
                .child("dog").child(dogName)
                .child("walkdates")

            for (record in records) {
                val key = "${record.day} ${record.startTime} ${record.endTime}"
                walkDatesRef.child(key).setValue(record).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllWalkHistory(uid: String): Result<Map<String, List<WalkRecordDto>>> {
        return try {
            val snapshot = database.getReference("Users").child(uid).child("dog")
                .get()
                .await()

            val historyMap = mutableMapOf<String, List<WalkRecordDto>>()

            if (snapshot.exists()) {
                for (dogSnapshot in snapshot.children) {
                    val dogName = dogSnapshot.child("name").getValue(String::class.java) ?: continue
                    val records = mutableListOf<WalkRecordDto>()

                    val walkDatesSnapshot = dogSnapshot.child("walkdates")
                    for (walkSnapshot in walkDatesSnapshot.children) {
                        walkSnapshot.getValue(WalkRecordDto::class.java)?.let {
                            val timeInfo = (walkSnapshot.key ?: "2000-10-14 00:00 10:14").split(" ")
                            val walkRecordDto = it.copy(day = timeInfo[0], startTime = timeInfo[1], endTime = timeInfo[2])
                            records.add(walkRecordDto)
                        }
                    }

                    historyMap[dogName] = records
                }
            }

            Result.success(historyMap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
