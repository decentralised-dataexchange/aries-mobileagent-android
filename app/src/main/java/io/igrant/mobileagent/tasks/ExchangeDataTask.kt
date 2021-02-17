package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.presentationExchange.CredentialValue
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.presentationExchange.RequestCredential
import io.igrant.mobileagent.utils.PresentationExchangeStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.ledger.LedgerResults
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ExchangeDataTask(
    private val commonHandler: CommonHandler,
    private val mPresentationExchange: PresentationExchange?,
    private val requestedAttributes: HashMap<String, CredentialValue>
) :
    AsyncTask<String, Void, Void>() {

    private var typedBytes: RequestBody? = null
    private var serviceEndPoint: String? = null
    private val TAG = "ExchangeDataTask"

    override fun doInBackground(vararg params: String?): Void? {

        val recordId: String = params[0] ?: ""
        val mConnectionId: String = params[1] ?: ""
        Pool.setProtocolVersion(2)

        val schemaParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
        val credParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
        var ledgerFailed: Boolean = false
        requestedAttributes.forEach() { (key, value) ->
            val proverCred =
                Anoncreds.proverGetCredential(WalletManager.getWallet, value.credId).get()
            val schemaResponse =
                Ledger.buildGetSchemaRequest(
                    null,
                    JSONObject(proverCred).getString("schema_id")
                )
                    .get()
            val requestResponse =
                Ledger.submitRequest(PoolManager.getPool, schemaResponse).get()
            try {
                val schemaParsed = Ledger.parseGetSchemaResponse(requestResponse).get()
                if (!schemaParsedList.contains(schemaParsed))
                    schemaParsedList.add(schemaParsed)

                val credDefResponse =
                    Ledger.buildGetCredDefRequest(
                        null,
                        JSONObject(proverCred).getString("cred_def_id")
                    )
                        .get()

                val credDefSubmitResponse =
                    Ledger.submitRequest(PoolManager.getPool, credDefResponse).get()

                val creedDefParsed = Ledger.parseGetCredDefResponse(credDefSubmitResponse).get()
                if (!credParsedList.contains(creedDefParsed))
                    credParsedList.add(creedDefParsed)
            } catch (e: Exception) {
                ledgerFailed = true
            }
        }

        if (!ledgerFailed) {

            val requestCredential = RequestCredential()
            requestCredential.requestedAttributes = requestedAttributes
            requestCredential.requestedPredicates = Object()
            requestCredential.selfAttestedAttributes = Object()


            val schemaMap = convertArrayListToHashMap(schemaParsedList)
            var schema = "{\n"
            schemaMap?.forEach { (s, jsonObject) ->
                schema += " \"${s}\": $jsonObject ,\n"
            }
            schema = schema.substring(0, schema.length - 2)
            schema += "}"

            val credMap = convertArrayListToHashMap(credParsedList)
            var credDef = "{\n"
            credMap?.forEach { (s, jsonObject) ->
                credDef += " \"${s}\": $jsonObject ,\n"
            }
            credDef = credDef.substring(0, credDef.length - 2)
            credDef += "}"

            val pr = WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest)
            val te = WalletManager.getGson.toJson(
                requestCredential
            )
            Log.d(
                TAG,
                "doInBackground: \n ${WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest)} \n ${WalletManager.getGson.toJson(
                    requestCredential
                )} \n $schema \n $credDef"
            )
            val proverProofResponse = Anoncreds.proverCreateProof(
                WalletManager.getWallet,
                WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
                WalletManager.getGson.toJson(requestCredential),
                "IGrantMobileAgent-000001",
                schema,
                credDef,
                "{}"
            ).get()

            mPresentationExchange?.presentation = JSONObject(proverProofResponse)
            mPresentationExchange?.state = PresentationExchangeStates.PRESENTATION_SENT

            Log.d(
                TAG,
                "initListener: ${WalletManager.getGson.toJson(mPresentationExchange)}"
            )

            if (recordId != "")
                WalletRecord.updateValue(
                    WalletManager.getWallet, WalletRecordType.PRESENTATION_EXCHANGE_V10,
                    recordId, WalletManager.getGson.toJson(mPresentationExchange)
                )

            val connectionObjectRecord =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\n" +
                            "  \"request_id\":\"${mPresentationExchange?.connectionId}\"\n" +
                            "}"
                )

            val connectionObject = WalletManager.getGson.fromJson(
                connectionObjectRecord.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            //get prover did
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.DID_KEY,
                "{\n" +
                        "  \"did\":\"${connectionObject.theirDid}\"\n" +
                        "}"
            )

            val base64 = Base64.encodeToString(
                proverProofResponse.toByteArray(),
                Base64.NO_WRAP
            )

            val data = "{\n" +
                    "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/presentation\",\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"~thread\": {\n" +
                    "    \"thid\": \"${mPresentationExchange?.threadId}\"\n" +
                    "  },\n" +
                    "  \"presentations~attach\": [\n" +
                    "    {\n" +
                    "      \"@id\": \"libindy-presentation-0\",\n" +
                    "      \"mime-type\": \"application/json\",\n" +
                    "      \"data\": {\n" +
                    "        \"base64\": \"$base64\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"comment\": \"auto-presented for proof request nonce=1234567890\"\n" +
                    "}"
            val didDocObject =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\n" +
                            "  \"did\":\"${connectionObject.theirDid}\"\n" +
                            "}"
                )

            val packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"${searchResponse.records?.get(0)?.value ?: ""}\"]",
                publicKey,
                data.toByteArray()
            ).get()

            typedBytes = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedMessage)
                }
            }
            val didDoc =
                WalletManager.getGson.fromJson(
                    didDocObject.records?.get(0)?.value,
                    DidDoc::class.java
                )
            serviceEndPoint =
                didDoc.service?.get(0)?.serviceEndpoint ?: ""
        }
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.onExchangeDataComplete(serviceEndPoint, typedBytes)
    }

    private fun convertArrayListToHashMap(arrayList: ArrayList<LedgerResults.ParseResponseResult>): HashMap<String, JSONObject>? {
        val hashMap: HashMap<String, JSONObject> = HashMap()
        for (str in arrayList) {
            hashMap[str.id] = JSONObject(str.objectJson)
        }
        return hashMap
    }
}