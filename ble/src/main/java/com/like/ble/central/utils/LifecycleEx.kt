package com.like.ble.central.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.reactivex.disposables.Disposable

@JvmOverloads
internal fun Disposable?.bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
): Disposable? {
    this ?: return this
    bindToLifecycleOwner(lifecycleOwner, events) {
        if (!this.isDisposed) {
            this.dispose()
        }
    }
    return this
}

private fun bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    events: Array<out Lifecycle.Event>,
    cancel: () -> Unit
) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            if (events.contains(Lifecycle.Event.ON_CREATE))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            if (events.contains(Lifecycle.Event.ON_START))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            if (events.contains(Lifecycle.Event.ON_RESUME))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            if (events.contains(Lifecycle.Event.ON_PAUSE))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            if (events.contains(Lifecycle.Event.ON_STOP))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            if (events.contains(Lifecycle.Event.ON_DESTROY))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        fun onAny() {
            if (events.contains(Lifecycle.Event.ON_ANY))
                cancel()
        }
    })
}