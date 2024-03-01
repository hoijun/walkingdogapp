package com.example.walkingdogapp

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.example.walkingdogapp.databinding.DoglistDialogBinding
import org.checkerframework.checker.nullness.qual.NonNull

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
                    val writeDogDialog = WriteDogDialog(context, callback)
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