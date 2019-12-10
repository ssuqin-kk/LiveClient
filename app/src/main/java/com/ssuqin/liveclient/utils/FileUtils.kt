package com.ssuqin.liveclient.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import java.io.*

/**
 * 文件操作类
 */
object FileUtils {

    private val TAG = "FileUtils"
    private val FILE_WRITING_ENCODING = "UTF-8"
    private val FILE_READING_ENCODING = "UTF-8"

    @Throws(Exception::class)
    fun readFile(_sFileName: String, _sEncoding: String?): String {
        var _sEncoding = _sEncoding
        var buffContent: StringBuffer? = null
        var sLine: String? = null

        var fis: FileInputStream? = null
        var buffReader: BufferedReader? = null
        if (_sEncoding == null || "" == _sEncoding) {
            _sEncoding = FILE_READING_ENCODING
        }

        try {
            fis = FileInputStream(_sFileName)
            buffReader = BufferedReader(InputStreamReader(fis, _sEncoding))
            var zFirstLine = "UTF-8".equals(_sEncoding, ignoreCase = true)
            while ({ sLine = buffReader!!.readLine(); sLine }() != null) {
                if (buffContent == null) {
                    buffContent = StringBuffer()
                } else {
                    buffContent.append("\n")
                }
                if (zFirstLine) {
                    sLine = removeBomHeaderIfExists(sLine)
                    zFirstLine = false
                }
                buffContent.append(sLine)
            }// end while
            return if (buffContent == null) "" else buffContent.toString()
        } catch (ex: FileNotFoundException) {
            throw Exception("要读取的文件没有找到!", ex)
        } catch (ex: IOException) {
            throw Exception("读取文件时错误!", ex)
        } finally {
            // 增加异常时资源的释放
            try {
                if (buffReader != null)
                    buffReader.close()
                if (fis != null)
                    fis.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    @Throws(Exception::class)
    fun writeFile(path: String, content: String, encoding: String, isOverride: Boolean): File {
        var encoding = encoding
        if (TextUtils.isEmpty(encoding)) {
            encoding = FILE_WRITING_ENCODING
        }
        val `is` = ByteArrayInputStream(content.toByteArray(charset(encoding)))
        return writeFile(`is`, path, isOverride)
    }

    @Throws(Exception::class)
    fun writeFile(inputStream: InputStream?, path: String, isOverride: Boolean): File {
        var path = path
        val sPath = extractFilePath(path)
        if (!pathExists(sPath)) {
            makeDir(sPath, true)
        }

        if (!isOverride && fileExists(path)) {
            if (path.contains(".")) {
                val suffix = path.substring(path.lastIndexOf("."))
                val pre = path.substring(0, path.lastIndexOf("."))
                path = pre + "_" + System.currentTimeMillis() + suffix
            } else {
                path = path + "_" + System.currentTimeMillis()
            }
        }

        var os: FileOutputStream? = null
        var file: File? = null

        try {
            file = File(path)
            os = FileOutputStream(file)
            var byteCount = 0
            val bytes = ByteArray(1024)

            while ({ byteCount = inputStream!!.read(bytes); byteCount }() != -1) {
                os.write(bytes, 0, byteCount)
            }
            os.flush()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("写文件错误", e)
        } finally {
            try {
                if (os != null)
                    os.close()
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 移除字符串中的BOM前缀
     *
     * @param _sLine 需要处理的字符串
     * @return 移除BOM后的字符串.
     */
    private fun removeBomHeaderIfExists(_sLine: String?): String? {
        if (_sLine == null) {
            return null
        }
        var line: String = _sLine
        if (line.length > 0) {
            var ch = line[0]
            // 使用while是因为用一些工具看到过某些文件前几个字节都是0xfffe.
            // 0xfeff,0xfffe是字节序的不同处理.JVM中,一般是0xfeff
            while (ch.toInt() == 0xfeff || ch.toInt() == 0xfffe) {
                line = line.substring(1)
                if (line.length == 0) {
                    break
                }
                ch = line[0]
            }
        }
        return line
    }

    /**
     * 从文件的完整路径名（路径+文件名）中提取 路径（包括：Drive+Directroy )
     *
     * @param _sFilePathName
     * @return
     */
    fun extractFilePath(_sFilePathName: String): String {
        var nPos = _sFilePathName.lastIndexOf('/')
        if (nPos < 0) {
            nPos = _sFilePathName.lastIndexOf('\\')
        }

        return if (nPos >= 0) _sFilePathName.substring(0, nPos + 1) else ""
    }

    /**
     * 检查指定文件的路径是否存在
     *
     * @param _sPathFileName 文件名称(含路径）
     * @return 若存在，则返回true；否则，返回false
     */
    fun pathExists(_sPathFileName: String): Boolean {
        val sPath = extractFilePath(_sPathFileName)
        return fileExists(sPath)
    }

    fun fileExists(_sPathFileName: String): Boolean {
        val file = File(_sPathFileName)
        return file.exists()
    }

    /**
     * 创建目录
     *
     * @param _sDir             目录名称
     * @param _bCreateParentDir 如果父目录不存在，是否创建父目录
     * @return
     */
    fun makeDir(_sDir: String, _bCreateParentDir: Boolean): Boolean {
        var zResult = false
        val file = File(_sDir)
        if (_bCreateParentDir)
            zResult = file.mkdirs() // 如果父目录不存在，则创建所有必需的父目录
        else
            zResult = file.mkdir() // 如果父目录不存在，不做处理
        if (!zResult)
            zResult = file.exists()
        return zResult
    }

    fun moveRawToDir(context: Context, rawName: String, dir: String) {
        try {
            writeFile(context.assets.open(rawName), dir, true)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.message)
        }

    }

    /**
     * 得到手机的缓存目录
     *
     * @param context
     * @return
     */
    fun getCacheDir(context: Context): File {
        Log.i("getCacheDir", "cache sdcard state: " + Environment.getExternalStorageState())
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            val cacheDir = context.externalCacheDir
            if (cacheDir != null && (cacheDir.exists() || cacheDir.mkdirs())) {
                Log.i("getCacheDir", "cache dir: " + cacheDir.absolutePath)
                return cacheDir
            }
        }

        val cacheDir = context.cacheDir
        Log.i("getCacheDir", "cache dir: " + cacheDir.absolutePath)

        return cacheDir
    }

    /**
     * 得到皮肤目录
     *
     * @param context
     * @return
     */
    fun getSkinDir(context: Context): File {
        val skinDir = File(getCacheDir(context), "skin")
        if (skinDir.exists()) {
            skinDir.mkdirs()
        }
        return skinDir
    }

    fun getSkinDirPath(context: Context): String {
        return getSkinDir(context).absolutePath
    }

    fun getSaveImagePath(context: Context): String {
        var path = getCacheDir(context).absolutePath
        if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            path = Environment.getExternalStorageDirectory().absolutePath
        }

        path = path + File.separator + "Pictures"
        val file = File(path)
        if (!file.exists()) {
            file.mkdir()
        }
        return path
    }

    fun generateFileNameByTime(): String {
        return System.currentTimeMillis().toString() + ""
    }

    fun getFileName(path: String): String {
        val index = path.lastIndexOf('/')
        return path.substring(index + 1)
    }

    /**
     * 拷贝文件
     *
     * @param sourceFilePath 源路径文件
     * @param targetPath     目标路径文件
     * @return 拷贝是否成功
     */
    fun copyFile(sourceFilePath: String, targetPath: String): Boolean {
        //如果路径相同，就不进行拷贝操作
        if (sourceFilePath == targetPath) {
            return true
        }

        var bool: Boolean
        var fis: FileInputStream? = null
        var fos: FileOutputStream? = null
        try {
            val file = File(sourceFilePath)
            if (!file.exists()) {
                return false
            }
            fis = FileInputStream(file)
            fos = FileOutputStream(targetPath)
            var readCount = 0
            val buffer = ByteArray(1024)
            while ({ readCount = fis.read(buffer); readCount }() >= 0) {
                fos.write(buffer, 0, readCount)
            }
            bool = true
        } catch (e: Exception) {
            e.printStackTrace()
            bool = false
        } finally {
            try {
                fis?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        return bool
    }

    /**
     * 删除单个文件
     *
     * @param filePath 文件路径
     */
    fun deleteFile(filePath: String): Boolean {
        var flag = false
        if (!StringUtils.isEmpty(filePath)) {
            var file: File? = File(filePath)
            if (file!!.exists()) {
                flag = file.delete()
            }
            file = null
        }

        return flag
    }

    /**
     * 删除整个目录
     *
     * @param path 要删除的目录路径
     */
    fun deleteDirectory(path: String) {
        if (!StringUtils.isEmpty(path)) {
            val directory = File(path)
            if (!directory.isDirectory) {
                return
            }

            val childFile = directory.listFiles()
            for (file in childFile!!) {
                if (file.isFile) {
                    file.delete()
                } else {
                    deleteDirectory(file.path)
                }
            }

            val result = if (directory.delete()) "成功" else "失败"
            Log.e(TAG,"删除目录：" + path + result)
        } else {
            Log.e(TAG,"删除无效，目录路径为空")
        }
    }

    /**
     * 删除整个目录下的子目录
     *
     * @param path 要删除的目录路径
     */
    fun deleteChildDirectory(path: String) {
        if (!StringUtils.isEmpty(path)) {
            val directory = File(path)
            if (!directory.isDirectory) {
                return
            }

            val childFile = directory.listFiles()
            for (file in childFile!!) {
                if (file.isFile) {
                    val result = if (file.delete()) "成功" else "失败"
                    Log.e(TAG,"删除文件：" + file + result)
                } else {
                    if (".nomedia" != file.name) {
                        deleteDirectory(file.path)
                    }
                }
            }
            Log.e(TAG,"删除目录：$path 下的子目录完成")
        } else {
            Log.e(TAG,"删除无效，目录路径为空")
        }
    }

    @JvmStatic
    fun saveBitmap(context: Context, bmp: Bitmap, path: String, name: String, capacity: Int): Boolean {
        // 首先保存图片
        val appDir = File(path)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        if (appDir.exists()) {
            val file = File(appDir, name)

            try {
                val fos = FileOutputStream(file)
                bmp.compress(Bitmap.CompressFormat.PNG, capacity, fos)
                fos.flush()
                fos.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return false
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        } else {
            return false
        }
        return true
    }

    @JvmStatic
    fun saveBitmap(bmp: Bitmap, filePath: String, capacity: Int): Boolean {
        val dir = filePath.substring(0,filePath.lastIndexOf("/"))

        // 首先保存图片
        val appDir = File(dir)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        if (appDir.exists()) {
            val file = File(filePath)

            try {
                val fos = FileOutputStream(file)
                bmp.compress(Bitmap.CompressFormat.PNG, capacity, fos)
                fos.flush()
                fos.close()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                return false
            } catch (e: IOException) {
                e.printStackTrace()
                return false
            }
        } else {
            return false
        }
        return true
    }
}