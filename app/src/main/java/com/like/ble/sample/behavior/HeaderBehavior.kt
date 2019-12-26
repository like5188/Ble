package com.like.ble.sample.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HeaderBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<LinearLayout>(context, attrs) {
    // 界面整体向上滑动，达到列表可滑动的临界点
    private var upReach: Boolean = false
    // 列表向上滑动后，再向下滑动，达到界面整体可滑动的临界点
    private var downReach: Boolean = false
    // 列表上一个全部可见的item位置
    private var lastPosition = -1

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: LinearLayout, ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downReach = false
                upReach = false
            }
        }
        return super.onInterceptTouchEvent(parent, child, ev)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: LinearLayout,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: LinearLayout,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
        if (target is RecyclerView) {
            // 列表第一个全部可见Item的位置
            val pos = (target.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            if (pos == 0 && pos < lastPosition) {
                downReach = true
            }
            // 整体可以滑动，否则RecyclerView消费滑动事件
            if (canScroll(child, dy.toFloat()) && pos == 0) {
                var finalY = child.translationY - dy
                if (finalY < -child.height) {
                    finalY = (-child.height).toFloat()
                    upReach = true
                } else if (finalY > 0) {
                    finalY = 0f
                }
                child.translationY = finalY
                // 让CoordinatorLayout消费滑动事件
                consumed[1] = dy
            }
            lastPosition = pos
        }
    }

    private fun canScroll(child: View, scrollY: Float): Boolean {
        if (scrollY > 0 && child.translationY == (-child.height).toFloat() && !upReach) {
            return false
        }

        return !downReach
    }
}