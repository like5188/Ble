package com.like.ble.sample

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.like.ble.BleManager
import com.like.ble.central.handler.CentralCommandHandler
import com.like.ble.sample.databinding.ActivityBleCentralBinding
import com.like.ble.sample.databinding.ViewConnectTabBinding

/**
 * 蓝牙中心设备
 */
class BleCentralActivity : AppCompatActivity() {
    private val mBinding: ActivityBleCentralBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_ble_central)
    }
    val mBleManager: BleManager by lazy { BleManager(CentralCommandHandler(this)) }
    private val mFragments = mutableListOf<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFragments.add(BleScanFragment.newInstance())
        mBinding.vp.adapter = ViewPagerAdapter(mFragments, this)
        TabLayoutMediator(mBinding.tabLayout, mBinding.vp) { tab, position ->
            if (position == 0) {
                tab.text = "扫描"
            } else {
                if (tab.customView == null) {
                    val customViewBinding = DataBindingUtil.inflate<ViewConnectTabBinding>(
                        LayoutInflater.from(this),
                        R.layout.view_connect_tab,
                        mBinding.tabLayout,
                        false
                    )
                    customViewBinding.ivClose.setOnClickListener {
                        removeBleConnectFragment(position)
                    }
                    customViewBinding.bleScanInfo = (mFragments[position] as BleConnectFragment).getBleScanInfo()
                    tab.customView = customViewBinding.root
                }
            }
        }.attach()
    }

    fun addBleConnectFragment(bleScanInfo: BleScanInfo?) {
        // 如果存在，就显示
        mFragments.forEachIndexed { index, fragment ->
            if (fragment is BleConnectFragment && fragment.getBleScanInfo() == bleScanInfo) {
                mBinding.vp.setCurrentItem(index, true)
                return
            }
        }
        // 如果不存在，就添加
        mFragments.add(BleConnectFragment.newInstance(bleScanInfo))
        mBinding.vp.adapter?.notifyItemInserted(mFragments.size - 1)
        mBinding.vp.setCurrentItem(mFragments.size - 1, true)
    }

    private fun removeBleConnectFragment(position: Int) {
        val bleConnectFragment = mFragments.removeAt(position) as BleConnectFragment
        bleConnectFragment.disconnect()
        mBinding.vp.adapter?.notifyItemRemoved(position)
    }

}
