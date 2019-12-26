package com.like.ble.sample.behavior

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import com.like.ble.sample.R

class RecyclerViewBehavior(context: Context, attrs: AttributeSet) : CoordinatorLayout.Behavior<RecyclerView>(context, attrs) {
    override fun layoutDependsOn(parent: CoordinatorLayout, child: RecyclerView, dependency: View): Boolean {
        return dependency.id == R.id.ll_header
    }

    override fun onLayoutChild(parent: CoordinatorLayout, child: RecyclerView, layoutDirection: Int): Boolean {
        parent.getDependencies(child)
            .firstOrNull { it.id == R.id.ll_header }
            ?.let { header ->
                val lp = child.layoutParams as CoordinatorLayout.LayoutParams
                val available = Rect()
                available.set(
                    parent.paddingLeft + lp.leftMargin,
                    header.bottom + lp.topMargin,
                    parent.width - parent.paddingRight - lp.rightMargin,
                    parent.height - parent.paddingBottom - lp.bottomMargin
                )
                val out = Rect()
                GravityCompat.apply(
                    resolveGravity(getFinalGravity(lp.gravity)),
                    child.measuredWidth,
                    child.measuredHeight,
                    available,
                    out,
                    layoutDirection
                )
                child.layout(out.left, out.top, out.right, out.bottom)
                return true
            }
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    // 获取当前控件的`layout_gravity`属性
    private fun getFinalGravity(gravity: Int): Int =
        when {
            gravity and Gravity.VERTICAL_GRAVITY_MASK == 0 -> gravity or Gravity.TOP
            gravity and Gravity.HORIZONTAL_GRAVITY_MASK == 0 -> gravity or Gravity.START
            else -> gravity
        }

    private fun resolveGravity(gravity: Int): Int =
        if (gravity == Gravity.NO_GRAVITY) GravityCompat.START or Gravity.TOP else gravity

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: RecyclerView, dependency: View): Boolean {
        //计算列表y坐标，最小为0
        var y = dependency.height + dependency.translationY
        if (y < 0) {
            y = 0f
        }
        child.y = y
        return true
    }
}