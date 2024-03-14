package com.example.walkingdogapp.album

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.HorizonSpacingItemDecoration
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentAlbumMapBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker

class AlbumMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentAlbumMapBinding? = null
    private val binding get() = _binding!!
    private val imgInfos = mutableListOf<AlbumMapImgInfo>()
    private lateinit var adaptar: AlbumMapitemlistAdaptar
    private val myViewModel: userInfoViewModel by activityViewModels()

    private lateinit var mynavermap: NaverMap
    private lateinit var camera : CameraUpdate
    private var markers = mutableListOf<InfoWindow>()

    private val storegePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { storagePermission ->
        when(storagePermission) {
            true -> {
                getAlbumImage()
                setRecyclerView()
            }
            false -> return@registerForActivityResult
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapFragment: MapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction().add(R.id.map_fragment, it).commit()
                }
        mapFragment.getMapAsync(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumMapBinding.inflate(inflater, container, false)
        binding.permissionBtn.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data =
                Uri.fromParts("package", requireContext().packageName, null)
            startActivity(intent)
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storegePermission)) {
            getAlbumImage()
            setRecyclerView()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setRecyclerView() {
        binding.apply {
            permissionBtn.visibility = View.GONE
            imgRecyclerView.visibility = View.VISIBLE
            adaptar = AlbumMapitemlistAdaptar(imgInfos , requireContext())
            adaptar.itemClickListener = AlbumMapitemlistAdaptar.OnItemClickListener { latLng, num ->
                moveCamera(latLng, CameraAnimation.Easing)
                for(marker in markers) {
                    if(marker.tag as Int == num) {
                        marker.zIndex = 10
                        continue
                    }
                    marker.zIndex = 0
                }
            }
            imgRecyclerView.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            imgRecyclerView.addItemDecoration(HorizonSpacingItemDecoration(3, Constant.dpTopx(12f, requireContext())))
            imgRecyclerView.adapter = adaptar
        }
    }

    private fun getAlbumImage() {
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("털뭉치")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val cursor = requireActivity().contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use { cursor ->
            val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val imagePath: String = cursor.getString(columnIndex)
                val contentUri = Uri.withAppendedPath(uri, imagePath)
                val imgView = ImageView(requireContext())
                imgView.layoutParams = ViewGroup.LayoutParams(150, 150)
                imgView.scaleType = ImageView.ScaleType.FIT_XY
                Glide.with(requireContext()).load(contentUri).override(300, 300).into(imgView)
                getImgLatLng(contentUri, requireContext())?.let { imgInfos.add(AlbumMapImgInfo(contentUri, it, imgView)) }
            }
        }
    }

    private fun getImgLatLng(uri: Uri, context: Context): LatLng? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLng = FloatArray(2)
            val hasLatLng = exifInterface?.getLatLong(latLng)

            if(hasLatLng == true)  {
                return LatLng(latLng[0].toDouble(), latLng[1].toDouble())
            }
        } catch (e: Exception) {
            return null
        }
        return null
    }

    private fun checkPermission(permissions : Array<out String>) : Boolean{
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

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false

        mynavermap.uiSettings.isZoomControlEnabled = false

        if(imgInfos.isNotEmpty()) {
            moveCamera(imgInfos[0].latLng, CameraAnimation.None)
        } else {
            myViewModel.currentCoord.value?.let {
                val coord = myViewModel.currentCoord.value!!
                moveCamera(LatLng(coord.latitude, coord.longitude), CameraAnimation.None)
            }
        }
        for ((markerNum, imgInfo) in imgInfos.withIndex()) {
            val imgMarker = InfoWindow()
            imgMarker.zIndex = 0
            imgMarker.adapter = object : InfoWindow.DefaultViewAdapter(requireContext()) {
                override fun getContentView(p0: InfoWindow): View {
                    return imgInfo.imgView?.rootView ?: ImageView(requireContext())
                }
            }
            imgMarker.tag = markerNum
            imgInfo.tag = imgMarker.tag as Int
            imgMarker.position = imgInfo.latLng
            imgMarker.map = mynavermap
            markers.add(imgMarker)
        }
    }

    private fun moveCamera(latLng: LatLng, animation: CameraAnimation, ) {
        camera = CameraUpdate.scrollAndZoomTo(latLng, 17.0).animate(animation)
        mynavermap.moveCamera(camera)
    }
}