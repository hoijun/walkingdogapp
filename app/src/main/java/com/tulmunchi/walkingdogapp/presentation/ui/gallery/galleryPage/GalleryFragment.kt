package com.tulmunchi.walkingdogapp.presentation.ui.gallery.galleryPage

import android.Manifest
import android.app.Activity
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.tulmunchi.walkingdogapp.common.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentGalleryBinding
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.util.ImageUtils
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener
import com.tulmunchi.walkingdogapp.presentation.viewmodel.GalleryViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val galleryViewModel: GalleryViewModel by activityViewModels()
    private var itemDecoration: GridSpacingItemDecoration? = null

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var navigationManager: NavigationManager

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                setGallery()
            }
        }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (galleryViewModel.isSelectMode()) {
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
            lifecycleOwner = viewLifecycleOwner

            galleryViewModel.selectMode.observe(viewLifecycleOwner) { isSelect ->
                isSelectMode = isSelect
            }

            btnBack.setOnClickListener {
                goMyPage()
            }

            imgRemoveBtn.setOnSingleClickListener {
                handleImageRemoval()
            }
        }

        // 이미지 로드 완료 시 RecyclerView 설정
        galleryViewModel.imgInfos.observe(viewLifecycleOwner) { images ->
            if (images.isNotEmpty()) {
                setRecyclerView()
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // onStart에서는 권한 요청 없이 체크만 수행 (API 27에서 중복 호출 방지)
        if (permissionHandler.checkPermissions(requireActivity(), storagePermission)) {
            updateRecyclerView()
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
        _binding = null
    }

    private fun handleImageRemoval() {
        try {
            if (galleryViewModel.isRemoveListEmpty()) {
                unSelectMode()
                return
            }

            val contentResolver = activity?.contentResolver ?: return
            val removeList = galleryViewModel.getRemoveList()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intentSender = MediaStore.createDeleteRequest(
                    contentResolver,
                    removeList
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
        val removeList = galleryViewModel.getRemoveList()
        val dialog = SelectDialog.newInstance(title = "사진을 삭제하시겠습니까?", showNegativeButton = true)
        dialog.onConfirmListener = SelectDialog.OnConfirmListener {
            for (uri in removeList) {
                contentResolver.delete(uri, null, null)
            }
            updateRecyclerView()
            unSelectMode()
        }
        dialog.show(parentFragmentManager, "deleteConfirm")
    }

    private fun setGallery() {
        galleryViewModel.loadImages()
    }

    private fun setRecyclerView() {
        binding.apply {
            val itemAnimator = DefaultItemAnimator()
            itemAnimator.supportsChangeAnimations = false
            galleryRecyclerview.itemAnimator = itemAnimator

            val imgNum = arguments?.getInt("currentImgIndex", 0) ?: 0
            val imgInfos = galleryViewModel.getImages()
            val adapter = GalleryItemListAdapter(imgInfos.toMutableList())
            adapter.itemClickListener = object : GalleryItemListAdapter.OnItemClickListener {
                override fun onItemClick(imgNum: Int) {
                    if (!galleryViewModel.isSelectMode()) {
                        navigationManager.navigateTo(NavigationState.WithoutBottomNav.DetailPicture(imgNum))
                    }
                }

                override fun onItemLongClick(imgUri: Uri) {
                    galleryViewModel.enterSelectMode()
                    galleryViewModel.toggleImageSelection(imgUri)
                }

                override fun onItemClickInSelectMode(imgUri: Uri) {
                    galleryViewModel.toggleImageSelection(imgUri)
                }
            }
            context?.let { ctx ->
                galleryRecyclerview.layoutManager = GridLayoutManager(ctx, 3)
            }
            galleryRecyclerview.scrollToPosition(imgNum)
            galleryRecyclerview.adapter = adapter
        }
    }

    private fun checkPermission(permissions: Array<String>): Boolean {
        return if (!permissionHandler.checkPermissions(requireActivity(), permissions)) {
            requestStoragePermission.launch(permissions)
            false
        } else {
            true
        }
    }

    private fun unSelectMode() {
        galleryViewModel.exitSelectMode()
        (binding.galleryRecyclerview.adapter as? GalleryItemListAdapter)?.unselectMode()
    }

    private fun goMyPage() {
        navigationManager.navigateTo(NavigationState.WithBottomNav.MyPage)
    }

    private fun updateRecyclerView() {
        val imgInfos = galleryViewModel.getImages().toMutableList()
        val indicesToRemove = mutableListOf<Int>()

        imgInfos.forEachIndexed { index, img ->
            activity?.let { act ->
                if (!ImageUtils.isImageExists(img.uri, act)) {
                    indicesToRemove.add(index)
                }
            }
        }

        indicesToRemove.sortedDescending().forEach { index ->
            galleryViewModel.removeImageAt(index)
            (binding.galleryRecyclerview.adapter as? GalleryItemListAdapter)?.notifyItemRemoved(index)
        }
    }
}