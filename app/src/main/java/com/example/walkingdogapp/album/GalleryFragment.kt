package com.example.walkingdogapp.album

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.deco.GridSpacingItemDecoration
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentGalleryBinding
import com.example.walkingdogapp.deco.HorizonSpacingItemDecoration
import com.example.walkingdogapp.mypage.MyPageFragment
import com.example.walkingdogapp.userinfo.userInfoViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val myViewModel: userInfoViewModel by activityViewModels()
    private val imgInfos = mutableListOf<GalleryImgInfo>()
    private val removeImgList = mutableListOf<Uri>()
    private var adaptar: GalleryitemlistAdaptar? = null
    private var isFragmentSwitched = false
    private lateinit var itemDecoration: GridSpacingItemDecoration
    private var selectMode = false

    private val storegePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { storagePermission ->
            when (storagePermission) {
                true -> {
                    getAlbumImage()
                    setRecyclerView()
                    binding.galleryRecyclerview.addItemDecoration(itemDecoration)
                }

                false -> return@registerForActivityResult
            }
        }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(!selectMode) {
                goMypage()
            } else {
                selectMode = false
                adaptar?.unselectMode()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        itemDecoration = GridSpacingItemDecoration(3, Constant.dpTopx(15f, requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        if (checkPermission(storegePermission)) {
            getAlbumImage()
            setRecyclerView()
        }
        binding.apply {
            btnBack.setOnClickListener {
                goMypage()
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storegePermission)) {
            val iterator = imgInfos.iterator()
            while (iterator.hasNext()) {
                val img = iterator.next()
                if (!Constant.isImageExists(img.uri, requireActivity())) {
                    lifecycleScope.launch {
                        val index = imgInfos.indexOf(img)
                        iterator.remove()
                        adaptar?.notifyItemRemoved(index)
                    }
                }
            }
            binding.galleryRecyclerview.addItemDecoration(itemDecoration)
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onStop() {
        super.onStop()
        binding.galleryRecyclerview.removeItemDecoration(itemDecoration)
        isFragmentSwitched = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setRecyclerView() {
        binding.apply {
            galleryRecyclerview.visibility = View.VISIBLE
            val imgNum = arguments?.getInt("select", 0) ?: 0
            adaptar = GalleryitemlistAdaptar(imgInfos, requireContext())
            adaptar!!.itemClickListener = object : GalleryitemlistAdaptar.OnItemClickListener {
                override fun onItemClick(imgNum: Int) {
                    val bundle = Bundle()
                    bundle.putInt("select", imgNum)
                    isFragmentSwitched = true
                    val detailPictureFragment = DetailPictureFragment().apply {
                        arguments = bundle
                    }
                    mainactivity.changeFragment(detailPictureFragment)
                }

                override fun onItemLongClick(imgUri: Uri) {
                    selectMode = true
                    removeImgList.add(imgUri)
                }

                override fun omItemClickInSelectMode(imgUri: Uri) {
                    removeImgList.add(imgUri)
                }

            }
            galleryRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
            galleryRecyclerview.scrollToPosition(imgNum)
            galleryRecyclerview.adapter = adaptar
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getAlbumImage() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATE_ADDED)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("털뭉치", "%munchi_%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"
        val cursor = requireActivity().contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use { cursor ->
            val columnIndexId: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val columnIndexTitle: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
            val columnIndexDate: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val imagePath: String = cursor.getString(columnIndexId)
                val imageTitle: String = cursor.getString(columnIndexTitle)
                val imageDate: Long = cursor.getLong(columnIndexDate)
                val contentUri = Uri.withAppendedPath(uri, imagePath)
                imgInfos.add(GalleryImgInfo(contentUri, imageTitle, Constant.convertLongToTime(SimpleDateFormat("yyyy년 MM월 dd일 HH:mm"), imageDate)))
            }
            myViewModel.savealbumImgs(imgInfos)
        }
    }

    private fun checkPermission(permissions: Array<out String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestStoragePermission.launch(permission)
                return false
            }
        }
        return true
    }

    private fun goMypage() {
        mainactivity.changeFragment(MyPageFragment())
    }

}

