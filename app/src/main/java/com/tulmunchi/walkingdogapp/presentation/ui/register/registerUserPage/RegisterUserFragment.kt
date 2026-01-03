package com.tulmunchi.walkingdogapp.presentation.ui.register.registerUserPage

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.graphics.toColorInt
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentRegisterUserBinding
import com.tulmunchi.walkingdogapp.domain.model.User
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialog
import com.tulmunchi.walkingdogapp.presentation.core.dialog.LoadingDialogFactory
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.RegisterUserViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RegisterUserFragment : Fragment() {
    private var _binding: FragmentRegisterUserBinding? = null
    private val binding get() = _binding!!

    private var from: String = "myPage"  // 어디서 왔는지 기억
    private var currentUser: User = User("", "", "", "")
    private val registerUserViewModel: RegisterUserViewModel by viewModels()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var loadingDialogFactory: LoadingDialogFactory

    @Inject
    lateinit var navigationManager: NavigationManager

    private var loadingDialog: LoadingDialog? = null

    private val backPressCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            selectGoMain()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressCallback)

        loadingDialog = loadingDialogFactory.create(parentFragmentManager)

        from = arguments?.getString("from") ?: "myPage"  // 어디서 왔는지 읽기

        setupViewModelObservers()

        binding.apply {
            mainViewModel.user.value?.apply {
                currentUser = this.copy()
            }

            user = currentUser

            btnUserIsFemale.setOnClickListener {
                currentUser = currentUser.copy(gender = "여")
                user = currentUser
            }

            btnUserIsMale.setOnClickListener {
                currentUser = currentUser.copy(gender = "남")
                user = currentUser
            }

            editUserBirth.setOnClickListener {
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
                    currentUser = currentUser.copy(birth = birth)
                    user = currentUser
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

            registerUser.setOnClickListener {
                if (!networkChecker.isNetworkAvailable()) {
                    return@setOnClickListener
                }
                currentUser.apply {
                    if (editName.text.toString() == "" || birth == "" || gender == "") {
                        val dialog = SelectDialog.newInstance(title = "빈칸이 남아있어요.")
                        dialog.show(parentFragmentManager, "emptyField")
                        return@setOnClickListener
                    }
                }

                val dialog = SelectDialog.newInstance(title = "등록 할까요?", showNegativeButton = true)
                dialog.onConfirmListener = SelectDialog.OnConfirmListener {
                    registerUserViewModel.updateUserInfo(currentUser)
                }
                dialog.show(parentFragmentManager, "register")
                return@setOnClickListener
            }

            btnBack.setOnClickListener {
                selectGoMain()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupViewModelObservers() {
        registerUserViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) showLoadingFragment() else hideLoadingDialog()
        }

        registerUserViewModel.userUpdated.observe(viewLifecycleOwner) { userUpdated ->
            if (!userUpdated) {
                Toast.makeText(requireContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } else {
                mainViewModel.updateUser(currentUser)
            }

            navigateToMyPage()
        }
    }

    private fun selectGoMain() {
        val dialog = SelectDialog.newInstance(title = "등록을 취소할까요?", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            navigateToMyPage()
        }
        dialog.show(parentFragmentManager, "back_navigation_dialog")
    }

    private fun navigateToMyPage() {
        // 어디서 왔는지에 따라 다른 화면으로 돌아가기 (보통은 mypage)
        when (from) {
            "myPage" -> navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
            else -> navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
        }
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

    object RegisterUserBindingAdapter {
        @BindingAdapter("userGender")
        @JvmStatic
        fun setButtonBackground(btn: Button, gender: String) {
            val isSelected = gender == btn.text.toString()
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

        @BindingAdapter("userNameLength")
        @JvmStatic
        fun setSelection(editText: EditText, length: Int) {
            editText.setSelection(length)
        }
    }
}