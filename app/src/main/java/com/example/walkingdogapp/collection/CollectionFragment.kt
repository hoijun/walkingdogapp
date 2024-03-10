package com.example.walkingdogapp.collection

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.R
import com.example.walkingdogapp.databinding.FragmentCollectionBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel

class CollectionFragment : Fragment() {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()
    private lateinit var collections: List<CollectionInfo>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val collectionInfo = myViewModel.collectioninfo.value ?: Constant.item_whether
        collections = listOf(
            CollectionInfo(
                "001",
                "하영 지원 등장!",
                "최애의 아이돌.",
                if (collectionInfo["001"]!!) R.drawable.collection_001 else R.drawable.waitimage,
                listOf("하영", "지원", "송하영", "박지원", "하냥", "하빵", "메건")
            ),
            CollectionInfo(
                "002",
                "치킨 버거 서연",
                "치킨 버거 패티",
                if (collectionInfo["002"]!!) R.drawable.collection_002 else R.drawable.waitimage,
                listOf("서연", "셔니", "이서연", "더여니")
            ),
            CollectionInfo(
                "003",
                "냥과 하영",
                "냥이 둘",
                if (collectionInfo["003"]!!) R.drawable.collection_003 else R.drawable.waitimage,
                listOf("하영", "송하영", "하냥", "하빵")
            ),
            CollectionInfo(
                "004",
                "마법 소녀 나경",
                "마법 소녀 변신!",
                if (collectionInfo["004"]!!) R.drawable.collection_004 else R.drawable.waitimage,
                listOf("나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "005",
                "앉아있는 채영",
                "앉아 있는 채영",
                if (collectionInfo["005"]!!) R.drawable.collection_005 else R.drawable.waitimage,
                listOf("채영", "이채영", "챙이")
            ),
            CollectionInfo(
                "006",
                "바이올린 규리",
                "바이올린 연주 중인 규리",
                if (collectionInfo["006"]!!) R.drawable.collection_006 else R.drawable.waitimage,
                listOf("규리", "장규리", "귤", "귤공")
            ),
            CollectionInfo(
                "007",
                "냥과 서연",
                "냥이 한입",
                if (collectionInfo["007"]!!) R.drawable.collection_007 else R.drawable.waitimage,
                listOf("서연", "셔니", "이서연", "더여니")
            ),
            CollectionInfo(
                "008",
                "떨어지는 빵빵즈",
                "빵빵즈가 간다!",
                if (collectionInfo["008"]!!) R.drawable.collection_008 else R.drawable.waitimage,
                listOf("하영", "송하영", "하냥", "하빵", "서연", "셔니", "이서연", "더여니", "채영", "이채영", "챙이", "나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "009",
                "모만타이 나경",
                "모만타이!",
                if (collectionInfo["009"]!!) R.drawable.collection_009 else R.drawable.waitimage,
                listOf("나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "010",
                "울음 참는 채영",
                "울음 멈춰!",
                if (collectionInfo["010"]!!) R.drawable.collection_010 else R.drawable.waitimage,
                listOf("채영", "이채영", "챙이")
            ),
            CollectionInfo(
                "011",
                "모만타이 나경2",
                "모만타이!",
                if (collectionInfo["011"]!!) R.drawable.collection_011 else R.drawable.waitimage,
                listOf("나경", "이나꼬", "이나경", "나꼬")
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        val gridListManager = GridLayoutManager(requireContext(), 3)
        val adapter = CollectionlistAdapter(collections, requireContext())
        adapter.itemClickListener = CollectionlistAdapter.OnItemClickListener { collection ->
            Log.d("savepoint", collection.collectionText)
        }

        binding.apply {
            collectionRecyclerview.layoutManager = gridListManager
            collectionRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, dpTopx(15f)))
            collectionRecyclerview.adapter = adapter

            openSearching.setOnClickListener {
                // 입력창의 보이는 지 안보이는 지에 따라
                if(collectionSearch.visibility == View.GONE) {
                    collectionSearch.visibility = View.VISIBLE
                    openSearching.setImageResource(com.google.android.material.R.drawable.material_ic_menu_arrow_up_black_24dp)
                } else {
                    collectionSearch.visibility = View.GONE
                    openSearching.setImageResource(android.R.drawable.ic_menu_search)
                }
            }

            searchview.setOnQueryTextListener(object: androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter?.filter(newText) // 입력 값에 따라 도감 필터
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
    private fun dpTopx(dp: Float): Int {
        val metrics = requireContext().resources.displayMetrics;
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }
}