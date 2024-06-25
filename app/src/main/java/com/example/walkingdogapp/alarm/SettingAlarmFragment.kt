package com.example.walkingdogapp.alarm

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import com.example.walkingdogapp.mainhome.HomeFragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentSettingAlarmBinding
import com.example.walkingdogapp.datamodel.AlarmDataModel
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class SettingAlarmFragment : Fragment() {
    private var _binding: FragmentSettingAlarmBinding? = null
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val userDataViewModel: UserInfoViewModel by activityViewModels()
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private var removeAlarmList = mutableListOf<AlarmDataModel>()
    private var alarmList = mutableListOf<AlarmDataModel>()
    private var adaptar: AlarmListAdapter? = null
    private var selectMode = false

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(!selectMode) {
                goHome()
            } else {
                binding.btnAlarmremove.visibility = View.GONE
                binding.btnAddAlarm.visibility = View.VISIBLE
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
                            if (alarmList.any { it.alarm_code == alarm.alarm_code }) {
                                return
                            }
                            coroutineScope.launch {
                                alarmFunctions.callAlarm(
                                    alarm.time,
                                    alarm.alarm_code,
                                    alarm.weeks
                                )
                                userDataViewModel.addAlarm(alarm)
                                alarmList.add(alarm)
                                alarmList.sortBy { alarmTimeToString(it.time).toInt() }
                                withContext(Dispatchers.Main) {
                                    adaptar?.notifyItemInserted(alarmList.indexOf(alarm))
                                }
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

            coroutineScope.launch {
                alarmList = userDataViewModel.getAlarmList().sortedBy { alarmTimeToString(it.time).toInt() }.toMutableList()
                adaptar = AlarmListAdapter(alarmList)
                withContext(Dispatchers.Main) {
                    adaptar?.onItemClickListener = object : AlarmListAdapter.OnItemClickListener {
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
                                        if (alarmList.any { it.alarm_code == newAlarm.alarm_code }) {
                                            return
                                        }
                                        coroutineScope.launch {
                                            alarmFunctions.cancelAlarm(oldAlarm.alarm_code)

                                            userDataViewModel.deleteAlarm(oldAlarm)
                                            userDataViewModel.addAlarm(newAlarm)

                                            if(oldAlarm.alarmOn) {
                                                alarmFunctions.callAlarm(
                                                    newAlarm.time,
                                                    newAlarm.alarm_code,
                                                    newAlarm.weeks,
                                                )
                                            }

                                            withContext(Dispatchers.Main) {
                                                val old = alarmList.indexOf(oldAlarm)
                                                alarmList.remove(oldAlarm)
                                                adaptar?.notifyItemRemoved(old)
                                                alarmList.add(newAlarm)
                                                alarmList.sortBy { alarmTimeToString(it.time).toInt() }
                                                adaptar?.notifyItemInserted(alarmList.indexOf(newAlarm))
                                            }
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
                            isChecked: Boolean
                        ) {
                            coroutineScope.launch {
                                if (isChecked) {
                                    val calendar = Calendar.getInstance().apply {
                                        val today = get(Calendar.DATE)
                                        timeInMillis = alarm.time
                                        set(Calendar.DATE, today)
                                        if (System.currentTimeMillis() > timeInMillis) {
                                            add(Calendar.DATE, 1)
                                        }
                                    }

                                    alarmFunctions.callAlarm(
                                        calendar.timeInMillis,
                                        alarm.alarm_code,
                                        alarm.weeks,
                                    )
                                } else {
                                    alarmFunctions.cancelAlarm(alarm.alarm_code)
                                }
                                alarmList[alarmList.indexOf(alarm)].alarmOn = isChecked
                                userDataViewModel.onOffAlarm(alarm, isChecked)
                            }
                        }
                    }
                    val itemAnimator = DefaultItemAnimator()
                    itemAnimator.supportsChangeAnimations = false
                    AlarmRecyclerView.itemAnimator = itemAnimator
                    AlarmRecyclerView.adapter = adaptar
                }
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
                                    userDataViewModel.deleteAlarm(alarm)
                                }
                                removeItems()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goHome() {
        mainactivity.changeFragment(HomeFragment())
    }

    private fun alarmTimeToString(time: Long): String {
        val setCalendar = Calendar.getInstance()
        setCalendar.timeInMillis = time
        return setCalendar.get(Calendar.HOUR_OF_DAY).toString() + String.format("%02d", setCalendar.get(Calendar.MINUTE))
    }

    private fun unSelectMode() {
        selectMode = false
        removeAlarmList.clear()
        adaptar?.unselectMode()
    }

    private fun removeItems() {
        val iterator = alarmList.iterator()
        while (iterator.hasNext()) {
            val alarm = iterator.next()
            if (removeAlarmList.contains(alarm)) {
                val index = alarmList.indexOf(alarm)
                iterator.remove()
                adaptar?.notifyItemRemoved(index)
            }
        }
    }
}