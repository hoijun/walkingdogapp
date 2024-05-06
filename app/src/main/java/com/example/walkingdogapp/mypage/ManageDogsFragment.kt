package com.example.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.databinding.FragmentManageDogsBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.userinfo.UserInfoViewModel

class ManageDogsFragment : Fragment() {
    private var _binding: FragmentManageDogsBinding? = null
    private val myViewModel: UserInfoViewModel by activityViewModels()
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMypage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.GONE
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
        MainActivity.preFragment = "Manage"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDogsBinding.inflate(inflater,container, false)
        val dogsList = myViewModel.dogsinfo.value?: listOf()
        val manageDogListAdapter = ManageDogListAdapter(dogsList, requireContext(), myViewModel)
        manageDogListAdapter.onitemClickListener = ManageDogListAdapter.OnitemClickListener {
            val dogInfoFragment = DogInfoFragment().apply {
                val bundle = Bundle()
                bundle.putSerializable("doginfo", it)
                bundle.putString("before", "manage")
                arguments = bundle
            }
            mainactivity.changeFragment(dogInfoFragment)
        }

        binding.apply {
            if (dogsList.size > 2) {
                btnAddDog.visibility = View.GONE
            }

            DogsRecyclerView.adapter = manageDogListAdapter

            btnAddDog.setOnClickListener {
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                startActivity(registerDogIntent)
            }

            btnBack.setOnClickListener {
                goMypage()
            }
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

    private fun goMypage() {
        mainactivity.changeFragment(MyPageFragment())
    }
}