package com.ssuqin.liveclient.media.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import com.ssuqin.liveclient.model.GLiveData;

import java.nio.ByteBuffer;

public class AudioManagerUtil {
    private final String TAG = "AudioManagerUtil";

    private int sampleRate = 8000; //44100;
    private int channelConfigOut = AudioFormat.CHANNEL_OUT_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private int minBufSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat);

    private G711UCodec g711UCodec = new G711UCodec();
    private AudioTrack mAudioTrack;

    /**
     * 初始化
     */
    public void init() {
        if (mAudioTrack != null) {
            release();
        }

        AudioFormat format =
                new AudioFormat.Builder().setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                            .build())
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBufSizeOut)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
        } else {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL,
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, minBufSizeOut,
                    AudioTrack.MODE_STREAM);
        }

        Thread playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                    mAudioTrack.play();
                } catch (Exception ignored) {
                }
            }
        });
        playThread.start();
    }

    public void play(GLiveData data) {
        int len = (int)data.getDwPacketSize().get();
        short[] byteArray = new short[len];
        g711UCodec.decode(byteArray, data.getNPacketBuffer(), len, 0); //解码

        play(byteArray, 0, byteArray.length);
    }

    /**
     * 将网络传输的数据进行解码并播放
     */
    public void playWithByteBuffer(ByteBuffer it) {
        //byte[] bytes = new byte[4096];
        byte[] bytes = new byte[it.capacity()];
        it.get(bytes, 0, bytes.length);

        int len = bytes.length;
        short[] byteArray = new short[len];
        g711UCodec.decode(byteArray, bytes, len, 0); //解码

        play(byteArray, 0, byteArray.length);
    }

    /**
     * 将解码后的音频数据写入audioTrack播放
     *
     * @param data   数据
     * @param offset 偏移
     * @param length 需要播放的长度
     */
    private void play(short[] data, int offset, int length) {
        if (data == null || data.length == 0) {
            return;
        }
        try {
            mAudioTrack.write(data, offset, length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onPlaying() {
        return mAudioTrack != null && mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
    }

    /**
     * 释放资源
     */
    public void release() {
        try {
            if (mAudioTrack != null) {
                if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
                    mAudioTrack.stop();
                }
                mAudioTrack.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "release exception " + e.toString());
        }
    }
}