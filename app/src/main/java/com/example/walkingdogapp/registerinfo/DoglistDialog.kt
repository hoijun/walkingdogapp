package com.example.walkingdogapp.registerinfo

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.WriteDialog
import com.example.walkingdogapp.databinding.DoglistDialogBinding

class DoglistDialog(context: Context, private val callback: (String) -> Unit) : Dialog(context){
    private lateinit var binding: DoglistDialogBinding
    private val dogs = listOf("직접 입력", "말티즈", "푸들", "포메라니안", "믹스견", "치와와", "시츄", "골든리트리버", "진돗개", "불독", "비글", "닥스훈트", "허스키", "a", "b", "c")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DoglistDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCanceledOnTouchOutside(true)

        resizeDialog()

        val dialogRecyclerView = binding.dialogRecyclerView
        dialogRecyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = DoglistAdpater(dogs)
        adapter.itemClickListener =
            DoglistAdpater.OnItemClickListener { name ->
                if(name == "직접 입력") {
                    val writeDogDialog = WriteDialog(context, "강아지 종을 입력해주세요.", callback)
                    writeDogDialog.show()
                }
                else {
                    callback(name)
                }
                dismiss()
            }
        dialogRecyclerView.adapter = adapter
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        val deviceHeight = Resources.getSystem().displayMetrics.heightPixels
        params?.width = (deviceWidth * 0.8).toInt()
        params?.height = (deviceHeight * 0.7).toInt()
        val layoutParams= binding.dialogRecyclerView.layoutParams
        layoutParams.width = (deviceWidth * 0.66).toInt()
        binding.dialogRecyclerView.layoutParams = layoutParams
        window?.attributes = params as WindowManager.LayoutParams
    }
}