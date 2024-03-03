package com.example.walkingdogapp.mypage

import android.graphics.Color
import android.os.Bundle
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
import okhttp3.internal.cookieToString

class DetailWalkInfoFragment(private val dateinfo: Walkdate) : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentDetailWalkInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var mynavermap: NaverMap
    private var trackingPath = PathOverlay()
    private lateinit var trackingCamera : CameraUpdate
    private var day = listOf<String>()

    private lateinit var mainactivity: MainActivity
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goWalkInfo()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = activity as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(map: NaverMap) {
        this.mynavermap = map
        mynavermap.uiSettings.isRotateGesturesEnabled = false
        mynavermap.uiSettings.isCompassEnabled = false

        trackingPath.width = 15
        trackingPath.color = Color.YELLOW

        val walkcoords = mutableListOf<LatLng>()

        for (coord in dateinfo.coords) {
            walkcoords.add(LatLng(coord.latititude, coord.longtitude))
        }

        if (walkcoords.isNotEmpty()) {
            trackingCamera =
                CameraUpdate.scrollAndZoomTo(walkcoords[walkcoords.size / 2], 16.0) // 현재 위치로 카메라 이동
            mynavermap.moveCamera(trackingCamera)

            trackingPath.coords = walkcoords
            trackingPath.map = mynavermap
        }
    }

    private fun goWalkInfo() {
        mainactivity.changeFragment(WalkInfoFragment(day))
    }
}