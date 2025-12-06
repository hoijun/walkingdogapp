package com.tulmunchi.walkingdogapp.presentation.ui.alarm

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
import com.tulmunchi.walkingdogapp.databinding.FragmentSettingAlarmBinding
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import com.tulmunchi.walkingdogapp.presentation.ui.home.HomeFragment
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class SettingAlarmFragment : Fragment() {
    private var _binding: FragmentSettingAlarmBinding? = null
    private val coroutineScope by lazy { CoroutineScope(Dispatchers.IO) }
    private val mainViewModel: MainViewModel by activityViewModels()
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null
    private var removeAlarmList = mutableListOf<Alarm>()
    private var alarmList = mutableListOf<Alarm>()
    private var adaptar: AlarmListAdapter? = null
    private var selectMode = false
    private var alarmFunctions: AlarmFunctions? = null

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
        activity?.let {
            mainActivity = it as? MainActivity
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

        binding.apply {
            btnAddAlarm.setOnClickListener {
                val settingAlarmDialog = SettingAlarmDialog()
                settingAlarmDialog.onAddAlarmListener =
                    object : SettingAlarmDialog.OnAddAlarmListener {
                        override fun onAddAlarm(alarm: Alarm) {
                            if (alarmList.any { it.alarmCode == alarm.alarmCode }) {
                                return
                            }

                            alarmFunctions?.let { functions ->
                                coroutineScope.launch {
                                    functions.callAlarm(
                                        alarm.time,
                                        alarm.alarmCode,
                                        alarm.weeks
                                    )
                                    mainViewModel.addAlarm(alarm)
                                    alarmList.add(alarm)
                                    alarmList.sortBy { alarmTimeToString(it.time).toInt() }
                                    withContext(Dispatchers.Main) {
                                        adaptar?.notifyItemInserted(alarmList.indexOf(alarm))
                                    }
                                }
                            }
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

            coroutineScope.launch {
                alarmList = (mainViewModel.alarms.value ?: emptyList()).sortedBy { alarmTimeToString(it.time).toInt() }.toMutableList()
                adaptar = AlarmListAdapter(alarmList)
                withContext(Dispatchers.Main) {
                    adaptar?.onItemClickListener = object : AlarmListAdapter.OnItemClickListener {
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
                                        if (alarmList.any { it.alarmCode == newAlarm.alarmCode }) {
                                            return
                                        }

                                        alarmFunctions?.let { functions ->
                                            coroutineScope.launch {
                                                functions.cancelAlarm(oldAlarm.alarmCode)

                                                // 순차적으로 실행: 삭제 → 추가
                                                mainViewModel.updateAlarm(oldAlarm.alarmCode, newAlarm)

                                                if (oldAlarm.isEnabled) {
                                                    functions.callAlarm(
                                                        newAlarm.time,
                                                        newAlarm.alarmCode,
                                                        newAlarm.weeks,
                                                    )
                                                }

                                                withContext(Dispatchers.Main) {
                                                    val old = alarmList.indexOf(oldAlarm)
                                                    alarmList.remove(oldAlarm)
                                                    adaptar?.notifyItemRemoved(old)
                                                    alarmList.add(newAlarm)
                                                    alarmList.sortBy { alarmTimeToString(it.time).toInt() }
                                                    adaptar?.notifyItemInserted(
                                                        alarmList.indexOf(
                                                            newAlarm
                                                        )
                                                    )
                                                }
                                            }
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
                            btnAlarmremove.visibility = View.VISIBLE
                            btnAddAlarm.visibility = View.GONE
                            selectMode = true
                            if (removeAlarmList.contains(alarm)) {
                                removeAlarmList.remove(alarm)
                            } else {
                                removeAlarmList.add(alarm)
                            }
                        }

                        override fun onItemClickInSelectMode(alarm: Alarm) {
                            if (removeAlarmList.contains(alarm)) {
                                removeAlarmList.remove(alarm)
                            } else {
                                removeAlarmList.add(alarm)
                            }
                        }

                        override fun onSwitchCheckedChangeListener(
                            alarm: Alarm,
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

                                    alarmFunctions?.callAlarm(
                                        calendar.timeInMillis,
                                        alarm.alarmCode,
                                        alarm.weeks,
                                    )
                                } else {
                                    alarmFunctions?.cancelAlarm(alarm.alarmCode)
                                }
                                mainViewModel.toggleAlarm(alarm.alarmCode, isChecked)
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
                alarmFunctions?.let { functions ->
                    showAlarmRemoveDialog(functions)
                }
            }

            btnBack.setOnClickListener {
                goHome()
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun showAlarmRemoveDialog(functions: AlarmFunctions) {
        context?.let { ctx ->
            val builder = AlertDialog.Builder(ctx)
            builder.setTitle("알림을 삭제하시겠습니까?")
            val listener = DialogInterface.OnClickListener { _, ans ->
                when (ans) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        coroutineScope.launch {
                            for (alarm in removeAlarmList) {
                                functions.cancelAlarm(alarm.alarmCode)
                                mainViewModel.deleteAlarm(alarm.alarmCode)
                            }
                            removeItems()
                            unSelectMode()
                        }
                        binding.btnAlarmremove.visibility = View.GONE
                        binding.btnAddAlarm.visibility = View.VISIBLE
                    }
                }
            }
            builder.setPositiveButton("네", listener)
            builder.setNegativeButton("아니오", null)
            builder.show()
        }
    }

    private fun goHome() {
        mainActivity?.changeFragment(HomeFragment())
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