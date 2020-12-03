//package io.igrant.mobileagent.tasks
//
//import android.os.AsyncTask
//import android.util.Base64
//import android.util.Log
//import com.google.gson.Gson
//import io.igrant.mobileagent.activty.InitializeActivity
//import io.igrant.mobileagent.handlers.CommonHandler
//import io.igrant.mobileagent.indy.PoolUtils
//import io.igrant.mobileagent.indy.WalletManager
//import io.igrant.mobileagent.models.credentialExchange.CredentialExchange
//import io.igrant.mobileagent.models.credentialExchange.IssueCredential
//import io.igrant.mobileagent.models.credentialExchange.RawCredential
//import io.igrant.mobileagent.models.did.DidResult
//import io.igrant.mobileagent.models.walletSearch.SearchResponse
//import io.igrant.mobileagent.utils.CredentialExchangeStates
//import io.igrant.mobileagent.utils.WalletRecordType
//import org.hyperledger.indy.sdk.ledger.Ledger
//import org.hyperledger.indy.sdk.non_secrets.WalletRecord
//import org.hyperledger.indy.sdk.non_secrets.WalletSearch
//import org.hyperledger.indy.sdk.pool.Pool
//import org.json.JSONObject
//
//class SaveIssueCredentialTask(private val handler: CommonHandler,private val data:JSONObject) : AsyncTask<Void, Void, Void>() {
//    private val TAG = "SaveIssueCredentialTask"
//    override fun doInBackground(vararg p0: Void?): Void? {
//        val gson = Gson()
//
//        val issueCredential = gson.fromJson(data.getString("message"), IssueCredential::class.java)
//        val rawCredential = gson.fromJson(
//            Base64.decode(issueCredential.credentialsAttach[0].data?.base64, Base64.URL_SAFE)
//                .toString(charset("UTF-8")), RawCredential::class.java
//        )
//
//        val string = data.getString("sender_verkey")
//
//        //getting did key
//        val searchDid = WalletSearch.open(
//            WalletManager.getWallet,
//            WalletRecordType.DID_KEY,
//            "{\"key\": \"${string}\"}",
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val didResponse =
//            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()
//
//        Log.d(TAG, "searchDid: $didResponse")
//        WalletManager.closeSearchHandle(searchDid)
//
//        val didData = JSONObject(didResponse).getJSONArray("records").get(0).toString()
//
//        val didResult = gson.fromJson(didData, DidResult::class.java)
//        //closing did key
//
//        //getting credential exchange
//        val credentialExchangeSearch = WalletSearch.open(
//            WalletManager.getWallet,
//            WalletRecordType.CREDENTIAL_EXCHANGE_V10,
//            "{\"thread_id\": \"${issueCredential.thread?.thid ?: ""}\"}",
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val credentialExchangeResponse =
//            WalletSearch.searchFetchNextRecords(
//                WalletManager.getWallet,
//                credentialExchangeSearch,
//                100
//            ).get()
//
//        Log.d(TAG, "credentialExchangeResult: $credentialExchangeResponse")
//        WalletManager.closeSearchHandle(credentialExchangeSearch)
//
//        val credentialExchangeResult = gson.fromJson(credentialExchangeResponse, SearchResponse::class.java)
//        //closing credential exchange
//
//        //getting did doc
//        val searchDidDoc = WalletSearch.open(
//            WalletManager.getWallet,
//            WalletRecordType.DID_DOC,
//            "{\"did\": \"${didResult.tags!!.did}\"}",
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val didDocResponse =
//            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDidDoc, 100).get()
//
//        Log.d(TAG, "searchDid: $didDocResponse")
//        WalletManager.closeSearchHandle(searchDidDoc)
//
//        val didDocResult = gson.fromJson(didDocResponse, SearchResponse::class.java)
//
//        if (credentialExchangeResult.totalCount ?: 0 > 0) {
//            val credentialExchange =
//                gson.fromJson(credentialExchangeResult.records?.get(0)?.value, CredentialExchange::class.java)
//            credentialExchange.rawCredential = rawCredential
//            credentialExchange.state = CredentialExchangeStates.CREDENTIAL_CREDENTIAL_RECEIVED
//
//            WalletRecord.updateValue(
//                WalletManager.getWallet,
//                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
//                "${issueCredential.thread?.thid ?: ""}",
//                gson.toJson(credentialExchange)
//            )
//
//            sendAcknoledge(
//                issueCredential.thread?.thid ?: "",
//                didResult.tags!!.did,
//                data.getString("sender_verkey"),
//                data.getString("recipient_verkey"),
//                credentialExchange.credentialOffer?.credDefId
//            )
//        }
//        return null
//    }
//
//    override fun onPreExecute() {
//        super.onPreExecute()
//        handler.taskStarted()
//    }
//
//    override fun onPostExecute(result: Void?) {
//        super.onPostExecute(result)
//        handler.taskCompleted()
//    }
//}