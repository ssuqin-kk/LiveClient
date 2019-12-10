package com.ssuqin.liveclient.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.tencent.bugly.crashreport.CrashReport
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * 捕获全局异常
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {

    private var mContext: WeakReference<Context>? = null

    /**
     * 系统默认UncaughtExceptionHandler
     */
    private var mDefaultHandler: Thread.UncaughtExceptionHandler? = null
    /**
     * 存储异常和参数信息
     */
    private val paramsMap = HashMap<String, String>()

    /**
     * SUPPORTED_64_BIT_ABIS=[Ljava.lang.String;@483905b versionCode=319 BOARD=MSM8916 BOOTLOADER=T550XXU1CQJ6
     * IS_TRANSLATION_ASSISTANT_ENABLED=false TYPE=user ID=NMF26X TIME=1509348313000 BRAND=samsung TAG=Build
     * HARDWARE=qcom SERIAL=3447b5280b400829 SUPPORTED_ABIS=[Ljava.lang.String;@36260f8 CPU_ABI=armeabi-v7a
     * IS_DEBUGGABLE=false RADIO=unknown MANUFACTURER=samsung IS_EMULATOR=false
     * SUPPORTED_32_BIT_ABIS=[Ljava.lang.String;@5a6236a TAGS=release-keys CPU_ABI2=armeabi
     * IS_SYSTEM_SECURE=false UNKNOWN=unknown PERMISSIONS_REVIEW_REQUIRED=false USER=dpi
     * FINGERPRINT=samsung/gt510wifixx/gt510wifi:7.1.1/NMF26X/T550XXU1CQJ6:user/release-keys
     * HOST=SWDG2903 versionName=3.1.9(201905081841) PRODUCT=gt510wifixx DISPLAY=NMF26X.T550XXU1CQJ6
     * MODEL=SM-T550 DEVICE=gt510wifi IS_SECURE=false
     */
    private val importantLst = listOf("user", "versionCode", "SERIAL", "DISPLAY")

    private object Instance {
        val crashHandler = CrashHandler()
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        if (!handleException(ex) && mDefaultHandler != null) {//如果自己没处理交给系统处理
            mDefaultHandler?.uncaughtException(thread, ex)
        } else {//处理退出否则会卡死
            try {//延迟3秒杀进程
                Thread.sleep(3000)
            } catch (e: InterruptedException) {
                Log.e("TAG", "error : \n$e")
            }

            //退出程序
            Process.killProcess(Process.myPid())
            System.exit(1)
        }
    }

    /**
     * 收集错误信息.发送到服务器
     * @return 处理了该异常返回true,否则false
     */
    private fun handleException(ex: Throwable?): Boolean {
        if (ex == null || mContext == null || mContext?.get() == null) {
            CrashReport.postCatchedException(Error("Uncaught exception and NULL!"))
            return false
        }

        mContext?.get()?.let {
            //收集设备参数信息
            collectDeviceInfo(it)
            //使用Toast来显示异常信息
            object : Thread() {
                override fun run() {
                    Looper.prepare()
                    Toast.makeText(it, "Unknown exception, please restart application!", Toast.LENGTH_LONG).show()
                    Looper.loop()
                }
            }.start()

            //保存日志文件
            //saveCrashInfo2File(ex);
            //使用bugly收集
            val crashInfo = StringBuilder()
            for ((key, value) in paramsMap) {
                if (importantLst.contains(key)) {
                    crashInfo.append(key).append("=").append(value).append(" ")
                }
            }
            val error = Error("Uncaught exception($crashInfo):", ex)
            Log.e("CrashHandler", ""+error.printStackTrace())
            CrashReport.postCatchedException(error)
        }

        return true
    }


    /**
     * 收集设备参数信息
     */
    private fun collectDeviceInfo(ctx: Context) {
        //获取versionName,versionCode
        try {
            val pm = ctx.packageManager
            val pi = pm.getPackageInfo(ctx.packageName, PackageManager.GET_ACTIVITIES)
            if (pi != null) {
                val versionName = if (pi.versionName == null) "null" else pi.versionName
                val versionCode = pi.versionCode.toString() + ""
                paramsMap["versionName"] = versionName
                paramsMap["versionCode"] = versionCode
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "an error occurred when collect package info!\n$e")
        }

        //获取所有系统信息
        val fields = Build::class.java.declaredFields
        for (field in fields) {
            try {
                field.isAccessible = true
                paramsMap[field.name] = field.get(null).toString()
            } catch (e: Exception) {
                Log.e("CrashHandler", "an error occurred when collect crash info!\n$e")
            }
        }
    }

    /**
     * 保存错误信息到文件中
     *
     * @return  返回文件名称,便于将文件传送到服务器
     */
    private fun saveCrashInfo2File(ex: Throwable): String? {
        val crashInfo = getAllCrashInfo(ex)
        try {
            val timestamp = System.currentTimeMillis()
            val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
            val time = format.format(Date())
            val fileName = "crash-$time-$timestamp.log"

            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val path = Environment.getExternalStorageDirectory().absolutePath + "/crash/"
                val dir = File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val fos = FileOutputStream(path + fileName)
                fos.write(crashInfo.toByteArray())
                fos.close()
            }

            return fileName
        } catch (e: Exception) {
            Log.e("CrashHandler", "an error occurred while writing file!\n$e")
        }

        return null
    }

    /**
     * 组装报错信息
     */
    private fun getAllCrashInfo(ex: Throwable, onlyImportant: Boolean = false): String {
        val sb = StringBuilder()
        for ((key, value) in paramsMap) {
            if (onlyImportant) {
                if (importantLst.contains(key)) {
                    sb.append(key).append("=").append(value).append("\n")
                }
            } else {
                sb.append(key).append("=").append(value).append("\n")
            }
        }

        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        ex.printStackTrace(printWriter)
        var cause: Throwable? = ex.cause
        while (cause != null) {
            cause.printStackTrace(printWriter)
            cause = cause.cause
        }
        printWriter.close()

        val result = writer.toString()
        sb.append(result)
        val crashInfo = sb.toString()
        Log.e("CrashHandler", "Uncaught exception:\n$crashInfo")

        return crashInfo
    }

    companion object {
        fun init(context: Context) {
            Instance.crashHandler.mContext = WeakReference(context)
            Instance.crashHandler.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            //设置该CrashHandler为系统默认的
            Thread.setDefaultUncaughtExceptionHandler(Instance.crashHandler)
        }
    }
}
