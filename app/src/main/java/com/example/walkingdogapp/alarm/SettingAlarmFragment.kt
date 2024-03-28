package com.example.walkingdogapp.alarm

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.walkingdogapp.HomeFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentSettingAlarmBinding


class SettingAlarmFragment : Fragment() {

    private var _binding: FragmentSettingAlarmBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingAlarmBinding.inflate(inflater, container, false)
        binding.apply {
            btnAddAlarm.setOnClickListener {
                val settingAlarmDialog = SettingAlarmDialog()
                settingAlarmDialog.show(requireActivity().supportFragmentManager, "alarm")
            }

            btnBack.setOnClickListener {
                goHome()
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun goHome() {
        mainactivity.changeFragment(HomeFragment())
    }
}