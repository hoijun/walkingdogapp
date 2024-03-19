package com.example.walkingdogapp.mypage

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentDetailWalkInfoBinding
import com.example.walkingdogapp.userinfo.Walkdate
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
    private var dateinfo = Walkdate()

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
        _binding = FragmentDetailWalkInfoBinding.inflate(inflater,container, false)

        dateinfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getSerializable("selectdate", Walkdate::class.java)?: Walkdate()
        } else {
            (arguments?.getSerializable("selectdate") ?: Walkdate()) as Walkdate
        }

        binding.apply {
            btnGoMypage.setOnClickListener {
                goWalkInfo()
            }
            day = dateinfo.day.split("-")
            walkday.text = "${day[0]}년 ${day[1]}월 ${day[2]}일"
            walkstart.text = dateinfo.startTime
            walkend.text = dateinfo.endTime
            val kmdistance = "%.1f".format(dateinfo.distance / 1000.0)
            walkdistance.text = "${kmdistance}km"
            walktime.text = "${(dateinfo.time / 60)}분"
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
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false

        mynavermap.uiSettings.isZoomControlEnabled = false

        walkPath.width = 15
        walkPath.color = Color.YELLOW

        val walkcoords = mutableListOf<LatLng>()

        for (coord in dateinfo.coords) {
            walkcoords.add(LatLng(coord.latititude, coord.longtitude))
        }

        if (walkcoords.isNotEmpty()) {
            camera =
                CameraUpdate.scrollAndZoomTo(walkcoords[walkcoords.size / 2], 16.0) //
            mynavermap.moveCamera(camera)

            walkPath.coords = walkcoords
            walkPath.map = mynavermap
        }
    }

    private fun goWalkInfo() {
        val bundle = Bundle()
        bundle.putStringArrayList("selectdate", day as ArrayList<String>)
        val walkInfoFragment = WalkInfoFragment().apply {
            arguments = bundle
        }
        mainactivity.changeFragment(walkInfoFragment)
        Log.d("savepoint", "aaaaa")
    }
}