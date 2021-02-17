package io.igrant.mobileagent.handlers

import okhttp3.RequestBody

interface CommonHandler {
    fun taskCompleted(){}
    fun onExchangeDataComplete(serviceEndPoint: String, typedBytes: RequestBody) {}
    fun onSaveConnection(
        typedBytes: RequestBody,
        connectionRequest: RequestBody,
        queryFeaturePackedBytes: RequestBody
    ){
    }
    fun onSaveDidComplete(typedBytes:RequestBody,serviceEndPoint:String){
    }
    fun taskStarted()
}