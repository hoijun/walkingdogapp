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
import com.naver.maps.map.overlay.PathOverlay
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.common.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.databinding.FragmentDetailWalkInfoBinding
import com.tulmunchi.walkingdogapp.domain.model.Dog
import com.tulmunchi.walkingdogapp.domain.model.WalkRecord
import com.tulmunchi.walkingdogapp.presentation.core.UiUtils
import com.tulmunchi.walkingdogapp.presentation.model.CollectionData
import com.tulmunchi.walkingdogapp.presentation.model.CollectionInfo
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.walkInfoOfDogsPage.walkInfoWithCalendarPage.WalkInfoFragment
import com.tulmunchi.walkingdogapp.presentation.ui.walking.CurrentCollectionItemListAdapter

class DetailWalkInfoFragment : Fragment(), OnMapReadyCallback { // 수정
    private var _binding: FragmentDetailWalkInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var mynavermap: NaverMap
    private var walkPath = PathOverlay()
    private lateinit var camera : CameraUpdate
    private var day = listOf<String>()
    private var walkRecord: WalkRecord? = null

    private var mainActivity: MainActivity? = null
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goWalkInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

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
            btnGoMypage.setOnClickListener {
                goWalkInfo()
            }

            day = walkRecord?.day?.split("-") ?: listOf()
            walkDay = day
            walkRecordInfo = walkRecord

            val collectionAdapter = if (currentCollections.isEmpty()) CurrentCollectionItemListAdapter(
                emptyCollections
            ) else CurrentCollectionItemListAdapter(currentCollections)
            context?.let { ctx ->
                getCollectionRecyclerView.layoutManager = GridLayoutManager(ctx, 3)
            }
            getCollectionRecyclerView.addItemDecoration(itemDecoration)
            getCollectionRecyclerView.adapter = collectionAdapter
            getCollectionRecyclerView.isNestedScrollingEnabled = false
        }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.GONE)
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.setAllGesturesEnabled(false)
        mynavermap.uiSettings.isZoomControlEnabled = false

        binding.zoom.map = mynavermap

        walkPath.width = 15
        walkPath.color = Color.YELLOW

        val walkCoords = mutableListOf<LatLng>()

        walkRecord?.coords?.forEach { coord ->
            walkCoords.add(LatLng(coord.latitude, coord.longitude))
        }

        if (walkCoords.size > 1) {
            camera =
                CameraUpdate.scrollAndZoomTo(walkCoords[walkCoords.size / 2], 16.0) //
            mynavermap.moveCamera(camera)

            walkPath.coords = walkCoords
            walkPath.map = mynavermap
        }
    }

    private fun goWalkInfo() {
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
        val bundle = Bundle()
        bundle.putStringArrayList("selectDateRecord", day as ArrayList<String>)
        bundle.putSerializable("selectDog", selectDog)
        val walkInfoFragment = WalkInfoFragment().apply {
            arguments = bundle
        }
        mainActivity?.changeFragment(walkInfoFragment)
    }
}