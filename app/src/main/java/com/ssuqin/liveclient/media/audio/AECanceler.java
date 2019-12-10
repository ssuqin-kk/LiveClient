package com.ssuqin.liveclient.media.audio;

import android.media.audiofx.AcousticEchoCanceler;

/**
 * 回音消除类，可能对国内Rom机型适配的问题，后续可使用第三方的回音消除程序：
 * WebRTC中的AEC/AECM
 * speex
 */
public class AECanceler {

    private AcousticEchoCanceler canceler;

    //判断当前机型是否支持AEC
    public boolean isDeviceSupportAEC() {
        return AcousticEchoCanceler.isAvailable();
    }

    //初始化AEC并开启
    public boolean initAEC(int audioSession) {
        if (!isDeviceSupportAEC() || canceler != null) {
            return false;
        }
        canceler = AcousticEchoCanceler.create(audioSession);
        canceler.setEnabled(true);

        return canceler.getEnabled();
    }

    /**
     * AEC开关
     */
    public boolean setAECEnabled(boolean enable) {
        if (null == canceler) {
            return false;
        }
        canceler.setEnabled(enable);
        return canceler.getEnabled();
    }

    /**
     * 释放资源
     */
    public boolean release() {
        try {
            if (null == canceler) {
                return false;
            }
            canceler.setEnabled(false);
            canceler.release();
        } catch (Exception ignored) {
        }
        return true;
    }
}
