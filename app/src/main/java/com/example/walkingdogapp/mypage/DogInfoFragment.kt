package com.example.walkingdogapp.mypage

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
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentDogInfoBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.viewmodel.UserInfoViewModel

class DogInfoFragment : Fragment() {
    private var _binding: FragmentDogInfoBinding? = null
    private val binding get() = _binding!!

    private val myViewModel: UserInfoViewModel by activityViewModels()
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

        val userdogInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("doginfo", DogInfo::class.java)?: DogInfo()
        } else {
            (arguments?.getSerializable("doginfo") ?: DogInfo()) as DogInfo
        }

        beforepage = arguments?.getString("before", "mypage")?: "mypage"

        binding.apply {
            btnBack.setOnClickListener {
                if(beforepage == "mypage") {
                    goMypage()
                } else {
                    goManage()
                }
            }

            btnSettingdog.setOnClickListener {
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                registerDogIntent.putExtra("doginfo", userdogInfo)
                startActivity(registerDogIntent)
            }

            doginfoName.text = userdogInfo.name
            doginfoBirth.text = userdogInfo.birth
            doginfoBreed.text = userdogInfo.breed
            doginfoGender.text = userdogInfo.gender
            doginfoNeutering.text = userdogInfo.neutering
            doginfoVaccination.text = userdogInfo.vaccination
            doginfoWeight.text = userdogInfo.weight.toString()
            doginfoFeature.text = userdogInfo.feature

            if (myViewModel.dogsimg.value?.get(userdogInfo.name) != null) {
                Glide.with(requireContext()).load(myViewModel.dogsimg.value?.get(userdogInfo.name))
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