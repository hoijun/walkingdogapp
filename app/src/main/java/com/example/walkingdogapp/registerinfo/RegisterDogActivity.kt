package com.example.walkingdogapp.registerinfo

import android.Manifest
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.ActivityRegisterDogBinding
import com.example.walkingdogapp.userinfo.DogInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.log10


class RegisterDogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterDogBinding
    private lateinit var doginfo: DogInfo
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()
    private var imguri: Uri? = null

    private val BackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectgoMain()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            getExtension(uri)?.let { Log.d("extension", it) }
        }
        if (uri != null && getExtension(uri) == "jpg") {
            imguri = PressImage(uri)
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

        this.onBackPressedDispatcher.addCallback(this, BackPressCallback)

        val userdogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("doginfo", DogInfo::class.java)
        } else {
            intent.getSerializableExtra("doginfo") as DogInfo?
        }

        doginfo = userdogInfo ?: DogInfo()

        binding.apply {
            if(MainActivity.profileImgUri != Uri.EMPTY) {
                Glide.with(this@RegisterDogActivity).load(MainActivity.profileImgUri).into(registerImage)
            }
            if (doginfo.name != "") {
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
                    if (getAge(birth) == -1) {
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

                val builder = AlertDialog.Builder(this@RegisterDogActivity)
                builder.setTitle("등록 할까요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            binding.settingscreen.visibility = View.INVISIBLE
                            binding.waitImage.visibility = View.VISIBLE
                            val uid = auth.currentUser?.uid
                            val userRef = db.getReference("Users")
                            val storgeRef = storage.getReference("$uid")
                            lifecycleScope.launch { // 강아지 정보 등록
                                val doginfoJob = async(Dispatchers.IO) {
                                    try {
                                        userRef.child("$uid").child("dog").setValue(doginfo)
                                            .await()
                                    } catch (e: Exception) {
                                        return@async
                                    }
                                }

                                val profileUriJob = async(Dispatchers.IO) {
                                    try {
                                        if (imguri == null) {
                                            return@async
                                        } else {
                                            storgeRef.child("images").child("profileimg")
                                                .putFile(imguri!!).await()
                                        }
                                    } catch (e: Exception) {
                                        return@async
                                    }
                                }

                                doginfoJob.await()
                                profileUriJob.await()

                                goHome()
                            }
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
                return@setOnClickListener
            }

            btnBack.setOnClickListener {
                selectgoMain()
            }
        }
    }

    private fun PressImage(uri: Uri): Uri {
        val bitmap = try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            return Uri.EMPTY
        }

        val pressBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 3, bitmap.height / 3, true)
        val byteArrayOutputStream = ByteArrayOutputStream()
        pressBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)

        val tempFile = File.createTempFile("pressed_img", ".jpg", this.cacheDir)
        val fileOutputStream = FileOutputStream(tempFile)
        fileOutputStream.write(byteArrayOutputStream.toByteArray())
        fileOutputStream.close()

        return Uri.fromFile(tempFile)
    }

    private fun selectgoMain() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("나가시겠어요?")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    goHome()
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니요", null)
        builder.show()
    }

    private fun getAge(date: String): Int {
        val currentDate = Calendar.getInstance()
        var age = -1

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val birthDate = dateFormat.parse(date)
        val calBirthDate = Calendar.getInstance().apply { time = birthDate }

        if (calBirthDate.time < currentDate.time) {
            age = currentDate.get(Calendar.YEAR) - calBirthDate.get(Calendar.YEAR)
            if (currentDate.get(Calendar.DAY_OF_YEAR) < calBirthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
        }
        return age
    }

    private fun goHome() {
        val backIntent = Intent(this, MainActivity::class.java)
        backIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(backIntent)
        finish()
    }

    private fun showDoglist() {
        val dialog = DoglistDialog(this) {
            binding.editBreed.text = it
            doginfo.breed = it
        }
        dialog.show()
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