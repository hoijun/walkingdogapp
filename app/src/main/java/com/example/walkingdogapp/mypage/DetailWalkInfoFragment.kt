package com.example.walkingdogapp.mypage

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
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.Utils
import com.example.walkingdogapp.databinding.FragmentDetailWalkInfoBinding
import com.example.walkingdogapp.datamodel.CollectionInfo
import com.example.walkingdogapp.datamodel.DogInfo
import com.example.walkingdogapp.datamodel.WalkRecord
import com.example.walkingdogapp.deco.GridSpacingItemDecoration
import com.example.walkingdogapp.walking.CurrentCollectionItemListAdapter
import com.example.walkingdogapp.walking.WalkingService
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.PathOverlay

class DetailWalkInfoFragment : Fragment(), OnMapReadyCallback { // 수정
    private var _binding: FragmentDetailWalkInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var mynavermap: NaverMap
    private var walkPath = PathOverlay()
    private lateinit var camera : CameraUpdate
    private var day = listOf<String>()
    private var walkRecord = WalkRecord()

    private lateinit var mainactivity: MainActivity
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goWalkInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
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
            arguments?.getSerializable("selectDateRecord", WalkRecord::class.java)?: WalkRecord()
        } else {
            (arguments?.getSerializable("selectDateRecord") ?: WalkRecord()) as WalkRecord
        }

        val collectionsMap = Utils.setCollectionMap()
        val currentCollections = arrayListOf<CollectionInfo>()
        val itemDecoration = GridSpacingItemDecoration(3, Utils.dpToPx(20f, requireContext()))
        walkRecord.collections.toList().forEach {
            currentCollections.add(collectionsMap[it]?: CollectionInfo())
        }

        val emptyCollections = arrayListOf(CollectionInfo())


        binding.apply {
            btnGoMypage.setOnClickListener {
                goWalkInfo()
            }

            day = walkRecord.day.split("-")
            walkDay = day
            walkRecordInfo = walkRecord

            val collectionAdapter = if (currentCollections.isEmpty()) CurrentCollectionItemListAdapter(emptyCollections) else CurrentCollectionItemListAdapter(currentCollections)
            getCollectionRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
            getCollectionRecyclerView.addItemDecoration(itemDecoration)
            getCollectionRecyclerView.adapter = collectionAdapter
            getCollectionRecyclerView.isNestedScrollingEnabled = false
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

        for (coord in walkRecord.coords) {
            walkCoords.add(LatLng(coord.latitude, coord.longitude))
        }

        if (walkCoords.size > 2) {
            camera =
                CameraUpdate.scrollAndZoomTo(walkCoords[walkCoords.size / 2], 16.0) //
            mynavermap.moveCamera(camera)

            walkPath.coords = walkCoords
            walkPath.map = mynavermap
        }
    }

    private fun goWalkInfo() {
        val selectDog = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectDog", DogInfo::class.java)?: DogInfo()
        } else {
            (arguments?.getSerializable("selectDog") ?: DogInfo()) as DogInfo
        }
        val bundle = Bundle()
        bundle.putStringArrayList("selectDateRecord", day as ArrayList<String>)
        bundle.putSerializable("selectDog", selectDog)
        val walkInfoFragment = WalkInfoFragment().apply {
            arguments = bundle
        }
        mainactivity.changeFragment(walkInfoFragment)
    }
}