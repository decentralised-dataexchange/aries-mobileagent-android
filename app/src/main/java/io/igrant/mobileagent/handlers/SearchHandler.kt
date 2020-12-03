package io.igrant.mobileagent.handlers

import io.igrant.mobileagent.models.walletSearch.SearchResponse

interface SearchHandler {
    fun taskCompleted(searchResponse: SearchResponse)
    fun taskStarted(){}
    fun onError(){}
}