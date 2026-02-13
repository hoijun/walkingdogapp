package com.tulmunchi.walkingdogapp.presentation.ui.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import com.tulmunchi.walkingdogapp.databinding.FragmentSettingAlarmBinding
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener
import com.tulmunchi.walkingdogapp.presentation.viewmodel.SettingAlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class SettingAlarmFragment : Fragment() {
    private var _binding: FragmentSettingAlarmBinding? = null
    private val binding get() = _binding!!

    private val alarmViewModel: SettingAlarmViewModel by activityViewModels()

    private var alarmListAdapter: AlarmListAdapter? = null
    private var alarmFunctions: AlarmFunctions? = null

    @Inject
    lateinit var navigationManager: NavigationManager

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(alarmViewModel.selectMode.value != true) {
                navigateToHome()
            } else {
                binding.btnAlarmRemove.visibility = View.GONE
                binding.btnAddAlarm.visibility = View.VISIBLE
                alarmViewModel.exitSelectMode()
                alarmListAdapter?.unselectMode()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingAlarmBinding.inflate(inflater, container, false)

        context?.let { ctx ->
            alarmFunctions = AlarmFunctions(ctx)
        }

        // Load alarms from database
        alarmViewModel.loadAlarms()

        // Observe alarm list changes
        // DiffUtil in adapter handles efficient updates (add, delete, modify, toggle)
        alarmViewModel.alarmList.observe(viewLifecycleOwner) { alarms ->
            alarmListAdapter?.updateList(alarms)
        }

        // Observe select mode changes
        alarmViewModel.selectMode.observe(viewLifecycleOwner) { isSelectMode ->
            if (isSelectMode) {
                binding.btnAlarmRemove.visibility = View.VISIBLE
                binding.btnAddAlarm.visibility = View.GONE
            } else {
                binding.btnAlarmRemove.visibility = View.GONE
                binding.btnAddAlarm.visibility = View.VISIBLE
            }
        }

        binding.apply {
            btnAddAlarm.setOnSingleClickListener {
                val settingAlarmDialog = SettingAlarmDialog()
                settingAlarmDialog.onAddAlarmListener =
                    object : SettingAlarmDialog.OnAddAlarmListener {
                        override fun onAddAlarm(alarm: Alarm) {
                            alarmFunctions?.callAlarm(
                                alarm.time,
                                alarm.alarmCode,
                                alarm.weeks
                            )
                            alarmViewModel.addAlarm(alarm)
                        }

                        override fun onChangeAlarm(
                            newAlarm: Alarm,
                            oldAlarm: Alarm
                        ) {
                            return
                        }
                    }

                parentFragmentManager.let {
                    try {
                        settingAlarmDialog.show(it, "alarm")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            alarmListAdapter = AlarmListAdapter(alarmViewModel.alarmList.value ?: emptyList())
            alarmListAdapter?.onItemClickListener = object : AlarmListAdapter.OnItemClickListener {
                override fun onItemClick(alarm: Alarm) {
                    val settingAlarmDialog = SettingAlarmDialog().apply {
                        val bundle = Bundle()
                        bundle.putParcelable("alarmInfo", alarm)
                        arguments = bundle
                    }

                    settingAlarmDialog.onAddAlarmListener =
                        object : SettingAlarmDialog.OnAddAlarmListener {
                            override fun onAddAlarm(alarm: Alarm) {
                                return
                            }

                            override fun onChangeAlarm(
                                newAlarm: Alarm,
                                oldAlarm: Alarm
                            ) {
                                alarmFunctions?.cancelAlarm(oldAlarm.alarmCode)

                                // 순차적으로 실행: 삭제 → 추가
                                alarmViewModel.updateAlarm(oldAlarm.alarmCode, newAlarm)

                                if (oldAlarm.isEnabled) {
                                    alarmFunctions?.callAlarm(
                                        newAlarm.time,
                                        newAlarm.alarmCode,
                                        newAlarm.weeks,
                                    )
                                }
                            }
                        }

                    parentFragmentManager.let {
                        try {
                            settingAlarmDialog.show(it, "modifyAlarm")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onItemLongClick(alarm: Alarm) {
                    alarmViewModel.enterSelectMode()
                    alarmViewModel.toggleAlarmSelection(alarm)
                }

                override fun onItemClickInSelectMode(alarm: Alarm) {
                    alarmViewModel.toggleAlarmSelection(alarm)
                }

                override fun onSwitchCheckedChangeListener(
                    alarm: Alarm,
                    isChecked: Boolean
                ) {
                    if (isChecked) {
                        val calendar = Calendar.getInstance().apply {
                            val today = get(Calendar.DATE)
                            timeInMillis = alarm.time
                            set(Calendar.DATE, today)
                            if (System.currentTimeMillis() > timeInMillis) {
                                add(Calendar.DATE, 1)
                            }
                        }

                        alarmFunctions?.callAlarm(
                            calendar.timeInMillis,
                            alarm.alarmCode,
                            alarm.weeks,
                        )
                    } else {
                        alarmFunctions?.cancelAlarm(alarm.alarmCode)
                    }
                    alarmViewModel.toggleAlarm(alarm.alarmCode, isChecked)
                }
            }

            val itemAnimator = DefaultItemAnimator()
            itemAnimator.supportsChangeAnimations = false
            AlarmRecyclerView.itemAnimator = itemAnimator
            AlarmRecyclerView.adapter = alarmListAdapter

            btnAlarmRemove.setOnClickListener {
                alarmFunctions?.let { functions ->
                    showAlarmRemoveDialog(functions)
                }
            }

            btnBack.setOnClickListener {
                navigateToHome()
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAlarmRemoveDialog(functions: AlarmFunctions) {
        val dialog = SelectDialog.newInstance(title = "알림을 삭제하시겠어요?", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            val alarmsToRemove = alarmViewModel.getAlarmsToRemove()
            for (alarm in alarmsToRemove) {
                functions.cancelAlarm(alarm.alarmCode)
            }
            alarmViewModel.deleteAlarms(alarmsToRemove)
            alarmViewModel.exitSelectMode()
            alarmListAdapter?.unselectMode()
        }

        dialog.show(parentFragmentManager, "removeAlarm")
    }

    private fun navigateToHome() {
        navigationManager.navigateTo(NavigationState.WithBottomNav.Home)
    }
}