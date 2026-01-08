package com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.detailWalkInfoPage

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.overlay.PathOverlay
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.common.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.databinding.FragmentDetailWalkInfoBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.core.dialog.SelectDialog
import com.tulmunchi.walkingdogapp.presentation.model.CollectionData
import com.tulmunchi.walkingdogapp.presentation.model.CollectionInfo
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.ui.walking.CurrentCollectionItemListAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DetailWalkInfoFragment : Fragment(), OnMapReadyCallback { // 수정
    private var _binding: FragmentDetailWalkInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var naverMap: NaverMap
    private var walkPath = PathOverlay()
    private lateinit var camera : CameraUpdate
    private var day = listOf<String>()
    private var walkRecord: WalkRecord? = null

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateToWalkInfo()
        }
    }

    @Inject
    lateinit var navigationManager: NavigationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapFragment: MapFragment =
            childFragmentManager.findFragmentById(R.id.Map) as MapFragment?
                ?: MapFragment.newInstance().also {
                    childFragmentManager.beginTransaction().add(R.id.Map, it).commit()
                }
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailWalkInfoBinding.inflate(inflater,container, false)

        walkRecord = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("selectDateRecord", WalkRecord::class.java)
        } else {
            arguments?.getParcelable("selectDateRecord")
        }

        val currentCollections = arrayListOf<CollectionInfo>()

        val itemDecoration = context?.let { ctx ->
            GridSpacingItemDecoration(3, UiUtils.dpToPx(20f, ctx))
        } ?: GridSpacingItemDecoration(3, 20) // 기본값

        walkRecord?.collections?.forEach {
            currentCollections.add(CollectionData.collectionMap[it]?: CollectionInfo())
        }

        val emptyCollections = arrayListOf(CollectionInfo())
        binding.apply {
            btnBack.setOnClickListener {
                navigateToWalkInfo()
            }

            day = walkRecord?.day?.split("-") ?: listOf()
            walkDay = day
            walkRecordInfo = walkRecord

            val collectionAdapter = if (currentCollections.isEmpty()) CurrentCollectionItemListAdapter(emptyCollections) else CurrentCollectionItemListAdapter(currentCollections)
            context?.let { ctx ->
                getCollectionRecyclerView.layoutManager = GridLayoutManager(ctx, 3)
            }
            getCollectionRecyclerView.addItemDecoration(itemDecoration)
            getCollectionRecyclerView.adapter = collectionAdapter
            getCollectionRecyclerView.isNestedScrollingEnabled = false
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(map: NaverMap) {
        this.naverMap = map
        naverMap.uiSettings.setAllGesturesEnabled(false)
        naverMap.uiSettings.isZoomControlEnabled = false

        binding.zoom.map = naverMap

        walkPath.width = 15
        walkPath.color = Color.parseColor("#b67e36")

        val walkCoords = walkRecord?.walkCoordinates?.map {
            LatLng(it.latitude, it.longitude)
        } ?: listOf()

        if (walkCoords.size > 1) {
            camera = CameraUpdate.scrollAndZoomTo(walkCoords[walkCoords.size / 2], 16.0) //
            naverMap.moveCamera(camera)

            addStartMarker(walkCoords.first())

            addEndMarker(walkCoords.last())

            walkRecord?.poopCoordinates?.forEach {
                addPoopMarker(LatLng(it.latitude, it.longitude))
            }

            walkRecord?.memoCoordinates?.forEach {
                addMemoMarker(LatLng(it.value.latitude, it.value.longitude), it.key)
            }

            walkPath.coords = walkCoords
            walkPath.map = naverMap
        }
    }

    private fun addStartMarker(start: LatLng) {
        val startMarker = Marker()
        startMarker.position = start
        startMarker.icon = OverlayImage.fromResource(R.drawable.icon_flag_start)
        startMarker.width = 150
        startMarker.height = 150
        startMarker.map = naverMap
    }

    private fun addEndMarker(end: LatLng) {
        val endMarker = Marker()
        endMarker.position = end
        endMarker.icon = OverlayImage.fromResource(R.drawable.icon_flag_end)
        endMarker.width = 150
        endMarker.height = 150
        endMarker.map = naverMap
    }

    private fun addPoopMarker(position: LatLng) {
        val marker = Marker()
        marker.position = position
        marker.icon = OverlayImage.fromResource(R.drawable.icon_poo)
        marker.width = 150
        marker.height = 150
        marker.map = naverMap
    }

    private fun addMemoMarker(position: LatLng, content: String) {
        val marker = Marker()
        marker.position = position
        marker.icon = OverlayImage.fromResource(R.drawable.icon_note)
        marker.width = 150
        marker.height = 150
        marker.map = naverMap
        marker.tag = content

        marker.setOnClickListener {
            // tag에서 메모 내용 가져오기
            val memoContent = it.tag as? String ?: ""
            val dialog = SelectDialog.newInstance(title = memoContent)
            dialog.show(childFragmentManager, "memo")
            true
        }
    }

    private fun navigateToWalkInfo() {
        val selectDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectDog", Dog::class.java)?: Dog(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0L
            )
        } else {
            (arguments?.getSerializable("selectDog") ?: Dog(
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                0L
            )) as Dog
        }

        navigationManager.navigateTo(NavigationState.WithoutBottomNav.WalkInfo(day, selectDog))
    }
}