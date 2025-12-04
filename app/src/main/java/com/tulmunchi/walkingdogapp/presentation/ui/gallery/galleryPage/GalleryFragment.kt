package com.tulmunchi.walkingdogapp.presentation.ui.gallery.galleryPage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.tulmunchi.walkingdogapp.common.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentGalleryBinding
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.model.GalleryImgInfo
import com.tulmunchi.walkingdogapp.presentation.ui.gallery.detailOfPicturePage.DetailPictureFragment
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.util.DateUtils
import com.tulmunchi.walkingdogapp.presentation.util.ImageUtils
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import javax.inject.Inject

@AndroidEntryPoint
class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val imgInfos = mutableListOf<GalleryImgInfo>()
    private val removeImgList = mutableListOf<Uri>()
    private var itemDecoration: GridSpacingItemDecoration? = null
    private var selectMode = MutableLiveData(false)

    @Inject
    lateinit var permissionHandler: PermissionHandler

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
                    itemDecoration?.let { decoration ->
                        binding.galleryRecyclerview.addItemDecoration(decoration)
                    }
                }

                false -> return@registerForActivityResult
            }
        }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(selectMode.value == true) {
                unSelectMode()
            } else {
                goMyPage()
            }
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        when(result.resultCode) {
            Activity.RESULT_OK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                updateRecyclerView()
                unSelectMode()
                context?.let { ctx ->
                    Toast.makeText(ctx, "사진을 삭제 했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

        context?.let { ctx ->
            itemDecoration = GridSpacingItemDecoration(3, UiUtils.dpToPx(15f, ctx))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        context?.let {
            if (checkPermission(storagePermission)) {
                setGallery()
            }
        }

        binding.apply {
            isSelectMode = selectMode
            lifecycleOwner = viewLifecycleOwner

            btnBack.setOnClickListener {
                goMyPage()
            }

            imgRemoveBtn.setOnClickListener {
                handleImageRemoval()
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
        context?.let {
            if (checkPermission(storagePermission)) {
                updateRecyclerView()
            }
        }
        itemDecoration?.let { decoration ->
            binding.galleryRecyclerview.addItemDecoration(decoration)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onStop() {
        super.onStop()
        unSelectMode()
        itemDecoration?.let { decoration ->
            binding.galleryRecyclerview.removeItemDecoration(decoration)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun handleImageRemoval() {
        try {
            if (removeImgList.isEmpty()) {
                unSelectMode()
                return
            }

            val contentResolver = activity?.contentResolver ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaStore.createDeleteRequest(
                    contentResolver,
                    removeImgList
                ).intentSender
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                showDeleteConfirmDialog()
            }
        } catch (e: Exception) {
            context?.let { ctx ->
                Toast.makeText(ctx, "삭제하는 데 오류가 발생 했어요.", Toast.LENGTH_SHORT).show()
            }
            unSelectMode()
        }
    }

    private fun showDeleteConfirmDialog() {
        val contentResolver = activity?.contentResolver ?: return
        context?.let { ctx ->
            val builder = AlertDialog.Builder(ctx)
            builder.setTitle("사진을 삭제하시겠습니까?")
            val listener = DialogInterface.OnClickListener { _, ans ->
                when (ans) {
                    DialogInterface.BUTTON_POSITIVE -> {
                        for (uri in removeImgList) {
                            contentResolver.delete(uri, null, null)
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
                    if (selectMode.value == false) {
                        val bundle = Bundle()
                        bundle.putInt("select", imgNum)
                        val detailPictureFragment = DetailPictureFragment().apply {
                            arguments = bundle
                        }
                        mainActivity?.changeFragment(detailPictureFragment)
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
            context?.let { ctx ->
                galleryRecyclerview.layoutManager = GridLayoutManager(ctx, 3)
            }
            galleryRecyclerview.scrollToPosition(imgNum)
            galleryRecyclerview.adapter = adapter
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun getAlbumImage() {
        try {
            val contentResolver = activity?.contentResolver ?: return

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.TITLE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.ORIENTATION
            )

            val selection: String
            val selectionArgs: Array<String>

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.IS_PENDING} = 0"
                selectionArgs = arrayOf("털뭉치", "%munchi_%")
            } else {
                selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
                selectionArgs = arrayOf("털뭉치", "%munchi_%")
            }

            val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"
            val cursor = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
            cursor?.use {
                val columnIndexId: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val columnIndexTitle: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE)
                val columnIndexDate: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val columnIndexWidth: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val columnIndexHeight: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val columnIndexOrientation: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.ORIENTATION)

                while (it.moveToNext()) {
                    val imagePath: String = it.getString(columnIndexId)
                    val imageTitle: String = it.getString(columnIndexTitle)
                    val imageDate: Long = it.getLong(columnIndexDate)
                    val imageWidth: Int = it.getInt(columnIndexWidth)
                    val imageHeight: Int = it.getInt(columnIndexHeight)
                    val contentUri = Uri.withAppendedPath(uri, imagePath)
                    val orientation = it.getInt(columnIndexOrientation)

                    val (finalWidth, finalHeight) = when (orientation) {
                        90, 270 -> Pair(imageHeight, imageWidth)
                        else -> Pair(imageWidth, imageHeight)
                    }

                    imgInfos.add(
                        GalleryImgInfo(
                            contentUri,
                            imageTitle,
                            DateUtils.convertLongToTime(
                                SimpleDateFormat("yyyy년 MM월 dd일 HH:mm"),
                                imageDate / 1000L
                            ),
                            finalWidth,
                            finalHeight
                        )
                    )
                }
            }
        } catch (e: Exception) {
            context?.let { ctx ->
                Toast.makeText(ctx, "이미지를 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT)
                    .show()
            }
        } finally {
            mainViewModel.saveAlbumImgs(imgInfos)
        }
    }

    private fun checkPermission(permissions: Array<String>): Boolean {
        return if (!permissionHandler.checkPermissions(requireActivity(), permissions)) {
            requestStoragePermission.launch(permissions[0])
            false
        } else {
            true
        }
    }

    private fun unSelectMode() {
        selectMode.value = false
        removeImgList.clear()
        (binding.galleryRecyclerview.adapter as GalleryItemListAdapter).unselectMode()
    }

    private fun goMyPage() {
        mainActivity?.changeFragment(MyPageFragment())
    }

    private fun updateRecyclerView() {
        val iterator = imgInfos.iterator()
        while (iterator.hasNext()) {
            val img = iterator.next()
            activity?.let { act ->
                if (!ImageUtils.isImageExists(img.uri, act)) {
                    val index = imgInfos.indexOf(img)
                    iterator.remove()
                    (binding.galleryRecyclerview.adapter as? GalleryItemListAdapter)?.notifyItemRemoved(index)
                }
            }
        }
    }
}