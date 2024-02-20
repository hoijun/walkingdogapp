package com.example.walkingdogapp

import android.Manifest
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.walkingdogapp.databinding.ActivitySettingDogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar


class SettingDogActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingDogBinding
    private val doginfo = DogInfo()
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.database
    private val storage = FirebaseStorage.getInstance()
    private lateinit var imguri: Uri

    private val BackPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectgoHome()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            getExtension(uri)?.let { Log.d("extension", it) }
        }
        if (uri != null && getExtension(uri) == "jpg") {
            imguri = uri
            Log.d("PhotoPickerAAA", "Selected URI: $uri")
            binding.settingImage.setImageURI(uri)
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
        binding = ActivitySettingDogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.onBackPressedDispatcher.addCallback(this, BackPressCallback)

        binding.apply {
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
                    val birth = "${year}/${month}/${day}"
                    doginfo.birth = birth
                    binding.editBirth.text = birth
                }
                DatePickerDialog(this@SettingDogActivity, dateCallback, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            }

            settingImage.setOnClickListener {
                if(checkPermission(storegePermission, storageCode)) {
                    pickMedia.launch("image/jpeg")
                }
            }

            registerDog.setOnClickListener {
                doginfo.apply {
                    if (editName.text.toString() == "" || editBreed.text == "" || editBirth.text == "" || editWeight.text.toString() == ""
                        || gender == "" || neutering == "" || vaccination == "") {
                        val builder = AlertDialog.Builder(this@SettingDogActivity)
                        builder.setTitle("빈칸이 남아있어요.")
                        builder.setPositiveButton("확인", null)
                        builder.show()
                        return@setOnClickListener
                    }

                    doginfo.name = editName.text.toString()
                    doginfo.weight = editWeight.text.toString().toInt()
                    doginfo.feature = editFeature.text.toString()

                    val builder = AlertDialog.Builder(this@SettingDogActivity)
                    builder.setTitle("등록 할까요?")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                binding.settingscreen.visibility = View.INVISIBLE
                                binding.waitImage.visibility = View.VISIBLE
                                val uid = auth.currentUser?.uid
                                val userRef = db.getReference("Users")
                                val storgeRef = storage.getReference("$uid")
                                lifecycleScope.launch {
                                    val doginfoJob = async(Dispatchers.IO) {
                                        userRef.child("$uid").child("dog").setValue(doginfo).await()
                                    }
                                    val profileUriJob = async(Dispatchers.IO) {
                                        storgeRef.child("images").child("profileimg")
                                            .putFile(imguri).await()
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
            }

            btnBack.setOnClickListener {
                selectgoHome()
            }
        }
    }

    private fun selectgoHome() {
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

    private fun getExtension(uri: Uri): String? {
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