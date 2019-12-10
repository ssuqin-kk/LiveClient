package com.ssuqin.liveclient.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * 字符串处理工具
 */
object StringUtils {

    private val emailer = Pattern
            .compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*")

    private val usaPhone = Pattern
            .compile("^[1-9]\\d{9,10}$")

    private val IMG_URL = Pattern
            .compile(".*?(gif|jpeg|png|jpg|bmp)")

    private val URL = Pattern
            .compile("^(https|http)://.*?$(net|com|.com.cn|org|me|)")

    private val dateFormatter = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        }
    }

    private val dateFormatter2 = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("yyyy-MM-dd")
        }
    }

    private val dateFormatter3 = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm:ss")
        }
    }

    private var formatDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var formatDay = SimpleDateFormat("d", Locale.getDefault())
    private var formatMonthDay = SimpleDateFormat("M-d", Locale.getDefault())
    private var formatDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 将字符串转位日期类型
     */
    @JvmOverloads
    fun toDate(sdate: String, iDateFormatter: SimpleDateFormat = dateFormatter.get()!!): Date? {
        try {
            return iDateFormatter.parse(sdate)
        } catch (e: ParseException) {
            return null
        }

    }

    fun getDateString(date: Date): String {
        return dateFormatter.get()!!.format(date)
    }

    /**
     * 获取当前日期
     *
     * @param dateFormat 日期格式
     * @return
     */
    @JvmStatic
    fun getCurDateStr(dateFormat: String): String {
        var iDateFormat = dateFormat
        val cal = Calendar.getInstance()
        iDateFormat = iDateFormat.toLowerCase()
        iDateFormat = iDateFormat.replace("-mm-".toRegex(), "-MM-")
        iDateFormat = iDateFormat.replace("hh:".toRegex(), "HH:")
        //SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        val sf = SimpleDateFormat(iDateFormat)
        return sf.format(cal.time)
    }

    /**
     * 获取当前日期(24小时制格式)
     *
     * @param dateFormat 日期格式
     * @return
     */
    fun getCurDateStr24Format(dateFormat: String): String {
        var dateFormat = dateFormat
        dateFormat = dateFormat.toLowerCase()
        dateFormat = dateFormat.replace("-mm-".toRegex(), "-MM-")
        dateFormat = dateFormat.replace("hh:".toRegex(), "HH:")
        //SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        val sf = SimpleDateFormat(dateFormat)
        return sf.format(Date())
    }

    /**
     * 获取当前日期（根据系统12-24小时格式）
     *
     * @param dateFormat 日期格式
     * @return
     */
    fun getCurDateStrBySystem(context: Context, dateFormat: String): String {
        var dateFormat = dateFormat
        dateFormat = dateFormat.toLowerCase()
        dateFormat = dateFormat.replace("-mm-".toRegex(), "-MM-")
        if (is24HourFormat(context)) {
            dateFormat = dateFormat.replace("hh:".toRegex(), "HH:")
        }
        //SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        val sf = SimpleDateFormat(dateFormat)
        return sf.format(Date())
    }

    /**
     * @param time 时间字符串截取
     * @param type 如果值为“Y”,则截取年月日，如果为“T”,截取时分秒，否则截取年月日，时分秒
     * @return
     */
    fun timeFormat(time: String, type: String): String {
        var time = time
        if ("Y".equals(type, ignoreCase = true) && time.length > 11) {
            time = time.substring(0, 10)
        } else if ("T".equals(type, ignoreCase = true) && time.length > 11 && time.length < 20) {
            val value = time.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            time = value[1]
        } else if (time.length > 20) {
            time = time.substring(0, 19)
        }
        return time
    }

    /**
     * 以友好的方式显示时间
     *
     * @param sdate
     * @return
     */
    fun friendly_time(sdate: String): String {
        var time: Date? = null

        if (isInEasternEightZones)
            time = toDate(sdate)
        else
            time = transformTime(
                toDate(sdate),
                    TimeZone.getTimeZone("GMT+08"), TimeZone.getDefault())

        if (time == null) {
            return "Unknown"
        }
        var ftime = ""
        val cal = Calendar.getInstance()

        // 判断是否是同一天
        val curDate = dateFormatter2.get()!!.format(cal.time)
        val paramDate = dateFormatter2.get()!!.format(time)
        if (curDate == paramDate) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            if (hour == 0)
                ftime = Math.max(
                        (cal.timeInMillis - time.time) / 60000, 1).toString() + "分钟前"
            else
                ftime = hour.toString() + "小时前"
            return ftime
        }

        val lt = time.time / 86400000
        val ct = cal.timeInMillis / 86400000
        val days = (ct - lt).toInt()
        if (days == 0) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            if (hour == 0)
                ftime = Math.max(
                        (cal.timeInMillis - time.time) / 60000, 1).toString() + "分钟前"
            else
                ftime = hour.toString() + "小时前"
        } else if (days == 1) {
            ftime = "昨天"
        } else if (days == 2) {
            ftime = "前天 "
        } else if (days > 2 && days < 31) {
            ftime = days.toString() + "天前"
        } else if (days >= 31 && days <= 2 * 31) {
            ftime = "一个月前"
        } else if (days > 2 * 31 && days <= 3 * 31) {
            ftime = "2个月前"
        } else if (days > 3 * 31 && days <= 4 * 31) {
            ftime = "3个月前"
        } else {
            ftime = dateFormatter2.get()!!.format(time)
        }
        return ftime
    }

    fun friendly_time2(sdate: String): String {
        var res = ""
        if (isEmpty(sdate))
            return ""

        val weekDays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
        val currentData = getDataTime("MM-dd")
        val currentDay = toInt(currentData.substring(3))
        val currentMoth = toInt(currentData.substring(0, 2))

        val sMoth = toInt(sdate.substring(5, 7))
        val sDay = toInt(sdate.substring(8, 10))
        val sYear = toInt(sdate.substring(0, 4))
        val dt = Date(sYear, sMoth - 1, sDay - 1)

        if (sDay == currentDay && sMoth == currentMoth) {
            res = "今天 / " + weekDays[getWeekOfDate(Date())]
        } else if (sDay == currentDay + 1 && sMoth == currentMoth) {
            res = "昨天 / " + weekDays[(getWeekOfDate(Date()) + 6) % 7]
        } else {
            if (sMoth < 10) {
                res = "0"
            }
            res += sMoth.toString() + "/"
            if (sDay < 10) {
                res += "0"
            }
            res += sDay.toString() + " / " + weekDays[getWeekOfDate(dt)]
        }

        return res
    }

    /**
     * 获取当前日期是星期几<br></br>
     *
     * @param dt
     * @return 当前日期是星期几
     */
    fun getWeekOfDate(dt: Date): Int {
        val cal = Calendar.getInstance()
        cal.time = dt
        var w = cal.get(Calendar.DAY_OF_WEEK) - 1
        if (w < 0)
            w = 0
        return w
    }

    /**
     * 判断给定字符串时间是否为今日
     *
     * @param sdate
     * @return boolean
     */
    fun isToday(sdate: String): Boolean {
        var b = false
        val time = toDate(sdate)
        val today = Date()
        if (time != null) {
            val nowDate = dateFormatter2.get()!!.format(today)
            val timeDate = dateFormatter2.get()!!.format(time)
            if (nowDate == timeDate) {
                b = true
            }
        }
        return b
    }

    /**
     * 返回long类型的今天的日期(20160330)
     *
     * @return
     */
    val today: Long
        get() {
            val cal = Calendar.getInstance()
            var curDate = dateFormatter2.get()!!.format(cal.time)
            curDate = curDate.replace("-", "")
            return java.lang.Long.parseLong(curDate)
        }

    /**
     * 返回long类型的离今天前的日期(20160320)
     *
     * @param day 几天前
     * @return
     */
    fun getDayBeforeToday(day: Int): Long {
        val cal = Calendar.getInstance()
        val currentMillis = cal.timeInMillis
        val beforeMillis = currentMillis - day * 86400000
        var curDate = dateFormatter2.get()!!.format(Date(beforeMillis))
        curDate = curDate.replace("-", "")
        return java.lang.Long.parseLong(curDate)
    }

    /**
     * 返回long类型的离今天后的日期(20160320)
     *
     * @param day 几天后
     * @return
     */
    fun getDayAfterToday(day: Int): Long {
        val cal = Calendar.getInstance()
        val currentMillis = cal.timeInMillis
        val afterMillis = currentMillis + day * 86400000
        var curDate = dateFormatter2.get()!!.format(Date(afterMillis))
        curDate = curDate.replace("-", "")
        return java.lang.Long.parseLong(curDate)
    }

    /**
     * HHmmss.jpg
     */
    val curTimeJPG: String
        get() {
            val dateFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
            val curTime = Date()

            return StringBuilder(dateFormat.format(curTime)).append(".jpg").toString()
        }

    /**
     * 当前时间
     * yyyy-MM-dd HH:mm:ss
     */
    val curTimeStr: String
        get() {
            val cal = Calendar.getInstance()
            return dateFormatter.get()!!.format(cal.time)
        }

    /**
     * 几天后时间
     * yyyy-MM-dd HH:mm:ss
     */
    fun getFeatureTimeStr(day: Int): String {
        val cal = Calendar.getInstance()
        val currentMillis = cal.timeInMillis
        val afterMillis = currentMillis + day * 86400000
        return dateFormatter.get()!!.format(Date(afterMillis))
    }

    /**
     * 指定时间的移动时间
     * yyyy-MM-dd HH:mm:ss
     */
    fun getTargetFeatureTime(time: String, second: Int): String {
        val currentSecond = calDate(time)
        val afterSecond = currentSecond + second
        return dateFormatter.get()!!.format(Date(afterSecond * 1000))
    }

    /**
     * 几天前时间
     * yyyy-MM-dd HH:mm:ss
     */
    fun getBeforeTimeStr(day: Int): String {
        val cal = Calendar.getInstance()
        val currentMillis = cal.timeInMillis
        val beforerMillis = currentMillis - day * 86400000
        return dateFormatter.get()!!.format(Date(beforerMillis))
    }

    /**
     * 计算从1970****到指定dateStr秒数
     *
     * @param dateStr
     * @return
     */
    fun calDate(dateStr: String): Long {
        var result: Long = 0

        try {
            val date = dateFormatter.get()!!.parse(dateStr)
            result = date.time// 毫秒ms

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result / 1000
    }

    /**
     * 计算指定timeStr的秒数(基秒18000)
     *
     * @param timeStr
     * @return
     */
    fun calTime(timeStr: String): Long {
        var result: Long = 0
        if (!isEmpty(timeStr)) {
            try {
                val date = dateFormatter3.get()!!.parse(timeStr)
                result = date.time// 毫秒ms

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        return result / 1000
    }

    /***
     * 计算两个时间差，返回的是的秒s
     *
     * @param dete1 形式如"yyyy-MM-dd HH:mm:ss"
     * @param date2 形式如"yyyy-MM-dd HH:mm:ss"
     * @return
     */
    fun calDateDifferent(dete1: String, date2: String): Long {

        var diff: Long = 0

        var d1: Date? = null
        var d2: Date? = null

        try {
            d1 = dateFormatter.get()!!.parse(dete1)
            d2 = dateFormatter.get()!!.parse(date2)

            // 毫秒ms
            diff = d2!!.time - d1!!.time

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return diff / 1000
    }

    /***
     * 计算两个时间差，返回的是的秒s
     *
     * @param time1 形式如"HH:mm:ss"
     * @param time2 形式如"HH:mm:ss"
     * @return
     */
    fun calTimeDifferent(time1: String, time2: String): Long {
        return calTime(time2) - calTime(time1)
    }

    /**
     * 系统时间采用是什么制式
     *
     * @return
     */
    fun is24HourFormat(context: Context): Boolean {
        var flag = false
        val cv = context.contentResolver
        val strTimeFormat = android.provider.Settings.System.getString(cv,
                android.provider.Settings.System.TIME_12_24)
        if (strTimeFormat == "24") {
            flag = true
        }

        return flag
    }

    /**
     * 转换为12小时制时间，如： 01:01:01->01:01:01AM 14:01:01->02:01:01PM
     *
     * @param time
     * @return
     */
    fun covertTimeToAmOrPm(time: String): String {
        val timeArr = time.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var hourStr = time + "AM"
        var hour = Integer.parseInt(timeArr[0])
        if (hour > 11) {
            hour = hour - 12
            if (hour < 10) {
                hourStr = "0" + hour + ":" + timeArr[1] + ":" + timeArr[2] + "PM"
            } else {
                hourStr = hour.toString() + ":" + timeArr[1] + ":" + timeArr[2] + "PM"
            }
        }

        return hourStr
    }

    /**
     * 转换时间为美国时间
     * 如：2017/01/20 16:35:33->01/20/2017 4:35:33PM
     * 2017-01-20 16:35:33->01/20/2017 4:35:33PM
     */
    @JvmStatic
    fun covertTimeToUSA(date: String): String {
        if (date.isEmpty()) return ""
        var date = date
        var covertTime = date
        try {
            date = date.replace("/", "-")
            val dateArr = date.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val year = dateArr[0]
            val time = dateArr[1]

            val yearArr = year.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val newYear = yearArr[1] + "/" + yearArr[2] + "/" + yearArr[0]
            val newTime = covertTimeToAmOrPm(time)

            covertTime = newYear + " " + newTime
        } catch (e: Exception) {

        }

        return covertTime
    }

    /**
     * 判断给定字符串是否空白串。 空白串是指由空格、制表符、回车符、换行符组成的字符串 若输入字符串为null或空字符串，返回true
     *
     * @param input
     * @return boolean
     */
    @JvmStatic
    fun isEmpty(input: String?): Boolean {
        if (input == null || "" == input)
            return true

        for (element in input) {
            val c = element
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return false
            }
        }
        return true
    }

    /**
     * 判断是不是一个合法的电子邮件地址
     *
     * @param email
     * @return
     */
    fun isEmail(email: String?): Boolean {
        return if (email == null || email.trim { it <= ' ' }.isEmpty()) false else emailer.matcher(email).matches()
    }

    /**
     * 判断是不是一个合法的美国电话
     *
     * @param phone
     * @return
     */
    fun isUSAPhone(phone: String?): Boolean {
        return if (phone == null || phone.trim { it <= ' ' }.isEmpty()) false else usaPhone.matcher(phone).matches()
    }

    /**
     * 判断一个url是否为图片url
     *
     * @param url
     * @return
     */
    fun isImgUrl(url: String?): Boolean {
        return if (url == null || url.trim { it <= ' ' }.length == 0) false else IMG_URL.matcher(url).matches()
    }

    /**
     * 判断是否为一个合法的url地址
     *
     * @param str
     * @return
     */
    fun isUrl(str: String?): Boolean {
        return if (str == null || str.trim { it <= ' ' }.length == 0) false else URL.matcher(str).matches()
    }

    /**
     * 判断输入的IP是否合法
     *
     * @param str
     * @return
     */
    @JvmStatic
    fun checkIP(str: String): Boolean {
        val pattern = Pattern
                .compile("^(([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])" + "(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3})$")
        return pattern.matcher(str).matches()
    }

    /**
     * 字符串转整数
     *
     * @param str
     * @param defValue
     * @return
     */
    fun toInt(str: String, defValue: Int): Int {
        try {
            return Integer.parseInt(str)
        } catch (e: Exception) {
        }

        return defValue
    }

    /**
     * 对象转整数
     *
     * @param obj
     * @return 转换异常返回 0
     */
    @JvmStatic
    fun toInt(obj: Any?): Int {
        return if (obj == null) 0 else toInt(obj.toString(), 0)
    }

    /**
     * 对象转长整数
     *
     * @param obj
     * @return 转换异常返回 0
     */
    fun toLong(obj: String): Long {
        try {
            return java.lang.Long.parseLong(obj)
        } catch (e: Exception) {
        }

        return 0
    }

    /**
     * 对象转Float
     *
     * @param obj
     * @return 转换异常返回 0
     */
    fun toFloat(obj: String): Float {
        try {
            return java.lang.Float.parseFloat(obj)
        } catch (e: Exception) {
        }

        return 0f
    }

    /**
     * 对象转Double
     *
     * @param obj
     * @return 转换异常返回 0
     */
    fun toDouble(obj: String): Double {
        try {
            return java.lang.Double.parseDouble(obj)
        } catch (e: Exception) {
        }

        return 0.0
    }

    /**
     * 字符串转布尔值
     *
     * @param b
     * @return 转换异常返回 false
     */
    fun toBool(b: String): Boolean {
        try {
            return java.lang.Boolean.parseBoolean(b)
        } catch (e: Exception) {
        }

        return false
    }

    fun getString(s: String?): String {
        return s ?: ""
    }

    /**
     * 将一个InputStream流转换成字符串
     *
     * @param is
     * @return
     */
    fun toConvertString(`is`: InputStream?): String {
        var `is` = `is`
        val res = StringBuffer()
        val isr = InputStreamReader(`is`!!)
        var read: BufferedReader? = BufferedReader(isr)
        try {
            var line: String?
            line = read!!.readLine()
            while (line != null) {
                res.append(line + "<br>")
                line = read.readLine()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                if (null != isr) {
                    isr.close()
                    isr.close()
                }
                if (null != read) {
                    read.close()
                    read = null
                }
                if (null != `is`) {
                    `is`.close()
                    `is` = null
                }
            } catch (e: IOException) {
            }

        }
        return res.toString()
    }

    /***
     * 截取字符串
     *
     * @param start 从那里开始，0算起
     * @param num   截取多少个
     * @param str   截取的字符串
     * @return
     */
    fun getSubString(start: Int, num: Int, str: String?): String {
        var start = start
        var num = num
        if (str == null) {
            return ""
        }
        val leng = str.length
        if (start < 0) {
            start = 0
        }
        if (start > leng) {
            start = leng
        }
        if (num < 0) {
            num = 1
        }
        var end = start + num
        if (end > leng) {
            end = leng
        }
        return str.substring(start, end)
    }

    /**
     * 获取当前时间为每年第几周
     *
     * @return
     */
    val weekOfYear: Int
        get() = getWeekOfYear(Date())

    /**
     * 获取当前时间为每年第几周
     *
     * @param date
     * @return
     */
    fun getWeekOfYear(date: Date): Int {
        val c = Calendar.getInstance()
        c.firstDayOfWeek = Calendar.MONDAY
        c.time = date
        var week = c.get(Calendar.WEEK_OF_YEAR) - 1
        week = if (week == 0) 52 else week
        return if (week > 0) week else 1
    }

    val currentDate: IntArray
        get() {
            val dateBundle = IntArray(3)
            val temp = getDataTime("yyyy-MM-dd").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            for (i in 0..2) {
                try {
                    dateBundle[i] = Integer.parseInt(temp[i])
                } catch (e: Exception) {
                    dateBundle[i] = 0
                }

            }
            return dateBundle
        }

    val currentDateStr: Array<String>
        get() {
            val temp = getDataTime("yyyy-MM-dd").split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            return getDateArrStr(temp)
        }

    fun getDateArrByDate(date: Date): Array<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val temp = dateFormat.format(date).split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return getDateArrStr(temp)
    }

    private fun getDateArrStr(temp: Array<String>): Array<String> {
        val dateBundle = arrayOf("", "", "")
        for (i in 0..2) {
            try {
                dateBundle[i] = temp[i]
            } catch (e: Exception) {
                dateBundle[i] = ""
            }
        }

        return dateBundle
    }

    /**
     * 返回当前系统时间
     */
    fun getDataTime(format: String): String {
        val df = SimpleDateFormat(format)

        return df.format(Date())
    }

    /**
     * 格式化不规则日期如 2017 1 4 -> 2017-01-04
     */
    fun formatDate(year: Int, month: Int, day: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)

        return dateFormatter2.get()!!.format(cal.time)
    }

    /**
     * 判断用户的设备时区是否为东八区（中国）
     */
    val isInEasternEightZones: Boolean
        get() {
            var defaultVaule = true
            if (TimeZone.getDefault() == TimeZone.getTimeZone("GMT+08"))
                defaultVaule = true
            else
                defaultVaule = false
            return defaultVaule
        }

    /**
     * 根据不同时区，转换时间
     */
    fun transformTime(date: Date?, oldZone: TimeZone, newZone: TimeZone): Date? {
        var finalDate: Date? = null
        if (date != null) {
            val timeOffset = oldZone.getOffset(date.time) - newZone.getOffset(date.time)
            finalDate = Date(date.time - timeOffset)
        }
        return finalDate
    }

    /***
     * 从当前 String 对象移除数组中指定的一组字符的所有尾部匹配项。
     *
     * @param str       移除String对象
     * @param trimChars 要删除的 Unicode 字符的数组，或 null
     * @return
     */
    fun trimEnd(str: String, vararg trimChars: Char): String {
        var str = str
        str = str.trim { it <= ' ' }
        for (i in trimChars.indices) {
            val trimChar = trimChars[i]
            if (str.endsWith(trimChar.toString())) {
                str = str.substring(0, str.length - 1)
            }
        }
        return str
    }

    /**
     * 将字符串进行md5转换
     *
     * @param str
     * @return
     */
    fun md5(str: String): String {
        if (isEmpty(str)) {
            return ""
        }

        var cacheKey: String
        try {
            val mDigest = MessageDigest.getInstance("MD5")
            mDigest.update(str.toByteArray())
            cacheKey = bytesToHexString(mDigest.digest())
        } catch (e: NoSuchAlgorithmException) {
            cacheKey = str.hashCode().toString()
        }

        return cacheKey.toUpperCase()
    }

    private fun bytesToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices) {
            val hex = Integer.toHexString(0xFF and bytes[i].toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }

    fun isNullOrEmpty(s: String?): Boolean {
        return s == null || s.length == 0
    }

    /**
     * 清除文本里面的HTML标签
     *
     * @param htmlStr
     * @return
     */
    fun clearHTMLTag(htmlStr: String): String {
        var htmlStr = htmlStr
        val regEx_script = "<script[^>]*?>[\\s\\S]*?<\\/script>" // 定义script的正则表达式
        val regEx_style = "<style[^>]*?>[\\s\\S]*?<\\/style>" // 定义style的正则表达式
        val regEx_html = "<[^>]+>" // 定义HTML标签的正则表达式
        Log.v("htmlStr", htmlStr)
        try {
            val p_script = Pattern.compile(regEx_script,
                    Pattern.CASE_INSENSITIVE)
            val m_script = p_script.matcher(htmlStr)
            htmlStr = m_script.replaceAll("") // 过滤script标签

            val p_style = Pattern.compile(regEx_style,
                    Pattern.CASE_INSENSITIVE)
            val m_style = p_style.matcher(htmlStr)
            htmlStr = m_style.replaceAll("") // 过滤style标签

            val p_html = Pattern.compile(regEx_html,
                    Pattern.CASE_INSENSITIVE)
            val m_html = p_html.matcher(htmlStr)
            htmlStr = m_html.replaceAll("") // 过滤html标签
        } catch (e: Exception) {
            htmlStr = "clear error"

        }

        return htmlStr // 返回文本字符串
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return 年月日
     */
    @JvmStatic
    fun formatDate(date: Date): String {
        return formatDate.format(date)
    }

    /**
     * 格式化日期
     *
     * @param date
     * @return 年月日 时分秒
     */
    fun formatDateTime(date: Date): String {
        return formatDateTime.format(date)
    }

    /**
     * 将时间戳解析成日期
     *
     * @param timeInMillis
     * @return 年月日
     */
    fun parseDate(timeInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        val date = calendar.time
        return formatDate(date)
    }

    /**
     * 将时间戳解析成日期
     *
     * @param timeInMillis
     * @return 年月日 时分秒
     */
    fun parseDateTime(timeInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        val date = calendar.time
        return formatDateTime(date)
    }

    /**
     * 解析日期
     *
     * @param date
     * @return
     */
    fun parseDate(date: String): Date? {
        var mDate: Date? = null
        try {
            mDate = formatDate.parse(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return mDate
    }

    /**
     * 解析日期
     *
     * @param datetime
     * @return
     */
    fun parseDateTime(datetime: String): Date? {
        var mDate: Date? = null
        try {
            mDate = formatDateTime.parse(datetime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return mDate
    }


    /**
     * 以友好的方式显示时间
     *
     * @param sdate
     * @return
     */
    fun friendlyTime(sdate: String): String {
        val time = parseDateTime(sdate) ?: return "Unknown"
        var ftime = ""
        val cal = Calendar.getInstance()

        // 判断是否是同一天
        val curDate = formatDate.format(cal.time)
        val paramDate = formatDate.format(time)
        if (curDate == paramDate) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            if (hour == 0)
                ftime = Math.max(
                        (cal.timeInMillis - time.time) / 60000, 1).toString() + "分钟前"
            else
                ftime = hour.toString() + "小时前"
            return ftime
        }

        val lt = time.time / 86400000
        val ct = cal.timeInMillis / 86400000
        val days = (ct - lt).toInt()
        if (days == 0) {
            val hour = ((cal.timeInMillis - time.time) / 3600000).toInt()
            if (hour == 0)
                ftime = Math.max(
                        (cal.timeInMillis - time.time) / 60000, 1).toString() + "分钟前"
            else
                ftime = hour.toString() + "小时前"
        } else if (days == 1) {
            ftime = "昨天"
        } else if (days == 2) {
            ftime = "前天"
        } else if (days > 2 && days <= 10) {
            ftime = days.toString() + "天前"
        } else if (days > 10) {
            ftime = formatDate.format(time)
        }
        return ftime
    }

    /**
     * 字符串转换成日期
     * @param dateStr
     * @return date
     */
    @JvmStatic
    fun strToDate(dateStr: String): Date? {
        val format = SimpleDateFormat("yyyy-MM-dd")
        var date: Date? = null
        try {
            date = format.parse(dateStr)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return date
    }

    fun getDateStr(strDate: String): String {
        var strDate = strDate
        /// <summary>切割年月日与时分秒称为数组</summary>
        /// <param name="strDate" type="string">待转换字符串</param>
        if (strDate.indexOf("T") > 0) {
            strDate = strDate.replace("T", " ")
        }
        if (strDate.indexOf("Z") > 0) {
            strDate = strDate.replace("Z", "")
        }
        return strDate.replace("/".toRegex(), "-")
    }

    /**
     * 将24小时转12小时制
     */
    private fun get12Time(_24Time: String): String? {
        if (TextUtils.isEmpty(_24Time)) {
            return _24Time
        }

        val fmt: SimpleDateFormat

        fmt = SimpleDateFormat("MM/dd/yyyy hh:mm:ss")

        var am_pm = "AM"
        val hour = _24Time.substring(_24Time.length - 8, _24Time.length - 6)

        if (Integer.parseInt(hour) >= 12 && Integer.parseInt(hour) < 24) {
            am_pm = "PM"
        }

        return fmt.format(Date(_24Time).time) + " " + am_pm
    }
}