#### 最新版本

模块|Ble
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Ble.svg)](https://jitpack.io/#like5188/Ble)

## 功能介绍
1、用协程封装了低功耗蓝牙相关api，支持central、peripheral两种模式。

2、封装好了权限、超时、并发，使用者不用再做相关处理。

3、支持一个central设备同时连接多个peripheral设备。

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
        // 核心库，必须添加
        implementation 'com.github.like5188.Ble:core:版本号'
        // 作为中心设备使用
        implementation 'com.github.like5188.Ble:central:版本号'
        // 作为外围设备使用
        implementation 'com.github.like5188.Ble:peripheral:版本号'
    }
```

2、central
```java
    // 扫描使用 AbstractScanExecutor 类，然后调用其中的方法即可
    private val scanExecutor: AbstractScanExecutor by lazy {
        ScanExecutorFactory.get(context)
    }
    // 释放资源
    scanExecutor.close()

    // 连接使用 AbstractConnectExecutor 类，然后调用其中的方法即可
    private val connectExecutor: AbstractConnectExecutor by lazy {
        ConnectExecutorFactory.get(context, address)
    }
    // 释放资源
    connectExecutor.close()
```

3、peripheral
```java
    // 广播使用 AbstractAdvertisingExecutor 类，然后调用其中的方法即可
    private val peripheralExecutor: AbstractAdvertisingExecutor by lazy {
        AdvertisingExecutorFactory.get(context)
    }
    // 释放资源
    peripheralExecutor.close()
```

4、权限

    使用 com.like.ble.util.PermissionUtils 工具类处理

5、常用第三方库的引用
```java
    implementation 'androidx.fragment:fragment-ktx:1.5.5'
```
