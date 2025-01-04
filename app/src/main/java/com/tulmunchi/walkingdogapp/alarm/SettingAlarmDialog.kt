package com.tulmunchi.walkingdogapp.alarm

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.databinding.SettingalarmDialogBinding
import com.tulmunchi.walkingdogapp.datamodel.AlarmDataModel
import java.util.Calendar

class SettingAlarmDialog : DialogFragment() {
    private var _binding: SettingalarmDialogBinding? = null
    private val binding get() = _binding!!

    interface OnAddAlarmListener {
        fun onAddAlarm(alarm: AlarmDataModel)
        fun onChangeAlarm(newAlarm: AlarmDataModel, oldAlarm: AlarmDataModel)
    }

    var onAddAlarmListener: OnAddAlarmListener? = null

    @SuppressLint("DefaultLocale")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingalarmDialogBinding.inflate(inflater, container, false)

        val alarmInfo = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("alarmInfo", AlarmDataModel::class.java)
        } else {
            (arguments?.getSerializable("alarmInfo") as AlarmDataModel?)
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

                val weeks = arrayOf(
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

                code += if (weeksToCode(weeks) == "") {
                    Toast.makeText(requireContext(), "요일을 선택 해주세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    weeksToCode(weeks)
                }

                if (alarmInfo != null) {
                    onAddAlarmListener?.onChangeAlarm(
                        AlarmDataModel(
                            code.toInt(),
                            calendar.timeInMillis,
                            weeks,
                            alarmInfo.alarmOn
                        ), alarmInfo
                    )
                } else {
                    onAddAlarmListener?.onAddAlarm(
                        AlarmDataModel(
                            code.toInt(),
                            calendar.timeInMillis,
                            weeks,
                            true
                        )
                    )
                }

                dismiss()
            }
        }

        isCancelable = true
        this.dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
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

    private fun weeksToCode(weeks: Array<Boolean>): String {
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