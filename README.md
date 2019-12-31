#### 最新版本

模块|Ble
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Ble.svg)](https://jitpack.io/#like5188/Ble)

## 功能介绍
1、低功耗蓝牙模块封装，支持central、peripheral两种模式。

2、封装好了权限申请、打开蓝牙功能，使用者不用再做相关处理。

3、所有命令的回调都是在主线程中。

4、支持一个central设备同时连接多个peripheral设备。

5、命令队列规则：①如果相同的命令正在执行，则抛弃。②StopScanCommand、DisconnectCommand、StopAdvertisingCommand命令会立即执行，其它命令会排队等候前面的命令完成。

6、如果版本低于API 21，那么可以用ScanRecordBelow21类来解析扫描结果。

7、系统内置了常用蓝牙命令：
```java
    peripheral：
        StartAdvertisingCommand（开始广播命令）
        StopAdvertisingCommand（停止广播命令）

    central：
        MacroCommand（宏命令）
        ConnectCommand（连接蓝牙设备命令）
        DisconnectCommand（断开蓝牙设备命令）
        ReadCharacteristicCommand（读特征值命令）
        WriteCharacteristicCommand（写特征值命令）
        ReadDescriptorCommand（读描述值命令）
        WriteDescriptorCommand（写描述值命令）
        ReadNotifyCommand（读取通知传来的数据命令）
        ReadRemoteRssiCommand（readRemoteRssi命令）
        RequestConnectionPriorityCommand（requestConnectionPriority命令）
        RequestMtuCommand（requestMtu命令）
        SetCharacteristicNotificationCommand（设置特征的notification或者indication的命令）
        StartScanCommand（开始扫描蓝牙设备命令）
        StopScanCommand（停止扫描蓝牙设备命令）
```

## 使用方法：

1、引用

在Project的gradle中加入：
```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```
在Module的gradle中加入：
```groovy
    dependencies {
        compile 'com.github.like5188:Ble:版本号'
    }
```

2、central
```java
    // 初始化
    private val mBleManager: BleManager by lazy { BleManager(CentralExecutor(this)) }
    // 发送单个命令
    mBleManager.sendCommand(ConnectCommand())
    // 发送宏命令
    val macroCommand = MacroCommand()
    val readNotifyCommand = ReadNotifyCommand()
    val writeCharacteristicCommand = WriteCharacteristicCommand()
    macroCommand.addCommand(readNotifyCommand, true)
    macroCommand.addCommand(writeCharacteristicCommand, false)
    mBleManager.sendCommand(macroCommand)
    // 释放资源
    mBleManager.close()
```

3、peripheral
```java
    // 初始化
    private val mBleManager: BleManager by lazy { BleManager(PeripheralExecutor(this)) }
    // 发送单个命令
    mBleManager.sendCommand(StartAdvertisingCommand())
    // 释放资源
    mBleManager.close()
```

4、常用第三方库的引用
```java
    // coroutines
    compileOnly 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2'
    compileOnly 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2'
    compileOnly 'androidx.lifecycle:lifecycle-runtime-ktx:2.2.0-rc03'// Activity 或 Fragment 对协程的支持：lifecycleScope
    // rxjava2
    compileOnly 'io.reactivex.rxjava2:rxjava:2.2.11'
    compileOnly 'com.github.tbruyelle:rxpermissions:0.10.2'

    compileOnly 'androidx.fragment:fragment-ktx:1.1.0'
```