package com.tulmunchi.walkingdogapp.album

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.FragmentDetailPictureBinding
import com.tulmunchi.walkingdogapp.datamodel.GalleryImgInfo
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tulmunchi.walkingdogapp.utils.FirebaseAnalyticHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


class DetailPictureFragment : Fragment() {
    private var _binding: FragmentDetailPictureBinding? = null
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private var imgList = mutableListOf<GalleryImgInfo>()
    private var bottomSheetFragment: BottomSheetDialogFragment? = null

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goGallery()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPictureBinding.inflate(inflater, container, false)
        val imgNum = arguments?.getInt("select", 0) ?: 0
        binding.apply {
            imgList = (mainViewModel.albumImgs.value?: mutableListOf()).toMutableList()
            val adapter = DetailPictureItemListAdapter(imgList)
            adapter.onClickItemListener = DetailPictureItemListAdapter.OnClickItemListener { imgInfo ->
                bottomSheetFragment = GalleryBottomSheetFragment().apply {
                    val bundle = Bundle()
                    bundle.putString("date", imgInfo.date)
                    bundle.putParcelable("uri", imgInfo.uri)
                    arguments = bundle

                    setStyle(DialogFragment.STYLE_NORMAL, R.style.RoundCornerBottomSheetDialogTheme)
                    onDeleteImgListener = GalleryBottomSheetFragment.OnDeleteImgListener {
                        try {
                            removePicture(detailViewpager2.currentItem)
                            if (adapter.itemCount > 0) {
                                if (detailViewpager2.currentItem == 0) {
                                    detailViewpager2.setCurrentItem(
                                        detailViewpager2.currentItem,
                                        true
                                    )
                                } else if (detailViewpager2.currentItem <= adapter.itemCount - 1) {
                                    detailViewpager2.setCurrentItem(
                                        detailViewpager2.currentItem - 1,
                                        true
                                    )
                                }
                            } else {
                                mainActivity?.changeFragment(GalleryFragment())
                            }
                        } catch (e: Exception) {
                            mainActivity?.changeFragment(GalleryFragment())
                        }
                    }
                }

                parentFragmentManager.let {
                    try {
                        bottomSheetFragment?.show(it, "bottomSheet")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 다이얼로그 표시 실패 시 처리 (로그만 기록)
                    }
                }
            }

            detailViewpager2.apply {
                this.adapter = adapter
                orientation = ViewPager2.ORIENTATION_HORIZONTAL
                offscreenPageLimit = 1
                setCurrentItem(imgNum, false)
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
        lifecycleScope.launch {
            try {
                val itemsToRemove = mutableListOf<GalleryImgInfo>()

                for (img in imgList) {
                    try {
                        val activity = mainActivity
                        if (activity != null && !Utils.isImageExists(img.uri, activity)) {
                            itemsToRemove.add(img)
                        } else if (activity == null) {
                            // Activity가 null이면 Fragment로 돌아가기
                            withContext(Dispatchers.Main) {
                                mainActivity?.changeFragment(GalleryFragment())
                            }
                            return@launch
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            mainActivity?.changeFragment(GalleryFragment())
                        }
                        return@launch
                    }
                }

                // UI 업데이트
                withContext(Dispatchers.Main) {
                    itemsToRemove.forEach { img ->
                        try {
                            val index = imgList.indexOf(img)
                            imgList.remove(img)

                            val recyclerViewAdapter = binding.detailViewpager2.adapter as DetailPictureItemListAdapter
                            recyclerViewAdapter.notifyItemRemoved(index)

                            binding.apply {
                                if (recyclerViewAdapter.itemCount > 0 && detailViewpager2.currentItem == index) {
                                    if (detailViewpager2.currentItem == 0) {
                                        detailViewpager2.setCurrentItem(
                                            detailViewpager2.currentItem,
                                            false
                                        )
                                    } else if (detailViewpager2.currentItem <= recyclerViewAdapter.itemCount - 1) {
                                        detailViewpager2.setCurrentItem(
                                            detailViewpager2.currentItem - 1,
                                            false
                                        )
                                    }
                                } else if (recyclerViewAdapter.itemCount == 0) {
                                    mainActivity?.changeFragment(GalleryFragment())
                                }
                            }
                        } catch (e: Exception) {
                            mainActivity?.changeFragment(GalleryFragment())
                            return@withContext
                        }
                    }
                    bottomSheetFragment?.dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    mainActivity?.changeFragment(GalleryFragment())
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        bottomSheetFragment?.let { dialog ->
            try {
                if (dialog.isAdded) {
                    dialog.dismiss()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bottomSheetFragment = null

        mainActivity = null
        _binding = null
    }

    private fun goGallery() {
        val bundle = Bundle()
        bundle.putInt("select", binding.detailViewpager2.currentItem)
        val galleryFragment = GalleryFragment().apply {
            arguments = bundle
        }
        mainActivity?.changeFragment(galleryFragment)
    }

    private fun removePicture(position: Int) {
        imgList.removeAt(position)
        (binding.detailViewpager2.adapter as DetailPictureItemListAdapter).notifyItemRemoved(position)
    }
}