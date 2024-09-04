package com.example.walkingdogapp.mainhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.NetworkManager
import com.example.walkingdogapp.alarm.SettingAlarmFragment
import com.example.walkingdogapp.databinding.FragmentHomeBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.example.walkingdogapp.walking.WalkingActivity
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val userInfoViewModel: UserInfoViewModel by activityViewModels()
    private lateinit var builder: AlertDialog.Builder
    private lateinit var mainactivity: MainActivity
    private val selectedDogList = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE

        if (!NetworkManager.checkNetworkState(requireContext())) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("인터넷을 연결해주세요!")
            builder.setPositiveButton("네", null)
            builder.setCancelable(false)
            builder.show()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        MainActivity.preFragment = "Home" // 다른 액티비티로 이동 할 때 홈에서 이동을 표시
        builder = AlertDialog.Builder(requireContext())

        binding.apply {
            viewmodel = userInfoViewModel
            lifecycleOwner = requireActivity()
            selectedDogs = selectedDogList.joinToString(", ")

            refresh.apply {
                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        userInfoViewModel.observeUser()
                    }
                }
                userInfoViewModel.successGetData.observe(requireActivity()) {
                    refresh.isRefreshing = false
                }
            }

            homeDogsViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    super.onPageScrollStateChanged(state)
                    refresh.setEnabled(state == ViewPager2.SCROLL_STATE_IDLE)
                }
            })

            btnAlarm.setOnClickListener {
                if(!NetworkManager.checkNetworkState(requireContext())) {
                    return@setOnClickListener
                }
                mainactivity.changeFragment(SettingAlarmFragment())
            }

            val dogsList = userInfoViewModel.dogsInfo.value?: listOf()
            val homeDogListAdapter = HomeDogListAdapter(dogsList, userInfoViewModel.isSuccessGetData())
            homeDogListAdapter.onClickDogListener = HomeDogListAdapter.OnClickDogListener { dogName ->
                if(selectedDogList.contains(dogName)) {
                    selectedDogList.remove(dogName)
                } else {
                    selectedDogList.add(dogName)
                }

                selectedDogs = selectedDogList.joinToString(", ")
            }

            homeDogsViewPager.adapter = homeDogListAdapter
            TabLayoutMediator(homeDogsIndicator, homeDogsViewPager) { _, _ -> }.attach()

            btnWalk.setOnClickListener {
                if(!NetworkManager.checkNetworkState(requireContext()) || !userInfoViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }

                if (dogsList.isEmpty()) {
                    builder.setTitle("산책을 하기 위해 \n강아지 정보를 입력 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                if(!NetworkManager.checkNetworkState(requireContext()) || !userInfoViewModel.isSuccessGetData()) {
                                    return@OnClickListener
                                }
                                val registerDogIntent =
                                    Intent(requireContext(), RegisterDogActivity::class.java)
                                startActivity(registerDogIntent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                    return@setOnClickListener
                }

                if (selectedDogList.isEmpty()) {
                    Toast.makeText(requireContext(), "함께 산책할 강아지를 선택해주세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    val builder = AlertDialog.Builder(requireContext())
                    builder.setTitle("산책을 위해 위치 권한을 \n항상 허용으로 해주세요!")
                    val listener = DialogInterface.OnClickListener { _, ans ->
                        when (ans) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                // 권한 창으로 이동
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                                startActivity(intent)
                            }
                        }
                    }
                    builder.setPositiveButton("네", listener)
                    builder.setNegativeButton("아니오", null)
                    builder.show()
                    return@setOnClickListener
                }

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = Intent(requireContext(), WalkingActivity::class.java).apply {
                            this.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogList))
                        }
                        startActivity(intent)
                    } else {
                        builder.setTitle("산책을 하기 위해 위치 권한을 \n항상 허용으로 해주세요!")
                        val listener = DialogInterface.OnClickListener { _, ans ->
                            when (ans) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    ActivityCompat.requestPermissions(
                                        requireActivity(),
                                        arrayOf(
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                                        ), 998
                                    )
                                }
                            }
                        }
                        builder.setPositiveButton("네", listener)
                        builder.setNegativeButton("아니오", null)
                        builder.show()
                    }
                } else {
                    val intent = Intent(requireContext(), WalkingActivity::class.java).apply {
                        this.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogList))
                    }
                    startActivity(intent)
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}