package com.like.ble.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.like.ble.CentralManager
import com.like.ble.IBleManager
import com.like.ble.command.ConnectCommand
import com.like.ble.command.DisconnectCommand
import com.like.ble.sample.databinding.ActivityConnectBinding

class ConnectActivity : AppCompatActivity() {
    private val mBinding: ActivityConnectBinding by lazy {
        DataBindingUtil.setContentView<ActivityConnectBinding>(this, R.layout.activity_connect)
    }
    private val mBleManager: IBleManager by lazy { CentralManager(this) }
    private lateinit var mData: BleInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mData = intent.getSerializableExtra("data") as? BleInfo ?: throw UnsupportedOperationException("data is null")
        mBinding.setVariable(BR.bleInfo, mData)
    }

    fun connect(view: View) {
        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_black_1))
        mBinding.tvConnectStatus.text = "连接中……"
        mBleManager.sendCommand(
            ConnectCommand(
                mData.address,
                10000L,
                {
                    runOnUiThread {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_blue))
                        mBinding.tvConnectStatus.text = "连接成功"
                    }
                },
                {
                    runOnUiThread {
                        mBinding.tvConnectStatus.setTextColor(ContextCompat.getColor(this, R.color.ble_text_red))
                        mBinding.tvConnectStatus.text = it.message
                    }
                })
        )
    }

    fun disconnect(view: View) {
        mBleManager.sendCommand(DisconnectCommand(mData.address))
    }

    override fun onDestroy() {
        mBleManager.close()
        super.onDestroy()
    }

}