package com.tulmunchi.walkingdogapp

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.core.graphics.drawable.toDrawable

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