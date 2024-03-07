package com.example.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentMyPageBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.registerinfo.RegisterUserActivity
import com.example.walkingdogapp.userinfo.DogInfo
import com.example.walkingdogapp.userinfo.UserInfo
import com.example.walkingdogapp.userinfo.Walkdate
import com.example.walkingdogapp.userinfo.totalWalkInfo
import com.example.walkingdogapp.userinfo.userInfoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class MyPageFragment : Fragment() {
    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var userdogInfo: DogInfo
    private lateinit var userInfo: UserInfo
    private lateinit var totalwalkInfo: totalWalkInfo
    private lateinit var walkdates: List<Walkdate>
    private lateinit var mainactivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = activity as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
        MainActivity.preFragment = "Mypage"  // 다른 액티비티로 이동 할 때 마이페이지에서 이동을 표시
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater,container, false)
        userdogInfo = myViewModel.doginfo.value ?: DogInfo()
        userInfo = myViewModel.userinfo.value ?: UserInfo()
        totalwalkInfo = myViewModel.totalwalkinfo.value ?: totalWalkInfo()
        walkdates = myViewModel.walkDates.value ?: listOf<Walkdate>()

        binding.apply {
            btnSetting.setOnClickListener {
                mainactivity.changeFragment(SettingFragment())
            }

            menuDogInfo.setOnClickListener {
                if(userdogInfo.name == "") {
                    Toast.makeText(requireContext(), "먼저 강아지 정보를 설정해주세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                mainactivity.changeFragment(DogInfoFragment())
            }

            modifydoginfo.setOnClickListener {
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                startActivity(registerDogIntent)
            }

            modifyuserinfo.setOnClickListener {
                val registerUserIntent = Intent(requireContext(), RegisterUserActivity::class.java)
                startActivity(registerUserIntent)
            }

            managepictures.setOnClickListener {

            }

            menuWalkinfo.setOnClickListener {
                mainactivity.changeFragment(WalkInfoFragment(listOf<String>()))
            }

            if(userInfo.name != "") {
                menuUsername.text = "${userInfo.name} 님"
            }

            if(userdogInfo.name != ""){
                menuDogname.text = userdogInfo.name
                menuDogfeature.text = "${getAge(userdogInfo.birth)}살/ ${userdogInfo.weight}kg / ${userdogInfo.breed}"
                if (myViewModel.imgdrawble.value != null) {
                    menuDogimg.setImageDrawable(myViewModel.imgdrawble.value)
                }
            }

            menuDistance.text = getString(R.string.totaldistance, totalwalkInfo.totaldistance / 1000.0)
            walkDistance.text = getString(R.string.totaldistance, totalwalkInfo.totaldistance / 1000.0)
            walkTime.text =  "${(totalwalkInfo.totaltime / 60)}분"
            walkCount.text = "${walkdates.size}회"
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getAge(date: String): Int {
        val currentDate = Calendar.getInstance()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val birthDate = dateFormat.parse(date)
        val calBirthDate = Calendar.getInstance().apply { time = birthDate }

        var age = currentDate.get(Calendar.YEAR) - calBirthDate.get(Calendar.YEAR)
        if (currentDate.get(Calendar.DAY_OF_YEAR) < calBirthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }
}