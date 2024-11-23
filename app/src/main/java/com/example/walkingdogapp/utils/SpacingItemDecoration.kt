package com.example.walkingdogapp.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class GridSpacingItemDecoration(
    private val spanCount: Int, // Grid의 column 수
    private val spacing: Int // 간격
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position: Int = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0
        val totalRows = (itemCount + spanCount - 1) / spanCount

        if (position >= 0) {
            val column = position % spanCount
            val currentRow = position / spanCount
            val isLastRow = currentRow == totalRows - 1

            outRect.apply {
                // spacing - column * ((1f / spanCount) * spacing)
                left = spacing - column * spacing / spanCount
                // (column + 1) * ((1f / spanCount) * spacing)
                right = (column + 1) * spacing / spanCount
                top = spacing
                if (isLastRow) {
                    bottom = spacing
                }
            }
        } else {
            outRect.apply {
                left = 0
                right = 0
                top = 0
                bottom = 0
            }
        }
    }
}

class HorizonSpacingItemDecoration(
    private val itemCount: Int, // 아이템 갯수
    private val spacing: Int // 간격
) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position: Int = parent.getChildAdapterPosition(view)

        if (position >= 0) {
            outRect.apply {
                left = if (position == 0) spacing else spacing / 2
                right = if (position == itemCount - 1) spacing else spacing / 2
                top = spacing
                bottom = spacing
            }
        } else {
            outRect.apply {
                left = 0
                right = 0
                top = 0
                bottom = 0
            }
        }
    }
}