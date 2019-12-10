package com.ssuqin.liveclient.model

import javolution.io.Struct
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GLiveData:Struct {
    var uWidth:Unsigned16 = Unsigned16()
    var uHeight:Unsigned16 = Unsigned16()
    var dwPacketSize:Unsigned32 = Unsigned32()
    var timeStamp:Long = -1
    var nPacketBuffer:ByteArray?= null

    var isVideoData:Boolean = false
    var isAudioData:Boolean = false

    // 一定要加上这个，不然会出现对齐的问题
    override fun isPacked(): Boolean {
        return true
    }

    // 设置为小端数据
    override fun byteOrder(): ByteOrder {
        return ByteOrder.LITTLE_ENDIAN
    }

    constructor(byteBuffer: ByteBuffer) {
    }

    fun isVideo():Boolean {
        return isVideoData
    }

    fun isAudio():Boolean {
        return isAudioData
    }

    fun isValidData():Boolean {
        val width = uWidth.get()
        val height = uHeight.get()
        if (width <= 0 || height <= 0) {
            return false
        }
        return true
    }
}