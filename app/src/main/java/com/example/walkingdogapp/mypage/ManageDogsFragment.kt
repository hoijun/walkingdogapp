package com.example.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.utils.utils.NetworkManager
import com.example.walkingdogapp.databinding.FragmentManageDogsBinding
import com.example.walkingdogapp.registerinfo.RegisterDogActivity
import com.example.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageDogsFragment : Fragment() {
    private var _binding: FragmentManageDogsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val binding get() = _binding!!
    private lateinit var mainactivity: MainActivity
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
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
        val dogsList = mainViewModel.dogsInfo.value?: listOf()

        val manageDogListAdapter = ManageDogListAdapter(dogsList)
        manageDogListAdapter.onItemClickListener = ManageDogListAdapter.OnItemClickListener {
            val dogInfoFragment = DogInfoFragment().apply {
                val bundle = Bundle()
                bundle.putSerializable("doginfo", it)
                bundle.putString("before", "manage")
                arguments = bundle
            }
            mainactivity.changeFragment(dogInfoFragment)
        }

        binding.apply {
            countDog = dogsList.size
            DogsRecyclerView.adapter = manageDogListAdapter

            refresh.apply {
                setOnChildScrollUpCallback { _, _ ->
                    if ((DogsRecyclerView.adapter as ManageDogListAdapter).itemCount != 0) {
                        val firstRecyclerViewItem = (DogsRecyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
                        return@setOnChildScrollUpCallback firstRecyclerViewItem != 0
                    }
                    false
                }

                this.setOnRefreshListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        mainViewModel.observeUser()
                    }
                }
                mainViewModel.successGetData.observe(requireActivity()) {
                    refresh.isRefreshing = false
                }
            }

            btnAddDog.setOnClickListener {
                if(!NetworkManager.checkNetworkState(requireContext()) || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }
                val registerDogIntent = Intent(requireContext(), RegisterDogActivity::class.java)
                startActivity(registerDogIntent)
            }

            btnBack.setOnClickListener {
                goMyPage()
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

    private fun goMyPage() {
        mainactivity.changeFragment(MyPageFragment())
    }
}