package io.igrant.mobileagent.handlers

import okhttp3.RequestBody

interface CommonHandler {
    fun taskCompleted(){}
    fun onExchangeDataComplete(serviceEndPoint: String, typedBytes: RequestBody) {}
    fun taskStarted()
}