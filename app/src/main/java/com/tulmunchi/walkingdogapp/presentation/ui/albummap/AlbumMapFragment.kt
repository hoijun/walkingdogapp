package com.tulmunchi.walkingdogapp.presentation.ui.albummap

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.common.HorizonSpacingItemDecoration
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.core.permission.PermissionHandler
import com.tulmunchi.walkingdogapp.databinding.FragmentAlbumMapBinding
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.model.AlbumMapImgInfo
import com.tulmunchi.walkingdogapp.presentation.util.setOnSingleClickListener
import com.tulmunchi.walkingdogapp.presentation.viewmodel.AlbumMapViewModel
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class AlbumMapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentAlbumMapBinding? = null
    private val binding get() = _binding!!
    private val imgInfos = mutableListOf<AlbumMapImgInfo>()
    private val mainViewModel: MainViewModel by activityViewModels()
    private val albumMapViewModel: AlbumMapViewModel by activityViewModels()

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private lateinit var myNaverMap: NaverMap
    private lateinit var camera : CameraUpdate
    private var markers = mutableListOf<InfoWindow>()

    private var itemDecoration: HorizonSpacingItemDecoration? = null

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
                albumMapViewModel.setStoragePermissionGranted(true)
                albumMapViewModel.selectedDay.value?.let { date ->
                    if (date.isNotEmpty()) {
                        albumMapViewModel.loadAlbumImages(date)
                    }
                }
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

        if (!networkChecker.isNetworkAvailable()) {
            val dialog = SelectDialog.newInstance(title = "인터넷을 연결해주세요!")
            dialog.isCancelable = false
            dialog.show(parentFragmentManager, "networkCheck")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAlbumMapBinding.inflate(inflater, container, false)
        context?.let { ctx ->
            itemDecoration = HorizonSpacingItemDecoration(UiUtils.dpToPx(12f, ctx))
        }

        binding.apply {
            albumViewModel = albumMapViewModel
            lifecycleOwner = viewLifecycleOwner

            refresh.apply {
                this.setOnRefreshListener {
                    mainViewModel.loadUserData()
                }
                mainViewModel.dataLoadSuccess.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            permissionBtn.setOnClickListener {
                handlePermissionButtonClick()
            }

            btnSelectDate.setOnSingleClickListener {
                handleDateSelectClick()
            }
        }

        // ViewModel 데이터 관찰
        albumMapViewModel.albumImages.observe(viewLifecycleOwner) { images ->
            imgInfos.clear()

            // AlbumImageData를 AlbumMapImgInfo로 변환
            val imgInfosWithView = images.map { data ->
                AlbumMapImgInfo(
                    uri = data.uriString.toUri(),
                    latLng = LatLng(data.latitude, data.longitude),
                    imgView = getMarkerImageView(data.uriString.toUri())
                )
            }

            imgInfos.addAll(imgInfosWithView)

            // RecyclerView 설정
            if (imgInfos.isNotEmpty()) {
                setRecyclerView()

                // 마커 업데이트
                if (::myNaverMap.isInitialized) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        removeMarker()
                        setMarker()
                        moveCamera(imgInfos[0].latLng, CameraAnimation.Easing)
                    }
                }
            } else {
                binding.isImgExisted = false
            }
        }

        albumMapViewModel.storagePermissionGranted.observe(viewLifecycleOwner) { granted ->
            binding.isStoragePermitted = granted
        }

        albumMapViewModel.selectedDay.observe(viewLifecycleOwner) { day ->
            binding.selectDay = day
        }

        albumMapViewModel.hasImages.observe(viewLifecycleOwner) { hasImages ->
            binding.isImgExisted = hasImages
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        context?.let {
            // 권한 요청은 하지 않고, 권한이 있는 경우에만 데이터 로드
            if (checkPermission(storagePermission)) {
                albumMapViewModel.setStoragePermissionGranted(true)
                albumMapViewModel.selectedDay.value?.let { date ->
                    if (date.isNotEmpty()) {
                        albumMapViewModel.loadAlbumImages(date)
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
                itemDecoration?.let { decoration ->
                    binding.imgRecyclerView.removeItemDecoration(decoration)
                }
                albumMapViewModel.clearImages()
                albumMapViewModel.selectDate(date)
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

    private fun setRecyclerView() {
        binding.apply {
            if (imgInfos.isNotEmpty()) {
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

    private fun checkPermission(permissions : Array<String>) : Boolean {
        val hasPermission = permissionHandler.checkPermissions(requireActivity(), permissions)
        return if (!hasPermission) {
            requestStoragePermission.launch(permissions)
            false
        } else {
            true
        }
    }

    override fun onMapReady(map: NaverMap) {
        this.myNaverMap = map
        myNaverMap.uiSettings.setAllGesturesEnabled(false)
        myNaverMap.uiSettings.isZoomControlEnabled = false

        binding.zoom.map = myNaverMap

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
        myNaverMap.moveCamera(camera)
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
                imgMarker.map = myNaverMap
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