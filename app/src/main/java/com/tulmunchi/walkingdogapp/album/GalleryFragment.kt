package com.tulmunchi.walkingdogapp.album

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.databinding.FragmentGalleryBinding
import com.tulmunchi.walkingdogapp.utils.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.datamodel.GalleryImgInfo
import com.tulmunchi.walkingdogapp.mypage.MyPageFragment
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import java.text.SimpleDateFormat


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private val imgInfos = mutableListOf<GalleryImgInfo>()
    private val removeImgList = mutableListOf<Uri>()
    private lateinit var itemDecoration: GridSpacingItemDecoration
    private var selectMode = MutableLiveData(false)

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                    setGallery()
                    binding.galleryRecyclerview.addItemDecoration(itemDecoration)
                }

                false -> return@registerForActivityResult
            }
        }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(!selectMode.value!!) {
                goMyPage()
            } else {
                unSelectMode()
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        when(result.resultCode) {
            Activity.RESULT_OK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                updateRecyclerView()
                unSelectMode()
                Toast.makeText(requireContext(), "사진을 삭제 했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        itemDecoration = GridSpacingItemDecoration(3, Utils.dpToPx(15f, requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        if (checkPermission(storagePermission)) {
            setGallery()
        }

        binding.apply {
            isSelectMode = selectMode
            lifecycleOwner = requireActivity()

            btnBack.setOnClickListener {
                goMyPage()
            }

            imgRemoveBtn.setOnClickListener {
                try {
                    if (removeImgList.isEmpty()) {
                        unSelectMode()
                        return@setOnClickListener
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intentSender = MediaStore.createDeleteRequest(
                            requireActivity().contentResolver,
                            removeImgList
                        ).intentSender
                        launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                    } else {
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setTitle("사진을 삭제하시겠습니까?")
                        val listener = DialogInterface.OnClickListener { _, ans ->
                            when (ans) {
                                DialogInterface.BUTTON_POSITIVE -> {
                                    for (uri in removeImgList) {
                                        requireActivity().contentResolver.delete(uri, null, null)
                                    }
                                    updateRecyclerView()
                                    unSelectMode()
                                }
                            }
                        }
                        builder.setPositiveButton("네", listener)
                        builder.setNegativeButton("아니오", null)
                        builder.show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "삭제하는 데 오류가 발생 했어요.", Toast.LENGTH_SHORT).show()
                    unSelectMode()
                    return@setOnClickListener
                }
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storagePermission)) {
            updateRecyclerView()
        }
        binding.galleryRecyclerview.addItemDecoration(itemDecoration)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onStop() {
        super.onStop()
        unSelectMode()
        binding.galleryRecyclerview.removeItemDecoration(itemDecoration)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setGallery() {
        getAlbumImage()
        setRecyclerView()
    }

    private fun setRecyclerView() {
        binding.apply {
            val itemAnimator = DefaultItemAnimator()
            itemAnimator.supportsChangeAnimations = false
            galleryRecyclerview.itemAnimator = itemAnimator

            val imgNum = arguments?.getInt("select", 0) ?: 0
            val adapter = GalleryItemListAdapter(imgInfos)
            adapter.itemClickListener = object : GalleryItemListAdapter.OnItemClickListener {
                override fun onItemClick(imgNum: Int) {
                    if(!selectMode.value!!) {
                        val bundle = Bundle()
                        bundle.putInt("select", imgNum)
                        val detailPictureFragment = DetailPictureFragment().apply {
                            arguments = bundle
                        }
                        mainactivity.changeFragment(detailPictureFragment)
                    }
                }

                override fun onItemLongClick(imgUri: Uri) {
                    selectMode.value = true
                    if (removeImgList.contains(imgUri)) {
                        removeImgList.remove(imgUri)
                    } else {
                        removeImgList.add(imgUri)
                    }
                }

                override fun onItemClickInSelectMode(imgUri: Uri) {
                    if (removeImgList.contains(imgUri)) {
                        removeImgList.remove(imgUri)
                    } else {
                        removeImgList.add(imgUri)
                    }
                }

            }
            galleryRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
            galleryRecyclerview.scrollToPosition(imgNum)
            galleryRecyclerview.adapter = adapter
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getAlbumImage() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.WIDTH, MediaStore.Images.Media.HEIGHT, MediaStore.Images.Media.ORIENTATION)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("털뭉치", "%munchi_%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        val cursor = requireActivity().contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
        cursor?.use {
            val columnIndexId: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val columnIndexTitle: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
            val columnIndexDate: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val columnIndexWidth: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val columnIndexHeight: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val columnIndexOrientation: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

            while (it.moveToNext()) {
                val imagePath: String = it.getString(columnIndexId)
                val imageTitle: String = it.getString(columnIndexTitle)
                val imageDate: Long = it.getLong(columnIndexDate)
                val imageWidth: Int = it.getInt(columnIndexWidth)
                val imageHeight: Int = it.getInt(columnIndexHeight)
                val contentUri = Uri.withAppendedPath(uri, imagePath)
                val orientation = it.getInt(columnIndexOrientation)

                val (finalWidth, finalHeight) = when(orientation) {
                    90, 270 -> Pair(imageHeight, imageWidth)
                    else -> Pair(imageWidth, imageHeight)
                }

                imgInfos.add(GalleryImgInfo(contentUri, imageTitle, Utils.convertLongToTime(SimpleDateFormat("yyyy년 MM월 dd일 HH:mm"), imageDate / 1000L), finalWidth, finalHeight))
            }
            mainViewModel.saveAlbumImgs(imgInfos)
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

    private fun unSelectMode() {
        selectMode.value = false
        removeImgList.clear()
        (binding.galleryRecyclerview.adapter as GalleryItemListAdapter).unselectMode()
    }

    private fun goMyPage() {
        mainactivity.changeFragment(MyPageFragment())
    }

    private fun updateRecyclerView() {
        val iterator = imgInfos.iterator()
        while (iterator.hasNext()) {
            val img = iterator.next()
            if (!Utils.isImageExists(img.uri, requireActivity())) {
                val index = imgInfos.indexOf(img)
                iterator.remove()
                (binding.galleryRecyclerview.adapter as GalleryItemListAdapter).notifyItemRemoved(index)
            }
        }
    }
}
