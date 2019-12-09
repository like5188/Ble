#### 最新版本

模块|Ble
---|---
最新版本|[![Download](https://jitpack.io/v/like5188/Ble.svg)](https://jitpack.io/#like5188/Ble)

## 功能介绍
1、蓝牙模块封装。

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
```