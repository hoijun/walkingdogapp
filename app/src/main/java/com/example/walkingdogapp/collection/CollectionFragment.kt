package com.example.walkingdogapp.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.walkingdogapp.utils.utils.Utils
import com.example.walkingdogapp.MainActivity
import com.example.walkingdogapp.utils.utils.NetworkManager
import com.example.walkingdogapp.utils.utils.GridSpacingItemDecoration
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentCollectionBinding
import com.example.walkingdogapp.datamodel.CollectionInfo
import com.example.walkingdogapp.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CollectionFragment : Fragment() {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var collections: List<CollectionInfo>
    private lateinit var mainactivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val collectionInfo = mainViewModel.collectionInfo.value ?: Utils.item_whether
        collections = setCollection(collectionInfo)

        if (!NetworkManager.checkNetworkState(requireContext())) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("인터넷을 연결해주세요!")
            builder.setPositiveButton("네", null)
            builder.setCancelable(false)
            builder.show()
        }

        MainActivity.preFragment = "Collection"
        mainactivity = requireActivity() as MainActivity
        mainactivity.binding.menuBn.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        val gridListManager = GridLayoutManager(requireContext(), 3)
        val adapter = CollectionListAdapter(collections)
        adapter.itemClickListener = CollectionListAdapter.OnItemClickListener { collection ->
            if (mainViewModel.collectionInfo.value?.get(collection.collectionNum) == true) {
                val detailCollectionDialog = DetailCollectionDialog().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("collectionInfo", collection)
                    arguments = bundle
                }
                detailCollectionDialog.show(
                    requireActivity().supportFragmentManager,
                    "detailCollection"
                )
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
                mainViewModel.successGetData.observe(requireActivity()) {
                    refresh.isRefreshing = false
                }
            }

            collectionRecyclerview.layoutManager = gridListManager
            collectionRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, Utils.dpToPx(15f, requireContext())))
            collectionRecyclerview.adapter = adapter
            collectionRecyclerview.itemAnimator = null

            openSearching.setOnClickListener {
                // 입력창이 보이는 지 안보이는 지에 따라
                if(collectionSearch.visibility == View.GONE) {
                    collectionSearch.visibility = View.VISIBLE
                    openSearching.setImageResource(com.google.android.material.R.drawable.material_ic_menu_arrow_up_black_24dp)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setCollection(collectionInfo: HashMap<String, Boolean>): List<CollectionInfo> {
        return listOf(
            CollectionInfo(
                "001",
                "밥알 곰",
                "나 이제 잘래..zz",
                if (collectionInfo.getOrDefault("001", false)) R.drawable.collection_001 else R.drawable.waitimage
            ),
            CollectionInfo(
                "002",
                "밥알 고양이",
                "츄르 내놔랑 냥!",
                if (collectionInfo.getOrDefault("002", false)) R.drawable.collection_002 else R.drawable.waitimage
            ),
            CollectionInfo(
                "003",
                "밥알 원숭이",
                "우끼끼! 나랑 놀자!",
                if (collectionInfo.getOrDefault("003", false)) R.drawable.collection_003 else R.drawable.waitimage
            ),
            CollectionInfo(
                "004",
                "밥알 펭귄",
                "나도 날고 싶다!",
                if (collectionInfo.getOrDefault("004", false)) R.drawable.collection_004 else R.drawable.waitimage
            ),
            CollectionInfo(
                "005",
                "밥알 쿼카",
                "나 만지면 벌금!",
                if (collectionInfo.getOrDefault("005", false)) R.drawable.collection_005 else R.drawable.waitimage
            ),
            CollectionInfo(
                "006",
                "밥알 토끼",
                "나 달로 돌아갈래~",
                if (collectionInfo.getOrDefault("006", false)) R.drawable.collection_006 else R.drawable.waitimage
            ),
            CollectionInfo(
                "007",
                "노트북 하는 강아지",
                "과제 힘들어..",
                if (collectionInfo.getOrDefault("007", false)) R.drawable.collection_007 else R.drawable.waitimage
            ),
            CollectionInfo(
                "008",
                "웃고있는 강아지",
                "헤헤..",
                if (collectionInfo.getOrDefault("008", false)) R.drawable.collection_008 else R.drawable.waitimage
            ),
            CollectionInfo(
                "009",
                "양치하는 강아지",
                "치카치카",
                if (collectionInfo.getOrDefault("009", false)) R.drawable.collection_009 else R.drawable.waitimage
            ),
            CollectionInfo(
                "010",
                "신난 코알라",
                "시인나안다아",
                if (collectionInfo.getOrDefault("010", false)) R.drawable.collection_010 else R.drawable.waitimage
            ),
            CollectionInfo(
                "011",
                "신난 고양이",
                "냥냥냥",
                if (collectionInfo.getOrDefault("011", false)) R.drawable.collection_011 else R.drawable.waitimage
            ),
            CollectionInfo(
                "012",
                "힘든 곰돌이",
                "힘들어...",
                if (collectionInfo.getOrDefault("012", false)) R.drawable.collection_012 else R.drawable.waitimage
            ),
            CollectionInfo(
                "013",
                "하얀 강아지",
                "멍멍!",
                if (collectionInfo.getOrDefault("013", false)) R.drawable.collection_013 else R.drawable.waitimage
            ),
            CollectionInfo(
                "014",
                "책 읽는 강아지",
                "음....",
                if (collectionInfo.getOrDefault("014", false)) R.drawable.collection_014 else R.drawable.waitimage
            ),
            CollectionInfo(
                "015",
                "치킨 먹는 강아지",
                "헤헤.. 맛있당",
                if (collectionInfo.getOrDefault("015", false)) R.drawable.collection_015 else R.drawable.waitimage
            ),
            CollectionInfo(
                "016",
                "귀여운 다람쥐",
                "반갑습니다람쥐",
                if (collectionInfo.getOrDefault("016", false)) R.drawable.collection_016 else R.drawable.waitimage
            ),
            CollectionInfo(
                "017",
                "책 읽는 돼지",
                "흡.. 휴",
                if (collectionInfo.getOrDefault("017", false)) R.drawable.collection_017 else R.drawable.waitimage
            ),
            CollectionInfo(
                "018",
                "행복한 곰돌이",
                "치킨 맛있당",
                if (collectionInfo.getOrDefault("018", false)) R.drawable.collection_018 else R.drawable.waitimage
            ),
            CollectionInfo(
                "019",
                "일보는 강아지",
                "저리가..",
                if (collectionInfo.getOrDefault("019", false)) R.drawable.collection_019 else R.drawable.waitimage
            ),
            CollectionInfo(
                "020",
                "귀여운 곰",
                "데헷!",
                if (collectionInfo.getOrDefault("020", false)) R.drawable.collection_020 else R.drawable.waitimage
            ),
            CollectionInfo(
                "021",
                "핸드폰 하는 악어",
                "뒹굴 뒹굴",
                if (collectionInfo.getOrDefault("021", false)) R.drawable.collection_021 else R.drawable.waitimage
            ),
            CollectionInfo(
                "022",
                "하트 강아지",
                "이거 받아",
                if (collectionInfo.getOrDefault("022", false)) R.drawable.collection_022 else R.drawable.waitimage
            ),
            CollectionInfo(
                "023",
                "버블티 강아지",
                "헤헤.. 시원해",
                if (collectionInfo.getOrDefault("023", false)) R.drawable.collection_023 else R.drawable.waitimage
            ),
            CollectionInfo(
                "024",
                "튜브 토끼",
                "신난당!",
                if (collectionInfo.getOrDefault("024", false)) R.drawable.collection_024 else R.drawable.waitimage
            )
        )
    }
}