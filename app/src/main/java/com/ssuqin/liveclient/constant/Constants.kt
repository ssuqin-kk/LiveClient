package com.ssuqin.liveclient.constant

object Constants {
    const val INTERVAL_MILLS = 500L

    const val WEBSOCKET_OPEN_TYPE = 0
    const val WEBSOCKET_STR_MSG_TYPE = 1
    const val WEBSOCKET_BLOB_MSG_TYPE = 2
    const val WEBSOCKET_CLOSE_TYPE = 3
    const val WEBSOCKET_ERROR_TYPE = 4

    // code
    const val LIVE_PLAYER_OK = 0
    const val LIVE_PLAYER_ERROR_UNSET = 1
    const val LIVE_PLAYER_ERROR_INVALID_URL = 2
    const val LIVE_PLAYER_ERROR_B_SN = 3
    const val LIVE_PLAYER_ERROR_CHANNEL = 4
    const val LIVE_PLAYER_ERROR_CLIENT = 5
    const val LIVE_PLAYER_ERROR_OTHER = 6
    const val LIVE_PLAYER_SOCKET_ERROR = 7
    const val LIVE_PLAYER_PREPARED = 8
    const val LIVE_PLAYER_MESSAGE_UNKNOW = -1

    // callback
    const val LIVE_PLAYER_CALL_PREPARED = 0
    const val LIVE_PLAYER_CALL_ERROR = 1
    const val LIVE_PLAYER_CALL_COMPLETE = 2
}