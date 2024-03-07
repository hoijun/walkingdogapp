package com.example.walkingdogapp.collection

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.walkingdogapp.databinding.FragmentCollectionBinding
import com.example.walkingdogapp.userinfo.userInfoViewModel

class CollectionFragment : Fragment() {
    private var _binding: FragmentCollectionBinding? = null
    private val binding get() = _binding!!
    private val myViewModel: userInfoViewModel by activityViewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectionBinding.inflate(inflater, container, false)
        val list = listOf("a", "b", "c", "d", "e")
        val gridListManager = GridLayoutManager(requireContext(), 3)
        val adapter = CollectionlistAdapter(list)
        binding.apply {
            collectionRecyclerview.layoutManager = gridListManager
            collectionRecyclerview.addItemDecoration(GridSpacingItemDecoration(3, dpTopx(15f)))
            collectionRecyclerview.adapter = adapter
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    fun dpTopx(dp: Float): Int {
        val metrics = requireContext().resources.displayMetrics;
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }
}