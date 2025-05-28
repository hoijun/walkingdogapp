package com.tulmunchi.walkingdogapp.mainhome

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayoutMediator
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.alarm.SettingAlarmFragment
import com.tulmunchi.walkingdogapp.databinding.FragmentHomeBinding
import com.tulmunchi.walkingdogapp.registerinfo.RegisterDogActivity
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import com.tulmunchi.walkingdogapp.walking.WalkingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private var mainActivity: MainActivity? = null
    private val selectedDogList = mutableListOf<String>()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

        context?.let { ctx ->
            if (!NetworkManager.checkNetworkState(ctx)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("인터넷을 연결해주세요!")
                builder.setPositiveButton("네", null)
                builder.setCancelable(false)
                builder.show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        MainActivity.preFragment = "Home" // 다른 액티비티로 이동 할 때 홈에서 이동을 표시

        binding.apply {
            viewmodel = mainViewModel
            lifecycleOwner = viewLifecycleOwner
            selectedDogs = selectedDogList.joinToString(", ")

            refresh.apply {
                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        mainViewModel.observeUser()
                    }
                }
                mainViewModel.successGetData.observe(viewLifecycleOwner) {
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
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx)) {
                        return@setOnClickListener
                    }
                    mainActivity?.changeFragment(SettingAlarmFragment())
                }
            }

            val dogsList = mainViewModel.dogsInfo.value ?: listOf()
            val homeDogListAdapter = HomeDogListAdapter(dogsList, mainViewModel.isSuccessGetData())
            homeDogListAdapter.onClickDogListener =
                HomeDogListAdapter.OnClickDogListener { dogName ->
                    if (selectedDogList.contains(dogName)) {
                        selectedDogList.remove(dogName)
                    } else {
                        selectedDogList.add(dogName)
                    }

                    selectedDogs = selectedDogList.joinToString(", ")
                }

            homeDogsViewPager.adapter = homeDogListAdapter
            TabLayoutMediator(homeDogsIndicator, homeDogsViewPager) { _, _ -> }.attach()

            btnWalk.setOnClickListener {
                handleWalkButtonClick(dogsList)
            }
        }
        return binding.root
    }

    private fun handleWalkButtonClick(dogsList: List<Any>) {
        val ctx = context ?: return

        if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
            return
        }

        if (dogsList.isEmpty()) {
            showRegisterDogDialog(ctx)
            return
        }

        if (selectedDogList.isEmpty()) {
            Toast.makeText(ctx, "함께 산책할 강아지를 선택해주세요!", Toast.LENGTH_SHORT).show()
            return
        }

        if (checkLocationPermissions(ctx)) {
            showPermissionDialog(ctx)
            return
        }

        val intent = Intent(ctx, WalkingActivity::class.java).apply {
            this.putStringArrayListExtra("selectedDogs", ArrayList(selectedDogList))
        }
        startActivity(intent)
    }

    private fun showRegisterDogDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("산책을 하기 위해 \n강아지 정보를 입력 해주세요!")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    if (!NetworkManager.checkNetworkState(context) || !mainViewModel.isSuccessGetData()) {
                        return@OnClickListener
                    }
                    val registerDogIntent =
                        Intent(context, RegisterDogActivity::class.java)
                    startActivity(registerDogIntent)
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니오", null)
        builder.show()
    }

    private fun showPermissionDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("산책을 위해 위치 권한을 \n항상 허용으로 해주세요!")
        val listener = DialogInterface.OnClickListener { _, ans ->
            when (ans) {
                DialogInterface.BUTTON_POSITIVE -> {
                    // 권한 창으로 이동
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.data =
                        Uri.fromParts("package", context.packageName, null)
                    startActivity(intent)
                }
            }
        }
        builder.setPositiveButton("네", listener)
        builder.setNegativeButton("아니오", null)
        builder.show()
    }

    private fun checkLocationPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.VISIBLE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }
}