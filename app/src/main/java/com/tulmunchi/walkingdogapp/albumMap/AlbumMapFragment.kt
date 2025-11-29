package com.tulmunchi.walkingdogapp.albumMap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraAnimation
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.InfoWindow
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.FragmentAlbumMapBinding
import com.tulmunchi.walkingdogapp.datamodel.AlbumMapImgInfo
import com.tulmunchi.walkingdogapp.common.HorizonSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.utils.Utils
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat

@AndroidEntryPoint
class AlbumMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentAlbumMapBinding? = null
    private val binding get() = _binding!!
    private val imgInfos = mutableListOf<AlbumMapImgInfo>()
    private val mainViewModel: MainViewModel by activityViewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

    private lateinit var mynavermap: NaverMap
    private lateinit var camera : CameraUpdate
    private var markers = mutableListOf<InfoWindow>()
    private var mainActivity: MainActivity? = null

    private var itemDecoration: HorizonSpacingItemDecoration? = null

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

        context?.let { ctx ->
            if (!networkChecker.isNetworkAvailable()) {
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("인터넷을 연결해주세요!")
                builder.setPositiveButton("네", null)
                builder.setCancelable(false)
                builder.show()
            }
        }

        MainActivity.preFragment = "AlbumMap"
        activity?.let {
            mainActivity = it as? MainActivity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumMapBinding.inflate(inflater, container, false)
        context?.let { ctx ->
            itemDecoration = HorizonSpacingItemDecoration(3, Utils.dpToPx(12f, ctx))
        }

        binding.apply {
            isStoragePermitted = false
            isImgExisted = false
            selectDay = selectday
            lifecycleOwner = viewLifecycleOwner

            refresh.apply {
                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        mainViewModel.observeUser()
                    }
                }
                mainViewModel.successGetData.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            permissionBtn.setOnClickListener {
                handlePermissionButtonClick()
            }

            btnSelectDate.setOnClickListener {
                handleDateSelectClick()
            }
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.VISIBLE)
        context?.let { ctx ->
            if (checkPermission(storagePermission, ctx)) {
                setAlbumMap(selectday.value ?: "")
                if (markers.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        removeMarker()
                        setMarker()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        imgInfos.clear()
        itemDecoration?.let { decoration ->
            binding.imgRecyclerView.removeItemDecoration(decoration)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    private fun handlePermissionButtonClick() {
        context?.let { ctx ->
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.fromParts("package", ctx.packageName, null)
            startActivity(intent)
        }
    }

    private fun handleDateSelectClick() {
        context?.let { ctx ->
            if (!networkChecker.isNetworkAvailable()) {
                return
            }

            val dialog = DateDialog()
            dialog.dateClickListener = DateDialog.OnDateClickListener { date ->
                lifecycleScope.launch(Dispatchers.Main) {
                    itemDecoration?.let { decoration ->
                        binding.imgRecyclerView.removeItemDecoration(decoration)
                    }
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

            parentFragmentManager.let {
                try {
                    dialog.show(it, "date")
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 다이얼로그 표시 실패 시 로그만 기록
                }
            }
        }
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

        try {
            val contentResolver = activity?.contentResolver ?: return

            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection =
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)

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

            cursor?.use { it ->
                val columnIndexId: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val columnIndexDate: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                while (it.moveToNext()) {
                    val imageDate = Utils.convertLongToTime(
                        SimpleDateFormat("yyyy-MM-dd"),
                        it.getLong(columnIndexDate) / 1000L
                    )
                    val imagePath: String = it.getString(columnIndexId)
                    val contentUri = Uri.withAppendedPath(uri, imagePath)
                    val imgView = getMarkerImageView(contentUri)
                    if (imageDate == selectDate) {
                        context?.let { ctx ->
                            getImgLatLng(contentUri, ctx)?.let { latLng ->
                                imgInfos.add(AlbumMapImgInfo(contentUri, latLng, imgView))
                            }
                        }

                        if (imgInfos.size == 20) {
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            context?.let { ctx ->
                Toast.makeText(ctx, "이미지를 불러오는 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
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

                context?.let { ctx ->
                    imgRecyclerView.layoutManager = LinearLayoutManager(ctx, LinearLayoutManager.HORIZONTAL, false)
                }
                itemDecoration?.let { decoration ->
                    imgRecyclerView.addItemDecoration(decoration)
                }
                imgRecyclerView.adapter = adapter
            }
        }
    }

    private fun getMarkerImageView(uri: Uri): ImageView? {
        return context?.let { ctx ->
            val imgView = ImageView(ctx)
            imgView.layoutParams = ViewGroup.LayoutParams(200, 200)
            imgView.scaleType = ImageView.ScaleType.CENTER_CROP

            try {
                Glide.with(ctx).load(uri).format(DecodeFormat.PREFER_ARGB_8888)
                    .override(200, 200)
                    .error(R.drawable.collection_003)
                    .into(imgView)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            imgView
        }
    }

    private fun getImgLatLng(uri: Uri, context: Context): LatLng? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exifInterface = inputStream?.let { ExifInterface(it) }
            val latLng = exifInterface?.getLatLong()

            if(latLng != null)  {
                return LatLng(latLng[0], latLng[1])
            }

            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun checkPermission(permissions : Array<out String>, context: Context) : Boolean{
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    context,
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
                val coord = mainViewModel.currentCoord.value ?: return@let
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
        context?.let { ctx ->
            for ((markerNum, imgInfo) in imgInfos.withIndex()) {
                val imgMarker = InfoWindow()
                imgMarker.zIndex = 0
                imgMarker.adapter = object : InfoWindow.DefaultViewAdapter(ctx) {
                    override fun getContentView(p0: InfoWindow): View {
                        return imgInfo.imgView?.rootView ?: ImageView(ctx)
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
    }

    private fun removeMarker() {
        for(marker in markers) {
            marker.map = null
        }
        markers.clear()
    }
}