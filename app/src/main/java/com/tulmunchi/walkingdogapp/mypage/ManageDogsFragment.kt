package com.tulmunchi.walkingdogapp.mypage

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.databinding.FragmentManageDogsBinding
import com.tulmunchi.walkingdogapp.registerinfo.RegisterDogActivity
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ManageDogsFragment : Fragment() {
    private var _binding: FragmentManageDogsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goMyPage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            mainActivity = it as? MainActivity
        }

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
            goDogInfoPage(dogInfoFragment)
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
                mainViewModel.successGetData.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            btnAddDog.setOnClickListener {
                context?.let { ctx ->
                    if (!NetworkManager.checkNetworkState(ctx) || !mainViewModel.isSuccessGetData()) {
                        return@let
                    }
                    val registerDogIntent = Intent(ctx, RegisterDogActivity::class.java)
                    startActivity(registerDogIntent)
                }
            }

            btnBack.setOnClickListener {
                goMyPage()
            }
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

    private fun goMyPage() {
        mainActivity?.changeFragment(MyPageFragment())
    }

    private fun goDogInfoPage(fragment: DogInfoFragment) {
        mainActivity?.changeFragment(fragment)
    }
}