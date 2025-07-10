package com.tulmunchi.walkingdogapp.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.tulmunchi.walkingdogapp.MainActivity
import com.tulmunchi.walkingdogapp.R
import com.tulmunchi.walkingdogapp.databinding.FragmentCollectionBinding
import com.tulmunchi.walkingdogapp.datamodel.CollectionInfo
import com.tulmunchi.walkingdogapp.utils.GridSpacingItemDecoration
import com.tulmunchi.walkingdogapp.utils.utils.NetworkManager
import com.tulmunchi.walkingdogapp.utils.utils.Utils
import com.tulmunchi.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CollectionFragment : Fragment() {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private var collectionWhether = HashMap<String, Boolean>()
    private var collections: List<CollectionInfo> = listOf()
    private var mainActivity: MainActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionWhether = mainViewModel.collectionWhether.value ?: Utils.item_whether
        collections = Utils.setCollectionMap().values.toList().sortedBy { it.collectionNum }

        context?.let { ctx ->
            if (!NetworkManager.checkNetworkState(ctx)) {
                val builder = AlertDialog.Builder(ctx)
                builder.setTitle("인터넷을 연결해주세요!")
                builder.setPositiveButton("네", null)
                builder.setCancelable(false)
                builder.show()
            }
        }

        MainActivity.preFragment = "Collection"

        activity?.let {
            mainActivity = it as? MainActivity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        val gridListManager = context?.let { ctx ->
            GridLayoutManager(ctx, 3)
        } ?: GridLayoutManager(requireContext(), 3)

        val adapter = CollectionListAdapter(collections, collectionWhether)
        adapter.itemClickListener = CollectionListAdapter.OnItemClickListener { collection ->
            if (mainViewModel.collectionWhether.value?.get(collection.collectionNum) == true) {
                val detailCollectionDialog = DetailCollectionDialog().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("collectionInfo", collection)
                    arguments = bundle
                }

                parentFragmentManager.let {
                    try {
                        detailCollectionDialog.show(it, "detailCollection")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 다이얼로그 표시 실패 시 로그만 기록
                    }
                }
            }
        }

        binding.apply {
            refresh.apply {
                setOnChildScrollUpCallback { _, _ ->
                    if((collectionRecyclerview.adapter as CollectionListAdapter).itemCount == 0) {
                        val firstRecyclerViewItem = (collectionRecyclerview.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
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

            val itemDecoration = context?.let { ctx ->
                GridSpacingItemDecoration(3, Utils.dpToPx(15f, ctx))
            } ?: GridSpacingItemDecoration(3, 15) // 기본값

            collectionRecyclerview.layoutManager = gridListManager
            collectionRecyclerview.addItemDecoration(itemDecoration)
            collectionRecyclerview.adapter = adapter
            collectionRecyclerview.itemAnimator = null

            openSearching.setOnClickListener {
                // 입력창이 보이는 지 안보이는 지에 따라
                if(collectionSearch.isGone) {
                    collectionSearch.visibility = View.VISIBLE
                    openSearching.setImageResource(R.drawable.arrow_drop_up)
                } else {
                    collectionSearch.visibility = View.GONE
                    openSearching.setImageResource(R.drawable.search)
                }
            }

            searchview.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    (collectionRecyclerview.adapter as CollectionListAdapter).filter.filter(newText) // 입력 값에 따라 도감 필터
                    return false
                }
            })
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        mainActivity?.setMenuVisibility(View.VISIBLE)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mainActivity = null
        _binding = null
    }
}