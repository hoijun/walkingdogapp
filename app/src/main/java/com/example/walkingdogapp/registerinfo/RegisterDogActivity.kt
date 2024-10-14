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
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.LoadingDialogFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.utils.utils.NetworkManager
import com.example.walkingdogapp.utils.utils.Utils
import com.example.walkingdogapp.databinding.ActivityRegisterDogBinding
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.WalkDateInfo
import com.example.walkingdogapp.viewmodel.RegisterDogViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

@AndroidEntryPoint
class RegisterDogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterDogBinding
    private var dogInfo = DogInfo()
    private var imguri: Uri? = null
    private val registerDogViewModel: RegisterDogViewModel by viewModels()

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectGoMain()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            getExtension(uri)?.let { Log.d("extension", it) }
        }
        if (uri != null && getExtension(uri) == "jpg") {
            imguri = pressImage(uri,this)
            Glide.with(this@RegisterDogActivity).load(uri)
                .format(DecodeFormat.PREFER_RGB_565).override(150, 150).into(binding.registerImage)
        } else {
            return@registerForActivityResult
        }
    }

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        val userDogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("doginfo", DogInfo::class.java)
        } else {
            intent.getSerializableExtra("doginfo") as DogInfo?
        }

        val walkRecords = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("walkRecord", WalkDateInfo::class.java) ?: arrayListOf()
        } else {
            intent.getParcelableArrayListExtra("walkRecord") ?: arrayListOf()
        }

        dogInfo = userDogInfo?: DogInfo()
        val beforeName = dogInfo.name

        if (dogInfo.creationTime == 0L) {
            dogInfo.creationTime = System.currentTimeMillis()
        }


        binding.apply {
            dog = dogInfo
            beforeDogName = beforeName

            if (MainActivity.dogUriList[beforeName] != null) {
                Glide.with(this@RegisterDogActivity).load(MainActivity.dogUriList[beforeName])
                    .format(DecodeFormat.PREFER_RGB_565).override(150, 150).into(registerImage)
            }

            btnDogismale.setOnClickListener {
                dogInfo.gender = "남"
                dog = dogInfo
            }
            btnDogisfemale.setOnClickListener {
                dogInfo.gender = "여"
                dog = dogInfo
            }

            btnNeuteryes.setOnClickListener {
                dogInfo.neutering = "예"
                dog = dogInfo
            }
            btnNeuterno.setOnClickListener {
                dogInfo.neutering = "아니요"
                dog = dogInfo
            }

            btnVaccyes.setOnClickListener {
                dogInfo.vaccination = "예"
                dog = dogInfo
            }
            btnVaccno.setOnClickListener {
                dogInfo.vaccination = "아니요"
                dog = dogInfo
            }

            editBreed.setOnClickListener {
                showDogList()
            }

            editBirth.setOnClickListener {
                val cal = Calendar.getInstance()
                val dateCallback = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val birth = "${year}/${month + 1}/${day}"
                    if (Utils.getAge(birth) == -1) {
                        Toast.makeText(
                            this@RegisterDogActivity,
                            "올바른 생일을 입력 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnDateSetListener
                    }
                    dogInfo.birth = birth
                    dog = dogInfo
                }

                val datePicker = DatePickerDialog(
                    this@RegisterDogActivity,
                    dateCallback,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                datePicker.datePicker.maxDate = cal.timeInMillis
                datePicker.show()
            }

            registerImage.setOnClickListener {
                if (checkPermission(storagePermission, storageCode)) {
                    pickMedia.launch("image/jpeg")
                }
            }

            registerDog.setOnClickListener {
                if(!NetworkManager.checkNetworkState(this@RegisterDogActivity)) {
                    return@setOnClickListener
                }
                dogInfo.apply {
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

                if (beforeName != dogInfo.name) {
                    if (MainActivity.dogNameList.contains(dogInfo.name)) {
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
                            val loadingDialogFragment = LoadingDialogFragment()
                            loadingDialogFragment.show(this@RegisterDogActivity.supportFragmentManager, "loading")
                            lifecycleScope.launch { // 강아지 정보 등록
                                withContext(Dispatchers.Main) {
                                    val onFailed = registerDogViewModel.updateDogInfo(dogInfo, beforeName, imguri, walkRecords, MainActivity.dogUriList, MainActivity.dogNameList)
                                    loadingDialogFragment.dismiss()
                                    if (onFailed) {
                                        Toast.makeText(this@RegisterDogActivity, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                        goMain()
                                    }
                                    goMain()
                                }
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
                if (beforeName == "") return@setOnClickListener
                if(!NetworkManager.checkNetworkState(this@RegisterDogActivity)) {
                    return@setOnClickListener
                }
                val builder = AlertDialog.Builder(this@RegisterDogActivity)
                builder.setTitle("정보를 삭제 하시겠어요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            val loadingDialogFragment = LoadingDialogFragment()
                            loadingDialogFragment.show(this@RegisterDogActivity.supportFragmentManager, "loading")

                            lifecycleScope.launch { // 강아지 정보 등록
                                registerDogViewModel.removeDogInfo(beforeName, MainActivity.dogUriList)
                                withContext(Dispatchers.Main) {
                                    loadingDialogFragment.dismiss()
                                    goMain()
                                }
                            }
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
            }

            btnBack.setOnClickListener {
                selectGoMain()
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

    private fun selectGoMain() {
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

    private fun showDogList() {
        val dialog = DogListDialog()
        dialog.onClickItemListener = DogListDialog.OnClickItemListener {
            binding.editBreed.text = it
            dogInfo.breed = it
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

    object RegisterDogBindingAdapter {
        private fun setButtonBackground(btn: Button, state: String) {
            val color = if (state == btn.text.toString()) "#ff444444" else "#ff888888"
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * btn.resources.displayMetrics.density  // 10dp를 픽셀로 변환
                setColor(Color.parseColor(color))
            }
            btn.background = shape
        }

        @BindingAdapter("dogGender")
        @JvmStatic
        fun setGenderBackground(btn: Button, gender: String) {
            setButtonBackground(btn, gender)
        }

        @BindingAdapter("dogNeutering")
        @JvmStatic
        fun setNeuteringBackground(btn: Button, neutering: String) {
            setButtonBackground(btn, neutering)
        }

        @BindingAdapter("dogVaccination")
        @JvmStatic
        fun setVaccinationBackground(btn: Button, vaccination: String) {
            setButtonBackground(btn, vaccination)
        }

        @BindingAdapter("dogNameLength")
        @JvmStatic
        fun setSelection(editText: EditText, length: Int) {
            editText.setSelection(length)
        }
    }
}