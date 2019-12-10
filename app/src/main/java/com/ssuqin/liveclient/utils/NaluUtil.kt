package com.ssuqin.liveclient.utils

import android.util.Log

class NaluUtil {
    class Nalu_t {
        var startcodeprefix_len:Int = 0
        var len:Int = 0
        var max_size:Int = 0
        var forbidden_bit:Int = 0
        var nal_reference_idc:Int = 0
        var buf:ByteArray ?= null
    }

    companion object {
        var TAG:String = "NaluUtil"

        fun findStartCode2(buf:ByteArray, offset: Int):Int {
            if (buf[offset].toInt() != 0 ||
                buf[offset+1].toInt() != 0 ||
                buf[offset+2].toInt() != 1) {
                return 0
            }
            return 1
        }

        fun findStartCode3(buf:ByteArray, offset:Int):Int{
            if (buf[offset].toInt() != 0 ||
                buf[offset+1].toInt() != 0 ||
                buf[offset+2].toInt() != 0 ||
                buf[offset+3].toInt() != 1) {
                return 0
            }
            return 1
        }

        fun checkH264(data: ByteArray, size:Int):Boolean {
            var pos = 0
            var nalu:Nalu_t = Nalu_t()
            nalu.buf = ByteArray(size)

            var checkPass:Boolean = true
            var offset:Int = 0

            var dataLength = getAnnexbNALU(data, size, nalu, offset)
            if (dataLength <= 4) {
                Log.i(TAG, "data_length[$dataLength]\n")
                checkPass = false
            }

            if(nalu.forbidden_bit != 0) {
                val forbidden_bit = nalu.forbidden_bit
                Log.i(TAG, "forbidden_bit[$forbidden_bit]\n")
                checkPass = false
            }
            return checkPass
        }

        fun getAnnexbNALU(data:ByteArray, size:Int, nalu:Nalu_t, offset:Int):Int {
            if (data == null || size < offset + 4) {
                return 0
            }
            var pos = 0
            var StartCodeFound:Int = 0
            var rewind:Int = 0

            nalu.startcodeprefix_len = 3

            var info3:Int
            var info2 = findStartCode2(data, offset)
            if (info2 == 1) {
                nalu.startcodeprefix_len = 3
                pos = 3
            } else {
                info3 = findStartCode3(data, offset);
                if (info3 != 1) {
                    return -1
                }
                pos = 4;
                nalu.startcodeprefix_len = 4;
            }

            StartCodeFound = 0
            info2 = 0
            info3 = 0

            while (StartCodeFound == 0) {
                if (pos + offset >= size) {
                    nalu.len = pos - nalu.startcodeprefix_len
                    System.arraycopy(nalu.buf!!, 0, data, offset + nalu.startcodeprefix_len, nalu.len)
                    nalu.forbidden_bit = nalu.buf!![0].toInt().and(0x80).toInt().shl(7)
                    return pos
                }
                pos++
                info3 = findStartCode3(data, pos-4 + offset)
                if (info3 != 1) {
                    info2 = findStartCode2(data, pos - 3 + offset)
                }
                StartCodeFound = if((info2 == 1).or(info3 == 1)) 1 else 0
            }
            rewind = if (info3 == 1) -4 else -3
            nalu.len = (pos + rewind) - nalu.startcodeprefix_len
            System.arraycopy(nalu.buf!!, 0, data, offset + nalu.startcodeprefix_len, nalu.len)
            nalu.forbidden_bit = nalu.buf!![0].toInt().and(0x80).toInt().shl(7)
            return pos + rewind
        }
    }
}