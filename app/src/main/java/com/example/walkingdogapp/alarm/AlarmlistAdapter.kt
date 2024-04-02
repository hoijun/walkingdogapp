package com.example.walkingdogapp.alarm

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.walkingdogapp.databinding.AlarmlistItemBinding
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AlarmlistAdapter(private val alarmList: List<AlarmDataModel>) : RecyclerView.Adapter<AlarmlistAdapter.AlarmitemlistViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(alarm: AlarmDataModel)
        fun onItemLongClick(alarm: AlarmDataModel)
        fun onItemClickInSelectMode(alarm: AlarmDataModel)
        fun onSwitchCheckedChangeListener(
            alarm: AlarmDataModel,
            ischecked: Boolean
        )
    }

    private var selectMode = false
    private val selectedItems = mutableListOf<AlarmDataModel>()
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmitemlistViewHolder {
        val binding =
            AlarmlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmitemlistViewHolder(binding)
    }

    override fun getItemCount(): Int = alarmList.size

    override fun onBindViewHolder(holder: AlarmitemlistViewHolder, position: Int) {
        holder.bind(alarmList[position])
    }

    inner class AlarmitemlistViewHolder(private val binding: AlarmlistItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                if (!selectMode) {
                    onItemClickListener?.onItemClick(alarmList[bindingAdapterPosition])
                } else {
                    onItemClickListener?.onItemClickInSelectMode(alarmList[bindingAdapterPosition])
                    toggleSelection(alarmList[bindingAdapterPosition])
                }
            }

            binding.root.setOnLongClickListener {
                onItemClickListener?.onItemLongClick(alarmList[bindingAdapterPosition])
                toggleSelection(alarmList[bindingAdapterPosition])
                selectMode = true
                true
            }
        }

        fun bind(alarm: AlarmDataModel) {
            val infos = getAlarmInfo(alarm)
            var time = ""
            val hour = infos[0].toInt()
            binding.apply {
                if (hour > 12) {
                    ampm.text = "오후"
                    time += (hour - 12).toString()
                } else if (hour == 12) {
                    ampm.text = "오후"
                    time += infos[0]
                } else if (hour == 0) {
                    ampm.text = "오전"
                    time += "12"
                } else {
                    ampm.text = "오전"
                    time += infos[0]
                }

                time += ":${infos[1]}"

                alarmtime.text = time

                week.text = infos[2]

                if (selectMode) {
                    checkBox.visibility = View.VISIBLE
                    checkBox.isChecked = selectedItems.contains(alarmList[bindingAdapterPosition])
                    Onoff.visibility = View.GONE
                } else {
                    checkBox.visibility = View.GONE
                    Onoff.visibility = View.VISIBLE
                }

                Onoff.isChecked = alarm.alarmOn
                Onoff.setOnCheckedChangeListener { _, isChecked ->
                    onItemClickListener?.onSwitchCheckedChangeListener(
                        alarmList[bindingAdapterPosition],
                        isChecked
                    )
                }
            }
        }
    }

    private fun toggleSelection(alarm: AlarmDataModel) {
        if (selectedItems.contains(alarm)) {
            selectedItems.remove(alarm)
        } else {
            selectedItems.add(alarm)
        }
        notifyItemRangeChanged(0, itemCount, null)
    }

    private fun getAlarmInfo(alarm: AlarmDataModel): List<String> {
        val time = alarm.time
        val setCalendar = Calendar.getInstance().also {
            it.timeInMillis = time
        }
        val hour = setCalendar.get(Calendar.HOUR_OF_DAY).toString()
        val minutes = setCalendar.get(Calendar.MINUTE)

        val weeks = alarm.weeks
        val weekWords = listOf("일", "월", "화", "수", "목", "금", "토")
        var onWeeks = ""
        for(i: Int in weeks.indices){
            if(weeks[i])
                onWeeks += weekWords[i]
        }

        return listOf(hour, String.format("%02d", minutes), onWeeks)
    }

    fun unselectMode() {
        selectMode = false
        selectedItems.clear()
        notifyItemRangeChanged(0, itemCount, null)
    }
}