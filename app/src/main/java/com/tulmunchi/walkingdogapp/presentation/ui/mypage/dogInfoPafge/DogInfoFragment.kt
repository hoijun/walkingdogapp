package com.tulmunchi.walkingdogapp.presentation.ui.mypage.dogInfoPafge

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentDogInfoBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage.ManageDogsFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DogInfoFragment : Fragment() {
    private var _binding: FragmentDogInfoBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private var beforePage = ""

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var navigationManager: NavigationManager

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(beforePage == "myPage") {
                navigateToMyPage()
            } else {
                navigateToManage()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDogInfoBinding.inflate(inflater, container, false)

        val userDogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("dogInfo", Dog::class.java)
        } else {
            arguments?.getSerializable("dogInfo") as? Dog
        } ?: return binding.root  // Dog가 없으면 빈 화면 반환

        beforePage = arguments?.getString("before", "myPage") ?: "myPage"

        binding.apply {
            dogInfo = userDogInfo
            btnBack.setOnClickListener {
                if (beforePage == "myPage") {
                    navigateToMyPage()
                } else {
                    navigateToManage()
                }
            }

            btnUpdateDog.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }

                navigationManager.navigateTo(
                    NavigationState.WithoutBottomNav.RegisterDog(
                        dog = userDogInfo,
                        from = "dogInfo:$beforePage",  // "dogInfo:mypage" 또는 "dogInfo:manage"
                    )
                )
            }

            val dogImg = mainViewModel.dogImages.value?.get(userDogInfo.name)
            if (dogImg != null) {
                context?.let { ctx ->
                    try {
                        Glide.with(ctx)
                            .load(dogImg)
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .override(300, 300)
                            .error(R.drawable.collection_unobtained) // 에러 시 기본 이미지
                            .into(imageDogInfo)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        imageDogInfo.setImageResource(R.drawable.collection_unobtained)
                    }
                }
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun navigateToMyPage() {
        navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
    }

    private fun navigateToManage() {
        navigationManager.navigateTo(NavigationState.WithoutBottomNav.ManageDogs)
    }
}