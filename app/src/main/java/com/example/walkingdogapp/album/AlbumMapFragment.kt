package com.example.walkingdogapp.album

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.example.walkingdogapp.utils.utils.Utils
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.utils.utils.NetworkManager
import com.example.walkingdogapp.utils.HorizonSpacingItemDecoration
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentAlbumMapBinding
import com.example.walkingdogapp.datamodel.AlbumMapImgInfo
import com.example.walkingdogapp.viewmodel.MainViewModel
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

class AlbumMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentAlbumMapBinding? = null
    private val binding get() = _binding!!
    private val imgInfos = mutableListOf<AlbumMapImgInfo>()
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var mynavermap: NaverMap
    private lateinit var camera : CameraUpdate
    private var markers = mutableListOf<InfoWindow>()
    private lateinit var mainactivity: MainActivity

    private lateinit var itemDecoration: HorizonSpacingItemDecoration

    private var selectday = MutableLiveData("")

    private val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
        when(permission) {
            true -> {
                setAlbumMap(selectday.value ?: "")
            }
            false -> return@registerForActivityResult
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapFragment: MapFragment =
            childFragmentManager.findFragmentById(R.id.Map) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction().add(R.id.Map, it).commit()
                }
        mapFragment.getMapAsync(this)

        if (!NetworkManager.checkNetworkState(requireContext())) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("인터넷을 연결해주세요!")
            builder.setPositiveButton("네", null)
            builder.setCancelable(false)
            builder.show()
        }

        MainActivity.preFragment = "AlbumMap"
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumMapBinding.inflate(inflater, container, false)
        itemDecoration = HorizonSpacingItemDecoration(3, Utils.dpToPx(12f, requireContext()))

        binding.apply {
            isStoragePermitted = false
            isImgExisted = false
            selectDay = selectday
            lifecycleOwner = requireActivity()

            refresh.apply {
                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        mainViewModel.observeUser()
                    }
                }
                mainViewModel.successGetData.observe(requireActivity()) {
                    refresh.isRefreshing = false
                }
            }

            permissionBtn.setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                startActivity(intent)
            }

            btnSelectDate.setOnClickListener {
                if(!NetworkManager.checkNetworkState(requireContext())) {
                    return@setOnClickListener
                }
                val dialog = DateDialog()
                dialog.dateClickListener = DateDialog.OnDateClickListener { date ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        imgRecyclerView.removeItemDecoration(itemDecoration)
                        imgInfos.clear()
                        selectday.value = date
                        setAlbumMap(selectday.value ?: "")
                        removeMarker()
                        setMarker()
                        if (imgInfos.isNotEmpty()) {
                            moveCamera(imgInfos[0].latLng, CameraAnimation.Easing)
                        }
                    }
                }
                dialog.show(requireActivity().supportFragmentManager, "date")
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (checkPermission(storagePermission)) {
            setAlbumMap(selectday.value ?: "")
            if (markers.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.Main) {
                    removeMarker()
                    setMarker()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        imgInfos.clear()
        binding.imgRecyclerView.removeItemDecoration(itemDecoration)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setAlbumMap(selectDate: String) {
        getAlbumImage(selectDate)
        setRecyclerView(selectDate)
    }

    @SuppressLint("SimpleDateFormat")
    private fun getAlbumImage(selectDate: String) {
        binding.isStoragePermitted = true

        if (selectDate.isEmpty()) {
            return
        }
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
        val selection =
            "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?"
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
            val columnIndexDate: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val imageDate = Utils.convertLongToTime(SimpleDateFormat("yyyy-MM-dd"), cursor.getLong(columnIndexDate))
                val imagePath: String = cursor.getString(columnIndexId)
                val contentUri = Uri.withAppendedPath(uri, imagePath)
                val imgView = getMarkerImageView(contentUri)
                if (imageDate == selectDate) {
                    getImgLatLng(contentUri, requireContext())?.let {
                        imgInfos.add(AlbumMapImgInfo(contentUri, it, imgView))
                    }

                    if (imgInfos.size == 20) {
                        return
                    }
                }
            }
        }
    }

    private fun setRecyclerView(selectDate: String) {
        if (selectDate == "") {
            return
        }
        binding.apply {
            if (imgInfos.isNotEmpty()) {
                isImgExisted = true
                val adapter = AlbumMapItemListAdapter(imgInfos)
                adapter.itemClickListener = AlbumMapItemListAdapter.OnItemClickListener { latLng, num ->
                        for (marker in markers) {
                            if (marker.tag as Int == num) {
                                marker.zIndex = 10
                                continue
                            }
                            marker.zIndex = 0
                        }
                        moveCamera(latLng, CameraAnimation.Easing)
                    }

                imgRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                imgRecyclerView.addItemDecoration(itemDecoration)
                imgRecyclerView.adapter = adapter
            }
        }
    }

    private fun getMarkerImageView(uri: Uri): ImageView {
        val imgView = ImageView(requireContext())
        imgView.layoutParams = ViewGroup.LayoutParams(200, 200)
        imgView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(requireContext()).load(uri).format(DecodeFormat.PREFER_RGB_565)
            .override(200, 200).into(imgView)
        return imgView
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

            return null
        } catch (e: Exception) {
            return null
        }
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
        mynavermap.uiSettings.setAllGesturesEnabled(false)
        mynavermap.uiSettings.isZoomControlEnabled = false

        binding.zoom.map = mynavermap

        if(imgInfos.isNotEmpty()) {
            moveCamera(imgInfos[0].latLng, CameraAnimation.None)
        } else {
            mainViewModel.currentCoord.value?.let {
                val coord = mainViewModel.currentCoord.value!!
                moveCamera(LatLng(coord.latitude, coord.longitude), CameraAnimation.None)
            }
        }
    }

    private fun moveCamera(latLng: LatLng, animation: CameraAnimation) {
        camera = CameraUpdate.scrollAndZoomTo(latLng, 17.0).animate(animation)
        mynavermap.moveCamera(camera)
    }

    private suspend fun setMarker() {
        delay(500)
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
        if (markers.isNotEmpty()) {
            markers[0].zIndex = 10
        }
    }

    private fun removeMarker() {
        for(marker in markers) {
            marker.map = null
        }
        markers.clear()
    }
}