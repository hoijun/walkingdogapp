package com.example.walkingdogapp.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.datamodel.AlarmDao
import com.example.walkingdogapp.datamodel.AlarmDataModel
import com.example.walkingdogapp.database.LocalUserDatabase
import com.example.walkingdogapp.datamodel.CollectionInfo
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkLatLng
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.walking.SaveWalkDate
import com.example.walkingdogapp.walking.WalkingService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.naver.maps.geometry.LatLng
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserInfoRepository(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()
    private val alarmDao: AlarmDao
    private val uid = auth.currentUser?.uid
    private val storageRef = storage.getReference("$uid").child("images")
    private val userRef = db.getReference("Users").child("$uid")
    private lateinit var alarmList: List<AlarmDataModel>

    init {
        val roomDb = LocalUserDatabase.getInstance(application)
        alarmDao = roomDb!!.alarmDao()
    }

    suspend fun observeUser(
        dogsInfo: MutableLiveData<List<DogInfo>>,
        userInfo: MutableLiveData<UserInfo>,
        totalWalkInfo: MutableLiveData<WalkInfo>,
        walkDates: MutableLiveData<HashMap<String, MutableList<WalkRecord>>>,
        collectionInfo: MutableLiveData<HashMap<String, Boolean>>,
        dogsImg: MutableLiveData<HashMap<String, Uri>>,
        successGetData: MutableLiveData<Boolean>
    ) {
        if (!NetworkManager.checkNetworkState(application)) {
            successGetData.postValue(false)
            return
        }

        if (successGetData.value == true) {
            successGetData.postValue(true)
        }

        val isError = AtomicBoolean(false)
        val dogsInfoDeferred = CompletableDeferred<List<DogInfo>>()
        val userDeferred = CompletableDeferred<UserInfo>()
        val totalWalkDeferred = CompletableDeferred<WalkInfo>()
        val walkDateDeferred = CompletableDeferred<HashMap<String, MutableList<WalkRecord>>>()
        val collectionDeferred = CompletableDeferred<HashMap<String, Boolean>>()
        val profileUriDeferred = CompletableDeferred<HashMap<String, Uri>>()


        try {
            userRef.child("dog").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dogsList = mutableListOf<DogInfo>()
                    val dogNames = mutableListOf<String>()
                    if (snapshot.exists()) {
                        for (dogInfo in snapshot.children) {
                            dogsList.add(
                                DogInfo(
                                    dogInfo.child("name").getValue(String::class.java)!!,
                                    dogInfo.child("breed").getValue(String::class.java)!!,
                                    dogInfo.child("gender").getValue(String::class.java)!!,
                                    dogInfo.child("birth").getValue(String::class.java)!!,
                                    dogInfo.child("neutering")
                                        .getValue(String::class.java)!!,
                                    dogInfo.child("vaccination")
                                        .getValue(String::class.java)!!,
                                    dogInfo.child("weight").getValue(String::class.java)!!,
                                    dogInfo.child("feature").getValue(String::class.java)!!,
                                    dogInfo.child("creationTime").getValue(Long::class.java)!!,
                                    dogInfo.child("walkInfo").getValue(WalkInfo::class.java)!!
                                )
                            )
                            dogNames.add(dogInfo.child("name").getValue(String::class.java)!!)
                        }
                    }
                    MainActivity.dogNameList = dogNames
                    dogsInfoDeferred.complete(dogsList.sortedBy { it.creationTime })
                }

                override fun onCancelled(error: DatabaseError) {
                    isError.set(true)
                    dogsInfoDeferred.complete(listOf<DogInfo>())
                }
            })

            userRef.child("user").get().addOnSuccessListener {
                userDeferred.complete(it.getValue(UserInfo::class.java) ?: UserInfo())
            }.addOnFailureListener {
                isError.set(true)
                userDeferred.complete(UserInfo())
            }

            userRef.child("totalWalkInfo").get().addOnSuccessListener {
                totalWalkDeferred.complete(it.getValue(WalkInfo::class.java) ?: WalkInfo())
            }.addOnFailureListener {
                isError.set(true)
                totalWalkDeferred.complete(WalkInfo())
            }

            dogsInfo.postValue(dogsInfoDeferred.await())

            val dogsWalkRecord = HashMap<String, MutableList<WalkRecord>>()

            if (MainActivity.dogNameList.isEmpty()) {
                walkDateDeferred.complete(dogsWalkRecord)
            } else {

                for (dog in MainActivity.dogNameList) {
                    userRef.child("dog").child(dog).child("walkdates")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val walkRecords = mutableListOf<WalkRecord>()
                                if (snapshot.exists()) {
                                    for (dateData in snapshot.children) {
                                        val walkDay = dateData.key.toString().split(" ")
                                        walkRecords.add(
                                            WalkRecord(
                                                walkDay[0], walkDay[1], walkDay[2],
                                                dateData.child("distance")
                                                    .getValue(Float::class.java)!!,
                                                dateData.child("time").getValue(Int::class.java)!!,
                                                dateData.child("coords")
                                                    .getValue<List<WalkLatLng>>()
                                                    ?: listOf<WalkLatLng>(),
                                                dateData.child("collections")
                                                    .getValue<List<String>>() ?: listOf<String>()
                                            )
                                        )
                                    }
                                }
                                dogsWalkRecord[dog] = walkRecords
                                if (dogsWalkRecord.size == MainActivity.dogNameList.size) {
                                    walkDateDeferred.complete(dogsWalkRecord)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                isError.set(true)
                                dogsWalkRecord[dog] = mutableListOf()
                                if (dogsWalkRecord.size == MainActivity.dogNameList.size) {
                                    walkDateDeferred.complete(dogsWalkRecord)
                                }
                            }
                        })
                }
            }

            userRef.child("collection").get().addOnSuccessListener {
                collectionDeferred.complete(it.getValue<HashMap<String, Boolean>>() ?: Constant.item_whether)
            }.addOnFailureListener {
                isError.set(true)
                collectionDeferred.complete(Constant.item_whether)
            }


            // 강아지 프로필 사진
            try {
                val dogImgs = HashMap<String, Uri>()
                var downloadCount = 0
                var imgCount: Int
                storageRef.listAll().addOnSuccessListener { listResult ->
                    imgCount = listResult.items.size
                    if (imgCount == 0) {
                        profileUriDeferred.complete(HashMap<String, Uri>())
                    }
                    listResult.items.forEach { item ->
                        item.downloadUrl.addOnSuccessListener { uri ->
                            downloadCount++
                            dogImgs[item.name] = uri

                            if (imgCount == downloadCount) {
                                profileUriDeferred.complete(dogImgs)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                isError.set(true)
                profileUriDeferred.complete(HashMap<String, Uri>())
            }

            userInfo.postValue(userDeferred.await())
            totalWalkInfo.postValue(totalWalkDeferred.await())
            walkDates.postValue(walkDateDeferred.await())
            collectionInfo.postValue(collectionDeferred.await())
            dogsImg.postValue(profileUriDeferred.await())

            if (!isError.get()) {
                successGetData.postValue(true)
            } else {
                successGetData.postValue(false)
            }


        } catch (e: Exception) {
            successGetData.postValue(false)
        }
    }

    fun add(alarm: AlarmDataModel) {
        alarmDao.addAlarm(alarm)
    }

    fun delete(alarm: AlarmDataModel) {
        alarmDao.deleteAlarm(alarm.alarm_code)
    }

    fun getAll(): List<AlarmDataModel> {
        alarmList = alarmDao.getAlarmsList()
        return alarmList
    }

    fun onOffAlarm(alarmCode: Int, alarmOn: Boolean) {
        alarmDao.updateAlarmStatus(alarmCode, alarmOn)
    }

    fun updateAlarmTime(alarmCode: Int, time: Long) {
        alarmDao.updateAlarmTime(alarmCode, time)
    }

    suspend fun updateUserInfo(userInfo: UserInfo) {
        val userInfoUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            val nameDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("name").setValue(userInfo.name).await()
                } catch (e: Exception) {
                    return@async
                }
            }

            val genderDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("gender").setValue(userInfo.gender).await()
                } catch (e: Exception) {
                    return@async
                }
            }

            val birthDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("birth").setValue(userInfo.birth).await()
                } catch (e: Exception) {
                    return@async
                }
            }

            nameDeferred.await()
            genderDeferred.await()
            birthDeferred.await()
        }

        userInfoUpdateJob.join()
    }

    suspend fun updateDogInfo(
        dogInfo: DogInfo,
        beforeName: String,
        imgUri: Uri?,
        walkRecords: ArrayList<WalkRecord>
    ): Boolean {
        var error = false
        val result = CoroutineScope(Dispatchers.IO).launch {
            val dogInfoJob = async(Dispatchers.IO) {
                try {
                    if (beforeName != "") { // 수정하는 경우
                        userRef.child("dog").child(beforeName).removeValue().await()
                    }
                    userRef.child("dog").child(dogInfo.name).setValue(dogInfo).await()
                } catch (e: Exception) {
                    error = true
                    return@async
                }
            }

            dogInfoJob.await()

            if (error) {
                return@launch
            }

            val walkRecordJob = async(Dispatchers.IO) {
                try {
                    for (walkRecord in walkRecords) {
                        val day = walkRecord.day + " " + walkRecord.startTime + " " + walkRecord.endTime
                        userRef.child("dog").child(dogInfo.name).child("walkdates")
                            .child(day)
                            .setValue(
                                SaveWalkDate(
                                    walkRecord.distance,
                                    walkRecord.time,
                                    walkRecord.coords,
                                    walkRecord.collections
                                )
                            )
                            .await()
                    }
                } catch (e: Exception) {
                    error = true
                    return@async
                }
            }

            val uploadJob = launch(Dispatchers.IO) upload@ {
                try {
                    if (imgUri != null) {
                        storageRef.child(dogInfo.name)
                            .putFile(imgUri).await()
                    } else {
                        if (MainActivity.dogUriList[beforeName] != null) {
                            val tempUri = suspendCoroutine { continuation ->
                                val tempFile = File.createTempFile(
                                    "temp",
                                    ".jpg",
                                    application.cacheDir
                                )
                                storage.getReferenceFromUrl(MainActivity.dogUriList[beforeName].toString())
                                    .getStream { _, inputStream ->
                                        val outputStream =
                                            FileOutputStream(tempFile)
                                        inputStream.copyTo(outputStream)
                                        val tempFileUri = Uri.fromFile(tempFile)
                                        continuation.resume(tempFileUri)
                                    }
                            }

                            storageRef.child(dogInfo.name)
                                .putFile(tempUri).await()
                        }
                    }
                } catch (e: Exception) {
                    error = true
                    return@upload
                }
            }

            uploadJob.join()

            val deleteJob = launch(Dispatchers.IO) delete@ {
                try {
                    if (beforeName == "") {
                        return@delete
                    }

                    if (!MainActivity.dogNameList.contains(dogInfo.name)) {
                        storageRef.child(beforeName).delete()
                            .await()
                    }
                } catch (e: Exception) {
                    error = true
                    return@delete
                }
            }

            walkRecordJob.await()
            deleteJob.join()
        }

        result.join()
        return error
    }

    suspend fun removeDogInfo(beforeName: String) {
        val result = CoroutineScope(Dispatchers.IO).launch {
            val removeDogInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.child("dog").child(beforeName).removeValue()
                        .await()
                } catch (e: Exception) {
                    return@async
                }
            }

            val removeDogImgJob = launch(Dispatchers.IO) remove@ {
                try {
                    if (MainActivity.dogUriList[beforeName] != null) {
                        storageRef.child(beforeName).delete()
                            .await()
                    }
                } catch (e: Exception) {
                    return@remove
                }
            }

            removeDogInfoJob.await()
            removeDogImgJob.join()
        }

        result.join()
    }

    suspend fun saveWalkInfo(
        walkDogs: ArrayList<String>,
        startTime: String,
        distance: Float,
        time: Int,
        coords: List<LatLng>,
        collections: List<String>
    ): Boolean {
        var isError = false
        val walkInfoUpdateJob = suspendCoroutine { continuation ->
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    val endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val walkDateInfo = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + startTime + " " + endTime

                    val totalWalk = snapshot.child("totalWalkInfo").getValue(WalkInfo::class.java)
                    val indivisualWalks = hashMapOf<String, WalkInfo?>().also {
                        for (name in walkDogs) {
                            it[name] = snapshot.child("dog").child(name).child("walkInfo").getValue(
                                WalkInfo::class.java
                            )
                        }
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val totalWalkJob = async(Dispatchers.IO) {
                            try {
                                if (totalWalk != null) {
                                    userRef.child("totalWalkInfo").setValue(
                                        WalkInfo(
                                            totalWalk.distance + distance,
                                            totalWalk.time + time
                                        )
                                    ).await()
                                } else {
                                    userRef.child("totalWalkInfo")
                                        .setValue(WalkInfo(distance, time))
                                        .await()
                                }
                            } catch (e: Exception) {
                                isError = true
                                return@async
                            }
                        }

                        val indivisualWalkJob = async(Dispatchers.IO) {
                            try {
                                for (dogName in walkDogs) {
                                    if (indivisualWalks[dogName] != null) {
                                        userRef.child("dog").child(dogName).child("walkInfo")
                                            .setValue(
                                                WalkInfo(
                                                    indivisualWalks[dogName]!!.distance + distance,
                                                    indivisualWalks[dogName]!!.time + time
                                                )
                                            ).await()
                                    } else {
                                        userRef.child("dog").child(dogName).child("walkInfo")
                                            .setValue(
                                                WalkInfo(distance, time)
                                            ).await()
                                    }
                                }
                            } catch (e: Exception) {
                                isError = true
                                return@async
                            }
                        }

                        val saveWalkDateJob = async(Dispatchers.IO) {
                            try {
                                val saveCoords = mutableListOf<WalkLatLng>()
                                for (coord in coords) {
                                    saveCoords.add(WalkLatLng(coord.latitude, coord.longitude))
                                }
                                for (dog in walkDogs) {
                                    userRef.child("dog").child(dog).child("walkdates")
                                        .child(walkDateInfo)
                                        .setValue(
                                            SaveWalkDate(
                                                distance,
                                                time,
                                                saveCoords,
                                                collections
                                            )
                                        )
                                        .await()
                                }
                            } catch (e: Exception) {
                                isError = true
                                return@async
                            }
                        }

                        val collectionInfoJob = async(Dispatchers.IO) {
                            try {
                                val update = mutableMapOf<String, Any>()
                                for (item in WalkingService.getCollectionItems) {
                                    update[item] = true
                                }
                                userRef.child("collection").updateChildren(update).await()
                            } catch (e: Exception) {
                                isError = true
                                return@async
                            }
                        }

                        totalWalkJob.await()
                        indivisualWalkJob.await()
                        saveWalkDateJob.await()
                        collectionInfoJob.await()

                        continuation.resume(isError)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(isError)
                }
            })
        }

        return walkInfoUpdateJob
    }

    suspend fun removeAccount(): Boolean {
        var error = false
        val result = CoroutineScope(Dispatchers.IO).async {
            val deleteProfileJob = async(Dispatchers.IO) {
                try {
                    storageRef.listAll().addOnSuccessListener { listResult ->
                        listResult.items.forEach { item ->
                            item.delete()
                        }
                    }
                } catch (e: Exception) {
                    Log.d("savepoint", e.message.toString())
                    error = true
                }
            }

            deleteProfileJob.await()

            if (error) {
                return@async false
            }

            val deleteInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.removeValue().await()
                } catch (e: Exception) {
                    Log.d("savepoint", e.message.toString())
                    error = true
                }
            }

            deleteInfoJob.await()

            return@async !error
        }
        return result.await()
    }
}