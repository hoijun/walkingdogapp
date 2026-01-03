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
import com.tulmunchi.walkingdogapp.databinding.FragmentRegisterDogBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.RegisterDogViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RegisterDogFragment : Fragment() {
    private var _binding: FragmentRegisterDogBinding? = null
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

    private enum class ResultOfRegisterDog {
        IsNotUpdatedDog, IsUpdatedDog, IsDeletedDog
    }

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
                    .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(binding.imageProfile)
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
        _binding = FragmentRegisterDogBinding.inflate(inflater, container, false)
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

        // 복사본을 사용하여 원본 객체가 변경되지 않도록 함
        userDog?.let { dog = it.copy() }
        beforeName = dog.name
        setUpViewModelObservers()

        binding.apply {
            dogInfo = dog
            beforeDogName = beforeName

            val dogImage = mainViewModel.dogImages.value?.get(beforeName)
            if (dogImage != null) {
                Glide.with(this@RegisterDogFragment).load(dogImage)
                    .format(DecodeFormat.PREFER_ARGB_8888).override(300, 300).into(imageProfile)
            }

            btnDogIsMale.setOnClickListener {
                dog = dog.copy(gender = "남")
                binding.dogInfo = dog
            }

            btnDogIsFemale.setOnClickListener {
                dog = dog.copy(gender = "여")
                binding.dogInfo = dog
            }

            btnNeuterIsYes.setOnClickListener {
                dog = dog.copy(neutering = "예")
                binding.dogInfo = dog
            }

            btnNeuterIsNo.setOnClickListener {
                dog = dog.copy(neutering = "아니요")
                binding.dogInfo = dog
            }

            btnVaccIsYes.setOnClickListener {
                dog = dog.copy(vaccination = "예")
                binding.dogInfo = dog
            }

            btnVaccIsNo.setOnClickListener {
                dog = dog.copy(vaccination = "아니요")
                binding.dogInfo = dog
            }

            editBreed.setOnSingleClickListener {
                showDogList()
            }

            editBirth.setOnSingleClickListener {
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

            btnRegisterProfileImg.setOnSingleClickListener {
                if (checkPermission(storagePermission, storageCode)) {
                    pickMedia.launch("image/jpeg")
                }
            }

            btnRegisterDog.setOnClickListener {
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
                    val dialog = SelectDialog.newInstance(title = "빈칸이 남아있어요.")
                    dialog.show(parentFragmentManager, "emptyField")
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

                val dialog = SelectDialog.newInstance(title = "등록 할까요?", showNegativeButton = true)
                dialog.onConfirmListener = SelectDialog.OnConfirmListener {
                    if (beforeName == "") {
                        registerDogViewModel.addDog(dog, imgUri?.toString())
                    } else {
                        registerDogViewModel.updateDog(
                            oldName = beforeName,
                            dog = dog,
                            imageUriString = imgUri?.toString(),
                            walkRecords = walkRecords,
                            existingDogNames = dogNames
                        )
                    }
                }
                dialog.show(parentFragmentManager, "register")
                return@setOnClickListener
            }

            removeBtn.setOnClickListener {
                if (beforeName == "") return@setOnClickListener
                if(!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                val dialog = SelectDialog.newInstance(title = "정보를 삭제 하시겠어요?", showNegativeButton = true)
                dialog.onConfirmListener = SelectDialog.OnConfirmListener {
                    registerDogViewModel.deleteDog(beforeName)
                }
                dialog.show(parentFragmentManager, "delete")
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
                navigateToMain(ResultOfRegisterDog.IsNotUpdatedDog)
            } else {
                navigateToMain(ResultOfRegisterDog.IsUpdatedDog)
            }
        }

        registerDogViewModel.dogDeleted.observe(viewLifecycleOwner) { isDeleted ->
            if (!isDeleted) {
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                navigateToMain(ResultOfRegisterDog.IsNotUpdatedDog)
            } else {
                navigateToMain(ResultOfRegisterDog.IsDeletedDog)
            }
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
        val dialog = SelectDialog.newInstance(title = "등록을 취소할까요?", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            navigateToMain(ResultOfRegisterDog.IsNotUpdatedDog)
        }
        dialog.show(parentFragmentManager, "back_navigation_dialog")
    }

    private fun navigateToMain(result: ResultOfRegisterDog) {
        when (result) {
            ResultOfRegisterDog.IsNotUpdatedDog -> { }
            ResultOfRegisterDog.IsUpdatedDog -> {
                mainViewModel.updateDog(dog, beforeName, imgUri?.toString())
            }

            ResultOfRegisterDog.IsDeletedDog -> {
                mainViewModel.deleteDog(dog.name)
            }
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
                    val dialog = SelectDialog.newInstance(title = "사진 설정을 위해 권한을 \n허용으로 해주세요!", showNegativeButton = true)
                    dialog.onConfirmListener = SelectDialog.OnConfirmListener {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent.data = Uri.fromParts("package", requireActivity().packageName, null)
                        startActivity(intent)
                    }
                    dialog.show(parentFragmentManager, "permission")
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
            val isSelected = state == btn.text.toString()
            val density = btn.resources.displayMetrics.density
            
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density

                if (isSelected) {
                    // 선택됨: 회색 배경 채우기
                    setColor("#e4e4e4".toColorInt())
                    setStroke((1 * density).toInt(), "#e4e4e4".toColorInt())
                } else {
                    // 선택 안됨: 투명 배경 + 테두리만
                    setColor(android.graphics.Color.TRANSPARENT)
                    setStroke((1 * density).toInt(), "#e4e4e4".toColorInt())
                }
            }
            
            btn.background = shape
            // 선택된 버튼은 진한 검정색, 선택 안된 버튼은 회색 텍스트
            btn.setTextColor(if (isSelected) "#000000".toColorInt() else "#888888".toColorInt())
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