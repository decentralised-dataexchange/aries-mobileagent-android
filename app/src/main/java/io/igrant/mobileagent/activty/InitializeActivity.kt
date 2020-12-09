package io.igrant.mobileagent.activty

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.igrant.mobileagent.R
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.handlers.PoolHandler
import io.igrant.mobileagent.handlers.SearchHandler
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.InitialActivityFunctions
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.ConfigResponse
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.certificateOffer.Base64Extracted
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.connectionRequest.*
import io.igrant.mobileagent.models.credentialExchange.CredentialExchange
import io.igrant.mobileagent.models.credentialExchange.CredentialProposalDict
import io.igrant.mobileagent.models.credentialExchange.IssueCredential
import io.igrant.mobileagent.models.credentialExchange.RawCredential
import io.igrant.mobileagent.models.did.DidResult
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.presentationExchange.PresentationRequest
import io.igrant.mobileagent.models.tagJsons.ConnectionId
import io.igrant.mobileagent.models.tagJsons.ConnectionTags
import io.igrant.mobileagent.models.tagJsons.UpdateInvitationKey
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.tasks.LoadLibIndyTask
import io.igrant.mobileagent.tasks.OpenWalletTask
import io.igrant.mobileagent.tasks.PoolTask
import io.igrant.mobileagent.tasks.WalletSearchTask
import io.igrant.mobileagent.utils.*
import io.igrant.mobileagent.utils.ConnectionStates.Companion.CONNECTION_ACTIVE
import io.igrant.mobileagent.utils.ConnectionStates.Companion.CONNECTION_INVITATION
import io.igrant.mobileagent.utils.ConnectionStates.Companion.CONNECTION_REQUEST
import io.igrant.mobileagent.utils.ConnectionStates.Companion.CONNECTION_RESPONSE
import io.igrant.mobileagent.utils.CredentialExchangeStates.Companion.CREDENTIAL_CREDENTIAL_ACK
import io.igrant.mobileagent.utils.CredentialExchangeStates.Companion.CREDENTIAL_CREDENTIAL_RECEIVED
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_CONNECTION_RESPONSE
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_ISSUE_CREDENTIAL
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_OFFER_CREDENTIAL
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_PING_RESPONSE
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_REQUEST_PRESENTATION
import io.igrant.mobileagent.utils.MessageTypes.Companion.TYPE_REQUEST_PRESENTATION_ACK
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CREDENTIAL_EXCHANGE_V10
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_DOC
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_KEY
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MEDIATOR_CONNECTION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MEDIATOR_CONNECTION_INVITATION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MEDIATOR_DID_DOC
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MEDIATOR_DID_KEY
import io.igrant.mobileagent.utils.WalletRecordType.Companion.MESSAGE_RECORDS
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.apache.commons.io.IOUtils
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class InitializeActivity : BaseActivity(),InitialActivityFunctions {

    companion object {
        private const val TAG = "InitializeActivity"
        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202

        var deviceId = ""
    }

    //views
    private lateinit var llProgressBar: LinearLayout
    private lateinit var clLoading: ConstraintLayout

    private lateinit var tvLoadingStatus: TextView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize)
        initViews()
        initToolbar()
        initLibIndy()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = "Data Wallet"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    private fun initLibIndy() {
        LoadLibIndyTask(object : CommonHandler {
            override fun taskCompleted() {
                loadPool()
                tvLoadingStatus.text = resources.getString(R.string.txt_creating_pool)
            }

            override fun taskStarted() {

            }
        }, applicationContext).execute()
    }

    private fun openWallet() {
        OpenWalletTask(object : CommonHandler {
            override fun taskCompleted() {
                getMediatorConfig()
                tvLoadingStatus.text = resources.getString(R.string.txt_finishing)
            }

            override fun taskStarted() {

            }
        }).execute()
    }

    private fun loadPool() {
        PoolTask(object : PoolHandler {
            override fun taskCompleted(pool: Pool) {
                PoolManager.setPool(pool)
                openWallet()
                tvLoadingStatus.text = resources.getString(R.string.txt_creating_wallet)
            }

            override fun taskStarted() {

            }
        }).execute()
    }

    private fun initViews() {
        llProgressBar = findViewById(R.id.llProgressBar)
        clLoading = findViewById(R.id.clLoadingScreen)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
        llProgressBar.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_new -> {
                if (PermissionUtils.hasPermissions(
                        this,
                        true,
                        PICK_IMAGE_REQUEST,
                        PERMISSIONS
                    )
                ) {
                    val i = Intent(this, QrCodeActivity::class.java)
                    startActivityForResult(
                        i,
                        REQUEST_CODE_SCAN_INVITATION
                    )
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN_INVITATION) {
            if (data == null) return

            val uri: Uri =
                Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
            val v: String = uri.getQueryParameter("c_i") ?: ""
            if (v != "") {
                saveConnection(v)
            } else {
                Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveConnection(data: String) {
        val json =
            Base64.decode(
                data,
                Base64.URL_SAFE
            ).toString(charset("UTF-8"))

        val gson = Gson()
        val invitation: Invitation = gson.fromJson(json, Invitation::class.java)
        if (ConnectionUtils.checkIfConnectionAvailable(invitation.recipientKeys!![0])) {
            Toast.makeText(
                this,
                resources.getString(R.string.err_connection_already_added),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val snackbar: Snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "Connecting to connection. Will update when the connection is active...",
                Snackbar.LENGTH_LONG
            )
            snackbar.show()

            saveConnectionRecord(invitation, false)
        }
    }

    private fun getMediatorConfig() {
        WalletSearchTask(object : SearchHandler {
            override fun taskCompleted(searchResponse: SearchResponse) {
                if (searchResponse.totalCount == 0) {
                    ApiManager.api.getService()?.getAgentConfig()
                        ?.enqueue(object : Callback<ConfigResponse> {
                            override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                                llProgressBar.visibility = View.GONE
                            }

                            override fun onResponse(
                                call: Call<ConfigResponse>,
                                response: Response<ConfigResponse>
                            ) {
                                if (response.code() == 200 && response.body() != null) {
                                    saveConnectionRecord(response.body()!!.invitation, true)
                                }
                            }
                        })
                } else {
                    val connectionData =
                        JSONObject(searchResponse.records?.get(0)?.value)

                    when (connectionData.getString("state")) {
                        CONNECTION_REQUEST, CONNECTION_INVITATION -> {
                            val myDid: String = connectionData.getString("my_did")
                            val requestId: String = connectionData.getString("request_id")

                            packConnectionRequestMessage(myDid, requestId)
                        }
                        CONNECTION_RESPONSE -> {
                            //GET DID DOC FROM RECORD FOR PUBLIC KEY
                            //CALL createInbox
                        }
                        CONNECTION_ACTIVE -> {
                            llProgressBar.visibility = View.GONE
                            val myDid: String = connectionData.getString("my_did")
                            pollMessagesInThread(myDid)
                            initFragment()

                            clLoading.visibility = View.GONE
                        }
                    }
                }
            }

            override fun taskStarted() {

            }
        }).execute(
            MEDIATOR_CONNECTION,
            "{}"
        )
    }

    private fun initFragment() {
        NavigationUtils.showWalletFragment(supportFragmentManager, false)
    }

    private fun deleteReadMessage(inboxItemId: String, myDid: String) {
        val data = "\n" +
                "{\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/basic-routing/1.0/delete-inbox-items\",\n" +
                "  \"inboxitemids\": [\n" +
                "    \"$inboxItemId\"\n" +
                "  ],\n" +
                "  \"~transport\": {\n" +
                "    \"return_route\": \"all\"\n" +
                "  }\n" +
                "}\n"

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val gson = Gson()
        val didSearch = WalletSearch.open(
            WalletManager.getWallet,
            MEDIATOR_DID_DOC,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, didSearch, 100).get()

        WalletManager.closeSearchHandle(didSearch)
        Log.d(TAG, "did doc: $value")
        val didDoc = JSONObject(
            JSONObject(value).getJSONArray("records").get(0).toString()
        ).getString("value")
        Log.d(TAG, "did doc 2: $didDoc")
        val test = gson.fromJson(didDoc, DidDoc::class.java)

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"${test.publicKey!![0].publicKeyBase58}\"]",
            key,
            data.toByteArray()
        ).get()

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
        ApiManager.api.getService()?.pollMessages(typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.d(TAG, "onFailure: ")
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                }
            })
    }

    private fun pollMessagesInThread(myDid: String) {
        val uuid = UUID.randomUUID().toString()
        val data = "\n" +
                "{\n" +
                "    \"@id\": \"$uuid\",\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/basic-routing/1.0/get-inbox-items\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val search = WalletSearch.open(
            WalletManager.getWallet,
            MEDIATOR_DID_DOC,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        Log.d(TAG, "did doc: $value")
        val gson = Gson()
        val didDoc = JSONObject(
            JSONObject(value).getJSONArray("records").get(0).toString()
        ).getString("value")
        Log.d(TAG, "did doc 2: $didDoc")
        val test = gson.fromJson(didDoc, DidDoc::class.java)

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"${test.publicKey?.get(0)?.publicKeyBase58}\"]",
            key,
            data.toByteArray()
        ).get()

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val typedBytes: RequestBody = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(packedMessage)
                    }
                }
                ApiManager.api.getService()?.pollMessages(typedBytes)
                    ?.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                Log.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                                val inputStream = response.body()!!.byteStream()
                                val str: String = IOUtils.toString(inputStream, "UTF-8")
                                Log.d(TAG, "onResponse: $str")
                                unPackPollMessage(str, myDid)
                            }
                        }
                    })
            }
        }, 0, 10000)
    }

    private fun unPackPollMessage(body: String, myDid: String) {

        Log.d(TAG, "unPackMessage: $body")
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        Log.d(TAG, "packConnectionRequestMessage: ${String(unpacked)}")
        val messageList = JSONObject(String(unpacked)).getString("message")
        val item = JSONObject(messageList).getJSONArray("Items")
        for (i in 0 until item.length()) {
            val o = item.getJSONObject(i).getString("Data")

            deleteReadMessage(
                item.getJSONObject(i).getString(
                    "@id"
                ), myDid
            )

            Log.d(TAG, "unPackPollMessage: item: ${item.getJSONObject(i)}")
            val unpack =
                Crypto.unpackMessage(WalletManager.getWallet, o.toString().toByteArray()).get()
            Log.d(TAG, "packConnectionRequest response: $i -  ${String(unpack)}")

            var type =
                JSONObject(JSONObject(String(unpack)).getString("message")).getString("@type")
            val index: Int = type.lastIndexOf('/')
            type = type.substring(index + 1, type.length)

            when (type) {
                TYPE_CONNECTION_RESPONSE -> {
                    unPackSigMessage(o, false)
                }
                TYPE_PING_RESPONSE -> {
                    val search = WalletSearch.open(
                        WalletManager.getWallet,
                        CONNECTION,
                        "{}",
                        "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
                    ).get()

                    val connection =
                        WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100)
                            .get()

                    WalletManager.closeSearchHandle(search)

                    val data = JSONObject(connection)

                    val connectionRecords = JSONArray(data.get("records").toString())
                    val gson = Gson()

                    val mediatorConnectionObject: MediatorConnectionObject =
                        gson.fromJson(
                            connectionRecords.getJSONObject(0).getString("value"),
                            MediatorConnectionObject::class.java
                        )

                    mediatorConnectionObject.state = CONNECTION_ACTIVE

                    val connectionUuid =
                        connectionRecords.getJSONObject(0).getString("id")

                    val value = gson.toJson(mediatorConnectionObject)

                    WalletRecord.updateValue(
                        WalletManager.getWallet,
                        CONNECTION,
                        connectionUuid,
                        value
                    )

                    val snackbar: Snackbar = Snackbar.make(
                        findViewById(android.R.id.content),
                        "Connection is now active",
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.show()
                }
                TYPE_OFFER_CREDENTIAL -> {
                    unPackOfferCredential(JSONObject(String(unpack)))
                }
                TYPE_ISSUE_CREDENTIAL -> {
                    unPackIssueCredential(JSONObject(String(unpack)))
                }
                TYPE_REQUEST_PRESENTATION -> {
                    unPackRequestPresentation(JSONObject(String(unpack)))
                }
                TYPE_REQUEST_PRESENTATION_ACK -> {
                    updatePresentProofToAck(JSONObject(String(unpack)))
                }
            }
        }
    }

    private fun updatePresentProofToAck(jsonObject: JSONObject) {

    }

    private fun unPackRequestPresentation(jsonObject: JSONObject) {
        val connectionObject = ConnectionUtils.getConnection(jsonObject.getString("sender_verkey"))

        val p = SearchUtils.searchWallet(
            WalletRecordType.PRESENTATION_EXCHANGE_V10,
            "{\"thread_id\":\"${JSONObject(jsonObject.getString("message")).getString("@id")}\"}"
        )

        val presentationRequestBase64 =
            JSONObject(JSONObject(jsonObject.getString("message")).getJSONArray("request_presentations~attach").get(0).toString())
                .getJSONObject("data").getString("base64")
        val presentationRequest = WalletManager.getGson.fromJson(
            Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                .toString(charset("UTF-8")), PresentationRequest::class.java
        )
        if (p.totalCount ?: 0 == 0) {
            val presentationExchange = PresentationExchange()
            presentationExchange.threadId = JSONObject(jsonObject.getString("message")).getString("@id")
            presentationExchange.createdAt = "2020-11-25 12:17:53.491756Z"
            presentationExchange.updatedAt = "2020-11-25 12:17:53.491756Z"
            presentationExchange.connectionId = connectionObject?.requestId
            presentationExchange.initiator = "external"
            presentationExchange.presentationProposalDict = null
            presentationExchange.presentationRequest = presentationRequest
            presentationExchange.role = "prover"
            presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
            presentationExchange.comment = JSONObject(jsonObject.getString("message")).getString("comment")

            val id = UUID.randomUUID().toString()
            val tag = "{\"thread_id\": \"${JSONObject(jsonObject.getString("message")).getString("@id")}\"}"
            WalletRecord.add(
                WalletManager.getWallet,
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                id,
                WalletManager.getGson.toJson(presentationExchange),
                tag
            )

            WalletRecord.add(
                WalletManager.getWallet,
                MESSAGE_RECORDS,
                JSONObject(jsonObject.getString("message")).getString("@id"),
                WalletManager.getGson.toJson(presentationExchange),
                "{\n" +
                        "  \"type\":\"$TYPE_REQUEST_PRESENTATION\",\n" +
                        "  \"connectionId\":\"${connectionObject?.requestId}\"\n" +
                        "}"
            )
        }
    }

    private fun unPackIssueCredential(body: JSONObject) {
        val gson = Gson()

        val issueCredential = gson.fromJson(body.getString("message"), IssueCredential::class.java)
        val rawCredential = gson.fromJson(
            Base64.decode(issueCredential.credentialsAttach[0].data?.base64, Base64.URL_SAFE)
                .toString(charset("UTF-8")), RawCredential::class.java
        )


        val string = body.getString("sender_verkey")
        val searchDid = WalletSearch.open(
            WalletManager.getWallet,
            DID_KEY,
            "{\"key\": \"${string}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val didResponse =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()

        Log.d(TAG, "searchDid: $didResponse")
        WalletManager.closeSearchHandle(searchDid)

        val didData = JSONObject(didResponse).getJSONArray("records").get(0).toString()

        val didResult = gson.fromJson(didData, DidResult::class.java)

        val credentialExchangeSearch = WalletSearch.open(
            WalletManager.getWallet,
            CREDENTIAL_EXCHANGE_V10,
            "{\"thread_id\": \"${issueCredential.thread?.thid ?: ""}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val credentialExchangeResponse =
            WalletSearch.searchFetchNextRecords(
                WalletManager.getWallet,
                credentialExchangeSearch,
                100
            ).get()

        Log.d(TAG, "credentialExchangeResult: $credentialExchangeResponse")
        WalletManager.closeSearchHandle(credentialExchangeSearch)


        val searchResponse = gson.fromJson(credentialExchangeResponse, SearchResponse::class.java)
        if (searchResponse.totalCount ?: 0 > 0) {
            val credentialExchange =
                gson.fromJson(searchResponse.records?.get(0)?.value, CredentialExchange::class.java)
            credentialExchange.rawCredential = rawCredential
            credentialExchange.state = CREDENTIAL_CREDENTIAL_RECEIVED

            WalletRecord.updateValue(
                WalletManager.getWallet,
                CREDENTIAL_EXCHANGE_V10,
                "${searchResponse.records?.get(0)?.id}",
                gson.toJson(credentialExchange)
            )

            sendAcknoledge(
                issueCredential.thread?.thid ?: "",
                didResult.tags!!.did,
                body.getString("sender_verkey"),
                body.getString("recipient_verkey"),
                credentialExchange.credentialOffer?.credDefId
            )
        }
    }

    private fun sendAcknoledge(
        thid: String,
        did: String?,
        recipientVerKey: String,
        senderVerKey: String,
        credDefId: String?
    ) {
        val gson = Gson()
        val data = "{\n" +
                "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/issue-credential/1.0/ack\",\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"~thread\": {\n" +
                "    \"thid\": \"$thid\"\n" +
                "  },\n" +
                "  \"status\": \"OK\"\n" +
                "}"

        val searchDid = WalletSearch.open(
            WalletManager.getWallet,
            DID_DOC,
            "{\"did\": \"${did}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val didResponse =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()

        Log.d(TAG, "searchDid: $didResponse")
        WalletManager.closeSearchHandle(searchDid)

        val searchResult = gson.fromJson(didResponse, SearchResponse::class.java)

        val didDoc =
            gson.fromJson(searchResult.records?.get(0)?.value, DidDoc::class.java)
        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$recipientVerKey\"]",
            senderVerKey,
            data.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
//
        ApiManager.api.getService()
            ?.postData(didDoc.service?.get(0)?.serviceEndpoint ?: "", typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        storeCredential(thid, credDefId)
                    }
                }
            })
    }

    private fun storeCredential(thid: String, credDefId: String?) {

        val builder = GsonBuilder()
        builder.serializeNulls()
        val gson: Gson = builder.setPrettyPrinting().create()

        val credDef =
            Ledger.buildGetCredDefRequest(
                null,
                credDefId
            ).get()

        val credDefResponse = Ledger.submitRequest(PoolManager.getPool, credDef).get()

        val parsedCredDefResponse = Ledger.parseGetCredDefResponse(credDefResponse).get()

        val credentialExchangeSearch = WalletSearch.open(
            WalletManager.getWallet,
            CREDENTIAL_EXCHANGE_V10,
            "{\"thread_id\": \"${thid}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val credentialExchangeResponse =
            WalletSearch.searchFetchNextRecords(
                WalletManager.getWallet,
                credentialExchangeSearch,
                100
            ).get()

        Log.d(TAG, "credentialExchangeResult: $credentialExchangeResponse")
        WalletManager.closeSearchHandle(credentialExchangeSearch)


        val searchResponse = gson.fromJson(credentialExchangeResponse, SearchResponse::class.java)
        if (searchResponse.totalCount ?: 0 > 0) {
            val credentialExchange =
                gson.fromJson(searchResponse.records?.get(0)?.value, CredentialExchange::class.java)

            Log.d(
                TAG,
                "storeCredential: \n credentialRequestMetadata: \n ${gson.toJson(credentialExchange.credentialRequestMetadata)} \n rawCredential: \n ${gson.toJson(
                    credentialExchange.rawCredential
                )} \n parsedCredDefResponse.objectJson: \n ${parsedCredDefResponse.objectJson}"
            )
            val uuid = UUID.randomUUID().toString()
            val credentialId = Anoncreds.proverStoreCredential(
                WalletManager.getWallet,
                uuid,
                gson.toJson(credentialExchange.credentialRequestMetadata),
                gson.toJson(credentialExchange.rawCredential),
                parsedCredDefResponse.objectJson,
                null
            ).get()

            Log.d(TAG, "storeCredential: $uuid \n $credentialId")
            credentialExchange.state = CREDENTIAL_CREDENTIAL_ACK

            WalletRecord.updateValue(
                WalletManager.getWallet,
                CREDENTIAL_EXCHANGE_V10,
                "${searchResponse.records?.get(0)?.id}",
                gson.toJson(credentialExchange)
            )

            WalletRecord.delete(
                WalletManager.getWallet,
                MESSAGE_RECORDS,
                thid
            ).get()

            WalletRecord.delete(
                WalletManager.getWallet,
                CREDENTIAL_EXCHANGE_V10,
                "${searchResponse.records?.get(0)?.id}"
            ).get()
        }

    }

    private fun packConnectionRequestMessage(myDid: String, requestId: String) {

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val search = WalletSearch.open(
            WalletManager.getWallet,
            MEDIATOR_CONNECTION_INVITATION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val connectionInvitationData = JSONObject(value)

        Log.d(TAG, "packConnectionRequestMessage0: $value")

        //json object
        val connectionInvitationRecords =
            JSONArray(connectionInvitationData.get("records").toString())
        val connectionInvitationRecord = connectionInvitationRecords.getJSONObject(0)
        val connectionInvitationValue = JSONObject(connectionInvitationRecord.getString("value"))

//        val recipientKeys =
//            JSONArray(connectionInvitationValue.getString("recipientKeys"))
//        val recipientKey: String = recipientKeys[0].toString()

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

        val service = Service()
        service.id = "did:sov:$myDid;indy"
        service.type = "IndyAgent"
        service.priority = 0
        service.recipientKeys = recipientsKey
        service.serviceEndpoint = ""

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

        // transport
        val transport = Transport()
        transport.returnRoute = "all"

        //connection request
        val connectionRequest = ConnectionRequest()
        connectionRequest.type = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/request"
        connectionRequest.id = requestId
        connectionRequest.label = "milan"
        connectionRequest.connection = did
        connectionRequest.transport = transport

        val str = WalletManager.getGson.toJson(connectionRequest)

        Log.d(TAG, "packConnectionRequestMessage1: $str")
        Log.d(
            TAG,
            "packConnectionRequestMessage2: ${connectionInvitationValue.getString("recipientKeys")} - $key \n ${str.toByteArray()}"
        )
        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            connectionInvitationValue.getString("recipientKeys"),
            key,
            str.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
//
        ApiManager.api.getService()?.postDetails(typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                        val inputStream = response.body()!!.byteStream()
                        val str: String = IOUtils.toString(inputStream, "UTF-8")
                        Log.d(TAG, "onResponse: $str")
                        unPackSigMessage(str, true)
                    }
                }
            })
    }

    private fun unPackOfferCredential(body: JSONObject) {
        val message = JSONObject(body.getString("message"))
        val certificateOffer =
            WalletManager.getGson.fromJson(message.toString(), CertificateOffer::class.java)

//        showPopUp(certificateOffer)
        searchDidKey(body.getString("sender_verkey"), certificateOffer)
    }

    private fun searchDidKey(string: String, certificateOffer: CertificateOffer) {
        val searchDid = WalletSearch.open(
            WalletManager.getWallet,
            DID_KEY,
            "{\"key\": \"${string}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val didResponse =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()

        Log.d(TAG, "searchDid: $didResponse")
        WalletManager.closeSearchHandle(searchDid)

        val didData = JSONObject(didResponse).getJSONArray("records").get(0).toString()
        val didResult = WalletManager.getGson.fromJson(didData, DidResult::class.java)

        val connectionResult = SearchUtils.searchWallet(
            CONNECTION,
            "{\"their_did\": \"${didResult.tags!!.did}\"}"
        )

        if (connectionResult.totalCount ?: 0 > 0) {
            val connecction = WalletManager.getGson.fromJson(
                connectionResult.records?.get(0)?.value, MediatorConnectionObject::class.java
            )

            val credentialExchangeSearch = SearchUtils.searchWallet(
                CREDENTIAL_EXCHANGE_V10,
                "{\"thread_id\": \"${certificateOffer.id}\"}"
            )

            if (credentialExchangeSearch.totalCount == 0) {
                saveCredentialExchange(
                    certificateOffer,
                    connectionResult.records?.get(0)
                )
            }

            WalletRecord.add(
                WalletManager.getWallet,
                MESSAGE_RECORDS,
                certificateOffer.id,
                WalletManager.getGson.toJson(certificateOffer),
                "{\n" +
                        "  \"type\":\"$TYPE_OFFER_CREDENTIAL\",\n" +
                        "  \"connectionId\":\"${connecction.requestId}\"\n" +
                        "}"
            )
        }
    }

    private fun saveCredentialExchange(
        certificateOffer: CertificateOffer,
        connectionRecord: Record?
    ) {
        val base64Sting =
            Base64.decode(certificateOffer.offersAttach!![0].data!!.base64, Base64.URL_SAFE)
                .toString(charset("UTF-8"))

        val credentialProposal =
            WalletManager.getGson.fromJson(base64Sting, Base64Extracted::class.java)
        val credentialProposalDict = CredentialProposalDict()
        credentialProposalDict.type =
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/issue-credential/1.0/propose-credential"
        credentialProposalDict.id = UUID.randomUUID().toString()
        credentialProposalDict.comment = "string"
        credentialProposalDict.schemaId = credentialProposal.schemaId
        credentialProposalDict.credDefId = credentialProposal.credDefId
        credentialProposalDict.credentialProposal = certificateOffer.credentialPreview

        val credentialExchange = CredentialExchange()
        credentialExchange.threadId = certificateOffer.id
        credentialExchange.createdAt = "2020-11-18 16:08:03.923715Z"
        credentialExchange.updatedAt = "2020-11-18 16:08:03.923715Z"
        credentialExchange.connectionId =
            connectionRecord?.tags?.get("request_id")
        credentialExchange.state = CredentialExchangeStates.CREDENTIAL_OFFER_RECEIVED
        credentialExchange.credentialProposalDict = credentialProposalDict
        credentialExchange.credentialOffer = credentialProposal

        Log.d(
            TAG,
            "saveCredentialExchange: ${WalletManager.getGson.toJson(credentialExchange)}"
        )
        val uudi = UUID.randomUUID().toString()
        WalletRecord.add(
            WalletManager.getWallet,
            CREDENTIAL_EXCHANGE_V10,
            uudi,
            WalletManager.getGson.toJson(credentialExchange).toString(),
            "{\"thread_id\": \"${certificateOffer.id}\"}"
        )


    }

    private fun unPackSigMessage(body: String, isMediator: Boolean) {

        Log.d(TAG, "unPackMessage: $body")
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        Log.d(TAG, "packConnectionRequestMessage: ${String(unpacked)}")

        val response = JSONObject(String(unpacked))

        val message = JSONObject(response.get("message").toString())

        val connectionSig = JSONObject(message.get("connection~sig").toString())
        val sigData = connectionSig.get("sig_data").toString()
        Log.d(
            TAG,
            "unPackMessage: decoded : ${Base64.decode(sigData, Base64.URL_SAFE)
                .toString(charset("UTF-8"))}"
        )
        val postion = Base64.decode(sigData, Base64.URL_SAFE)
            .toString(charset("UTF-8")).indexOf("{")
        Log.d(TAG, "unPackMessage: positon : $postion")
        val data =
            Base64.decode(sigData, Base64.URL_SAFE).toString(charset("UTF-8"))
                .substring(postion)

        saveDidDoc(data, isMediator)
    }

    private fun saveDidDoc(data: String, isMediator: Boolean) {
        Log.d(TAG, "saveDidDoc: $data")
        val didData = JSONObject(data)
        val didDoc = didData.getString("DIDDoc")
        val did = didData.getString("DID")

        val didDocUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$did\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_DID_DOC else DID_DOC,
            didDocUuid,
            didDoc.toString(),
            tagJson
        )

        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
            .getString("publicKeyBase58")
        addDidKey(publicKey, did, isMediator)
    }

    private fun addDidKey(publicKey: String, did: String, isMediator: Boolean) {

        val didKeyUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$did\", \"key\": \"$publicKey\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_DID_KEY else DID_KEY,
            didKeyUuid,
            publicKey,
            tagJson
        )
        updateRecord(publicKey, did, isMediator)
    }

    private fun updateRecord(publicKey: String, did: String, isMediator: Boolean) {

        val search = WalletSearch.open(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val data = JSONObject(connection)
        Log.d(TAG, "getMediatorConfig: $connection")

        val connectionRecords = JSONArray(data.get("records").toString())

        val mediatorConnectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionRecords.getJSONObject(0).getString("value"),
                MediatorConnectionObject::class.java
            )
        mediatorConnectionObject.theirDid = did
        mediatorConnectionObject.state = CONNECTION_RESPONSE

        val connectionUuid =
            connectionRecords.getJSONObject(0).getString("id")

        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

        WalletRecord.updateValue(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            value
        )

        val requestId = mediatorConnectionObject.requestId
        val myDid = mediatorConnectionObject.myDid
        val invitationKey = mediatorConnectionObject.invitationKey
        updateTag(requestId, myDid, invitationKey, connectionUuid, did, publicKey, isMediator)
    }

    private fun updateTag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        theirDid: String,
        publicKey: String,
        isMediator: Boolean
    ) {

        val tagJson = "{\n" +
                "  \"their_did\": \"$theirDid\",\n" +
                "  \"request_id\": \"$requestId\",\n" +
                "  \"my_did\": \"$myDid\",\n" +
                "  \"invitation_key\": \"$recipient\"\n" +
                "}"
        WalletRecord.updateTags(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            tagJson
        )

        if (isMediator)
            createInbox(myDid, publicKey)
        else
            trustPing(recipient, myDid)
    }

    private fun trustPing(recipient: String?, myDid: String?) {
        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val publicKey = metaObject.getString("verkey")

        val data = "{\n" +
                "  \"@type\": \"https://didcomm.org/trust_ping/1.0/ping\",\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"comment\": \"ping\",\n" +
                "  \"response_requested\": true\n" +
                "}\n"

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$recipient\"]",
            publicKey,
            data.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }

        ApiManager.api.getService()
            ?.postData("https://demo-aries-agent.igrant.io/", typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                }
            })
    }

    private fun createInbox(
        myDid: String?,
        publicKey: String
    ) {

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val data = "\n" +
                "{\n" +
                "    \"@id\": \"${UUID.randomUUID().toString()}\",\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/basic-routing/1.0/create-inbox\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$publicKey\"]",
            key,
            data.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }

        ApiManager.api.getService()?.postDetails(typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                        val inputStream = response.body()!!.byteStream()
                        val str: String = IOUtils.toString(inputStream, "UTF-8")
                        Log.d(TAG, "onResponse: $str")

                        val unpacked =
                            Crypto.unpackMessage(WalletManager.getWallet, str.toByteArray())
                                .get()
                        Log.d(TAG, "packConnectionRequestMessage: ${String(unpacked)}")

                        val message = JSONObject(String(unpacked)).getString("message")

                        val inboxId = JSONObject(message).getString("InboxId")
                        val inboxKey = JSONObject(message).getString("InboxKey")
                        //inbox
                        updateRecordWithInboxDetails(inboxId, inboxKey)
                    }
                }
            })
    }

    private fun updateRecordWithInboxDetails(inboxId: String, inboxKey: String) {
        WalletSearchTask(object : SearchHandler {
            override fun taskCompleted(searchResponse: SearchResponse) {
                val mediatorConnectionObject: MediatorConnectionObject =
                    WalletManager.getGson.fromJson(
                        searchResponse.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )
                mediatorConnectionObject.inboxId = inboxId
                mediatorConnectionObject.inboxKey = inboxKey
                mediatorConnectionObject.state = CONNECTION_ACTIVE

                val connectionUuid =
                    searchResponse.records?.get(0)?.id

                val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                WalletRecord.updateValue(
                    WalletManager.getWallet,
                    MEDIATOR_CONNECTION,
                    connectionUuid,
                    value
                )

                getMediatorConfig()
            }
        }).execute(
            MEDIATOR_CONNECTION,
            "{}"
        )
    }

    private fun saveConnectionRecord(invitation: Invitation?, isMediator: Boolean) {

        val value =
            WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
        val connectionUuid = UUID.randomUUID().toString()

        val connectionTag = ConnectionTags()
        connectionTag.invitationKey = invitation?.recipientKeys!![0]
        connectionTag.state = CONNECTION_INVITATION

        val tagJson =
            WalletManager.getGson.toJson(connectionTag)

        WalletRecord.add(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            value.toString(),
            tagJson.toString()
        )

        saveConnectionInvitationRecord(connectionUuid, invitation, isMediator)
    }

    private fun saveConnectionInvitationRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean
    ) {
        val tagJson = WalletManager.getGson.toJson(ConnectionId(connectionUuid))
        val connectionInvitationUuid = UUID.randomUUID().toString()

        Log.d(TAG, "saveRecord2: wallet value : $tagJson")
        Log.d(TAG, "saveRecord2: wallet UUID : $connectionInvitationUuid")

        WalletRecord.add(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION_INVITATION else WalletRecordType.CONNECTION_INVITATION,
            connectionInvitationUuid,
            WalletManager.getGson.toJson(invitation),
            tagJson
        )

        updateConnectionRecord(connectionUuid, invitation, isMediator)
    }

    private fun updateConnectionRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean
    ) {

        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        val myDid = myDidResult.did
//        val key = Did.keyForLocalDid(WalletManager.getWallet, myDid).get()

        Log.d(TAG, "DIDIDIDIDID:\n \n \n $myDid \n \n")
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
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            value
        )

        updateRecord1Tag(
            requestId,
            myDid,
            invitation?.recipientKeys?.get(0),
            connectionUuid,
            isMediator,
            invitation?.serviceEndpoint
        )
    }

    private fun updateRecord1Tag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        isMediator: Boolean,
        serviceEndpoint: String?
    ) {
        val tagJson =
            WalletManager.getGson.toJson(UpdateInvitationKey(requestId, myDid, recipient, null))
        WalletRecord.updateTags(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            tagJson
        )
        if (isMediator)
            getMediatorConfig()
        else
            createRoute(myDid, recipient, serviceEndpoint)
    }

    private fun createRoute(
        myDid: String?,
        recipient: String?,
        serviceEndpoint: String?
    ) {
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
            MEDIATOR_CONNECTION,
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

        WalletSearchTask(object : SearchHandler {
            override fun taskCompleted(searchResponse: SearchResponse) {
                val didDoc = searchResponse.records?.get(0)?.value
                val didDocObj = WalletManager.getGson.fromJson(didDoc, DidDoc::class.java)

                val packedMessage = Crypto.packMessage(
                    WalletManager.getWallet,
                    "[\"${didDocObj.publicKey!![0].publicKeyBase58}\"]",
                    connectedKey,
                    data.toByteArray()
                ).get()

                val typedBytes: RequestBody = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(packedMessage)
                    }
                }

                ApiManager.api.getService()?.cloudConnection(typedBytes)
                    ?.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            llProgressBar.visibility = View.GONE
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                sendInvitation(
                                    serviceEndpoint,
                                    myDid,
                                    key,
                                    didDocObj.service!![0].routingKeys!![0],
                                    recipient
                                )
                            }
                        }
                    })
            }
        }).execute(
            MEDIATOR_DID_DOC,
            "{}"
        )
    }

    private fun sendInvitation(
        serviceEndpoint: String?,
        myDid: String?,
        newVKey: String,
        routingKey: String,
        recipient: String?
    ) {
        //public keys
        val publicKey = PublicKey()
        publicKey.id = "did:sov:$myDid#1"
        publicKey.type = "Ed25519VerificationKey2018"
        publicKey.controller = "did:sov:$myDid"
        publicKey.publicKeyBase58 = newVKey

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
        recipientsKey.add(newVKey)

        //service
        val routis: ArrayList<String> = ArrayList()
        routis.add(routingKey)

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
//        val transport: Transport = Transport()
//        transport.returnRoute = "all"

        //connection request
        val connectionRequest = ConnectionRequest()
        connectionRequest.type = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/connections/1.0/request"
        connectionRequest.id = UUID.randomUUID().toString()
        connectionRequest.label = "Mobile agent 0018"
        connectionRequest.connection = did
//        connectionRequest.transport = transport

        val data = WalletManager.getGson.toJson(connectionRequest)

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$recipient\"]",
            newVKey,
            data.toByteArray()
        ).get()

        Log.d(TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }

        ApiManager.api.getService()?.postData(serviceEndpoint ?: "", typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {

                }
            })
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
        connectionObject.state = if (did != null) CONNECTION_REQUEST else CONNECTION_INVITATION

        return connectionObject
    }

    override fun onDestroy() {
        super.onDestroy()
        WalletManager.closeWallet
        PoolManager.getPool?.close()
        PoolManager.removePool
    }

    override fun onTitleChange(title: String) {
        supportActionBar?.title = title
    }

    fun setActionBarTitle(title: String?) {
        try {
            supportActionBar!!.title = title
        } catch (e: Exception) {
//            e.printStackTrace();
        }
    }

    fun getActionBarTitle(): String? {
        try {
            return supportActionBar!!.title.toString()
        } catch (e: Exception) {
//            e.printStackTrace();
        }
        return ""
    }
}