package com.ssuqin.liveclient.network

import android.util.Log
import com.ssuqin.liveclient.model.WebSocketFirstRequest
import com.google.gson.Gson
import com.ssuqin.liveclient.constant.Constants
import com.ssuqin.liveclient.model.BaseServerDataRespond
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.simple.eventbus.EventBus
import java.lang.reflect.InvocationTargetException
import java.net.URI
import java.nio.ByteBuffer

class LiveWebSocketClient(url: String) : WebSocketClient(URI(url)) {
    private val TAG = "LiveWebSocketClient"

    override fun onOpen(handshakedata: ServerHandshake) {
        println("Socket onOpen")
        val request = WebSocketFirstRequest()
        val gson = Gson().toJson(request)

        println("sent gson: $gson")
        send(gson)
        postData(Constants.WEBSOCKET_OPEN_TYPE, "open")
    }

    override fun onMessage(message: String) {
        postData(Constants.WEBSOCKET_STR_MSG_TYPE, message)
    }

    override fun onMessage(bytes: ByteBuffer?) {
        postData(Constants.WEBSOCKET_BLOB_MSG_TYPE, "blob", bytes)
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        println("Socket onClose: " + getURI() + "; Code: " + code + " " + reason)
        postData(Constants.WEBSOCKET_CLOSE_TYPE, "close")
    }

    override fun onError(ex: Exception) {
        println("Socket onError: $ex")
        postData(Constants.WEBSOCKET_ERROR_TYPE, "error")
    }

    private fun postData(code: Int, msg: String, bytes: ByteBuffer? = null) {
        val data = BaseServerDataRespond<ByteBuffer>()
        data.err = code
        data.msg = msg
        data.obj = bytes

        if (code == Constants.WEBSOCKET_BLOB_MSG_TYPE) {
            if (bytes == null) {
                return
            }
            try {
                EventBus.getDefault().post(data, code.toString())
            } catch (e:InvocationTargetException) {
                Log.i(TAG, "InvocationTargetException:"+e.message)
            }
        } else {
            try {
                EventBus.getDefault().post(data)
            } catch (e:InvocationTargetException) {
                Log.i(TAG, "InvocationTargetException 2:"+e.message)
            }
        }
    }
}