package com.like.ble.sample

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(private val fragments: List<Fragment>, fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    /**
     * 默认实现适用于不添加、移动和移除项的集合。因为默认使用 position 来作为 itemId，这在添加删除时，会出错的，因为 position 时随添加删除而变化的
     */
    override fun getItemId(position: Int): Long {
        return if (position == 0) {// 扫描界面
            0
        } else {// 连接界面
            (fragments[position] as BleConnectFragment).getBleScanInfo().hashCode().toLong()
        }
    }

}
