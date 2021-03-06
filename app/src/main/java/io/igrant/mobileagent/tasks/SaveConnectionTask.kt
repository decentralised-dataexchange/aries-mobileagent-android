package io.igrant.mobileagent.tasks

import android.os.AsyncTask
import android.util.Log
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.*
import io.igrant.mobileagent.models.tagJsons.ConnectionId
import io.igrant.mobileagent.models.tagJsons.UpdateInvitationKey
import io.igrant.mobileagent.utils.*
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
) : AsyncTask<String, Void, Void>() {

    private lateinit var connectionRequestTypedBytes: RequestBody
    private lateinit var typedBytes: RequestBody
    private val TAG = "SaveConnectionTask"

    override fun doInBackground(vararg p0: String?): Void? {

        val myDid = p0[0] ?: ""
        val key = p0[1] ?: ""
        val orgId = p0[2] ?: ""
        val requestId = p0[3] ?: ""
        val location = p0[4] ?: ""

        val connectionValue =
            WalletManager.getGson.toJson(
                setUpMediatorConnectionObject(
                    invitation,
                    null,
                    null
                )
            )
        val connectionUuid = UUID.randomUUID().toString()

        var invitationKey = UpdateInvitationKey(requestId, myDid, invitation.recipientKeys!![0], "", "")
        invitationKey.state = ConnectionStates.CONNECTION_INVITATION
        invitationKey.myKey = key
        invitationKey.orgId= orgId

        val connectionTagJson =
            WalletManager.getGson.toJson(invitationKey)

        WalletRecord.add(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            connectionUuid,
            connectionValue.toString(),
            connectionTagJson.toString()
        )

        val connectionInvitationTagJson =
            WalletManager.getGson.toJson(ConnectionId(connectionUuid))
        val connectionInvitationUuid = UUID.randomUUID().toString()

        WalletRecord.add(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION_INVITATION,
            connectionInvitationUuid,
            WalletManager.getGson.toJson(invitation),
            connectionInvitationTagJson
        )

        var connectionObject = setUpMediatorConnectionObject(
            invitation,
            requestId,
            myDid
        )
        connectionObject.orgId = orgId
        connectionObject.location = location
        val value = WalletManager.getGson.toJson(
            connectionObject
        )

        WalletRecord.updateValue(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            connectionUuid,
            value
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

        val packedMessage = PackingUtils.packMessage("[\"${didDocObj.publicKey!![0].publicKeyBase58}\"]",connectedKey,data)

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

        val connectionRequestPackedMessage =
            PackingUtils.packMessage(invitation, key, connectionRequestData)

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
            connectionRequestTypedBytes
        )
    }

    private fun setUpMediatorConnectionObject(
        invitation: Invitation?,
        requestId: String?,
        did: String?
    ): MediatorConnectionObject {
        val connectionObject = MediatorConnectionObject()
        connectionObject.theirLabel = invitation?.label ?: ""
        connectionObject.theirImageUrl = invitation?.image_url ?: invitation?.imageUrl ?: ""
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