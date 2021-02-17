package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.utils.ConnectionStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_DOC
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_KEY
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import java.io.IOException
import java.util.*

class SaveDidDocTask(
    private val commonHandler: CommonHandler,
    private val body: String,
    private val iGrantEnabled: Boolean
) :
    AsyncTask<Void, Void, Void>() {

    private lateinit var serviceEndPoint: String
    private lateinit var typedBytes: RequestBody
    private val TAG = "SaveDidDocTask"

    override fun doInBackground(vararg p0: Void?): Void? {

        //todo add a new tag in the connection with invitaion key and extra my var key then update my key not invitation key
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()

        val response = JSONObject(String(unpacked))

        val message = JSONObject(response.get("message").toString())

        val recipientKey = response.getString("sender_verkey")

        val connectionSig = JSONObject(message.get("connection~sig").toString())
        val sigData = connectionSig.get("sig_data").toString()
        val position = Base64.decode(sigData, Base64.URL_SAFE)
            .toString(charset("UTF-8")).indexOf("{")
        val data =
            Base64.decode(sigData, Base64.URL_SAFE).toString(charset("UTF-8"))
                .substring(position)

        val didData = JSONObject(data)
        val didDoc = didData.getString("DIDDoc")
        val theirDid = didData.getString("DID")

        val didDocUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            DID_DOC,
            didDocUuid,
            didDoc.toString(),
            tagJson
        )

        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
            .getString("publicKeyBase58")

        val didKeyUuid = UUID.randomUUID().toString()

        val didKeyTagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            DID_KEY,
            didKeyUuid,
            publicKey,
            didKeyTagJson
        )

        val connectionSearch = SearchUtils.searchWallet(CONNECTION,
            "{\"invitation_key\":\"$recipientKey\"}")

        val mediatorConnectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        mediatorConnectionObject.theirDid = theirDid
        mediatorConnectionObject.state = ConnectionStates.CONNECTION_RESPONSE
        mediatorConnectionObject.isIGrantEnabled = iGrantEnabled

        val connectionUuid =
            connectionSearch.records?.get(0)?.id

        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

        WalletRecord.updateValue(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            value
        )

        val requestId = mediatorConnectionObject.requestId
        val myDid = mediatorConnectionObject.myDid


        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val publicKey2 = metaObject.getString("verkey")

        val didDocSearch = SearchUtils.searchWallet(
            DID_DOC,
            "{\"did\":\"$theirDid\"}"
        )

        serviceEndPoint = ""
        var recipient = ""
        if (didDocSearch.totalCount ?: 0 > 0) {
            val didDoc = WalletManager.getGson.fromJson(
                didDocSearch.records?.get(0)?.value,
                DidDoc::class.java
            )

            serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
            recipient = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
        }

        val connectionTagJson = "{\n" +
                "  \"their_did\": \"$theirDid\",\n" +
                "  \"request_id\": \"$requestId\",\n" +
                "  \"my_did\": \"$myDid\",\n" +
                "  \"invitation_key\": \"$recipientKey\",\n" +
                "  \"recipient_key\": \"$recipient\",\n" +
                "  \"orgId\": \"${mediatorConnectionObject.orgId}\"\n" +
                "}"

        WalletRecord.updateTags(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            connectionTagJson
        )

        val trustPingData = "{\n" +
                "  \"@type\": \"https://didcomm.org/trust_ping/1.0/ping\",\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"comment\": \"ping\",\n" +
                "  \"response_requested\": true\n" +
                "}\n"

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$recipient\"]",
            publicKey2,
            trustPingData.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        typedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.onSaveDidComplete(typedBytes,serviceEndPoint)
    }
}