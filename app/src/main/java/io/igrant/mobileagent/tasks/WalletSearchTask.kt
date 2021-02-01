package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import io.igrant.mobileagent.handlers.SearchHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import io.igrant.mobileagent.utils.SearchUtils
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

class WalletSearchTask(private val searchHandler: SearchHandler) :
    AsyncTask<String, Void, Void>() {

    private val TAG = "OpenWalletTask"

    private var searchResponseObj = SearchResponse()
    override fun doInBackground(vararg params: String?): Void? {
        val type: String = params[0] ?: "{}"
        val queryJson: String = params[1] ?: ""
        val search = SearchUtils.searchWallet(type,
            queryJson)
//        WalletSearch.open(
//            WalletManager.getWallet,
//            type,
//            queryJson,
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val searchResponse =
//            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()
//
//        WalletManager.closeSearchHandle(search)
        searchResponseObj = search
//            WalletManager.getGson.fromJson(searchResponse, SearchResponse::class.java)
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        searchHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        searchHandler.taskCompleted(searchResponseObj)
    }
}