package com.ssuqin.liveclient.utils

import android.content.Context
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log

import java.io.IOException

object MediaUtils {
    private val TAG = "MediaUtils"

    private val sMCL = MediaCodecList(MediaCodecList.REGULAR_CODECS)

    private fun hasCodecForMime(encoder: Boolean, mime: String): Boolean {
        for (info in sMCL.codecInfos) {
            if (encoder != info.isEncoder) {
                continue
            }

            for (type in info.supportedTypes) {
                if (type.equals(mime, ignoreCase = true)) {
                    Log.i(TAG, "found codec " + info.name + " for mime " + mime)
                    return true
                }
            }
        }
        return false
    }

    fun hasDecoder(mimes: String): Boolean {
        return hasCodecForMime(false /* encoder */, mimes)
    }

    @Throws(IOException::class)
    fun createMediaExtractorForMimeType(
        context: Context, resourceId: Uri, mimeTypePrefix: String
    ): MediaExtractor {
        val extractor = MediaExtractor()
        extractor.setDataSource(resourceId.toString(), null)
        var trackIndex: Int
        trackIndex = 0
        while (trackIndex < extractor.trackCount) {
            val trackMediaFormat = extractor.getTrackFormat(trackIndex)
            if (trackMediaFormat.getString(MediaFormat.KEY_MIME)!!.startsWith(mimeTypePrefix)) {
                extractor.selectTrack(trackIndex)
                break
            }
            trackIndex++
        }
        if (trackIndex == extractor.trackCount) {
            extractor.release()
            throw IllegalStateException("couldn't get a track for $mimeTypePrefix")
        }

        return extractor
    }
}