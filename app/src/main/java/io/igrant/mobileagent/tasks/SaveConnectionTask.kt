package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import android.util.Log
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.*
import io.igrant.mobileagent.models.tagJsons.ConnectionId
import io.igrant.mobileagent.models.tagJsons.ConnectionTags
import io.igrant.mobileagent.models.tagJsons.UpdateInvitationKey
import io.igrant.mobileagent.utils.*
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
import org.json.JSONObject
import java.io.IOException
import java.util.*

class SaveConnectionTask(
    private val commonHandler: CommonHandler,
    private val invitation: Invitation
) :
    AsyncTask<Void, Void, Void>() {

    private lateinit var queryFeaturePackedBytes: RequestBody
    private lateinit var connectionRequestTypedBytes: RequestBody
    private lateinit var typedBytes: RequestBody
    private val TAG = "SaveConnectionTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        val connectionValue =
            WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
        val connectionUuid = UUID.randomUUID().toString()

        val connectionTag = ConnectionTags()
        connectionTag.invitationKey = invitation.recipientKeys!![0]
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

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val queryFeatureData = "{\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/discover-features/1.0/query\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"query\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/igrantio-operator/*\",\n" +
                "    \"comment\": \"Querying features available.\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        val queryFeaturePacked: ByteArray

        queryFeaturePacked =
            if (invitation.routingKeys != null && invitation.routingKeys?.size ?: 0 > 0) {
                PackingUtils.packMessage(
                    "[\"${invitation.recipientKeys?.get(0) ?: ""}\"]",
                    WalletManager.getGson.toJson(invitation.routingKeys),
                    key,
                    queryFeatureData
                )
            } else {
                PackingUtils.packMessage(
                    "[\"${invitation.recipientKeys?.get(0) ?: ""}\"]",
                    key,
                    queryFeatureData
                )
            }
//        val queryFeaturePacked = Crypto.packMessage(
//            WalletManager.getWallet,
//            "[\"${invitation.recipientKeys?.get(0) ?: ""}\"]",
//            key,
//            queryFeatureData.toByteArray()
//        ).get()

        queryFeaturePackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryFeaturePacked)
            }
        }

        val tagJson =
            WalletManager.getGson.toJson(
                UpdateInvitationKey(
                    requestId,
                    myDid,
                    invitation.recipientKeys!![0],
                    null,
                    null
                )
            )
        WalletRecord.updateTags(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            tagJson
        )

        val messageUuid = UUID.randomUUID().toString()

        val data = "{\n" +
                "    \"@id\": \"$messageUuid\",\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/basic-routing/1.0/add-route\",\n" +
                "    \"routedestination\": \"$key\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val mediatorConnection = SearchUtils.searchWallet(
            WalletRecordType.MEDIATOR_CONNECTION,
            "{}"
        )

        val mediatorConnectionObject = WalletManager.getGson.fromJson(
            mediatorConnection.records?.get(0)?.value,
            MediatorConnectionObject::class.java
        )
        val connectionDid = mediatorConnectionObject.myDid

        val connectionMetaString =
            Did.getDidWithMeta(WalletManager.getWallet, connectionDid).get()
        val connectionMetaObject = JSONObject(connectionMetaString)
        val connectedKey = connectionMetaObject.getString("verkey")

        val mediatorDidDoc = SearchUtils.searchWallet(MEDIATOR_DID_DOC, "{}")

        val didDocObj = WalletManager.getGson.fromJson(
            mediatorDidDoc.records?.get(0)?.value,
            DidDoc::class.java
        )

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
        connectionRequest.label = DeviceUtils.getDeviceName() ?: ""
        connectionRequest.connection = did
        connectionRequest.transport = transport

        val connectionRequestData = WalletManager.getGson.toJson(connectionRequest)

        val connectionRequestPackedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"${invitation.recipientKeys?.get(0)}\"]",
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
        commonHandler.onSaveConnection(
            typedBytes,
            connectionRequestTypedBytes,
            queryFeaturePackedBytes
        )
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
        connectionObject.state =
            if (did != null) ConnectionStates.CONNECTION_REQUEST else ConnectionStates.CONNECTION_INVITATION

        return connectionObject
    }

}