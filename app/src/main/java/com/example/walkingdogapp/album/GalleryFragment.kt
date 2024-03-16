package com.example.walkingdogapp.album

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.deco.GridSpacingItemDecoration
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentGalleryBinding
import com.example.walkingdogapp.mypage.MyPageFragment
import java.text.SimpleDateFormat
import java.util.Calendar


class GalleryFragment : Fragment() {
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private lateinit var mainactivity: MainActivity

    private val imgInfos = mutableListOf<GalleryImgInfo>()
    private var adaptar: GalleryitemlistAdaptar? = null

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
                }

                false -> return@registerForActivityResult
            }
        }

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMypage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = activity as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storegePermission)) {
            getAlbumImage()
            setRecyclerView()
        }
    }

    override fun onStop() {
        super.onStop()
        imgInfos.clear()
        binding.galleryRecyclerview.removeItemDecorationAt(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setRecyclerView() {
        binding.apply {
            permissionBtn.visibility = View.GONE
            galleryRecyclerview.visibility = View.VISIBLE
            btnBack.setOnClickListener {
                goMypage()
            }
            adaptar = GalleryitemlistAdaptar(imgInfos, requireContext())
            adaptar!!.itemClickListener = GalleryitemlistAdaptar.OnItemClickListener { imgInfo ->
                Log.d("savepoint", imgInfo.date + " " + imgInfo.title)
            }
            galleryRecyclerview.layoutManager = GridLayoutManager(requireContext(), 3)
            galleryRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, Constant.dpTopx(15f, requireContext())))
            galleryRecyclerview.adapter = adaptar
        }
    }

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
                imgInfos.add(GalleryImgInfo(contentUri, imageTitle, convertLongToTime(imageDate)))
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun convertLongToTime(time: Long): String {
        val format = SimpleDateFormat("yyyy.MM.dd HH:mm")
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time * 1000;
        return format.format(calendar.time)
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

