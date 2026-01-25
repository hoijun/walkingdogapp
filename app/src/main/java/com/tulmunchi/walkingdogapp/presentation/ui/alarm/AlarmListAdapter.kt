package com.tulmunchi.walkingdogapp.presentation.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.tulmunchi.walkingdogapp.databinding.ItemAlarmListBinding
import com.tulmunchi.walkingdogapp.domain.model.Alarm
import java.util.Calendar

class AlarmListAdapter(private var alarmList: List<Alarm>) : RecyclerView.Adapter<AlarmListAdapter.AlarmItemListViewHolder>() {

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
            ItemAlarmListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlarmItemListViewHolder(binding)
    }

    override fun getItemCount(): Int = alarmList.size

    override fun onBindViewHolder(holder: AlarmItemListViewHolder, position: Int) {
        holder.bind(alarmList[position])
    }

    override fun onBindViewHolder(
        holder: AlarmItemListViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            // No payload: full bind (for add, delete, modify operations)
            onBindViewHolder(holder, position)
        }
    }

    inner class AlarmItemListViewHolder(private val binding: ItemAlarmListBinding) :
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

                OnOff.isChecked = alarm.isEnabled
                OnOff.setOnCheckedChangeListener { _, isChecked ->
                    onItemClickListener?.onSwitchCheckedChangeListener(alarmList[bindingAdapterPosition], isChecked)
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

    fun updateList(newList: List<Alarm>) {
        val diffCallback = AlarmDiffCallback(alarmList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        alarmList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * DiffUtil.Callback for efficient list updates
     * Detects changes and provides payloads for partial updates
     */
    private class AlarmDiffCallback(
        private val oldList: List<Alarm>,
        private val newList: List<Alarm>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Same item if alarmCode matches
            return oldList[oldItemPosition].alarmCode == newList[newItemPosition].alarmCode
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // Same content if all fields are equal
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldAlarm = oldList[oldItemPosition]
            val newAlarm = newList[newItemPosition]

            // Check what changed and return appropriate payload
            val timeChanged = oldAlarm.time != newAlarm.time
            val weeksChanged = oldAlarm.weeks != newAlarm.weeks
            val enabledChanged = oldAlarm.isEnabled != newAlarm.isEnabled

            // If only isEnabled changed, return TOGGLE payload
            if (enabledChanged && !timeChanged && !weeksChanged) {
                return PAYLOAD_TOGGLE
            }

            // If time or weeks changed (modify operation), return null for full rebind
            if (timeChanged || weeksChanged) {
                return null
            }

            // Default: full rebind
            return null
        }

        companion object {
            const val PAYLOAD_TOGGLE = "TOGGLE"
        }
    }
}