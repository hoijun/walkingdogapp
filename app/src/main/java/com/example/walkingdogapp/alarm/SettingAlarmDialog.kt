package com.example.walkingdogapp.alarm

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.walkingdogapp.databinding.SettingalarmDialogBinding
import java.util.Calendar

class SettingAlarmDialog : DialogFragment() {
    private lateinit var binding: SettingalarmDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SettingalarmDialogBinding.inflate(inflater, container, false)
        binding.apply {
            exist.setOnClickListener {
                dialog?.dismiss()
            }

            addAlarm.setOnClickListener {
                val calendar = Calendar.getInstance()

                val weeks = arrayOf(sunday.isChecked, monday.isChecked, tuesday.isChecked, wednesday.isChecked, thursday.isChecked, friday.isChecked, saturday.isChecked)
                val hour = timepicker.hour
                val minute = timepicker.minute

                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)

                if(System.currentTimeMillis() > calendar.timeInMillis) {
                    calendar.add(Calendar.DATE, 1)
                }

                var code = hour.toString() + String.format("%02d", minute)

                code += if(weeksTocode(weeks) == "") {
                    Toast.makeText(requireContext(), "요일을 선택 해주세요!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else if (weeksTocode(weeks).length != 7) {
                    weeksTocode(weeks)
                } else {
                    "7"
                }

                Log.d("savepoint", code)

                val alarmFunctions = AlarmFunctions(requireContext())
                alarmFunctions.callAlarm(calendar.timeInMillis, code.toInt(), weeks)

                dismiss()
            }
        }

        isCancelable = true
        resizeDialog()
        this.dialog?.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return binding.root
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.99).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }

    private fun weeksTocode(weeks: Array<Boolean>): String {
        var code = ""
        for (i: Int in weeks.indices) {
            if (weeks[i]) {
                code += i.toString()
            }
        }
        return code
    }

    private fun saveAlarm(code: Int) {

    }
}