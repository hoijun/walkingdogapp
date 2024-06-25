package com.example.walkingdogapp.album

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
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentDetailPictureBinding
import com.example.walkingdogapp.viewmodel.UserInfoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DetailPictureFragment : Fragment() {
    private var _binding: FragmentDetailPictureBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val userDataViewModel: UserInfoViewModel by activityViewModels()
    private var imgList = mutableListOf<GalleryImgInfo>()
    private var bottomSheetFragment: BottomSheetDialogFragment? = null

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goGallery()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPictureBinding.inflate(inflater, container, false)
        val imgNum = arguments?.getInt("select", 0) ?: 0
        binding.apply {
            imgList = (userDataViewModel.albumImgs.value?: mutableListOf()).toMutableList()
            val adapter = DetailPictureItemListAdapter( imgList, requireContext())
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
                                mainactivity.changeFragment(GalleryFragment())
                            }
                        } catch (e: Exception) {
                            mainactivity.changeFragment(GalleryFragment())
                        }
                    }
                }
                bottomSheetFragment?.show(requireActivity().supportFragmentManager, "bottomSheet")
            }
            detailViewpager2.adapter = adapter
            detailViewpager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            detailViewpager2.setCurrentItem(imgNum, false)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        val iterator = imgList.iterator()
        while (iterator.hasNext()) {
            val img = iterator.next()
            if(!Constant.isImageExists(img.uri, requireActivity())) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val index = imgList.indexOf(img)
                    val recyclerViewAdapter = binding.detailViewpager2.adapter as DetailPictureItemListAdapter
                    iterator.remove()
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
                            mainactivity.changeFragment(GalleryFragment())
                        }
                    }
                    bottomSheetFragment?.dismiss()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun goGallery() {
        val bundle = Bundle()
        bundle.putInt("select", binding.detailViewpager2.currentItem)
        val galleryFragment = GalleryFragment().apply {
            arguments = bundle
        }
        mainactivity.changeFragment(galleryFragment)
    }

    private fun removePicture(position: Int) {
        imgList.removeAt(position)
        (binding.detailViewpager2.adapter as DetailPictureItemListAdapter).notifyItemRemoved(position)
    }
}