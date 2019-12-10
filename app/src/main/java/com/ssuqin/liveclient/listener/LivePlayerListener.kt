package com.ssuqin.liveclient.listener

import android.graphics.Bitmap
import com.ssuqin.liveclient.model.LivePlayMessage

interface LivePlayerListener {
    fun onPlayCallback(callbackType:Int, url:String?, message:LivePlayMessage?)

    /**
     * 播放第一帧回调
     */
    fun onPlayFirstVideoFrameCallback(url:String?, bitmap: Bitmap)
}