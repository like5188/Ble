package com.like.ble.model

/**
 * 蓝牙的状态
 */
enum class BleStatus(val des: String) {
    ON("蓝牙已打开"),
    OFF("蓝牙已关闭"),

    INIT_SUCCESS("蓝牙初始化成功"),
    INIT_FAILURE("蓝牙初始化失败"),

    START_ADVERTISING_SUCCESS("广播成功"),
    START_ADVERTISING_FAILURE("广播失败"),
    STOP_ADVERTISING("停止广播"),

    START_SCAN_DEVICE("开始扫描蓝牙设备"),
    STOP_SCAN_DEVICE("停止扫描蓝牙设备"),

    CONNECTED("连接蓝牙设备成功"),
    DISCONNECTED("连接蓝牙设备失败"),

    ON_CHARACTERISTIC_CHANGED("特征值改变了"),
    ON_CHARACTERISTIC_READ_SUCCESS("读特征值成功"),
    ON_CHARACTERISTIC_READ_FAILURE("读特征值失败"),
    ON_CHARACTERISTIC_WRITE_SUCCESS("写特征值成功"),
    ON_CHARACTERISTIC_WRITE_FAILURE("写特征值失败"),
    ON_DESCRIPTOR_READ_SUCCESS("读描述值成功"),
    ON_DESCRIPTOR_READ_FAILURE("读描述值失败"),
    ON_DESCRIPTOR_WRITE_SUCCESS("写描述值成功"),
    ON_DESCRIPTOR_WRITE_FAILURE("写描述值失败"),
    ON_READ_REMOTE_RSSI_SUCCESS("读信号值成功"),
    ON_READ_REMOTE_RSSI_FAILURE("读信号值失败"),
    ON_MTU_CHANGED_SUCCESS("设置MTU成功"),
    ON_MTU_CHANGED_FAILURE("设置MTU失败");

    override fun toString(): String {
        return "BleStatus(des='$des')"
    }

}