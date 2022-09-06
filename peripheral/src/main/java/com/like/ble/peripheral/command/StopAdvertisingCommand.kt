package com.like.ble.peripheral.command

import com.like.ble.command.Command

/**
 * 停止广播命令
 */
class StopAdvertisingCommand : Command("停止广播命令", immediately = true)