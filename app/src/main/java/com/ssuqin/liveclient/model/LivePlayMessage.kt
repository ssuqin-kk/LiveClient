package com.ssuqin.liveclient.model

class LivePlayMessage {
    public var mCode:Int = 0
    public var mError:String = ""

    constructor(code:Int, error:String) {
        this.mCode = code
        this.mError = error
    }
}
