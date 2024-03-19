package com.example.walkingdogapp.registerinfo

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.WriteDialog
import com.example.walkingdogapp.databinding.DoglistDialogBinding
import com.example.walkingdogapp.databinding.WriteDialogBinding

class DoglistDialog : DialogFragment(){
    private lateinit var binding: DoglistDialogBinding
    private val dogs = listOf("직접 입력", "말티즈", "푸들", "포메라니안", "믹스견", "치와와", "시츄", "골든리트리버", "진돗개", "불독", "비글", "닥스훈트", "허스키", "a", "b", "c")

    fun interface OnClickItemListener {
        fun onClickItem(text: String)
    }

    var onClickItemListener: OnClickItemListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DoglistDialogBinding.inflate(inflater, container, false)
        val dialogRecyclerView = binding.dialogRecyclerView
        dialogRecyclerView.layoutManager = LinearLayoutManager(context)
        val adaptar = DoglistAdpatar(dogs)
        adaptar.itemClickListener =
            DoglistAdpatar.OnItemClickListener { name ->
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
        dialogRecyclerView.adapter = adaptar
        resizeDialog()
        this.dialog?.setCanceledOnTouchOutside(true)
        return binding.root
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        val deviceHeight = Resources.getSystem().displayMetrics.heightPixels
        params?.width = (deviceWidth * 0.8).toInt()
        params?.height = (deviceHeight * 0.7).toInt()
        val layoutParams= binding.dialogRecyclerView.layoutParams
        layoutParams.width = (deviceWidth * 0.66).toInt()
        binding.dialogRecyclerView.layoutParams = layoutParams
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}