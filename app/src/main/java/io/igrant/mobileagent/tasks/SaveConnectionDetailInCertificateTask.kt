package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import android.util.Log
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ReceiveCertificateEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.connection.Connection
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.utils.DidCommPrefixUtils
import io.igrant.mobileagent.utils.PackingUtils
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class SaveConnectionDetailInCertificateTask() : AsyncTask<String, Void, Void>() {

    override fun doInBackground(vararg params: String?): Void? {

        val connectionId: String = params[0] ?: ""

        val certificateId: String = params[1] ?: ""

        val searchConnection = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"request_id\":\"$connectionId\"}"
        )

        if (searchConnection.totalCount ?: 0 > 0) {
            val connectionObj = WalletManager.getGson.fromJson(
                searchConnection.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
            val searchDidDoc = SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObj.theirDid}\"}"
            )

            if (searchDidDoc.totalCount ?: 0 > 0) {

                val disDoc = WalletManager.getGson.fromJson(
                    searchDidDoc.records?.get(0)?.value,
                    DidDoc::class.java
                )
                val orgData =
                    "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"${"$connectionId"}\" , \"~transport\": {" +
                            "\"return_route\": \"all\"}\n}"

                val metaString =
                    Did.getDidWithMeta(WalletManager.getWallet, connectionObj?.myDid).get()
                val metaObject = JSONObject(metaString)
                val key = metaObject.getString("verkey")

                val orgDetailPacked = PackingUtils.packMessage(
                    disDoc, key,
                    orgData,""
                )

                val orgDetailTypedArray = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(orgDetailPacked)
                    }
                }


                ApiManager.api.getService()
                    ?.postData(disDoc.service?.get(0)?.serviceEndpoint ?: "", orgDetailTypedArray)
                    ?.enqueue(object :
                        Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {

                        }

                        override fun onResponse(
                            call: Call<ConfigPostResponse>,
                            response: Response<ConfigPostResponse>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                val unpack =
                                    Crypto.unpackMessage(
                                        WalletManager.getWallet,
                                        WalletManager.getGson.toJson(response.body()).toString()
                                            .toByteArray()
                                    ).get()

                                Log.d(
                                    "milan",
                                    "onResponse: ${JSONObject(String(unpack)).getString("message")}"
                                )
                                val connectionData = WalletManager.getGson.fromJson(
                                    JSONObject(String(unpack)).getString("message"),
                                    Connection::class.java
                                )

                                val walletSearch = SearchUtils.searchWallet(
                                    WalletRecordType.WALLET,
                                    "{\"credential_id\":\"$certificateId\"}"
                                )

                                if (walletSearch.totalCount ?: 0 > 0) {
                                    val wallet = WalletManager.getGson.fromJson(
                                        walletSearch.records?.get(0)?.value,
                                        WalletModel::class.java
                                    )
                                    wallet.organization = connectionData
//
                                    WalletRecord.updateValue(
                                        WalletManager.getWallet,
                                        WalletRecordType.WALLET,
                                        certificateId,
                                        WalletManager.getGson.toJson(wallet)
                                    )

                                    EventBus.getDefault().post(ReceiveCertificateEvent())
                                }
                            }
                        }
                    })
            }
        }




        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
    }
}