package com.example.walkingdogapp.album

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


class DetailPictureFragment : Fragment() {
    private var _binding: FragmentDetailPictureBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var adaptar: DetailPictureitemlistAdapter
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
            imgList = (myViewModel.albumImgs.value?: mutableListOf()).toMutableList()
            adaptar = DetailPictureitemlistAdapter( imgList, requireContext())
            adaptar.onClickItemListener = DetailPictureitemlistAdapter.OnClickItemListener { imgInfo ->
                bottomSheetFragment = GalleryBottomSheetFragment().apply {
                    val bundle = Bundle()
                    bundle.putString("date", imgInfo.date)
                    bundle.putParcelable("uri", imgInfo.uri)
                    arguments = bundle

                    setStyle(DialogFragment.STYLE_NORMAL, R.style.RoundCornerBottomSheetDialogTheme)
                    onDeleteImgListener = GalleryBottomSheetFragment.OnDeleteImgListener {
                        try {
                            removePicture(detailViewpager2.currentItem)
                            if (adaptar.itemCount > 0) {
                                if (detailViewpager2.currentItem == 0) {
                                    detailViewpager2.setCurrentItem(
                                        detailViewpager2.currentItem,
                                        true
                                    )
                                } else if (detailViewpager2.currentItem <= adaptar.itemCount - 1) {
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
                bottomSheetFragment?.show(requireActivity().supportFragmentManager, "bottomsheet")
            }
            detailViewpager2.adapter = adaptar
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
                lifecycleScope.launch {
                    val index = imgList.indexOf(img)
                    iterator.remove()
                    adaptar.notifyItemRemoved(index)
                    withContext(Dispatchers.Main) {
                        binding.apply {
                            if (adaptar.itemCount > 0 && detailViewpager2.currentItem == index) {
                                if (detailViewpager2.currentItem == 0) {
                                    detailViewpager2.setCurrentItem(
                                        detailViewpager2.currentItem,
                                        false
                                    )
                                } else if (detailViewpager2.currentItem <= adaptar.itemCount - 1) {
                                    detailViewpager2.setCurrentItem(
                                        detailViewpager2.currentItem - 1,
                                        false
                                    )
                                }
                            } else if (adaptar.itemCount == 0) {
                                mainactivity.changeFragment(GalleryFragment())

                            }
                        }
                        bottomSheetFragment?.dismiss()
                    }
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
        adaptar.notifyItemRemoved(position)
    }
}