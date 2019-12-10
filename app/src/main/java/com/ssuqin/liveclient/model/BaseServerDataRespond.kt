package com.ssuqin.liveclient.model

/**
 * 通用服务数据基础类
 */
class BaseServerDataRespond<T> {
    var err: Int = 0
    var msg: String = ""
    var obj: T? = null
}