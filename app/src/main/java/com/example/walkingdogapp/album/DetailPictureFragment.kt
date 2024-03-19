package com.example.walkingdogapp.album

import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentDetailPictureBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DetailPictureFragment : Fragment() {
    private var _binding: FragmentDetailPictureBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val myViewModel: userInfoViewModel by activityViewModels()

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

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailPictureBinding.inflate(inflater, container, false)
        val imgNum = arguments?.getInt("select", 0) ?: 0
        binding.apply {
            val adaptar = DetailPictureitemlistAdapter(myViewModel.albumImgs.value!!, requireContext())
            adaptar.onClickItemListener = DetailPictureitemlistAdapter.OnClickItemListener { imgInfo ->
                val bottomSheetFragment = GalleryBottomSheetFragment().apply {
                    val bundle = Bundle()
                    bundle.putString("date", imgInfo.date)
                    bundle.putParcelable("uri", imgInfo.uri)
                    arguments = bundle

                    setStyle(DialogFragment.STYLE_NORMAL, R.style.RoundCornerBottomSheetDialogTheme)
                    onDeleteImgListener = GalleryBottomSheetFragment.OnDeleteImgListener {
                        mainactivity.changeFragment(GalleryFragment())
                    }
                }
                bottomSheetFragment.show(requireActivity().supportFragmentManager, "bottomsheet")
            }
            detailViewpager2.adapter = adaptar
            detailViewpager2.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            detailViewpager2.setCurrentItem(imgNum, false)
        }
        return binding.root
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
}