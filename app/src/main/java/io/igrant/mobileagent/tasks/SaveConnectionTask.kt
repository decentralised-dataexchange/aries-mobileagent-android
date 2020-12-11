package io.igrant.mobileagent.tasks

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import io.igrant.mobileagent.activty.InitializeActivity
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.*
import io.igrant.mobileagent.models.tagJsons.ConnectionId
import io.igrant.mobileagent.models.tagJsons.ConnectionTags
import io.igrant.mobileagent.models.tagJsons.UpdateInvitationKey
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import io.igrant.mobileagent.utils.ConnectionStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CONNECTION_INVITATION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MEDIATOR_DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*

class SaveConnectionTask(private val commonHandler: CommonHandler,private val invitation :Invitation) :
    AsyncTask<Void, Void, Void>() {

    private lateinit var connectionRequestTypedBytes: RequestBody
    private lateinit var typedBytes: RequestBody
    private val TAG = "SaveConnectionTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        val connectionValue =
            WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
        val connectionUuid = UUID.randomUUID().toString()

        val connectionTag = ConnectionTags()
        connectionTag.invitationKey = invitation?.recipientKeys!![0]
        connectionTag.state = ConnectionStates.CONNECTION_INVITATION

        val connectionTagJson =
            WalletManager.getGson.toJson(connectionTag)

        WalletRecord.add(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            connectionValue.toString(),
            connectionTagJson.toString()
        )

        val connectionInvitationTagJson = WalletManager.getGson.toJson(ConnectionId(connectionUuid))
        val connectionInvitationUuid = UUID.randomUUID().toString()

        Log.d(TAG, "saveRecord2: wallet value : $connectionTagJson")
        Log.d(TAG, "saveRecord2: wallet UUID : $connectionInvitationUuid")

        WalletRecord.add(
            WalletManager.getWallet,
            CONNECTION_INVITATION,
            connectionInvitationUuid,
            WalletManager.getGson.toJson(invitation),
            connectionInvitationTagJson
        )

        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        val myDid = myDidResult.did

        val requestId = UUID.randomUUID().toString()
        val value = WalletManager.getGson.toJson(
            setUpMediatorConnectionObject(
                invitation,
                requestId,
                myDid
            )
        )

        WalletRecord.updateValue(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            value
        )

        val tagJson =
            WalletManager.getGson.toJson(UpdateInvitationKey(requestId, myDid, invitation?.recipientKeys?.get(0), null))
        WalletRecord.updateTags(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            tagJson
        )

        val messageUuid = UUID.randomUUID().toString()

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val data = "{\n" +
                "    \"@id\": \"$messageUuid\",\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/basic-routing/1.0/add-route\",\n" +
                "    \"routedestination\": \"$key\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.MEDIATOR_CONNECTION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val connectionData = JSONObject(connection)
        Log.d(TAG, "getMediatorConfig: $connection")

        val connectionRecords = JSONArray(connectionData.get("records").toString())
        val connectionRecord =
            JSONObject(connectionRecords.getJSONObject(0).getString("value"))
        val connectionDid = connectionRecord.getString("my_did")

        val connectionMetaString =
            Did.getDidWithMeta(WalletManager.getWallet, connectionDid).get()
        val connectionMetaObject = JSONObject(connectionMetaString)
        val connectedKey = connectionMetaObject.getString("verkey")

        val mediatorDidDoc = SearchUtils.searchWallet(MEDIATOR_DID_DOC,"{}")

        val didDocObj = WalletManager.getGson.fromJson(mediatorDidDoc.records?.get(0)?.value, DidDoc::class.java)

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"${didDocObj.publicKey!![0].publicKeyBase58}\"]",
            connectedKey,
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

        //public keys
        val publicKey = PublicKey()
        publicKey.id = "did:sov:$myDid#1"
        publicKey.type = "Ed25519VerificationKey2018"
        publicKey.controller = "did:sov:$myDid"
        publicKey.publicKeyBase58 = key

        val publicKeys: ArrayList<PublicKey> = ArrayList()
        publicKeys.add(publicKey)

        //authentication
        val authentication = Authentication()
        authentication.type = "Ed25519SignatureAuthentication2018"
        authentication.publicKey = "did:sov:$myDid#1"

        val authentications: ArrayList<Authentication> = ArrayList()
        authentications.add(authentication)

        //service
        val recipientsKey: ArrayList<String> = ArrayList()
        recipientsKey.add(key)

        //service
        val routis: ArrayList<String> = ArrayList()
        routis.add(didDocObj.service!![0].routingKeys!![0])

        val service = Service()
        service.id = "did:sov:$myDid;indy"
        service.type = "IndyAgent"
        service.priority = 0
        service.recipientKeys = recipientsKey
        service.routingKeys = routis
        service.serviceEndpoint = "https://mediator.igrant.io"

        val services: ArrayList<Service> = ArrayList()
        services.add(service)

        //did doc
        val didDoc = DidDoc()
        didDoc.context = "https://w3id.org/did/v1"
        didDoc.id = "did:sov:$myDid"
        didDoc.publicKey = publicKeys
        didDoc.authentication = authentications
        didDoc.service = services

        //did
        val did = DID()
        did.did = myDid
        did.didDoc = didDoc

//         transport
        val transport = Transport()
        transport.returnRoute = "all"

        //connection request
        val connectionRequest = ConnectionRequest()
        connectionRequest.type = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/request"
        connectionRequest.id = UUID.randomUUID().toString()
        connectionRequest.label = "Mobile agent 0018"
        connectionRequest.connection = did
        connectionRequest.transport = transport

        val connectionRequestData = WalletManager.getGson.toJson(connectionRequest)

        val connectionRequestPackedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"${invitation?.recipientKeys?.get(0)}\"]",
            key,
            connectionRequestData.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        connectionRequestTypedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(connectionRequestPackedMessage)
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
        commonHandler.onSaveConnection(typedBytes,connectionRequestTypedBytes)
    }

    private fun setUpMediatorConnectionObject(
        invitation: Invitation?,
        requestId: String?,
        did: String?
    ): MediatorConnectionObject {
        val connectionObject = MediatorConnectionObject()
        connectionObject.theirLabel = invitation?.label ?: ""
        connectionObject.theirImageUrl = invitation?.imageUrl ?: ""
        connectionObject.theirDid = ""
        connectionObject.inboxId = ""
        connectionObject.inboxKey = ""
        connectionObject.requestId = requestId
        connectionObject.myDid = did

        if (invitation != null && !(invitation.recipientKeys.isNullOrEmpty()))
            connectionObject.invitationKey = invitation.recipientKeys!![0]
        else
            connectionObject.invitationKey = ""

        connectionObject.createdAt = "2020-10-22 12:20:23.188047Z"
        connectionObject.updatedAt = "2020-10-22 12:20:23.188047Z"

        connectionObject.theirLabel = invitation?.label
        connectionObject.state = if (did != null) ConnectionStates.CONNECTION_REQUEST else ConnectionStates.CONNECTION_INVITATION

        return connectionObject
    }
}