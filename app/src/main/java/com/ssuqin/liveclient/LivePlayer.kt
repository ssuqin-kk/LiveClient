/**
 * 参考:https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/EncodeDecodeTest.java
 */

package com.ssuqin.liveclient

import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.ssuqin.liveclient.constant.Constants
import com.ssuqin.liveclient.listener.LivePlayerListener
import com.ssuqin.liveclient.listener.ScreenshotListener
import com.ssuqin.liveclient.media.audio.AudioManagerUtil
import com.ssuqin.liveclient.model.BaseServerDataRespond
import com.ssuqin.liveclient.model.GLiveData
import com.ssuqin.liveclient.model.LivePlayMessage
import com.ssuqin.liveclient.model.YUVImageData
import com.ssuqin.liveclient.network.LiveWebSocketClient
import com.ssuqin.liveclient.utils.*
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.simple.eventbus.EventBus
import org.simple.eventbus.Subscriber
import org.simple.eventbus.ThreadMode
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

class LivePlayer:SurfaceView,  SurfaceHolder.Callback{
    private val TAG:String = "LivePlayer"

    private var mSurfaceView:SurfaceView ?= null
    private var mSurface:Surface?=null
    private var mSurfaceHolder:SurfaceHolder?=null
    private var mOptionBuilder:OptionBuilder?= null
    private var mWebSocketClient: LiveWebSocketClient?=null
    private var mLivePlayerListener: LivePlayerListener?=null

    private var isFinishing = false

    private var mAudioManagerUtil:AudioManagerUtil?=null
    private var mVideoDecoder:Decoder?=null

    private var mWebSocketRetryTimes = 3 // websocket重连次数
    private var mCondition = Object()

    private var isAudioConfigured = false
    private var isSuraceDestroyed = false
    private var isPlayFirstVideoFrame = true
    private var isMute = false
    private var isFixingWH = true
    private var isStartPlay = false

    private var mSurfaceWidth = 0;
    private var mSurfaceHeight = 0;

    private val changeSurfaceViewWhHandler by lazy<Handler> {
        // 视频宽高修正handler
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message?) {
                super.handleMessage(msg)
                if (msg?.obj != null) {
                    val layoutParams = msg.obj as ViewGroup.LayoutParams
                    mSurfaceView!!.layoutParams = layoutParams
                    postDelayed({
                        // 标记视频宽高修订已经完成了
                        isFixingWH = false
                        // surfaceView已经显示第一帧画面后又重新修改surfaceView宽高导致解析发生异常，需要重新初始化所有播放线程并播放
                        startPlay()
                    }, 300) // 延迟300毫秒修改状态，确保视屏宽高修正完毕
                }
            }
        }
    }

    constructor(context: Context):super(context){
        holder.addCallback(this)
    }

    constructor(context: Context, attr: AttributeSet):super(context, attr) {
        holder.addCallback(this)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        Log.i(TAG,"surfaceChanged")
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        EventBus.getDefault().register(this)
        synchronized(mCondition) {
            mSurfaceView = this

            mSurfaceHolder = holder
            init()
            mSurfaceHolder!!.setKeepScreenOn(true)
            mSurface = mSurfaceHolder!!.surface
        }
        livePlayerCallback(Constants.LIVE_PLAYER_CALL_PREPARED, "", getMessage(Constants.LIVE_PLAYER_PREPARED, "prepared"))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        mSurfaceView = null
        EventBus.getDefault().unregister(this)
        tearDown(true)
    }

    private fun reset() {
        isSuraceDestroyed = false
        mWebSocketRetryTimes = 3
        isAudioConfigured = false
        isFixingWH = true
    }

    private fun init() {
        reset()
        mVideoDecoder = Decoder(this)
    }

    /**
     * 刷新功能
     */
    public fun onRefresh():Boolean {
        tearDown(false)

        init()
        return true
    }

    fun setOptionBuilder(optionBuilder: OptionBuilder) {
        this.mOptionBuilder = optionBuilder
    }

    fun setLivePlayerListener(livePlayerListener: LivePlayerListener) {
        this.mLivePlayerListener = livePlayerListener
    }

    fun getMessage(code:Int, error: String):LivePlayMessage {
        return LivePlayMessage(code, error)
    }

    private fun livePlayerCallback(callbackType:Int, url:String, message:LivePlayMessage) {
        if (mLivePlayerListener != null) {
            mLivePlayerListener?.onPlayCallback(callbackType, url, message)
        } else {
            Log.e(TAG, message.mError)
        }
    }

    /**
     * 播放视频第一帧回调
     */
    fun playFirstVideoFrameCallback(bytes:ByteArray?, width:Int, height: Int) {
        if (null != mLivePlayerListener && isPlayFirstVideoFrame) {
            isPlayFirstVideoFrame = false
            var bitmap = createBitmap(bytes!!, width, height)
            var url = if(null != mOptionBuilder) mOptionBuilder!!.mUrl else ""
            mLivePlayerListener!!.onPlayFirstVideoFrameCallback(url, bitmap!!)
        }
    }

    private fun startCheck():Boolean {
        if(mOptionBuilder == null) {
            livePlayerCallback(Constants.LIVE_PLAYER_CALL_PREPARED, "", getMessage(Constants.LIVE_PLAYER_ERROR_UNSET, "option is unset"))
            return false
        } else if (mOptionBuilder?.mUrl == null || mOptionBuilder?.mUrl?.isEmpty()!!) {
            livePlayerCallback(Constants.LIVE_PLAYER_CALL_PREPARED, "", getMessage(Constants.LIVE_PLAYER_ERROR_INVALID_URL, "url is unset!"))
            return false
        }
        return true
    }

    fun start() {
        if (!startCheck()) {
            return
        }

        Observable.create(ObservableOnSubscribe<Boolean> {
            it.onNext(contactWebsocket())
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Boolean> {
                override fun onComplete() {}
                override fun onError(e: Throwable) {}
                override fun onNext(result: Boolean) {}
                override fun onSubscribe(d: Disposable) {}
            })
    }

    fun startPlay() {
        if (isStartPlay) {
            return
        }
        isStartPlay = true
        if (null != mVideoDecoder) {
            mVideoDecoder!!.start()
        }
    }

    fun stopPlay() {
        isStartPlay = false
    }

    /**
     * 连接websocket流
     */
    private fun contactWebsocket():Boolean{
        if (isFinishing) {
            return true
        }

        synchronized(mCondition) {
            if (null != mWebSocketClient) {
                mWebSocketClient!!.reconnect()
                return true
            }

            var url:String = mOptionBuilder?.mUrl!!
            mWebSocketClient = LiveWebSocketClient(url)
            mWebSocketClient!!.connectionLostTimeout = 20
            mWebSocketClient!!.connectBlocking()
        }
        return true
    }

    fun runPause(pauseTime:Long) {
        var waitTime = 0
        while (!isFinishing) {
            if (waitTime > pauseTime) {
                break;
            }
            try {
                Thread.sleep(5)
            } catch (e:java.lang.Exception) {
            }
            waitTime += 5
        }
    }

    @Subscriber(mode = ThreadMode.ASYNC)
    fun socketDataHandle(data: BaseServerDataRespond<ByteBuffer>) {
        val code = data.err
        when (code) {
            Constants.WEBSOCKET_CLOSE_TYPE ->  {
                runPause(Constants.INTERVAL_MILLS)
                if (isSuraceDestroyed || isFinishing) {
                    return
                }

                if (--mWebSocketRetryTimes > 0) {
                    start()
                } else {
                    if (mLivePlayerListener != null) {
                        livePlayerCallback(Constants.LIVE_PLAYER_CALL_ERROR,
                            mOptionBuilder?.mUrl!!, LivePlayMessage(Constants.LIVE_PLAYER_SOCKET_ERROR, "err"))
                    }
                }
            } else -> {
            // 忽略错误：SConstants.WEBSOCKET_ERROR_TYPE
        }
        }
    }

    @Subscriber(tag = Constants.WEBSOCKET_BLOB_MSG_TYPE.toString(), mode = ThreadMode.POST)
    fun socketBlobDataHandle(data: BaseServerDataRespond<ByteBuffer>) {
        if (!isFinishing && !isSuraceDestroyed) {
            val byteBuffer = data.obj
            byteBuffer?.let {
                val gLiveData = GLiveData(it)

                synchronized(mCondition) {
                    val isAudio:Boolean = gLiveData.isAudio()
                    val isVideo:Boolean = gLiveData.isVideo()
                    if(isAudio && !this.isMute) {
                        playAudio(gLiveData)
                    } else if (isVideo) {
                        playVideo(gLiveData)
                    }
                }
            }
        }
    }

    private fun playVideo(data:GLiveData) {
        if (null == mVideoDecoder) {
            return
        }

        fixHW(data)
        mVideoDecoder!!.addData(data)
    }

    fun playAudio(data: GLiveData) {
        if (!isStartPlay) {
            return
        }

        if (!isAudioConfigured) {
            mAudioManagerUtil = AudioManagerUtil()
            mAudioManagerUtil!!.init()
            isAudioConfigured = true
        }
        mAudioManagerUtil!!.play(data)
    }

    /**
     * 打开静音
     */
    fun muteOpen() {
        this.isMute = true
    }

    /**
     * 关闭静音
     */
    fun muteClose() {
        this.isMute = false
    }

    /**
     * 静音是否开启
     */
    fun isMute():Boolean {
        return this.isMute
    }

    private fun createBitmap(buf:ByteArray, width: Int, height: Int):Bitmap? {
        var bitmap:Bitmap ?= null
        if (buf != null) {
            var image:YuvImage = YuvImage(buf, ImageFormat.NV21, width, height, null)
            if(image!=null){
                var stream: ByteArrayOutputStream = ByteArrayOutputStream();
                image.compressToJpeg(Rect(0, 0, width, height), 100, stream);
                val jdata = stream.toByteArray()
                val bitmapFatoryOptions = BitmapFactory.Options()
                bitmapFatoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bitmap = BitmapFactory.decodeByteArray(jdata, 0, jdata.size, bitmapFatoryOptions)
            }
        }
        return bitmap
    }

    /**
     * 截图
     */
    fun screenShot(screenShotCallback: ScreenshotListener) {
        if (null == mVideoDecoder) {
            screenShotCallback.onError("video get fail!")
            return
        }

        var bitmap:Bitmap? = null
        Observable.create(ObservableOnSubscribe<Boolean> {
            var data:YUVImageData? = null
            synchronized(mCondition) {
                data = mVideoDecoder?.getYUVImageData()
            }
            var width = data!!.mWidth
            var height = data!!.mHeight
            bitmap = createBitmap(data!!.mYUVData!!, width, height)
            it.onNext(true)
        }).subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<Boolean> {
                override fun onComplete() {}

                override fun onError(e: Throwable) {}

                override fun onNext(result: Boolean) {
                    if (result) {
                        screenShotCallback.onSuccess(bitmap)
                    }
                }

                override fun onSubscribe(d: Disposable) {
                }
            })
    }

    fun tearDown(isReleaseSurface:Boolean) {
        if (isReleaseSurface) {
            isSuraceDestroyed = true
        }

        synchronized(mCondition) {
            stopPlay()

            closeWebsocketClient()

            if (mAudioManagerUtil != null) {
                mAudioManagerUtil!!.release()
                mAudioManagerUtil = null
            }

            if (mVideoDecoder != null) {
                mVideoDecoder!!.release()
                mVideoDecoder = null
            }

            if (null != mSurface && isReleaseSurface) {
                mSurface!!.release()
                mSurface = null
            }
        }
    }

    private fun closeWebsocketClient() {
        if (null != mWebSocketClient) {
            try {
                mWebSocketClient!!.close()
            } catch (e:Exception) {
            } finally {
                mWebSocketClient = null
            }
        }
    }

    /**
     * ================================================== 修正视频宽高相关逻辑 ==================================================
     */
    // 获取视频宽高以缩放渲染分辨率
    private fun fixHW(data:GLiveData) {
        if (!isFixingWH) {
            return
        }

        val width = data.uWidth.get()
        val height = data.uHeight.get()
        mSurfaceWidth = SystemUtils.getScreenWidth(context) - 60
        mSurfaceHeight = mSurfaceWidth / width * height
        doFix(data)
    }

    private fun doFix(data:GLiveData) {
        try {
            if (!mVideoDecoder!!.setup(mSurface, data)) {
                return
            }
        } catch (e:IllegalArgumentException) {
            e.printStackTrace()
        } catch (e:IllegalStateException) {
            e.printStackTrace()
        }

        // 运算videoSurfaceView的宽高以保持纵横比
        val surfaceLayoutParams = layoutParams
        // 将实际应该有的宽高赋值给LayoutParams
        surfaceLayoutParams.width = mSurfaceWidth
        surfaceLayoutParams.height = mSurfaceHeight
        fixVideoLayout(surfaceLayoutParams)
        runPause(300)
    }

    // 应用videoSurfaceView的宽高，由于解码线程在子线程，这里需要切回主线程执行
    private fun fixVideoLayout(surfaceLayoutParams: ViewGroup.LayoutParams) {
        val msg = Message()
        msg.obj = surfaceLayoutParams
        changeSurfaceViewWhHandler.sendMessage(msg)
    }

    class Decoder {
        private val TAG = "Decoder"
        private val VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private val FRAME_RATE:Int = 20;               // 20fps
        private val VIDEO_FRAME_TIMESTAMP:Long = 33000

        private var mLivePlayer:LivePlayer ?= null
        private var mDecoder:MediaCodec ?= null

        private val mConditionCallback = Object()
        private val mYUVImgDataLock = Object()

        private var mLstTimestampUs:Long = 0
        private var mCurrTimestampUs:Long = 0
        // 用来缓存接收的视频数据
        private val mGLiveDatas by lazy { Vector<GLiveData>() }
        private var mYUVImageData = YUVImageData()
        private var mGLiveDataLock = Object()


        constructor(livePlayer: LivePlayer) {
            this.mLivePlayer = livePlayer
        }

        fun getMediaFormat(data: GLiveData):MediaFormat {
            var mediaFormat:MediaFormat
            var mimeType = getMimeType()
            mediaFormat = MediaFormat.createVideoFormat(mimeType, data.uWidth.get(), data.uHeight.get())
            // 表示编码器会尽量把输出码率控制为设定值
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ)
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE)
            return mediaFormat
        }

        private fun getMimeType():String {
            return VIDEO_MIME_TYPE
        }

        private fun isPlayAvailable():Boolean {
            if (null == mDecoder || !mLivePlayer!!.isStartPlay) {
                return false
            }
            return true
        }

        fun setup(surface:Surface?, data:GLiveData):Boolean {
            if (mDecoder != null) {
                return true
            }

            var mediaFormat:MediaFormat = getMediaFormat(data)
            val mimeType = getMimeType()
            if (!MediaUtils.hasDecoder(mimeType)) {
                Log.i(TAG,"No decoder found for mimeType= $mimeType")
                return false
            }

            mDecoder = MediaCodec.createDecoderByType(mimeType)
            mDecoder!!.configure(mediaFormat, surface, null, 0)

            mDecoder!!.setCallback(object:MediaCodec.Callback() {
                override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                    synchronized(mConditionCallback) {
                        if (codec != mDecoder) {
                            return
                        }

                        getScreenData(index)
                        if (info.size > 0) {
                            codec.releaseOutputBuffer(index, info.presentationTimeUs * 1000)
                        } else {
                            codec.releaseOutputBuffer(index, false)
                        }
                    }
                }

                override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                    var data = getData()
                    while (data == null) {
                        if (!isPlayAvailable()) {
                            return
                        }

                        data = getData()
                        try {
                            Thread.sleep(1)
                        } catch (e:Exception) {
                        }
                    }

                    synchronized(mConditionCallback) {
                        if (!isPlayAvailable() || null == data) {
                            return
                        }

                        val size = data.dwPacketSize.get().toInt()
                        var buf = data.nPacketBuffer
                        val width:Int = data.uWidth.get()
                        val height:Int = data.uHeight.get()

                        var inputBuffer = codec.getInputBuffer(index)
                        // 清空buffer
                        inputBuffer!!.clear()
                        inputBuffer.put(buf, 0, size)

                        var presentationTimeUs:Long = data.timeStamp * 1000
                        if (mCurrTimestampUs <= 0) {
                            mCurrTimestampUs = VIDEO_FRAME_TIMESTAMP
                            var realVideoFrameTimestamp = presentationTimeUs - mLstTimestampUs
                            if (realVideoFrameTimestamp <= VIDEO_FRAME_TIMESTAMP * 10) {
                                mCurrTimestampUs = realVideoFrameTimestamp
                            }
                        } else {
                            var realVideoFrameTimestamp = presentationTimeUs - mLstTimestampUs
                            mCurrTimestampUs += realVideoFrameTimestamp
                        }
                        mLstTimestampUs = presentationTimeUs
                        // 解码
                        try {
                            var value:Int = buf!![4].and(0x0f).toInt() //nalu, 5是I帧, 7是sps 8是pps.
                            Log.i(TAG, "value:$value width:$width height:$height")
                            if (value == 7) { // 如果不能保证第一帧写入的是sps和pps， 可以用这种方式等待sps和pps发送到之后写入解码器
                                mDecoder!!.queueInputBuffer(index, 0, size, mCurrTimestampUs,  MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
                            } else if (value == 5) {
                                mDecoder!!.queueInputBuffer(index, 0, size, mCurrTimestampUs, 0) // 将缓冲区入队
                            } else {
                                mDecoder!!.queueInputBuffer(index, 0, size, mCurrTimestampUs, 0) //将缓冲区入队
                            }
                        } catch (e:Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                }

                override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                }
            })
            return true
        }

        fun start() {
            if(null != mDecoder) {
                try {
                    mDecoder!!.start()
                } catch (e:Exception) {
                    e.printStackTrace()
                }
                // 设置视频保持纵横比，此方法必须在configure和start之后执行才有效
                mDecoder!!.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
            }
        }

        fun addData(data: GLiveData) {
            synchronized(mGLiveDataLock) {
                if (!mLivePlayer!!.isStartPlay ||
                    !NaluUtil.checkH264(data.nPacketBuffer!!, data.dwPacketSize.get().toInt())) {
                    return
                }

                if (mGLiveDatas.size > 10) {
                    return
                }
                mGLiveDatas.add(data)
            }
        }

        fun getData():GLiveData? {
            synchronized(mGLiveDataLock) {
                if (mGLiveDatas.size <= 0) {
                    return null
                }
                var data = mGLiveDatas[0]
                mGLiveDatas.remove(data)
                return data
            }
        }

        private fun clearData() {
            synchronized(mGLiveDataLock) {
                mGLiveDatas.clear()
            }
        }

        fun getScreenData(outputBufferIndex:Int) {
            try {
                var byteBuffer:ByteBuffer? = mDecoder!!.getOutputBuffer(outputBufferIndex)
                var nv21Datas = ByteArray(byteBuffer!!.capacity())
                byteBuffer.get(nv21Datas, 0, nv21Datas.size)

                var mediaFormat:MediaFormat = mDecoder!!.outputFormat
                if (null != mediaFormat) {
                    val colorFormat:Int = mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)
                    val width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH)
                    val height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT)

                    when (mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar -> {
                        }
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar -> {
                        }
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> {
                        }
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar -> {
                            nv21Datas = YUVUtil.yuv420spToYuv420P(nv21Datas, width, height)
                            nv21Datas = YUVUtil.I420ToNV21(nv21Datas, width, height)
                        }
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> {
                        }
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar -> {
                            nv21Datas = YUVUtil.I420ToNV21(nv21Datas, width, height)
                        } else -> {
                        // nv21Datas = YUVUtil.yuv_420_888toNV(nv21Datas, width, height)
                    }
                    }
                    setYUVImageData(nv21Datas, width, height)

                    if (null != mLivePlayer) {
                        mLivePlayer!!.playFirstVideoFrameCallback(nv21Datas, width, height)
                    }
                }
            } catch (e:Exception) {
            }
        }

        fun release() {
            synchronized(mConditionCallback) {
                if (mDecoder != null) {
                    try {
                        // 停止解码，此时可以再次调用cnfigure()方法
                        mDecoder!!.stop()
                    } catch (e: IllegalStateException) {
                    }
                    // 释放内存
                    mDecoder!!.release()
                    mDecoder = null

                    clearData()
                }
            }
        }

        private fun setYUVImageData(bytes:ByteArray?, width:Int, height: Int) {
            synchronized(mYUVImgDataLock) {
                if (mYUVImageData == null) {
                    mYUVImageData = YUVImageData()
                }
                mYUVImageData?.mYUVData = bytes
                mYUVImageData?.mWidth = width
                mYUVImageData?.mHeight = height
                mYUVImageData?.mSize = bytes?.size!!
            }
        }

        public fun getYUVImageData():YUVImageData? {
            var yuvImageData:YUVImageData? = null
            synchronized(mYUVImgDataLock) {
                if (mYUVImageData != null) {
                    yuvImageData = YUVImageData()
                    yuvImageData?.mYUVData = ByteArray(mYUVImageData?.mSize!!)
                    yuvImageData?.mSize = mYUVImageData?.mSize!!
                    System.arraycopy(mYUVImageData?.mYUVData!!,
                        0, yuvImageData?.mYUVData, 0, yuvImageData?.mSize!!)
                    yuvImageData?.mWidth = mYUVImageData!!.mWidth
                    yuvImageData?.mHeight = mYUVImageData!!.mHeight
                }
            }
            return yuvImageData
        }
    }

    class OptionBuilder {
        var mUrl = ""

        fun setUrl(url:String):OptionBuilder? {
            this.mUrl = url
            return this
        }
    }

    fun onDestroy() {
        isFinishing = true
        tearDown(true)

        if (null != changeSurfaceViewWhHandler) {
            changeSurfaceViewWhHandler.removeCallbacksAndMessages(null)
        }
        Log.i(TAG, "onDestroy")
    }
}