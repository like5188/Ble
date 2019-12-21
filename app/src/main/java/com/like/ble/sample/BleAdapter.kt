package com.like.ble.sample

import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.os.Build
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.like.ble.IBleManager
import com.like.ble.command.*
import com.like.ble.sample.databinding.ItemBleScanBinding
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder

class BleAdapter(private val mActivity: Activity, private val mBleManager: IBleManager) :
    BaseAdapter() {
    private val mCommandArray =
        arrayOf(
            "读特征",
            "写特征",
            "设置MTU",
            "读RSSI",
            "RequestConnectionPriority",
            "开启notify",
            "关闭notify",
            "开启indicate",
            "关闭indicate",
            "读取通知传来的数据"
        )

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        super.bindOtherVariable(holder, position, item)
        if (item !is BleInfo) return
        val binding = holder.binding
        if (binding !is ItemBleScanBinding) return
        val address = item.address
        binding.tvConnectStatus.setOnClickListener {
            binding.tvConnectStatus.setTextColor(ContextCompat.getColor(mActivity, R.color.ble_text_black_1))
            if (item.isConnected.get()) {
                mBleManager.sendCommand(DisconnectCommand(address))
            } else {
                binding.tvConnectStatus.text = "连接中……"
                mBleManager.sendCommand(
                    ConnectCommand(
                        address,
                        10000L,
                        {
                            if (item.isConnected.get()) {
                                item.isConnected.notifyChange()// 必须调用，否则如果本来就是true，就不能触发更新界面
                            } else {
                                item.isConnected.set(true)
                            }

                        },
                        {
                            if (item.isConnected.get()) {
                                item.isConnected.set(false)
                            } else {
                                item.isConnected.notifyChange()// 必须调用，否则如果本来就是false，就不能触发更新界面
                            }
                        })
                )
            }
        }

        binding.flexBoxLayout.removeAllViews()
        mCommandArray.forEachIndexed { index, title ->
            // 通过代码向FlexboxLayout添加View
            val textView = TextView(mActivity)
            textView.text = title
            textView.gravity = Gravity.CENTER
            textView.setPadding(20, 10, 20, 10)
            textView.setTextColor(ContextCompat.getColor(mActivity, R.color.ble_text_white))
            textView.background = ContextCompat.getDrawable(mActivity, R.drawable.flexbox_textview_bg)
            binding.flexBoxLayout.addView(textView)

            // 通过FlexboxLayout.LayoutParams 设置子元素支持的属性
            val params = textView.layoutParams
            if (params is FlexboxLayout.LayoutParams) {
                params.leftMargin = 10
                params.topMargin = 10
                params.rightMargin = 10
                params.bottomMargin = 10
            }
            textView.setOnClickListener {
                val command = when (index) {
                    0 -> ReadCharacteristicCommand(
                        address,
                        "0000fff1-0000-1000-8000-00805f9b34fb",
                        10000,
                        {
                            mActivity.longToastBottom("读特征成功。数据长度：${it?.size} ${it?.contentToString()}")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    1 -> WriteCharacteristicCommand(
                        address,
                        listOf(byteArrayOf(0x1)),
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        5000,
                        {
                            mActivity.longToastBottom("写特征成功")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    2 -> SetMtuCommand(
                        address,
                        50,
                        3000,
                        {
                            mActivity.longToastBottom("设置MTU成功 $it")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    3 -> ReadRemoteRssiCommand(
                        address,
                        3000,
                        {
                            mActivity.longToastBottom("读RSSI成功 $it")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    4 -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        RequestConnectionPriorityCommand(
                            address,
                            BluetoothGatt.CONNECTION_PRIORITY_HIGH,
                            {
                                mActivity.longToastBottom("requestConnectionPriorityCommand成功")
                            },
                            {
                                mActivity.longToastBottom(it.message)
                            }
                        )
                    } else {
                        null
                    }
                    5 -> EnableCharacteristicNotifyCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.longToastBottom("开启notify成功")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    6 -> DisableCharacteristicNotifyCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.longToastBottom("关闭notify成功")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    7 -> EnableCharacteristicIndicateCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.longToastBottom("开启indicate成功")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    8 -> DisableCharacteristicIndicateCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.longToastBottom("关闭indicate成功")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    9 -> ReadNotifyCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        5000,
                        1024,
                        {
                            it.get(it.position() - 1) == Byte.MAX_VALUE
                        },
                        {
                            mActivity.longToastBottom("读取通知传来的数据成功。数据长度：${it?.size} ${it?.contentToString()}")
                        },
                        {
                            mActivity.longToastBottom(it.message)
                        }
                    )
                    else -> null
                }
                command?.let {
                    mBleManager.sendCommand(it)
                }
            }
        }
    }

}