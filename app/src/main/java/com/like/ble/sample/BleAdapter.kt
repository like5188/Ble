package com.like.ble.sample

import android.app.Activity
import android.view.Gravity
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.like.ble.IBleManager
import com.like.ble.command.*
import com.like.ble.sample.databinding.ItemBleBinding
import com.like.livedatarecyclerview.adapter.BaseAdapter
import com.like.livedatarecyclerview.model.IRecyclerViewItem
import com.like.livedatarecyclerview.viewholder.CommonViewHolder
import java.util.*

class BleAdapter(private val mActivity: Activity, private val mBleManager: IBleManager) :
    BaseAdapter() {
    private val mCommandArray = arrayOf("读特征", "写特征", "设置MTU", "开启通知", "关闭通知", "开启标志", "关闭标志", "写特征并接收通知")

    override fun bindOtherVariable(
        holder: CommonViewHolder,
        position: Int,
        item: IRecyclerViewItem?
    ) {
        super.bindOtherVariable(holder, position, item)
        if (item !is BleInfo) return
        val binding = holder.binding
        if (binding !is ItemBleBinding) return
        val address = item.address
        binding.tvConnectStatus.setOnClickListener {
            binding.tvConnectStatus.setTextColor(ContextCompat.getColor(mActivity, R.color.ble_text_black_1))
            if (item.isConnected.get()) {
                binding.tvConnectStatus.text = "断开连接中……"
                mBleManager.sendCommand(
                    DisconnectCommand(
                        address,
                        {
                            item.isConnected.set(false)
                        },
                        {
                            item.isConnected.set(true)
                            item.isConnected.notifyChange()// 必须调用，否则不能触发更新界面，因为本来就是true
                        })
                )
            } else {
                binding.tvConnectStatus.text = "连接中……"
                mBleManager.sendCommand(
                    ConnectCommand(
                        address,
                        5000L,
                        {
                            item.isConnected.set(true)
                        },
                        {
                            item.isConnected.set(false)
                            item.isConnected.notifyChange()// 必须调用，否则不能触发更新界面，因为本来就是false
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
                        5000,
                        300,
                        {
                            true
                        },
                        {
                            mActivity.shortToastCenter("读特征成功 ${it?.contentToString()}")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    1 -> WriteCharacteristicCommand(
                        byteArrayOf(0x1),
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        5000,
                        20,
                        {
                            mActivity.shortToastCenter("写特征成功")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    2 -> SetMtuCommand(
                        address,
                        50,
                        {
                            mActivity.shortToastCenter("设置MTU成功 $it")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    3 -> EnableCharacteristicNotifyCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.shortToastCenter("开启通知成功")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    4 -> DisableCharacteristicNotifyCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.shortToastCenter("关闭通知成功")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    5 -> EnableCharacteristicIndicateCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.shortToastCenter("开启标志成功")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    6 -> DisableCharacteristicIndicateCommand(
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        "00002902-0000-1000-8000-00805f9b34fb",
                        {
                            mActivity.shortToastCenter("关闭标志成功")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
                        }
                    )
                    7 -> WriteNotifyCommand(
                        byteArrayOf(0x2),
                        address,
                        "0000fff2-0000-1000-8000-00805f9b34fb",
                        5000,
                        20,
                        300,
                        {
                            true
                        },
                        {
                            mActivity.shortToastCenter("写数据并获取通知数据成功：${Arrays.toString(it)}")
                        },
                        {
                            mActivity.shortToastCenter(it.message)
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