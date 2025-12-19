package com.tulmunchi.walkingdogapp.presentation.ui.register.registerDogPage

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.ActivityRegisterDogBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.RegisterDogViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RegisterDogFragment : Fragment() {
    private var _binding: ActivityRegisterDogBinding? = null
    private val binding get() = _binding!!

    private var dog = Dog()
    private var beforeName = ""
    private var imgUri: Uri? = null
    private var from: String = "home"  // 어디서 왔는지 기억
    private val registerDogViewModel: RegisterDogViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var navigationManager: NavigationManager

    private var loadingDialog: LoadingDialog? = null

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectNavigateToMain()
        }
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            getExtension(uri)?.let { Log.d("extension", it) }
        }
        if (uri != null && getExtension(uri) == "jpg") {
            val processedUri = pressImage(uri, requireContext())
            if (processedUri != null) {
                imgUri = processedUri
                Glide.with(this@RegisterDogFragment).load(uri)
                    .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(binding.registerImage)
            }
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = ActivityRegisterDogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        loadingDialog = loadingDialogFactory.create(parentFragmentManager)

        val userDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("dogInfo", Dog::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getSerializable("dogInfo") as? Dog
        }

        val walkRecords = userDog?.let { dog -> mainViewModel.walkHistory.value?.get(dog.name) ?: emptyList() } ?: emptyList()

        from = arguments?.getString("from") ?: "home"  // 어디서 왔는지 읽기

        userDog?.let { dog = it }
        beforeName = dog.name
        setUpViewModelObservers()

        binding.apply {
            dogInfo = dog
            beforeDogName = beforeName

            val dogImage = mainViewModel.dogImages.value?.get(beforeName)
            if (dogImage != null) {
                Glide.with(this@RegisterDogFragment).load(dogImage)
                    .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(registerImage)
            }

            btnDogismale.setOnClickListener {
                dog = dog.copy(gender = "남")
                binding.dogInfo = dog
            }

            btnDogisfemale.setOnClickListener {
                dog = dog.copy(gender = "여")
                binding.dogInfo = dog
            }

            btnNeuteryes.setOnClickListener {
                dog = dog.copy(neutering = "예")
                binding.dogInfo = dog
            }

            btnNeuterno.setOnClickListener {
                dog = dog.copy(neutering = "아니요")
                binding.dogInfo = dog
            }

            btnVaccyes.setOnClickListener {
                dog = dog.copy(vaccination = "예")
                binding.dogInfo = dog
            }

            btnVaccno.setOnClickListener {
                dog = dog.copy(vaccination = "아니요")
                binding.dogInfo = dog
            }

            editBreed.setOnClickListener {
                showDogList()
            }

            editBirth.setOnClickListener {
                val cal = Calendar.getInstance()
                val dateCallback = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                    val birth = "${year}/${month + 1}/${day}"
                    if (DateUtils.getAge(birth) == -1) {
                        Toast.makeText(
                            requireContext(),
                            "올바른 생일을 입력 해주세요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnDateSetListener
                    }
                    dog = dog.copy(birth = birth)
                    binding.dogInfo = dog
                }

                val datePicker = DatePickerDialog(
                    requireContext(),
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
                if(!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }

                // 강아지 정보 업데이트
                dog = dog.copy(
                    name = editName.text.toString(),
                    weight = editWeight.text.toString(),
                    feature = editFeature.text.toString()
                )

                if (dog.name.isEmpty() || dog.breed.isEmpty() || dog.birth.isEmpty() ||
                    dog.weight.isEmpty() || dog.gender.isEmpty() || dog.neutering.isEmpty() ||
                    dog.vaccination.isEmpty()
                ) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("빈칸이 남아있어요.")
                    builder.setPositiveButton("확인", null)
                    builder.show()
                    return@setOnClickListener
                }

                val dogNames = mainViewModel.dogNames.value ?: emptyList()

                if (beforeName != dog.name) {
                    if (dogNames.contains(dog.name)) {
                        Toast.makeText(
                            requireContext(),
                            "같은 이름의 강아지가 있어요!",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }

                if(dogNames.size > 2 && beforeName == "") {
                    Toast.makeText(
                        requireContext(),
                        "3마리 까지 등록 가능해요!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("등록 할까요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            registerDogViewModel.updateDog(
                                oldName = beforeName,
                                dog = dog,
                                imageUriString = imgUri?.toString(),
                                walkRecords = walkRecords,
                                existingDogNames = dogNames
                            )
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
                if(!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("정보를 삭제 하시겠어요?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            mainViewModel.removeDog(beforeName)
                            registerDogViewModel.deleteDog(beforeName)
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니요", null)
                builder.show()
            }

            btnBack.setOnClickListener {
                selectNavigateToMain()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setUpViewModelObservers() {
        registerDogViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showLoadingFragment()
            } else {
                hideLoadingDialog()
            }
        }

        // Observe ViewModel
        registerDogViewModel.dogUpdated.observe(viewLifecycleOwner) { isUpdated ->
            if (!isUpdated) {
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }

            navigateToMain(true)
        }

        registerDogViewModel.dogDeleted.observe(viewLifecycleOwner) { isDeleted ->
            if (!isDeleted) {
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }

            navigateToMain(false)
        }
    }

    private fun pressImage(uri: Uri, context: Context): Uri? {
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }

            val maxSize = 1400f

            val ratio = if (maxOf(bitmap.width, bitmap.height) > maxSize) {
                maxSize / maxOf(bitmap.width, bitmap.height)
            } else {
                1.0f
            }

            val targetWidth = (bitmap.width * ratio).toInt()
            val targetHeight = (bitmap.height * ratio).toInt()

            val scaledBitmap = bitmap.scale(targetWidth, targetHeight)

            // scale이 원본을 반환할 수도 있으므로, 다른 경우에만 원본 recycle
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }

            val tempFile = File.createTempFile("profile_img", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { fileOutputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileOutputStream)
            }

            // compress 완료 후 scaledBitmap recycle
            scaledBitmap.recycle()

            return Uri.fromFile(tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("savepoint", e.message.toString())
            Toast.makeText(context, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun selectNavigateToMain() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("나가시겠어요?")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    navigateToMain(false)
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니요", null)
        builder.show()
    }

    private fun navigateToMain(isUpdateImg: Boolean) {
        if (isUpdateImg) {
            mainViewModel.updateDog(dog, beforeName, imgUri?.toString())
        }

        // 어디서 왔는지에 따라 다른 화면으로 돌아가기
        when {
            from.startsWith("dogInfo") -> {
                // DogInfo에서 왔으면 원래 페이지(MyPage 또는 ManageDogs)로 이동
                val before = when (from) {
                    "dogInfo:myPage" -> "myPage"
                    "dogInfo:manage" -> "manage"
                    else -> "myPage"
                }
                if (before == "myPage") {
                    navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
                } else {
                    navigationManager.navigateTo(NavigationState.WithoutBottomNav.ManageDogs)
                }
            }
            from == "home" -> navigationManager.navigateTo(NavigationState.WithBottomNav.Home)
            from == "myPage" -> navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
            from == "manage" -> navigationManager.navigateTo(NavigationState.WithoutBottomNav.ManageDogs)
            else -> navigationManager.navigateTo(NavigationState.WithBottomNav.Home)
        }
    }

    private fun showDogList() {
        val dialog = DogListDialog()
        dialog.onClickItemListener = DogListDialog.OnClickItemListener {
            binding.editBreed.text = it
            dog = dog.copy(breed = it)
            binding.dogInfo = dog
        }
        dialog.show(parentFragmentManager, "doglist")
    }

    private fun getExtension(uri: Uri): String? {
        val contentResolver = requireContext().contentResolver
        val mimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    private fun showLoadingFragment() {
        if (isDetached) {
            return
        }
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        if (isDetached) {
            return
        }
        loadingDialog?.dismiss()
    }

    private fun checkPermission(permissions : Array<String>, code: Int) : Boolean{
        return if (!permissionHandler.checkPermissions(requireActivity(), permissions)) {
            permissionHandler.requestPermissions(requireActivity(), permissions, code)
            false
        } else {
            true
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            storageCode -> {
                if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("사진 설정을 위해 권한을 \n허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.data = Uri.fromParts("package", requireActivity().packageName, null)
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
        @Suppress("DEPRECATION")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    object RegisterDogBindingAdapter {
        private fun setButtonBackground(btn: Button, state: String) {
            val color = if (state == btn.text.toString()) "#ff444444" else "#ff888888"
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * btn.resources.displayMetrics.density
                setColor(color.toColorInt())
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