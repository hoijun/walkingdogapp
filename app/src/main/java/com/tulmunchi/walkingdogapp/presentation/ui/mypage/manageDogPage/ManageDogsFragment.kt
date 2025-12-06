package com.tulmunchi.walkingdogapp.presentation.ui.mypage.manageDogPage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tulmunchi.walkingdogapp.core.network.NetworkChecker
import com.tulmunchi.walkingdogapp.databinding.FragmentManageDogsBinding
import com.tulmunchi.walkingdogapp.presentation.ui.main.MainActivity
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationManager
import com.tulmunchi.walkingdogapp.presentation.ui.main.NavigationState
import com.tulmunchi.walkingdogapp.presentation.ui.mypage.myPagePage.MyPageFragment
import com.tulmunchi.walkingdogapp.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ManageDogsFragment : Fragment() {
    private var _binding: FragmentManageDogsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val binding get() = _binding!!
    private var mainActivity: MainActivity? = null

    @Inject
    lateinit var networkChecker: NetworkChecker

    @Inject
    lateinit var navigationManager: NavigationManager

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDogsBinding.inflate(inflater,container, false)
        val dogsList = mainViewModel.dogs.value?: listOf()
        val dogImages = mainViewModel.dogImages.value ?: emptyMap()

        val manageDogListAdapter = ManageDogListAdapter(dogsList, dogImages)
        manageDogListAdapter.onItemClickListener = ManageDogListAdapter.OnItemClickListener {
            navigationManager.navigateTo(
                NavigationState.WithoutBottomNav.DogInfo(
                    dog = it,
                    before = "manage"
                )
            )
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
                    mainViewModel.loadUserData()
                }
                mainViewModel.dataLoadSuccess.observe(viewLifecycleOwner) {
                    refresh.isRefreshing = false
                }
            }

            btnAddDog.setOnClickListener {
                if (!networkChecker.isNetworkAvailable() || !mainViewModel.isSuccessGetData()) {
                    return@setOnClickListener
                }
                navigationManager.navigateTo(
                    NavigationState.WithoutBottomNav.RegisterDog(
                        dog = null,
                        from = "manage"
                    )
                )
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
}