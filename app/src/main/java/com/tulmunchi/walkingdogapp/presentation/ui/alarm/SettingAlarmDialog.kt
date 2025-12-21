package com.tulmunchi.walkingdogapp.presentation.ui.alarm

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.DialogSettingAlarmBinding
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import java.util.Calendar

class SettingAlarmDialog : DialogFragment() {
    private var _binding: DialogSettingAlarmBinding? = null
    private val binding get() = _binding!!

    interface OnAddAlarmListener {
        fun onAddAlarm(alarm: Alarm)
        fun onChangeAlarm(newAlarm: Alarm, oldAlarm: Alarm)
    }

    var onAddAlarmListener: OnAddAlarmListener? = null

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogSettingAlarmBinding.inflate(inflater, container, false)

        val alarmInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("alarmInfo", Alarm::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable("alarmInfo")
        }

        binding.apply {
            val setCalendar = Calendar.getInstance()
            setCalendar.timeInMillis = alarmInfo?.time ?: System.currentTimeMillis()
            timepicker.hour = setCalendar.get(Calendar.HOUR_OF_DAY)
            timepicker.minute = setCalendar.get(Calendar.MINUTE)

            alarmInfo?.also {
                weeks = alarmInfo.weeks.toList()
            }

            exist.setOnClickListener {
                dialog?.dismiss()
            }

            saveAlarm.setOnClickListener {
                val calendar = Calendar.getInstance()

                val weeksList = listOf(
                    sunday.isChecked,
                    monday.isChecked,
                    tuesday.isChecked,
                    wednesday.isChecked,
                    thursday.isChecked,
                    friday.isChecked,
                    saturday.isChecked
                )
                val hour = timepicker.hour
                val minute = timepicker.minute

                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                if (System.currentTimeMillis() > calendar.timeInMillis) {
                    calendar.add(Calendar.DATE, 1)
                }

                var code = if (hour < 10) {
                    "-" + String.format("%2d", hour).replace(" ", "1") + String.format(
                        "%02d",
                        minute
                    )
                } else {
                    hour.toString() + String.format("%02d", minute)
                }

                code += if (weeksToCode(weeksList) == "") {
                    Toast.makeText(requireContext(), "요일을 선택 해주세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    weeksToCode(weeksList)
                }

                if (alarmInfo != null) {
                    onAddAlarmListener?.onChangeAlarm(
                        Alarm(
                            alarmCode = code.toInt(),
                            time = calendar.timeInMillis,
                            weeks = weeksList,
                            isEnabled = alarmInfo.isEnabled
                        ), alarmInfo
                    )
                } else {
                    onAddAlarmListener?.onAddAlarm(
                        Alarm(
                            alarmCode = code.toInt(),
                            time = calendar.timeInMillis,
                            weeks = weeksList,
                            isEnabled = true
                        )
                    )
                }

                dismiss()
            }
        }

        isCancelable = true
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    private fun weeksToCode(weeks: List<Boolean>): String {
        var code = ""
        for (i: Int in weeks.indices) {
            if (weeks[i]) {
                code += i.toString()
            }
        }

        if (code.length == 6) {
            code = when (code) {
                "123456" -> "00"
                "023456" -> "11"
                "013456" -> "22"
                "012456" -> "33"
                "012356" -> "44"
                "012346" -> "55"
                else -> "66"
            }
        } else if (code.length == 7) {
            code = "7"
        }

        return code
    }

}