package com.example.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentDogInfoBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.userinfo.DogInfo
import com.example.walkingdogapp.userinfo.UserInfoViewModel

class DogInfoFragment : Fragment() {
    private var _binding: FragmentDogInfoBinding? = null
    private val binding get() = _binding!!

    private val myViewModel: UserInfoViewModel by activityViewModels()
    private lateinit var userdogInfo: DogInfo
    private lateinit var mainactivity: MainActivity

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMypage()
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

        userdogInfo = myViewModel.doginfo.value ?: DogInfo()

        binding.apply {
            btnBack.setOnClickListener {
                goMypage()
            }

            btnSettingdog.setOnClickListener {
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                registerDogIntent.putExtra("doginfo", userdogInfo)
                startActivity(registerDogIntent)
            }

            if (myViewModel.imgdrawble.value != null) {
                doginfoImage.setImageDrawable(myViewModel.imgdrawble.value)
            }

            doginfoName.text = userdogInfo.name
            doginfoBirth.text = userdogInfo.birth
            doginfoBreed.text = userdogInfo.breed
            doginfoGender.text = userdogInfo.gender
            doginfoNeutering.text = userdogInfo.neutering
            doginfoVaccination.text = userdogInfo.vaccination
            doginfoWeight.text = userdogInfo.weight.toString()
            doginfoFeature.text = userdogInfo.feature
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun goMypage() {
        mainactivity.changeFragment(MyPageFragment())
    }
}