package com.tulmunchi.walkingdogapp.presentation.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tulmunchi.walkingdogapp.databinding.AlarmlistItemBinding
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import java.util.Calendar

class AlarmListAdapter(private val alarmList: List<Alarm>) : RecyclerView.Adapter<AlarmListAdapter.AlarmItemListViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(alarm: Alarm)
        fun onItemLongClick(alarm: Alarm)
        fun onItemClickInSelectMode(alarm: Alarm)
        fun onSwitchCheckedChangeListener(
            alarm: Alarm,
            isChecked: Boolean
        )
    }

    private var selectMode = false
    private val selectedItems = mutableListOf<Alarm>()
    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmItemListViewHolder {
        val binding =
            AlarmlistItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmItemListViewHolder(binding)
    }

    override fun getItemCount(): Int = alarmList.size

    override fun onBindViewHolder(holder: AlarmItemListViewHolder, position: Int) {
        holder.bind(alarmList[position])
    }

    inner class AlarmItemListViewHolder(private val binding: AlarmlistItemBinding) :
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

        fun bind(alarm: Alarm) {
            val alarmInfos = getAlarmInfo(alarm)
            var time = ""
            val hour = alarmInfos[0].toInt()
            binding.apply {
                isSelectMode = selectMode
                if (hour > 12) {
                    alarmAmPm = "오후"
                    time += (hour - 12).toString()
                } else if (hour == 12) {
                    alarmAmPm = "오후"
                    time += alarmInfos[0]
                } else if (hour == 0) {
                    alarmAmPm = "오전"
                    time += "12"
                } else {
                    alarmAmPm = "오전"
                    time += alarmInfos[0]
                }

                time += ":${alarmInfos[1]}"

                alarmTime = time

                week.text = alarmInfos[2]

                if (selectMode) {
                    isSelectMode = selectMode
                    checkBox.isChecked = selectedItems.contains(alarmList[bindingAdapterPosition])
                } else {
                    isSelectMode = selectMode
                }

                checkBox.setOnClickListener {
                    onItemClickListener?.onItemClickInSelectMode(alarmList[bindingAdapterPosition])
                    toggleSelection(alarmList[bindingAdapterPosition])
                }

                Onoff.isChecked = alarm.isEnabled
                Onoff.setOnCheckedChangeListener { _, isChecked ->
                    onItemClickListener?.onSwitchCheckedChangeListener(
                        alarmList[bindingAdapterPosition],
                        isChecked
                    )
                }
            }
        }
    }

    private fun toggleSelection(alarm: Alarm) {
        if (selectedItems.contains(alarm)) {
            selectedItems.remove(alarm)
        } else {
            selectedItems.add(alarm)
        }
        notifyItemRangeChanged(0, itemCount, null)
    }

    private fun getAlarmInfo(alarm: Alarm): List<String> {
        val time = alarm.time
        val setCalendar = Calendar.getInstance().apply {
            timeInMillis = time
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