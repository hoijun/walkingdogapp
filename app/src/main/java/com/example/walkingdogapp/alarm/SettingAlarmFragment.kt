package com.example.walkingdogapp.alarm

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.HomeFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.album.GalleryitemlistAdaptar
import com.example.walkingdogapp.databinding.FragmentSettingAlarmBinding
import com.example.walkingdogapp.userinfo.UserInfoViewModel
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class SettingAlarmFragment : Fragment() {

    private var _binding: FragmentSettingAlarmBinding? = null
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val myViewModel: UserInfoViewModel by activityViewModels()
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private var removeAlarmList = mutableListOf<AlarmDataModel>()
    private var adaptar: AlarmlistAdapter? = null
    private var selectMode = false

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(!selectMode) {
                goHome()
            } else {
                unSelectMode()
            }
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

        val alarmFunctions = AlarmFunctions(requireContext())

        binding.apply {
            btnAddAlarm.setOnClickListener {
                val settingAlarmDialog = SettingAlarmDialog()
                settingAlarmDialog.onAddAlarmListener =
                    object : SettingAlarmDialog.OnAddAlarmListener {
                        override fun onAddAlarm(alarm: AlarmDataModel) {
                            coroutineScope.launch {
                                alarmFunctions.callAlarm(alarm.time, alarm.alarm_code, alarm.weeks, alarm.alarmOn)
                                myViewModel.addAlarm(alarm)
                            }
                        }

                        override fun onChangeAlarm(
                            newAlarm: AlarmDataModel,
                            oldAlarm: AlarmDataModel
                        ) {
                            return
                        }
                    }
                settingAlarmDialog.show(requireActivity().supportFragmentManager, "alarm")
            }

            myViewModel.getAlarmList().observe(viewLifecycleOwner) { alarmList ->
                val sortedList = alarmList.sortedBy { alarmTimeToString(it.time).toInt() }
                adaptar = AlarmlistAdapter(sortedList)
                adaptar?.onItemClickListener = object : AlarmlistAdapter.OnItemClickListener {
                    override fun onItemClick(alarm: AlarmDataModel) {
                        val settingAlarmDialog = SettingAlarmDialog().apply {
                            val bundle = Bundle()
                            bundle.putSerializable("alarmInfo", alarm)
                            arguments = bundle
                        }

                        settingAlarmDialog.onAddAlarmListener =
                            object : SettingAlarmDialog.OnAddAlarmListener {
                                override fun onAddAlarm(alarm: AlarmDataModel) {
                                    return
                                }

                                override fun onChangeAlarm(
                                    newAlarm: AlarmDataModel,
                                    oldAlarm: AlarmDataModel
                                ) {
                                    coroutineScope.launch {
                                        alarmFunctions.cancelAlarm(oldAlarm.alarm_code)
                                        alarmFunctions.callAlarm(
                                            newAlarm.time,
                                            newAlarm.alarm_code,
                                            newAlarm.weeks,
                                            alarm.alarmOn
                                        )
                                        myViewModel.deleteAlarm(oldAlarm)
                                        myViewModel.addAlarm(newAlarm)
                                    }
                                }
                            }

                        settingAlarmDialog.show(
                            requireActivity().supportFragmentManager,
                            "modifyAlarm"
                        )
                    }

                    override fun onItemLongClick(alarm: AlarmDataModel) {
                        btnAlarmremove.visibility = View.VISIBLE
                        btnAddAlarm.visibility = View.GONE
                        selectMode = true
                        if (removeAlarmList.contains(alarm)) {
                            removeAlarmList.remove(alarm)
                        } else {
                            removeAlarmList.add(alarm)
                        }
                    }

                    override fun onItemClickInSelectMode(alarm: AlarmDataModel) {
                        if (removeAlarmList.contains(alarm)) {
                            removeAlarmList.remove(alarm)
                        } else {
                            removeAlarmList.add(alarm)
                        }
                    }

                    override fun onSwitchCheckedChangeListener(
                        alarm: AlarmDataModel,
                        ischecked: Boolean
                    ) {
                        coroutineScope.launch {
                            myViewModel.onOffAlarm(alarm, ischecked)
                        }
                    }
                }
                AlarmRecyclerView.itemAnimator = null
                AlarmRecyclerView.adapter = adaptar
            }

            btnAlarmremove.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("알림을 삭제하시겠습니까?")
                val listener = DialogInterface.OnClickListener { _, ans ->
                    when (ans) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            coroutineScope.launch {
                                for (alarm in removeAlarmList) {
                                    alarmFunctions.cancelAlarm(alarm.alarm_code)
                                    myViewModel.deleteAlarm(alarm)
                                }
                                unSelectMode()
                            }
                            btnAlarmremove.visibility = View.GONE
                            btnAddAlarm.visibility = View.VISIBLE
                        }
                    }
                }
                builder.setPositiveButton("네", listener)
                builder.setNegativeButton("아니오", null)
                builder.show()
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

    private fun alarmTimeToString(time: Long): String {
        val setCalendar = Calendar.getInstance()
        setCalendar.timeInMillis = time
        return setCalendar.get(Calendar.HOUR_OF_DAY).toString() + setCalendar.get(Calendar.MINUTE).toString()
    }

    private fun unSelectMode() {
        selectMode = false
        removeAlarmList.clear()
        adaptar?.unselectMode()
    }
}