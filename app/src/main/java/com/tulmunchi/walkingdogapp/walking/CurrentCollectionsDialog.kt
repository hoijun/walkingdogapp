package com.tulmunchi.walkingdogapp.walking

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.tulmunchi.walkingdogapp.utils.Utils
import com.tulmunchi.walkingdogapp.databinding.CurrentcollectionsDialogBinding
import com.tulmunchi.walkingdogapp.datamodel.CollectionInfo
import com.tulmunchi.walkingdogapp.common.GridSpacingItemDecoration
import androidx.core.graphics.drawable.toDrawable

class CurrentCollectionsDialog: DialogFragment() {
    private var _binding: CurrentcollectionsDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = CurrentcollectionsDialogBinding.inflate(inflater, container, false)
        val currentCollections = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList("currentCollections", CollectionInfo::class.java)?: arrayListOf()
        } else {
            arguments?.getParcelableArrayList("currentCollections") ?: arrayListOf<CollectionInfo>()
        }

        val adapter = CurrentCollectionItemListAdapter(currentCollections)
        val itemDecoration = GridSpacingItemDecoration(3, Utils.dpToPx(20f, requireContext()))

        binding.apply {
            getCollectionRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            getCollectionRecyclerView.addItemDecoration(itemDecoration)
            getCollectionRecyclerView.adapter = adapter
        }

        this.dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        resizeDialog()
    }

    private fun resizeDialog() {
        val params: ViewGroup.LayoutParams? = this.dialog?.window?.attributes
        val deviceWidth = Resources.getSystem().displayMetrics.widthPixels
        params?.width = (deviceWidth * 0.8).toInt()
        this.dialog?.window?.attributes = params as WindowManager.LayoutParams
    }
}