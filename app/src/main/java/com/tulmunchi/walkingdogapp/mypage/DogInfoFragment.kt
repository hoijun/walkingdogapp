package com.tulmunchi.walkingdogapp.mypage

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
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.databinding.FragmentDogInfoBinding
import com.tulmunchi.walkingdogapp.registerinfo.RegisterDogActivity
import com.tulmunchi.walkingdogapp.datamodel.DogInfo
import com.tulmunchi.walkingdogapp.datamodel.WalkDateInfo
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel

class DogInfoFragment : Fragment() {
    private var _binding: FragmentDogInfoBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var mainactivity: MainActivity
    private var beforepage = ""

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
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDogInfoBinding.inflate(inflater,container, false)

        val userDogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("doginfo", DogInfo::class.java)?: DogInfo()
        } else {
            (arguments?.getSerializable("doginfo") ?: DogInfo()) as DogInfo
        }

        beforepage = arguments?.getString("before", "mypage")?: "mypage"

        binding.apply {
            dogInfo = userDogInfo
            btnBack.setOnClickListener {
                if(beforepage == "mypage") {
                    goMypage()
                } else {
                    goManage()
                }
            }

            btnSettingdog.setOnClickListener {
                if(!NetworkManager.checkNetworkState(requireContext()) || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                val walkDateInfoArrayLists: ArrayList<WalkDateInfo> = (mainViewModel.walkDates.value?.get(userDogInfo.name) ?: mutableListOf()) as ArrayList<WalkDateInfo>
                registerDogIntent.putExtra("doginfo", userDogInfo)
                registerDogIntent.putParcelableArrayListExtra("walkRecord", walkDateInfoArrayLists)
                startActivity(registerDogIntent)
            }

            if (mainViewModel.dogsImg.value?.get(userDogInfo.name) != null) {
                Glide.with(requireContext()).load(mainViewModel.dogsImg.value?.get(userDogInfo.name))
                    .format(DecodeFormat.PREFER_RGB_565).override(500, 500).into(doginfoImage)
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goMypage() {
        mainactivity.changeFragment(MyPageFragment())
    }

    private fun goManage() {
        mainactivity.changeFragment(ManageDogsFragment())
    }
}