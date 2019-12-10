package com.ssuqin.liveclient.listener

import android.graphics.Bitmap
import com.ssuqin.liveclient.model.LivePlayMessage

interface ScreenshotListener {
    fun onError(err:String)
    fun onSuccess(bitmap: Bitmap?)
}