package com.tulmunchi.walkingdogapp.registerinfo

import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tulmunchi.walkingdogapp.WriteDialog
import com.tulmunchi.walkingdogapp.databinding.DoglistDialogBinding
import androidx.core.graphics.drawable.toDrawable

class DogListDialog : DialogFragment(){
    private var _binding: DoglistDialogBinding? = null
    private val binding get() = _binding!!
    private val dogs = listOf(
        "직접 입력", "말티즈", "푸들", "포메라니안", "믹스견", "치와와", "시츄",
        "골든 리트리버", "진돗개", "불독", "비글", "닥스훈트", "허스키",
        "래브라도 리트리버", "요크셔 테리어", "시바 이누", "보더 콜리", "웰시 코기",
        "셰퍼드", "도베르만", "잭 러셀 테리어", "그레이트 데인"
    )

    fun interface OnClickItemListener {
        fun onClickItem(text: String)
    }

    var onClickItemListener: OnClickItemListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DoglistDialogBinding.inflate(inflater, container, false)
        val dialogRecyclerView = binding.dialogRecyclerView
        dialogRecyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = DogListAdapter(dogs)
        adapter.itemClickListener =
            DogListAdapter.OnItemClickListener { name ->
                if(name == "직접 입력") {
                    val writeDogDialog = WriteDialog()
                    writeDogDialog.clickYesListener = WriteDialog.OnClickYesListener { text ->
                        onClickItemListener?.onClickItem(text)
                    }
                    val bundle = Bundle()
                    bundle.putString("text", "강아지 종을 입력해주세요.")
                    writeDogDialog.arguments = bundle
                    writeDogDialog.show(requireActivity().supportFragmentManager, "doglist")
                }
                else {
                    onClickItemListener?.onClickItem(name)
                }
                dismiss()
            }
        dialogRecyclerView.adapter = adapter
        this.dialog?.setCanceledOnTouchOutside(true)
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        val deviceHeight = Resources.getSystem().displayMetrics.heightPixels
        params?.width = (deviceWidth * 0.8).toInt()
        params?.height = (deviceHeight * 0.7).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}