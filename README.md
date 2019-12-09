#### 最新版本

模块|Ble
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Ble.svg)](https://jitpack.io/#like5188/Ble)

## 功能介绍
1、低功耗蓝牙模块封装，包括central、peripheral。

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

5、常用第三方库的引用
```java
    // coroutines
    compileOnly 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2'
    compileOnly 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.2'
    compileOnly 'androidx.lifecycle:lifecycle-runtime-ktx:2.2.0-rc03'// Activity 或 Fragment 对协程的支持：lifecycleScope
    // rxjava2
    compileOnly 'io.reactivex.rxjava2:rxjava:2.2.11'
    compileOnly 'com.github.tbruyelle:rxpermissions:0.10.2'
```