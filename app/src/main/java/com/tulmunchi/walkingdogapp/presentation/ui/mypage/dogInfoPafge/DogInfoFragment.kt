package com.tulmunchi.walkingdogapp.presentation.ui.mypage.dogInfoPafge

import android.content.Intent
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
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage.ManageDogsFragment
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.ui.register.registerDogPage.RegisterDogActivity
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DogInfoFragment : Fragment() {
    private var _binding: FragmentDogInfoBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private var mainActivity: MainActivity? = null
    private var beforepage = ""

    @Inject
    lateinit var networkChecker: NetworkChecker

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(beforepage == "mypage") {
                goMypage()
            } else {
                goManage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDogInfoBinding.inflate(inflater, container, false)

        val userDogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("doginfo", Dog::class.java)
        } else {
            arguments?.getSerializable("doginfo") as? Dog
        } ?: return binding.root  // Dog가 없으면 빈 화면 반환

        beforepage = arguments?.getString("before", "mypage") ?: "mypage"

        binding.apply {
            dogInfo = userDogInfo
            btnBack.setOnClickListener {
                if (beforepage == "mypage") {
                    goMypage()
                } else {
                    goManage()
                }
            }

            btnSettingdog.setOnClickListener {
                context?.let { ctx ->
                    if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                        return@setOnClickListener
                    }
                    val registerDogIntent = Intent(ctx, RegisterDogActivity::class.java)
                    val walkRecords: ArrayList<WalkRecord> = ArrayList(mainViewModel.walkHistory.value?.get(userDogInfo.name) ?: emptyList())
                    registerDogIntent.putExtra("doginfo", userDogInfo)
                    registerDogIntent.putParcelableArrayListExtra(
                        "walkRecord",
                        walkRecords
                    )
                    startActivity(registerDogIntent)
                }
            }

            val dogImg = MainActivity.Companion.dogImageUrls[userDogInfo.name]
            if (dogImg != null) {
                context?.let { ctx ->
                    try {
                        Glide.with(ctx)
                            .load(dogImg)
                            .format(DecodeFormat.PREFER_ARGB_8888)
                            .override(300, 300)
                            .error(R.drawable.collection_003) // 에러 시 기본 이미지
                            .into(dogInfoImage)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        dogInfoImage.setImageResource(R.drawable.collection_003)
                    }
                }
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
    }


    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun goMypage() {
        mainActivity?.changeFragment(MyPageFragment())
    }

    private fun goManage() {
        mainActivity?.changeFragment(ManageDogsFragment())
    }
}