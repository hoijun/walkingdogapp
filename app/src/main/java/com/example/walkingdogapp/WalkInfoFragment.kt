package com.example.walkingdogapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.example.walkingdogapp.databinding.FragmentWalkInfoBinding
import kotlin.system.exitProcess


class WalkInfoFragment : Fragment() {
    private lateinit var mainactivity: MainActivity
    private var _binding: FragmentWalkInfoBinding? = null
    private val binding get() = _binding!!

    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            mainactivity.changeFragment(MyPageFragment())
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
        _binding = FragmentWalkInfoBinding.inflate(inflater,container, false)
        return binding.root
    }
}