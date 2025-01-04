package com.tulmunchi.walkingdogapp.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.tulmunchi.walkingdogapp.datamodel.AlarmDao
import com.tulmunchi.walkingdogapp.datamodel.AlarmDataModel
import com.tulmunchi.walkingdogapp.datamodel.DogInfo
import com.tulmunchi.walkingdogapp.datamodel.TotalWalkInfo
import com.tulmunchi.walkingdogapp.datamodel.UserInfo
import com.tulmunchi.walkingdogapp.datamodel.WalkDateInfo
import com.tulmunchi.walkingdogapp.datamodel.WalkDateInfoInSave
import com.tulmunchi.walkingdogapp.datamodel.WalkLatLng
import com.tulmunchi.walkingdogapp.utils.FirebaseAnalyticHelper
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.naver.maps.geometry.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class UserInfoRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val alarmDao: AlarmDao
) {
    private var uid = auth.currentUser?.uid
    private var userRef = database.getReference("Users").child("$uid")
    private var storageRef = storage.getReference("$uid").child("images")
    private lateinit var alarmList: List<AlarmDataModel>

    @Inject
    lateinit var firebaseHelper: FirebaseAnalyticHelper

    fun setUser() {
        uid = auth.currentUser?.uid
        userRef = database.getReference("Users").child("$uid")
        storageRef = storage.getReference("$uid").child("images")
    }

    suspend fun signUp(email: String, successSignUp: MutableLiveData<Boolean>) {
        val userRef = database.getReference("Users")
        val userInfo = UserInfo(email = email)
        val errorReason = mutableListOf<Pair<String, String>>()
        val isError = AtomicBoolean(false)

        CoroutineScope(Dispatchers.IO).launch {
            val userInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.child("$uid").child("user")
                        .setValue(userInfo)
                        .await()
                } catch (e: Exception) {
                    errorReason.add("Error: userInfo" to e.message.toString())
                    isError.set(true)
                }
            }

            val totalTotalWalkInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.child("$uid").child("totalWalk")
                        .setValue(TotalWalkInfo()).await()
                } catch (e: Exception) {
                    errorReason.add("Error: totalWalkInfo" to e.message.toString())
                    isError.set(true)
                }
            }

            val collectionInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.child("$uid").child("collection")
                        .setValue(Utils.item_whether).await()
                } catch (e: Exception) {
                    errorReason.add("Error: collectionInfo" to e.message.toString())
                    isError.set(true)
                }
            }

            val termsOfServiceJob = async(Dispatchers.IO) {
                try {
                    userRef.child("$uid").child("termsOfService")
                        .setValue(true).await()
                } catch (e: Exception) {
                    errorReason.add("Error: termsOfService" to e.message.toString())
                    isError.set(true)
                }
            }

            userInfoJob.await()
            totalTotalWalkInfoJob.await()
            collectionInfoJob.await()
            termsOfServiceJob.await()

            if (!isError.get()) {
                successSignUp.postValue(true)
            } else {
                firebaseHelper.logEvent(
                    listOf(
                        "type" to "SignUp_Fail",
                        "api" to "Firebase",
                    ) + errorReason
                )
                successSignUp.postValue(false)
            }
        }
    }

    suspend fun observeUser(
        dogsInfo: MutableLiveData<List<DogInfo>>,
        userInfo: MutableLiveData<UserInfo>,
        totalWalkInfo: MutableLiveData<TotalWalkInfo>,
        walkDates: MutableLiveData<HashMap<String, MutableList<WalkDateInfo>>>,
        collectionInfo: MutableLiveData<HashMap<String, Boolean>>,
        dogsImg: MutableLiveData<HashMap<String, Uri>>,
        dogNames: MutableLiveData<List<String>>,
        successGetData: MutableLiveData<Boolean>
    ) {
        if (!NetworkManager.checkNetworkState(context)) {
            successGetData.postValue(false)
            return
        }

        if (successGetData.value == true) {
            successGetData.postValue(true)
        }

        val isError = AtomicBoolean(false)
        val errorReason = mutableListOf<Pair<String, String>>()
        val dogsInfoDeferred = CompletableDeferred<List<DogInfo>>()
        val userDeferred = CompletableDeferred<UserInfo>()
        val totalWalkDeferred = CompletableDeferred<TotalWalkInfo>()
        val walkDateDeferred = CompletableDeferred<HashMap<String, MutableList<WalkDateInfo>>>()
        val collectionDeferred = CompletableDeferred<HashMap<String, Boolean>>()
        val profileUriDeferred = CompletableDeferred<HashMap<String, Uri>>()
        var dogNameList = mutableListOf<String>()

        try {
            userRef.child("dog").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val dogsList = mutableListOf<DogInfo>()
                    val dogNamesList = mutableListOf<String>()
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
                                    dogInfo.child("totalWalkInfo").getValue(TotalWalkInfo::class.java)!!
                                )
                            )
                            dogNamesList.add(dogInfo.child("name").getValue(String::class.java)!!)
                        }
                    }
                    dogNameList = dogNamesList
                    dogsInfoDeferred.complete(dogsList.sortedBy { it.creationTime })
                }

                override fun onCancelled(error: DatabaseError) {
                    isError.set(true)
                    errorReason.add("Error: dogsInfo" to error.message)
                    dogsInfoDeferred.complete(listOf())
                }
            })

            userRef.child("user").get().addOnSuccessListener {
                userDeferred.complete(it.getValue(UserInfo::class.java) ?: UserInfo())
            }.addOnFailureListener {
                errorReason.add("Error: userInfo" to it.message.toString())
                isError.set(true)
                userDeferred.complete(UserInfo())
            }

            userRef.child("totalWalkInfo").get().addOnSuccessListener {
                totalWalkDeferred.complete(it.getValue(TotalWalkInfo::class.java) ?: TotalWalkInfo())
            }.addOnFailureListener {
                errorReason.add("Error: totalWalkInfo" to it.message.toString())
                isError.set(true)
                totalWalkDeferred.complete(TotalWalkInfo())
            }

            dogsInfo.postValue(dogsInfoDeferred.await())

            val dogsWalkDateInfo = HashMap<String, MutableList<WalkDateInfo>>()

            if (dogNameList.isEmpty()) {
                walkDateDeferred.complete(dogsWalkDateInfo)
            } else {
                for (dog in dogNameList) {
                    userRef.child("dog").child(dog).child("walkdates")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val walkDateInfos = mutableListOf<WalkDateInfo>()
                                if (snapshot.exists()) {
                                    for (dateData in snapshot.children) {
                                        val walkDay = dateData.key.toString().split(" ")
                                        walkDateInfos.add(
                                            WalkDateInfo(
                                                walkDay[0], walkDay[1], walkDay[2],
                                                dateData.child("distance")
                                                    .getValue(Float::class.java)!!,
                                                dateData.child("time").getValue(Int::class.java)!!,
                                                dateData.child("coords")
                                                    .getValue<List<WalkLatLng>>()
                                                    ?: listOf(),
                                                dateData.child("collections")
                                                    .getValue<List<String>>() ?: listOf()
                                            )
                                        )
                                    }
                                }
                                dogsWalkDateInfo[dog] = walkDateInfos
                                if (dogsWalkDateInfo.size == dogNameList.size) {
                                    walkDateDeferred.complete(dogsWalkDateInfo)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                isError.set(true)
                                errorReason.add("Error: walkDateInfo" to error.message)
                                dogsWalkDateInfo[dog] = mutableListOf()
                                if (dogsWalkDateInfo.size == dogNameList.size) {
                                    walkDateDeferred.complete(dogsWalkDateInfo)
                                }
                            }
                        })
                }
            }

            userRef.child("collection").get().addOnSuccessListener {
                collectionDeferred.complete(
                    it.getValue<HashMap<String, Boolean>>() ?: Utils.item_whether
                )
            }.addOnFailureListener {
                errorReason.add("Error: collectionInfo" to it.message.toString())
                isError.set(true)
                collectionDeferred.complete(Utils.item_whether)
            }

            // 강아지 프로필 사진
            val dogImgs = HashMap<String, Uri>()
            var downloadCount = 0
            var imgCount: Int
            storageRef.listAll().addOnSuccessListener { listResult ->
                imgCount = listResult.items.size
                if (imgCount == 0) {
                    profileUriDeferred.complete(HashMap())
                }
                listResult.items.forEach { item ->
                    item.downloadUrl.addOnSuccessListener { uri ->
                        downloadCount++
                        dogImgs[item.name] = uri
                        if (imgCount == downloadCount) {
                            profileUriDeferred.complete(dogImgs)
                        }
                    }.addOnFailureListener {
                        downloadCount++
                        dogImgs[item.name] = Uri.EMPTY
                        if (imgCount == downloadCount) {
                            profileUriDeferred.complete(dogImgs)
                        }
                    }
                }
            }.addOnFailureListener {
                isError.set(true)
                errorReason.add("Error: profileUri" to it.message.toString())
                profileUriDeferred.complete(HashMap())
            }

            dogNames.postValue(dogNameList)
            userInfo.postValue(userDeferred.await())
            totalWalkInfo.postValue(totalWalkDeferred.await())
            walkDates.postValue(walkDateDeferred.await())
            collectionInfo.postValue(collectionDeferred.await())
            dogsImg.postValue(profileUriDeferred.await())

            if (!isError.get()) {
                successGetData.postValue(true)
            } else {
                firebaseHelper.logEvent(
                    listOf(
                        "type" to "GetData_Fail",
                        "api" to "Firebase",
                    ) + errorReason
                )
                successGetData.postValue(false)
            }

        } catch (e: Exception) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "GetData_Fail",
                    "api" to "Firebase",
                    "reason" to e.message.toString()
                )
            )
            successGetData.postValue(false)
        }
    }

    fun add(alarm: AlarmDataModel) {
        try {
            alarmDao.addAlarm(alarm)
        } catch (e: Exception) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "AddAlarm_Fail",
                    "api" to "",
                    "reason" to e.message.toString()
                )
            )
        }
    }

    fun delete(alarm: AlarmDataModel) {
        try {
            alarmDao.deleteAlarm(alarm.alarm_code)
        } catch (e: Exception) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "DeleteAlarm_Fail",
                    "api" to "",
                    "reason" to e.message.toString()
                )
            )
        }
    }

    fun getAll(): List<AlarmDataModel> {
        try {
            alarmList = alarmDao.getAlarmsList()
        } catch (e: Exception) {
            alarmList = listOf()
            firebaseHelper.logEvent(
                listOf(
                    "type" to "GetAllAlarm_Fail",
                    "api" to "",
                    "reason" to e.message.toString()
                )
            )
        }

        return alarmList
    }

    fun onOffAlarm(alarmCode: Int, alarmOn: Boolean) {
        try {
            alarmDao.updateAlarmStatus(alarmCode, alarmOn)
        } catch (e: Exception) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "UpdateAlarmStatus_Fail",
                    "api" to "",
                    "reason" to e.message.toString()
                )
            )
        }
    }

    fun updateAlarmTime(alarmCode: Int, time: Long) {
        try {
            alarmDao.updateAlarmTime(alarmCode, time)
        } catch (e: Exception) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "UpdateAlarmTime_Fail",
                    "api" to "",
                    "reason" to e.message.toString()
                )
            )
        }
    }

    suspend fun updateUserInfo(userInfo: UserInfo) {
        val errorReason = mutableListOf<Pair<String, String>>()
        val userInfoUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            val nameDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("name").setValue(userInfo.name).await()
                } catch (e: Exception) {
                    errorReason.add("Error: name" to e.message.toString())
                    return@async
                }
            }

            val genderDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("gender").setValue(userInfo.gender).await()
                } catch (e: Exception) {
                    errorReason.add("Error: gender" to e.message.toString())
                    return@async
                }
            }

            val birthDeferred = async(Dispatchers.IO) {
                try {
                    userRef.child("user").child("birth").setValue(userInfo.birth).await()
                } catch (e: Exception) {
                    errorReason.add("Error: birth" to e.message.toString())
                    return@async
                }
            }

            if (errorReason.isNotEmpty()) {
                firebaseHelper.logEvent(
                    listOf(
                        "type" to "UpdateUserInfo_Fail",
                        "api" to "Firebase",
                    ) + errorReason
                )
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
        walkDateInfos: ArrayList<WalkDateInfo>,
        dogUriList: HashMap<String, Uri>,
        dogNameList: List<String>,
    ): Boolean {
        var error = false
        val errorReason = mutableListOf<Pair<String, String>>()
        val result = CoroutineScope(Dispatchers.IO).launch {
            val dogInfoJob = async(Dispatchers.IO) {
                try {
                    if (beforeName != "") { // 수정하는 경우
                        userRef.child("dog").child(beforeName).removeValue().await()
                    }
                    userRef.child("dog").child(dogInfo.name).setValue(dogInfo).await()
                } catch (e: Exception) {
                    errorReason.add("Error: dogInfo" to e.message.toString())
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
                    for (walkRecord in walkDateInfos) {
                        val day = walkRecord.day + " " + walkRecord.startTime + " " + walkRecord.endTime
                        userRef.child("dog").child(dogInfo.name).child("walkdates").child(day)
                            .setValue(
                                WalkDateInfoInSave(
                                    walkRecord.distance,
                                    walkRecord.time,
                                    walkRecord.coords,
                                    walkRecord.collections
                                )
                            ).await()
                    }
                } catch (e: Exception) {
                    errorReason.add("Error: walkRecord" to e.message.toString())
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
                        if (dogUriList[beforeName] != null) {
                            val tempUri = suspendCoroutine { continuation ->
                                val tempFile = File.createTempFile(
                                    "temp",
                                    ".jpg",
                                    context.cacheDir
                                )
                                storage.getReferenceFromUrl(dogUriList[beforeName].toString())
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
                    errorReason.add("Error: upload" to e.message.toString())
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

                    if (!dogNameList.contains(dogInfo.name) && dogUriList[beforeName] != null) {
                        storageRef.child(beforeName).delete().await()
                    }
                } catch (e: Exception) {
                    errorReason.add("Error: delete" to e.message.toString())
                    error = true
                    return@delete
                }
            }

            walkRecordJob.await()
            deleteJob.join()
        }

        result.join()

        if (errorReason.isNotEmpty()) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "UpdateDogInfo_Fail",
                    "api" to "Firebase",
                ) + errorReason
            )
        }

        return error
    }

    suspend fun removeDogInfo(beforeName: String, dogUriList: HashMap<String, Uri>) {
        val errorReason = mutableListOf<Pair<String, String>>()
        val result = CoroutineScope(Dispatchers.IO).launch {
            val removeDogInfoJob = async(Dispatchers.IO) {
                try {
                    userRef.child("dog").child(beforeName).removeValue()
                        .await()
                } catch (e: Exception) {
                    errorReason.add("Error: dogInfo" to e.message.toString())
                    return@async
                }
            }

            val removeDogImgJob = launch(Dispatchers.IO) remove@ {
                try {
                    if (dogUriList[beforeName] != null) {
                        storageRef.child(beforeName).delete()
                            .await()
                    }
                } catch (e: Exception) {
                    errorReason.add("Error: dogImg" to e.message.toString())
                    return@remove
                }
            }

            removeDogInfoJob.await()
            removeDogImgJob.join()
        }

        result.join()

        if (errorReason.isNotEmpty()) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "RemoveDogInfo_Fail",
                    "api" to "Firebase",
                ) + errorReason
            )
        }
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
        val errorReason = mutableListOf<Pair<String, String>>()
        val walkInfoUpdateJob = suspendCoroutine { continuation ->
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    val walkDateInfo = LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " " + startTime + " " + endTime

                    val totalWalk = snapshot.child("totalWalkInfo").getValue(TotalWalkInfo::class.java)
                    val indivisualWalks = hashMapOf<String, TotalWalkInfo?>().also {
                        for (name in walkDogs) {
                            it[name] = snapshot.child("dog").child(name).child("totalWalkInfo").getValue(
                                TotalWalkInfo::class.java
                            )
                        }
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val totalWalkJob = async(Dispatchers.IO) {
                            try {
                                if (totalWalk != null) {
                                    userRef.child("totalWalkInfo").setValue(
                                        TotalWalkInfo(
                                            totalWalk.distance + distance,
                                            totalWalk.time + time
                                        )
                                    ).await()
                                } else {
                                    userRef.child("totalWalkInfo")
                                        .setValue(TotalWalkInfo(distance, time))
                                        .await()
                                }
                            } catch (e: Exception) {
                                isError = true
                                errorReason.add("Error: totalWalkInfo" to e.message.toString())
                                return@async
                            }
                        }

                        val indivisualWalkJob = async(Dispatchers.IO) {
                            try {
                                for (dogName in walkDogs) {
                                    if (indivisualWalks[dogName] != null) {
                                        userRef.child("dog").child(dogName).child("totalWalkInfo")
                                            .setValue(
                                                TotalWalkInfo(
                                                    indivisualWalks[dogName]!!.distance + distance,
                                                    indivisualWalks[dogName]!!.time + time
                                                )
                                            ).await()
                                    } else {
                                        userRef.child("dog").child(dogName).child("totalWalkInfo")
                                            .setValue(
                                                TotalWalkInfo(distance, time)
                                            ).await()
                                    }
                                }
                            } catch (e: Exception) {
                                isError = true
                                errorReason.add("Error: indivisualWalkInfo" to e.message.toString())
                                return@async
                            }
                        }

                        val walkDateInfoInSaveJob = async(Dispatchers.IO) {
                            try {
                                val saveCoords = mutableListOf<WalkLatLng>()
                                for (coord in coords) {
                                    saveCoords.add(WalkLatLng(coord.latitude, coord.longitude))
                                }
                                for (dog in walkDogs) {
                                    userRef.child("dog").child(dog).child("walkdates")
                                        .child(walkDateInfo)
                                        .setValue(
                                            WalkDateInfoInSave(
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
                                errorReason.add("Error: walkDateInfo" to e.message.toString())
                                return@async
                            }
                        }

                        val collectionInfoJob = async(Dispatchers.IO) {
                            try {
                                val update = mutableMapOf<String, Any>()
                                for (item in collections) {
                                    update[item] = true
                                }
                                userRef.child("collection").updateChildren(update).await()
                            } catch (e: Exception) {
                                isError = true
                                errorReason.add("Error: collectionInfo" to e.message.toString())
                                return@async
                            }
                        }

                        totalWalkJob.await()
                        indivisualWalkJob.await()
                        walkDateInfoInSaveJob.await()
                        collectionInfoJob.await()

                        continuation.resume(isError)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resume(isError)
                }
            })
        }

        if (errorReason.isNotEmpty()) {
            firebaseHelper.logEvent(
                listOf(
                    "type" to "SaveWalkInfo_Fail",
                    "api" to "Firebase",
                ) + errorReason
            )
        }

        return walkInfoUpdateJob
    }

    suspend fun removeAccount(): Boolean {
        var error = false
        val errorReason = mutableListOf<Pair<String, String>>()
        val result = CoroutineScope(Dispatchers.IO).async {
            val deleteProfileJob = async(Dispatchers.IO) {
                try {
                    storageRef.listAll().addOnSuccessListener { listResult ->
                        listResult.items.forEach { item ->
                            item.delete()
                        }
                    }
                } catch (e: Exception) {
                    errorReason.add("Error: profile" to e.message.toString())
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
                    errorReason.add("Error: User" to e.message.toString())
                    error = true
                }
            }

            deleteInfoJob.await()

            if (errorReason.isNotEmpty()) {
                firebaseHelper.logEvent(
                    listOf(
                        "type" to "RemoveAccount_Fail",
                        "api" to "Firebase",
                    ) + errorReason
                )
            }

            return@async !error
        }
        return result.await()
    }
}