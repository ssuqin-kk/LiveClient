package com.ssuqin.liveclient

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import com.ssuqin.liveclient.listener.ScreenshotListener
import com.ssuqin.liveclient.constant.Constants
import com.ssuqin.liveclient.listener.LivePlayerListener
import com.ssuqin.liveclient.model.LivePlayMessage
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var mLivePlayer: LivePlayer?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mLivePlayer = findViewById(R.id.livePlayer)

        findViewById<Button>(R.id.btnScreenshot).setOnClickListener {
            mLivePlayer?.screenShot(object:ScreenshotListener {
                override fun onError(err: String) {
                }

                override fun onSuccess(bitmap: Bitmap?) {
                    if(bitmap != null) {
                        findViewById<ImageView>(R.id.ivShowScreenshot).setImageBitmap(bitmap)
                    } else {
                        Log.e(TAG,"bitmap is null")
                    }
                }
            })
        }

        findViewById<Button>(R.id.btnMuteCtrl).setOnClickListener {
            if (null != mLivePlayer) {
                if (mLivePlayer!!.isMute()) {
                    findViewById<Button>(R.id.btnMuteCtrl).setText("打开静音")
                    mLivePlayer!!.muteClose()
                } else {
                    findViewById<Button>(R.id.btnMuteCtrl).setText("关闭静音")
                    mLivePlayer!!.muteOpen()
                }
            }
        }

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            if (null != mLivePlayer) {
                mLivePlayer!!.onRefresh()
            }
        }
    }

    override fun onRestart() {
        Log.i(TAG, "onRestart")
        super.onRestart()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()

        var optionBuilder: LivePlayer.OptionBuilder = LivePlayer.OptionBuilder()
        optionBuilder.setUrl("xxxxxxx")
        mLivePlayer?.setOptionBuilder(optionBuilder)
        mLivePlayer?.setLivePlayerListener(object:LivePlayerListener {
            override fun onPlayFirstVideoFrameCallback(url: String?, bitmap: Bitmap) {
                runOnUiThread(Runnable {
                    findViewById<ImageView>(R.id.ivShowFirstVideoFrame).setImageBitmap(bitmap)
                })
            }

            override fun onPlayCallback(
                callbackType: Int,
                url: String?,
                message: LivePlayMessage?) {
                if (callbackType == Constants.LIVE_PLAYER_CALL_ERROR) {
                    try {
                        Thread.sleep(10)
                    } catch (e:Exception) {
                    }
                    if (null == message) {
                        return
                    }

                    if(message.mCode == Constants.LIVE_PLAYER_SOCKET_ERROR) {
                        mLivePlayer?.start()
                    }
                } else if(callbackType == Constants.LIVE_PLAYER_CALL_PREPARED) {
                    mLivePlayer?.start()
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onBackPressed() {
        Log.i(TAG, "onBackPressed()")
        finish()
        super.onBackPressed()
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        if (null != mLivePlayer) {
            mLivePlayer!!.onDestroy()
        }
        super.onDestroy()
    }
}