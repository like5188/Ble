package com.like.ble.sample

import android.app.Dialog
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager

abstract class BaseDialogFragment<T : ViewDataBinding> : DialogFragment() {
    private var mBinding: T? = null
    private var mWidth = WindowManager.LayoutParams.WRAP_CONTENT
    private var mHeight = WindowManager.LayoutParams.WRAP_CONTENT
    private var mGravity = Gravity.CENTER
    private var mDimAmount = 0.6f

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // 去掉 dialog 的标题栏。
        // 不能放到 onStart() 方法中，因为 requestFeature() must be called before adding content，
        // 即必须在 setContentView() 之前调用，而这个方法是在 onActivityCreated() 方法中调用的
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        initDialog(dialog)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layoutResId = getLayoutResId()
        if (layoutResId <= 0) return null
        mBinding = DataBindingUtil.inflate(inflater, layoutResId, container, false)
        return mBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding?.let {
            initView(it, savedInstanceState)
        }
    }

    override fun onStart() {
        super.onStart()
        // 设置 window 相关，必须放到 onStart() 里面才有效。
        dialog?.window?.let {
            // 宽高
            val layoutParams = it.attributes
            layoutParams.width = mWidth
            layoutParams.height = mHeight
            // 位置
            layoutParams.gravity = mGravity
            // 透明度
            layoutParams.dimAmount = mDimAmount
            it.attributes = layoutParams
            // 设置背景透明，并去掉 dialog 默认的 padding
            it.setBackgroundDrawableResource(android.R.color.transparent)
            initWindow(it)
        }
    }

    fun show(activity: FragmentActivity) {
        show(activity.supportFragmentManager)
    }

    fun show(fragment: Fragment) {
        fragment.fragmentManager?.apply {
            show(this)
        }
    }

    /**
     * bug：Can not perform this action after onSaveInstanceState
     * onSaveInstanceState方法是在该Activity即将被销毁前调用，来保存Activity数据的，如果在保存玩状态后再给它添加Fragment就会出错
     * 解决方法就是把show()方法中的 commit（）方法替换成 commitAllowingStateLoss()、或者直接try
     */
    fun show(fragmentManager: FragmentManager) {
        val tag = this::class.java.simpleName
        val fragment = fragmentManager.findFragmentByTag(tag)
        if (fragment == null || !fragment.isAdded || fragment.isHidden) {
            try {
                this.show(fragmentManager, tag)
            } catch (e: Exception) {// 相当于重写了 show() 方法，至于其中的 mDismissed、mShownByMe 这两个变量的值，在 try 中已经设置好了。
                val ft = fragmentManager.beginTransaction()
                ft.add(this, tag)
                ft.commitAllowingStateLoss()
            }
        }
    }

    override fun dismiss() {
        //防止横竖屏切换时 getFragmentManager置空引起的问题：
        //Attempt to invoke virtual method 'android.app.FragmentTransaction
        //android.app.FragmentManager.beginTransaction()' on a null object reference
        fragmentManager ?: return
        try {
            super.dismiss()
        } catch (e: Exception) {
            dismissAllowingStateLoss()
        }
    }

    fun getBinding() = mBinding

    fun setWidth(width: Int) {
        mWidth = width
    }

    fun setHeight(height: Int) {
        mHeight = height
    }

    fun setGravity(gravity: Int) {
        mGravity = gravity
    }

    fun setDimAmount(dimAmount: Float) {
        mDimAmount = dimAmount
    }

    open fun initDialog(dialog: Dialog) {}
    open fun initView(binding: T, savedInstanceState: Bundle?) {}
    open fun initWindow(window: Window) {}
    abstract fun getLayoutResId(): Int
}