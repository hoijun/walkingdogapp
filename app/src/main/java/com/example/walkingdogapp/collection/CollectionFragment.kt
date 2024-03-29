package com.example.walkingdogapp.collection

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walkingdogapp.Constant
import com.example.walkingdogapp.deco.GridSpacingItemDecoration
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
                "a",
                "최애의 아이돌.",
                if (collectionInfo["001"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("하영", "지원", "송하영", "박지원", "하냥", "하빵", "메건")
            ),
            CollectionInfo(
                "002",
                "b",
                "치킨 버거 패티",
                if (collectionInfo["002"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("서연", "셔니", "이서연", "더여니")
            ),
            CollectionInfo(
                "003",
                "c",
                "냥이 둘",
                if (collectionInfo["003"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("하영", "송하영", "하냥", "하빵")
            ),
            CollectionInfo(
                "004",
                "d",
                "마법 소녀 변신!",
                if (collectionInfo["004"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "005",
                "e",
                "앉아 있는 채영",
                if (collectionInfo["005"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("채영", "이채영", "챙이")
            ),
            CollectionInfo(
                "006",
                "f",
                "바이올린 연주 중인 규리",
                if (collectionInfo["006"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("규리", "장규리", "귤", "귤공")
            ),
            CollectionInfo(
                "007",
                "g",
                "냥이 한입",
                if (collectionInfo["007"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("서연", "셔니", "이서연", "더여니")
            ),
            CollectionInfo(
                "008",
                "h",
                "빵빵즈가 간다!",
                if (collectionInfo["008"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("하영", "송하영", "하냥", "하빵", "서연", "셔니", "이서연", "더여니", "채영", "이채영", "챙이", "나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "009",
                "i",
                "모만타이!",
                if (collectionInfo["009"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("나경", "이나꼬", "이나경", "나꼬")
            ),
            CollectionInfo(
                "010",
                "j",
                "울음 멈춰!",
                if (collectionInfo["010"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
                listOf("채영", "이채영", "챙이")
            ),
            CollectionInfo(
                "011",
                "k",
                "모만타이!",
                if (collectionInfo["011"]!!) R.drawable.waitimage else R.drawable.ic_launcher_background,
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
        val adaptar = CollectionlistAdaptar(collections, requireContext())
        adaptar.itemClickListener = CollectionlistAdaptar.OnItemClickListener { collection ->
            if (collection.collectionImg != R.drawable.waitimage) {
                val detailCollectionDialog = DetailCollectionDialog().apply {
                    val bundle = Bundle()
                    bundle.putSerializable("collectionInfo", collection)
                    arguments = bundle
                }
                detailCollectionDialog.show(
                    requireActivity().supportFragmentManager,
                    "detailcollection"
                )
            }
        }

        binding.apply {
            collectionRecyclerview.layoutManager = gridListManager
            collectionRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, Constant.dpTopx(15f, requireContext())))
            collectionRecyclerview.adapter = adaptar

            openSearching.setOnClickListener {
                // 입력창이 보이는 지 안보이는 지에 따라
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
                    adaptar.filter.filter(newText) // 입력 값에 따라 도감 필터
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
}