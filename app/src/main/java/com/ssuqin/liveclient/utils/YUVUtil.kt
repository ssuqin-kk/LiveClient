package com.ssuqin.liveclient.utils

import java.nio.ByteBuffer

class YUVUtil {
    companion  object {
        fun yuv420spToYuv420P(yuv420spData: ByteArray, width: Int, height: Int): ByteArray {
            val yuv420pData = ByteArray(width * height * 3 / 2)
            val ySize = width * height
            System.arraycopy(yuv420spData, 0, yuv420pData, 0, ySize)  //拷贝 Y 分量

            var j = 0
            var i = 0
            while (j < ySize / 2) {
                yuv420pData[ySize + i] = yuv420spData[ySize + j]   //U 分量
                yuv420pData[ySize * 5 / 4 + i] = yuv420spData[ySize + j + 1]   //V 分量
                j += 2
                i++
            }
            return yuv420pData
        }

        fun I420ToNV21(input:ByteArray, width:Int, height:Int):ByteArray {
            var output:ByteArray = ByteArray(width * height * 3/2)
            var frameSize = width * height
            var qFrameSize:Int = frameSize / 4
            var tempFrameSize:Int = frameSize * 5 / 4
            System.arraycopy(input, 0, output, 0, frameSize)
            for(index in 0 until  qFrameSize) {
                output[frameSize + index * 2] = input[tempFrameSize + index]
                output[frameSize + index * 2 + 1] = input[frameSize + index]
            }
            return output
        }

        fun nv21ToI420(data:ByteArray, width:Int, height:Int):ByteArray {
            var ret:ByteArray = ByteArray(width * height * 3/2);
            val total = width * height;

            val bufferY:ByteBuffer = ByteBuffer.wrap(ret, 0, total);
            val bufferU:ByteBuffer = ByteBuffer.wrap(ret, total, total / 4);
            val bufferV:ByteBuffer = ByteBuffer.wrap(ret, total + total / 4, total / 4);

            bufferY.put(data, 0, total)
            for (index in total until data.size step 2) {
                bufferV.put(data[index])
                bufferU.put(data[index+1]);
            }
            return ret;
        }

        fun yuv_420_888toNV(data:ByteArray, width: Int, height: Int):ByteArray {
            var ret:ByteArray = ByteArray(width * height * 3/2);
            val total = width * height;

            val bufferY:ByteBuffer = ByteBuffer.wrap(ret, 0, total);
            val bufferU:ByteBuffer = ByteBuffer.wrap(ret, total, total / 4);
            val bufferV:ByteBuffer = ByteBuffer.wrap(ret, total + total / 4, total / 4);

            bufferY.put(data, 0, total)
            for (index in total until data.size step 2) {
                bufferV.put(data[index])
                bufferU.put(data[index+1]);
            }
            return yuv_420_888toNV(bufferY, bufferU, bufferV, false)
        }

        fun yuv_420_888toNV(
            yBuffer: ByteBuffer,
            uBuffer: ByteBuffer,
            vBuffer: ByteBuffer,
            nv12: Boolean
        ): ByteArray {
            val nv: ByteArray

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            nv = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv, 0, ySize)
            if (nv12) {//U and V are swapped
                vBuffer.get(nv, ySize, vSize)
                uBuffer.get(nv, ySize + vSize, uSize)
            } else {
                uBuffer.get(nv, ySize, uSize)
                vBuffer.get(nv, ySize + uSize, vSize)
            }
            return nv
        }
    }
}