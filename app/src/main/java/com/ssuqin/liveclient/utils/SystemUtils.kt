package com.ssuqin.liveclient.utils


import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout


/**
 * 与系统有关的工具类
 */
object SystemUtils {

    /**
     * 获取设备的制造商
     *
     * @return 设备制造商
     */
    val deviceManufacture: String
        get() = android.os.Build.MANUFACTURER

    /**
     * 获取设备名称
     *
     * @return 设备名称
     */
    val deviceName: String
        get() = android.os.Build.MODEL

    /**
     * 获取系统版本号
     *
     * @return 系统版本号
     */
    val systemVersion: String
        get() = android.os.Build.VERSION.RELEASE

    /**
     * 获取设备号
     *
     * @param context
     * @return
     */
    @SuppressLint("MissingPermission")
    fun getDeviceIMEI(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (TextUtils.isEmpty(telephonyManager.deviceId)) {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } else {
            telephonyManager.deviceId
        }
    }

    /**
     * 获取屏幕的宽度
     *
     * @param context context
     * @return
     */
    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.widthPixels
    }

    /**
     * 获取屏幕的高度
     *
     * @param context context
     * @return
     */
    fun getScreenHeight(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        return displayMetrics.heightPixels
    }

    //转换dp为px
    fun dp2px(context: Context, dip: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dip * scale + 0.5f * if (dip >= 0) 1 else -1).toInt()
    }

    //转换px为dp
    fun px2dp(context: Context, px: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (px / scale + 0.5f * if (px >= 0) 1 else -1).toInt()
    }

    //转换sp为px
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    //转换px为sp
    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    fun getThemeColorPrimary(ctx: Context): Int {
        val typedValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.theme, typedValue, true)
        val attribute = intArrayOf(android.R.attr.colorPrimary)
        val array = ctx.obtainStyledAttributes(typedValue.resourceId, attribute)
        val color = array.getColor(0, -1)
        array.recycle()
        return color
    }

    fun getThemeColorPrimaryDark(ctx: Context): Int {
        val typedValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.theme, typedValue, true)
        val attribute = intArrayOf(android.R.attr.colorPrimaryDark)
        val array = ctx.obtainStyledAttributes(typedValue.resourceId, attribute)
        val color = array.getColor(0, -1)
        array.recycle()
        return color
    }

    fun getThemeColorAccent(ctx: Context): Int {
        val typedValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.theme, typedValue, true)
        val attribute = intArrayOf(android.R.attr.colorAccent)
        val array = ctx.obtainStyledAttributes(typedValue.resourceId, attribute)
        val color = array.getColor(0, -1)
        array.recycle()
        return color
    }

    fun singleClickEvent(editText: EditText, event: RegisterEvent) {
        editText.setOnClickListener { v -> event.onClick(editText) }
        editText.setOnFocusChangeListener { view, b ->
            if (b) {
                event.onClick(editText)
            }
        }
    }

    interface RegisterEvent {
        fun onClick(et: EditText)
    }

    /**
     * 滚动到指定控件位置
     *
     * @param parent 可滚动父控件
     * @param target 指定控件，需被LinearLayout包裹
     */
    fun viewScroll(parent: View, target: View, requestFocus: Boolean = true) {
        val view = target.parent as LinearLayout?
        parent.postDelayed({
            //延时是为了有时间等待输入法键盘消失，否则有可能无法正常定位
            if (view != null) {
                parent.scrollTo(0, view.top)
                if (requestFocus) {
                    target.requestFocus()
                    if (target is EditText) {
                        target.setSelection(target.text.length)
                    }
                }
            }
        }, 400)
    }

    /**
     * 滚动到指定控件位置
     */
    fun setEditTextAndFocus(target: EditText, value: String?) {
        value?.let {
            target.setText(it)
            target.requestFocus()
            target.setSelection(it.length)
        } ?: target.requestFocus()
    }

    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     * @param context
     * @return 平板返回 True，手机返回 False
     */
    fun isTablet(context: Context): Boolean {
        return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private var mClipboardManager: ClipboardManager? = null
    private var mNewCliboardManager: android.content.ClipboardManager? = null


    private fun isNew(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
    }

    private fun instance(context: Context) {
        if (isNew()) {
            if (mNewCliboardManager == null)
                mNewCliboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        } else {
            if (mClipboardManager == null)
                mClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    }

    /**
     * 获取剪切板的内容
     *
     * @param context
     * @return
     */
    fun getText(context: Context): CharSequence {
        val sb = StringBuilder()
        if (isNew()) {
            instance(context)
            if (!mNewCliboardManager!!.hasPrimaryClip()) {
                return sb.toString()
            } else {
                val clipData = mNewCliboardManager!!.primaryClip
                val count = clipData!!.itemCount

                for (i in 0 until count) {
                    val item = clipData.getItemAt(i)
                    val str = item.coerceToText(context)
                    sb.append(str)
                }
            }
        } else {
            instance(context)
            sb.append(mClipboardManager!!.text)
        }
        return sb.toString()
    }
}