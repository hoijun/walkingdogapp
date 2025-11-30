package com.tulmunchi.walkingdogapp.data.source.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tulmunchi.walkingdogapp.data.model.DogDto
import com.tulmunchi.walkingdogapp.data.model.WalkStatsDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Firebase DataSource for Dog operations
 */
interface FirebaseDogDataSource {
    suspend fun getAllDogs(uid: String): Result<List<DogDto>>
    suspend fun getDog(uid: String, dogName: String): Result<DogDto>
    suspend fun saveDog(uid: String, dog: DogDto): Result<Unit>
    suspend fun updateDog(uid: String, oldName: String, dog: DogDto): Result<Unit>
    suspend fun deleteDog(uid: String, dogName: String): Result<Unit>
}

class FirebaseDogDataSourceImpl @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth
) : FirebaseDogDataSource {

    override suspend fun getAllDogs(uid: String): Result<List<DogDto>> = suspendCoroutine { cont ->
        database.getReference("Users").child(uid).child("dog")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dogs = mutableListOf<DogDto>()
                    if (snapshot.exists()) {
                        for (dogSnapshot in snapshot.children) {
                            val dog = DogDto(
                                name = dogSnapshot.child("name").getValue(String::class.java) ?: "",
                                breed = dogSnapshot.child("breed").getValue(String::class.java) ?: "",
                                gender = dogSnapshot.child("gender").getValue(String::class.java) ?: "",
                                birth = dogSnapshot.child("birth").getValue(String::class.java) ?: "",
                                neutering = dogSnapshot.child("neutering").getValue(String::class.java) ?: "",
                                vaccination = dogSnapshot.child("vaccination").getValue(String::class.java) ?: "",
                                weight = dogSnapshot.child("weight").getValue(String::class.java) ?: "",
                                feature = dogSnapshot.child("feature").getValue(String::class.java) ?: "",
                                creationTime = dogSnapshot.child("creationTime").getValue(Long::class.java) ?: 0L,
                                totalWalkInfo = dogSnapshot.child("totalWalkInfo").getValue(WalkStatsDto::class.java) ?: WalkStatsDto()
                            )
                            dogs.add(dog)
                        }
                    }
                    cont.resume(Result.success(dogs.sortedBy { it.creationTime }))
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
    }

    override suspend fun getDog(uid: String, dogName: String): Result<DogDto> = suspendCoroutine { cont ->
        database.getReference("Users").child(uid).child("dog").child(dogName)
            .get()
            .addOnSuccessListener { snapshot ->
                val dog = snapshot.getValue(DogDto::class.java) ?: DogDto()
                cont.resume(Result.success(dog))
            }
            .addOnFailureListener { exception ->
                cont.resume(Result.failure(exception))
            }
    }

    override suspend fun saveDog(uid: String, dog: DogDto): Result<Unit> {
        return try {
            database.getReference("Users").child(uid).child("dog").child(dog.name)
                .setValue(dog)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDog(uid: String, oldName: String, dog: DogDto): Result<Unit> {
        return try {
            val dogRef = database.getReference("Users").child(uid).child("dog")

            // If name changed, delete old and create new
            if (oldName != dog.name) {
                dogRef.child(oldName).removeValue().await()
            }

            dogRef.child(dog.name).setValue(dog).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDog(uid: String, dogName: String): Result<Unit> {
        return try {
            database.getReference("Users").child(uid).child("dog").child(dogName)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
