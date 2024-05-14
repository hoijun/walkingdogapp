package com.example.walkingdogapp.registerinfo

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.ActivityRegisterDogBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.walking.SaveWalkDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class RegisterDogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterDogBinding
    private lateinit var doginfo: DogInfo
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()
    private var imguri: Uri? = null

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectgoMain()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            getExtension(uri)?.let { Log.d("extension", it) }
        }
        if (uri != null && getExtension(uri) == "jpg") {
            imguri = pressImage(uri,this)
            Log.d("PhotoPickerAAA", "Selected URI: $uri")
            binding.registerImage.setImageURI(uri)
        } else {
            return@registerForActivityResult
        }
    }

    private val storegePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val storageCode = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterDogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.onBackPressedDispatcher.addCallback(this, backPressCallback)


        val uid = auth.currentUser?.uid
        val userRef = db.getReference("Users")
        val storageRef = storage.getReference("$uid")

        val userdogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("doginfo", DogInfo::class.java)
        } else {
            intent.getSerializableExtra("doginfo") as DogInfo?
        }

        val walkRecords = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("walkRecord", WalkRecord::class.java) ?: arrayListOf()
        } else {
            intent.getParcelableArrayListExtra("walkRecord") ?: arrayListOf()
        }

        doginfo = userdogInfo?: DogInfo()
        val beforeName = doginfo.name

        if (doginfo.creationTime == 0L) {
            doginfo.creationTime = System.currentTimeMillis()
        }


        binding.apply {
            if (MainActivity.dogUriList[beforeName] != null) {
                Glide.with(this@RegisterDogActivity).load(MainActivity.dogUriList[beforeName])
                    .format(DecodeFormat.PREFER_RGB_565).override(100, 100).into(registerImage)
            }

            if (doginfo.name != "") {
                removeBtn.visibility = View.VISIBLE

                editName.setText(doginfo.name)
                editName.setSelection(doginfo.name.length)

                editWeight.setText(doginfo.weight.toString())
                editFeature.setText(doginfo.feature)

                editBreed.text = doginfo.breed
                editBirth.text = doginfo.birth

                when (doginfo.gender) {
                    "남" -> btnDogismale.setBackgroundColor(Color.DKGRAY)
                    "여" -> btnDogisfemale.setBackgroundColor(Color.DKGRAY)
                }

                when (doginfo.neutering) {
                    "예" -> btnNeuteryes.setBackgroundColor(Color.DKGRAY)
                    "아니요" -> btnNeuterno.setBackgroundColor(Color.DKGRAY)
                }

                when (doginfo.vaccination) {
                    "예" -> btnVaccyes.setBackgroundColor(Color.DKGRAY)
                    "아니요" -> btnVaccno.setBackgroundColor(Color.DKGRAY)
                }
            }

            btnDogismale.setOnClickListener {
                doginfo.gender = btnDogismale.text.toString()
                btnDogismale.setBackgroundColor(Color.DKGRAY)
                btnDogisfemale.setBackgroundColor(Color.GRAY)
            }
            btnDogisfemale.setOnClickListener {
                doginfo.gender = btnDogisfemale.text.toString()
                btnDogisfemale.setBackgroundColor(Color.DKGRAY)
                btnDogismale.setBackgroundColor(Color.GRAY)
            }

            btnNeuteryes.setOnClickListener {
                doginfo.neutering = btnNeuteryes.text.toString()
                btnNeuteryes.setBackgroundColor(Color.DKGRAY)
                btnNeuterno.setBackgroundColor(Color.GRAY)
            }
            btnNeuterno.setOnClickListener {
                doginfo.neutering = btnNeuterno.text.toString()
                btnNeuterno.setBackgroundColor(Color.DKGRAY)
                btnNeuteryes.setBackgroundColor(Color.GRAY)
            }

            btnVaccyes.setOnClickListener {
                doginfo.vaccination = btnVaccyes.text.toString()
                btnVaccyes.setBackgroundColor(Color.DKGRAY)
                btnVaccno.setBackgroundColor(Color.GRAY)
            }
            btnVaccno.setOnClickListener {
                doginfo.vaccination = btnVaccno.text.toString()
                btnVaccno.setBackgroundColor(Color.DKGRAY)
                btnVaccyes.setBackgroundColor(Color.GRAY)
            }

            editBreed.setOnClickListener {
                showDoglist()
            }

            editBirth.setOnClickListener {
                val cal = Calendar.getInstance()
                val dateCallback = DatePickerDialog.OnDateSetListener { view, year, month, day ->
                    val birth = "${year}/${month + 1}/${day}"
                    if (Constant.getAge(birth) == -1) {
                        Toast.makeText(
                            this@RegisterDogActivity,
                            "올바른 생일을 입력 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnDateSetListener
                    }
                    doginfo.birth = birth
                    binding.editBirth.text = birth
                }
                val datepicker = DatePickerDialog(
                    this@RegisterDogActivity,
                    dateCallback,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                datepicker.datePicker.maxDate = cal.timeInMillis
                datepicker.show()
            }

            registerImage.setOnClickListener {
                if (checkPermission(storegePermission, storageCode)) {
                    pickMedia.launch("image/jpeg")
                }
            }

            registerDog.setOnClickListener {
                doginfo.apply {
                    if (editName.text.toString() == "" || editBreed.text == "" || editBirth.text == "" || editWeight.text.toString() == ""
                        || gender == "" || neutering == "" || vaccination == ""
                    ) {
                        val builder = AlertDialog.Builder(this@RegisterDogActivity)
                        builder.setTitle("빈칸이 남아있어요.")
                        builder.setPositiveButton("확인", null)
                        builder.show()
                        return@setOnClickListener
                    }
                }

                doginfo.name = editName.text.toString()
                doginfo.weight = editWeight.text.toString().toInt()
                doginfo.feature = editFeature.text.toString()

                if (beforeName != doginfo.name) {
                    if (MainActivity.dogNameList.contains(doginfo.name)) {
                        Toast.makeText(
                            this@RegisterDogActivity,
                            "같은 이름의 강아지가 있어요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }

                if(MainActivity.dogNameList.size > 2 && beforeName == "") {
                    Toast.makeText(
                        this@RegisterDogActivity,
                        "3마리 까지 등록 가능해요!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val builder = AlertDialog.Builder(this@RegisterDogActivity)
                builder.setTitle("등록 할까요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            var error = false
                            binding.settingscreen.visibility = View.INVISIBLE
                            binding.waitImage.visibility = View.VISIBLE
                            lifecycleScope.launch { // 강아지 정보 등록
                                val dogInfoJob = async(Dispatchers.IO) {
                                    try {
                                        if(beforeName != "") { // 수정하는 경우
                                            userRef.child("$uid").child("dog").child(beforeName).removeValue().await()
                                        }
                                        userRef.child("$uid").child("dog").child(doginfo.name).setValue(doginfo).await()
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
                                        for(walkRecord in walkRecords) {
                                            val day = walkRecord.day + " " + walkRecord.startTime + " " + walkRecord.endTime
                                            userRef.child("$uid").child("dog").child(doginfo.name).child("walkdates").child(day)
                                                .setValue(SaveWalkDate(walkRecord.distance, walkRecord.time, walkRecord.coords, walkRecord.collections))
                                                .await()
                                        }
                                    }  catch (e: Exception) {
                                        error = true
                                        return@async
                                    }
                                }

                                val uploadJob = launch(Dispatchers.IO) {
                                    try {
                                        if (imguri != null) {
                                            storageRef.child("images").child(doginfo.name)
                                                .putFile(imguri!!).await()
                                        } else {
                                            if (MainActivity.dogUriList[beforeName] != null) {
                                                val tempUri = suspendCoroutine { continuation ->
                                                    val tempFile = File.createTempFile(
                                                        "temp",
                                                        ".jpg",
                                                        this@RegisterDogActivity.cacheDir
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

                                                storageRef.child("images").child(doginfo.name)
                                                    .putFile(tempUri).await()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        error = true
                                        return@launch
                                    }
                                }

                                uploadJob.join()

                                val deleteJob = launch(Dispatchers.IO) {
                                    try {
                                        if (beforeName == "") {
                                            return@launch
                                        }

                                        if (!MainActivity.dogNameList.contains(doginfo.name)) {
                                            storageRef.child("images").child(beforeName).delete()
                                                .await()
                                        }
                                    } catch (e: Exception) {
                                        error = true
                                        return@launch
                                    }
                                }

                                walkRecordJob.await()

                                if (error) {
                                    return@launch
                                }

                                deleteJob.join()
                                goMain()
                            }

                            if(error) {
                                Toast.makeText(this@RegisterDogActivity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                goMain()
                            }
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
                return@setOnClickListener
            }

            removeBtn.setOnClickListener {
                val builder = AlertDialog.Builder(this@RegisterDogActivity)
                builder.setTitle("정보를 삭제 하시겠어요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            lifecycleScope.launch { // 강아지 정보 등록
                                val removeDogInfoJob = async(Dispatchers.IO) {
                                    try {
                                        userRef.child("$uid").child("dog").child(beforeName).removeValue()
                                            .await()
                                    } catch (e: Exception) {
                                        return@async
                                    }
                                }

                                val removeDogImgJob = launch(Dispatchers.IO) {
                                    try {
                                        if (MainActivity.dogUriList[beforeName] != null) {
                                            storageRef.child("images").child(beforeName).delete()
                                                .await()
                                        }
                                    } catch (e: Exception) {
                                        return@launch
                                    }
                                }

                                removeDogInfoJob.await()
                                removeDogImgJob.join()
                                goMain()
                            }
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
            }

            btnBack.setOnClickListener {
                selectgoMain()
            }
        }
    }

    private fun pressImage(uri: Uri, context: Context): Uri {
        val bitmap = try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            return Uri.EMPTY
        }

        val tempFile = File.createTempFile("pressed_img", ".jpg", context.cacheDir)
        FileOutputStream(tempFile).use { fileOutputStream ->
            // 이미지 압축 및 크기 조정 로직
            val compressedBitmap =
                Bitmap.createScaledBitmap(bitmap, bitmap.width / 3, bitmap.height / 3, true)
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
        }
        return Uri.fromFile(tempFile)
    }

    private fun selectgoMain() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("나가시겠어요?")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    goMain()
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니요", null)
        builder.show()
    }

    private fun goMain() {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(backIntent)
        finish()
    }

    private fun showDoglist() {
        val dialog = DoglistDialog()
        dialog.onClickItemListener = DoglistDialog.OnClickItemListener {
            binding.editBreed.text = it
            doginfo.breed = it
        }
        dialog.show(supportFragmentManager, "doglist")
    }

    private fun getExtension(uri: Uri): String? { // 파일 확장자
        val contentResolver = contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun checkPermission(permissions : Array<out String>, code: Int) : Boolean{
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissions, code)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            storageCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("사진 설정을 위해 권한을 \n허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.data = Uri.fromParts("package", packageName, null)
                                startActivity(intent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                } else {
                    pickMedia.launch("image/jpeg")
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}