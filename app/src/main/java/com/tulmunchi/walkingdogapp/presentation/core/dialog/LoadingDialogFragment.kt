package com.tulmunchi.walkingdogapp.presentation.core.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.tulmunchi.walkingdogapp.R

class LoadingDialogFragment : DialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        setCancelable(false)
        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return inflater.inflate(R.layout.fragment_loading_dialog, container, false)
    }
}