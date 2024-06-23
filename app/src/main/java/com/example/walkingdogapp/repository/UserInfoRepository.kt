package com.example.walkingdogapp.repository

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.datamodel.AlarmDao
import com.example.walkingdogapp.datamodel.AlarmDataModel
import com.example.walkingdogapp.database.LocalUserDatabase
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.UserInfo
import com.example.walkingdogapp.datamodel.WalkInfo
import com.example.walkingdogapp.datamodel.WalkLatLng
import com.example.walkingdogapp.datamodel.WalkRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserInfoRepository(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()
    private val alarmDao: AlarmDao
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

        try {
            val uid = auth.currentUser?.uid
            val userRef = db.getReference("Users").child("$uid")
            val storageRef = storage.getReference("$uid").child("images")

            val dogsDeferred = suspendCoroutine { continuation ->
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
                                        dogInfo.child("weight").getValue(Int::class.java)!!,
                                        dogInfo.child("feature").getValue(String::class.java)!!,
                                        dogInfo.child("creationTime").getValue(Long::class.java)!!,
                                        dogInfo.child("walkInfo").getValue(WalkInfo::class.java)!!
                                    )
                                )
                                dogNames.add(dogInfo.child("name").getValue(String::class.java)!!)
                            }
                        }
                        MainActivity.dogNameList = dogNames
                        continuation.resume(dogsList.sortedBy { it.creationTime })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        continuation.resume(listOf<DogInfo>())
                    }
                })
            }

            val userDeferred = coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        userRef.child("user").get().await().getValue(UserInfo::class.java)
                            ?: UserInfo()
                    } catch (e: Exception) {
                        UserInfo()
                    }
                }
            }

            val totalWalkDeferred = coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        userRef.child("totalWalkInfo").get().await().getValue(WalkInfo::class.java)
                            ?: WalkInfo()
                    } catch (e: Exception) {
                        WalkInfo()
                    }
                }
            }

            val walkDateDeferred = suspendCoroutine { continuation ->
                val dogsWalkRecord = HashMap<String, MutableList<WalkRecord>>()
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
                                    dogsWalkRecord[dog] = walkRecords
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                dogsWalkRecord[dog] = mutableListOf()
                            }
                        })
                }

                continuation.resume(dogsWalkRecord)
            }

            val collectionDeferred = coroutineScope {
                async(Dispatchers.IO) {
                    try {
                        userRef.child("collection").get().await()
                            .getValue<HashMap<String, Boolean>>() ?: Constant.item_whether
                    } catch (e: Exception) {
                        Constant.item_whether
                    }
                }
            }

            // 강아지 프로필 사진
            val profileUriDeferred = suspendCoroutine { continuation ->
                try {
                    val dogImgs = HashMap<String, Uri>()
                    var downloadCount = 0
                    var imgCount: Int
                    storageRef.listAll().addOnSuccessListener { listResult ->
                        imgCount = listResult.items.size
                        if (imgCount == 0) {
                            continuation.resume(HashMap<String, Uri>())
                        }
                        listResult.items.forEach { item ->
                            item.downloadUrl.addOnSuccessListener { uri ->
                                downloadCount++
                                dogImgs[item.name] = uri

                                if (imgCount == downloadCount) {
                                    continuation.resume(dogImgs)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    continuation.resume(HashMap<String, Uri>())
                }
            }

            dogsInfo.postValue(dogsDeferred)
            userInfo.postValue(userDeferred.await())
            totalWalkInfo.postValue(totalWalkDeferred.await())
            walkDates.postValue(walkDateDeferred)
            collectionInfo.postValue(collectionDeferred.await())
            dogsImg.postValue(profileUriDeferred)

            successGetData.postValue(true)


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

    suspend fun removeAccount(): Boolean {
        var error = false
        val uid = auth.currentUser?.uid
        val storageRef = storage.getReference("$uid").child("images")
        val userRef = db.getReference("Users").child("$uid")
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