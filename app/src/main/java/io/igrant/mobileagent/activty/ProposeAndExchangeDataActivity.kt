package io.igrant.mobileagent.activty

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.ExchangeRequestAttributeAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.mobileagent.events.GoHomeEvent
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.presentationExchange.CredentialValue
import io.igrant.mobileagent.models.presentationExchange.ExchangeAttributes
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.presentationExchange.PresentationRequest
import io.igrant.mobileagent.tasks.ExchangeDataTask
import io.igrant.mobileagent.utils.ConnectionUtils
import io.igrant.mobileagent.utils.PresentationExchangeStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProposeAndExchangeDataActivity : BaseActivity(),
    ConnectionProgressDailogFragment.OnConnectionSuccess {

    private var mConnectionId: String = ""
    private lateinit var proposalData: PresentationRequest
    private lateinit var invitation: Invitation
    private lateinit var proposal: String
    private lateinit var toolbar: Toolbar
    private lateinit var tvDesc: TextView
    private lateinit var tvHead: TextView
//    private lateinit var btReject: Button
    private lateinit var btAccept: Button
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    private var mPresentationExchange: PresentationExchange? = null

    private lateinit var adapter: ExchangeRequestAttributeAdapter

    private var attributelist: ArrayList<ExchangeAttributes> = ArrayList()

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

    private var isInsufficientData = false

    companion object {
        private const val TAG = "ExchangeDataActivity"
        const val EXTRA_PRESENTATION_PROPOSAL =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.proposal"
        const val EXTRA_PRESENTATION_INVITATION =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.invitation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_data)
        initViews()
        initListener()
        getIntentData()
        setUpToolbar()
        initValues()
    }

    private fun initValues() {

        tvDesc.text =
            resources.getString(R.string.txt_exchange_data_desc, invitation.label ?: "")

        tvHead.text = (proposalData.name ?: "").toUpperCase()
        val searchHandle = CredentialsSearchForProofReq.open(
            WalletManager.getWallet,
            WalletManager.getGson.toJson(proposalData),
            "{}"
        ).get()

        requestedAttributes = HashMap()
        attributelist.clear()
        var credentialValue: CredentialValue
        proposalData.requestedAttributes?.forEach { (key, value) ->

            val searchResult = searchHandle.fetchNextCredentials(key, 100).get()

            if (JSONArray(searchResult).length() > 0) {
                val referent =
                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                        .getString("referent")

                credentialValue = CredentialValue()
                credentialValue.credId = referent
                credentialValue.revealed = true

                requestedAttributes[key] = credentialValue

                val data =
                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                        .getJSONObject("attrs").getString(value.name ?: "")

                val attributes = ExchangeAttributes()
                attributes.name = value.name
                attributes.value = data
                attributes.credDefId =
                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                        .getString("cred_def_id")
                attributes.referent =
                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                        .getString("referent")

                attributelist.add(attributes)
            } else {
                isInsufficientData = true
            }
        }

        searchHandle.closeSearch()

        adapter = ExchangeRequestAttributeAdapter(
            attributelist
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun getIntentData() {
        proposal = intent.extras!!.getString(EXTRA_PRESENTATION_PROPOSAL).toString()
        invitation = intent.extras!!.getSerializable(EXTRA_PRESENTATION_INVITATION) as Invitation
        proposalData = WalletManager.getGson.fromJson(proposal, PresentationRequest::class.java)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_data_agreement)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvDesc = findViewById(R.id.tvDesc)
        tvHead = findViewById(R.id.tvHead)
        btAccept = findViewById(R.id.btAccept)
//        btReject = findViewById(R.id.btReject)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun initListener() {
        btAccept.setOnClickListener {
            if (!isInsufficientData) {
                val connection =
                    ConnectionUtils.getConnectionWithInvitationKey(invitation.recipientKeys!![0])
                mConnectionId = connection?.requestId ?: ""
                if (connection != null) {
                    llProgressBar.visibility = View.VISIBLE
                    btAccept.isEnabled = false
//                    btReject.isEnabled = false
                    if (mPresentationExchange != null) {
                        exchangeData(UUID.randomUUID().toString())
                    } else {
                        sendProposal(mConnectionId)
                    }
                } else {
                    saveConnection(invitation)
                }
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.err_insufficient_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendProposal(mConnectionId: String) {
        val threadId = UUID.randomUUID().toString()
        var data = "{\n" +
                "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/propose-presentation\",\n" +
                "  \"@id\": \"$threadId\",\n" +
                "  \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    },\n" +
                "  \"presentation_proposal\": {\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/presentation-preview\",\n" +
                "    \"attributes\": [\n"
        for (attribute in attributelist) {
            data = data + "{\n" +
                    "        \"name\": \"${attribute.name}\",\n" +
                    "        \"cred_def_id\": \"${attribute.credDefId}\",\n" +
                    "        \"value\": \"${attribute.value}\",\n" +
                    "        \"referent\": \"${attribute.referent}\"\n" +
                    "      },\n"
        }
        data = data.substring(0, data.length - 2)
        data = data +
                "    ],\n" +
                "    \"predicates\": []\n" +
                "  },\n" +
                "  \"comment\": \"Proposing credentials\"\n" +
                "}"


        val connectionList = SearchUtils.searchWallet(WalletRecordType.CONNECTION,"{\"request_id\":\"$mConnectionId\"}")

        if (connectionList.totalCount?:0>0){
            val connectionObject = WalletManager.getGson.fromJson(connectionList.records?.get(0)?.value,MediatorConnectionObject::class.java)

            val metaString = Did.getDidWithMeta(WalletManager.getWallet, connectionObject?.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            val didDocSearch = SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObject?.theirDid}\"}"
            )

            var serviceEndPoint = ""
            var recipient = ""
            if (didDocSearch.totalCount ?: 0 > 0) {
                val didDoc = WalletManager.getGson.fromJson(
                    didDocSearch.records?.get(0)?.value,
                    DidDoc::class.java
                )

                serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
                recipient = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
            }

            Log.d(TAG, "sendProposal: $recipient \n $publicKey \n $data")
            val packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"$recipient\"]",
                publicKey,
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

            ApiManager.api.getService()?.postData(serviceEndPoint, typedBytes)
                ?.enqueue(object : Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        llProgressBar.visibility = View.GONE
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        Log.d(TAG, "onResponse: " + response.body())

                        val unpack =
                            Crypto.unpackMessage(
                                WalletManager.getWallet,
                                WalletManager.getGson.toJson(response.body()).toString().toByteArray()
                            ).get()

                        val message = JSONObject(String(unpack)).getString("message")

                        val presentationRequestBase64 =
                            JSONObject(
                                JSONObject(message).getJSONArray("request_presentations~attach")
                                    .get(0).toString()
                            ).getJSONObject("data").getString("base64")
                        val presentationRequest = WalletManager.getGson.fromJson(
                            Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                                .toString(charset("UTF-8")), PresentationRequest::class.java
                        )

                        val presentationExchange = PresentationExchange()
                        presentationExchange.threadId =
                            JSONObject(message).getJSONObject("~thread").getString("thid")
                        presentationExchange.createdAt = "2020-11-25 12:17:53.491756Z"
                        presentationExchange.updatedAt = "2020-11-25 12:17:53.491756Z"
                        presentationExchange.connectionId = connectionObject?.requestId
                        presentationExchange.initiator = "external"
                        presentationExchange.presentationProposalDict = null
                        presentationExchange.presentationRequest = presentationRequest
                        presentationExchange.role = "prover"
                        presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
                        presentationExchange.comment =
                            JSONObject(message).getString("comment")

                        mPresentationExchange = presentationExchange

                        val searchHandle2 = CredentialsSearchForProofReq.open(
                            WalletManager.getWallet,
                            WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
                            "{}"
                        ).get()

                        requestedAttributes = HashMap()
                        attributelist.clear()
                        var credentialValue: CredentialValue
                        mPresentationExchange?.presentationRequest?.requestedAttributes?.forEach { (key, value) ->
                            val searchResult = searchHandle2.fetchNextCredentials(key, 100).get()

                            if (JSONArray(searchResult).length() > 0) {
                                val referent =
                                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                                        .getString("referent")

                                credentialValue = CredentialValue()
                                credentialValue.credId = referent
                                credentialValue.revealed = true

                                requestedAttributes[key] = credentialValue

                            } else {
                                isInsufficientData = true
                            }
                        }
                        exchangeData(threadId)
                    }
                })
        }

    }

    private fun sendProposalWithOrgId(mConnectionId: String) {
        val threadId = UUID.randomUUID().toString()
        var data = "{\n" +
                "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/propose-presentation\",\n" +
                "  \"@id\": \"$threadId\",\n" +
                "  \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    },\n" +
                "  \"presentation_proposal\": {\n" +
                "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/presentation-preview\",\n" +
                "    \"attributes\": [\n"
        for (attribute in attributelist) {
            data = data + "{\n" +
                    "        \"name\": \"${attribute.name}\",\n" +
                    "        \"cred_def_id\": \"${attribute.credDefId}\",\n" +
                    "        \"value\": \"${attribute.value}\",\n" +
                    "        \"referent\": \"${attribute.referent}\"\n" +
                    "      },\n"
        }
        data = data.substring(0, data.length - 2)
        data = data +
                "    ],\n" +
                "    \"predicates\": []\n" +
                "  },\n" +
                "  \"comment\": \"Proposing credentials\"\n" +
                "}"


        val connectionList = SearchUtils.searchWallet(WalletRecordType.CONNECTION,"{\"orgId\":\"$mConnectionId\"}")

        if (connectionList.totalCount?:0>0){
            val connectionObject = WalletManager.getGson.fromJson(connectionList.records?.get(0)?.value,MediatorConnectionObject::class.java)

            val metaString = Did.getDidWithMeta(WalletManager.getWallet, connectionObject?.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            val didDocSearch = SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObject?.theirDid}\"}"
            )

            var serviceEndPoint = ""
            var recipient = ""
            if (didDocSearch.totalCount ?: 0 > 0) {
                val didDoc = WalletManager.getGson.fromJson(
                    didDocSearch.records?.get(0)?.value,
                    DidDoc::class.java
                )

                serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
                recipient = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
            }

            Log.d(TAG, "sendProposal: $recipient \n $publicKey \n $data")
            val packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"$recipient\"]",
                publicKey,
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

            ApiManager.api.getService()?.postData(serviceEndPoint, typedBytes)
                ?.enqueue(object : Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        llProgressBar.visibility = View.GONE
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        Log.d(TAG, "onResponse: " + response.body())

                        val unpack =
                            Crypto.unpackMessage(
                                WalletManager.getWallet,
                                WalletManager.getGson.toJson(response.body()).toString().toByteArray()
                            ).get()

                        val message = JSONObject(String(unpack)).getString("message")

                        val presentationRequestBase64 =
                            JSONObject(
                                JSONObject(message).getJSONArray("request_presentations~attach")
                                    .get(0).toString()
                            ).getJSONObject("data").getString("base64")
                        val presentationRequest = WalletManager.getGson.fromJson(
                            Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                                .toString(charset("UTF-8")), PresentationRequest::class.java
                        )

                        val presentationExchange = PresentationExchange()
                        presentationExchange.threadId =
                            JSONObject(message).getJSONObject("~thread").getString("thid")
                        presentationExchange.createdAt = "2020-11-25 12:17:53.491756Z"
                        presentationExchange.updatedAt = "2020-11-25 12:17:53.491756Z"
                        presentationExchange.connectionId = connectionObject?.requestId
                        presentationExchange.initiator = "external"
                        presentationExchange.presentationProposalDict = null
                        presentationExchange.presentationRequest = presentationRequest
                        presentationExchange.role = "prover"
                        presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
                        presentationExchange.comment =
                            JSONObject(message).getString("comment")

                        mPresentationExchange = presentationExchange

                        val searchHandle2 = CredentialsSearchForProofReq.open(
                            WalletManager.getWallet,
                            WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
                            "{}"
                        ).get()

                        requestedAttributes = HashMap()
                        attributelist.clear()
                        var credentialValue: CredentialValue
                        mPresentationExchange?.presentationRequest?.requestedAttributes?.forEach { (key, value) ->
                            val searchResult = searchHandle2.fetchNextCredentials(key, 100).get()

                            if (JSONArray(searchResult).length() > 0) {
                                val referent =
                                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                                        .getString("referent")

                                credentialValue = CredentialValue()
                                credentialValue.credId = referent
                                credentialValue.revealed = true

                                requestedAttributes[key] = credentialValue

                            } else {
                                isInsufficientData = true
                            }
                        }
                        exchangeData(threadId)
                    }
                })
        }

    }

    private fun exchangeData(threadId:String) {
        ExchangeDataTask(object : CommonHandler {
            override fun taskStarted() {

            }

            override fun onExchangeDataComplete(
                serviceEndPoint: String,
                typedBytes: RequestBody
            ) {
                ApiManager.api.getService()
                    ?.postDataWithoutData(
                        serviceEndPoint,
                        typedBytes
                    )
                    ?.enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(
                            call: Call<ResponseBody>,
                            t: Throwable
                        ) {
                            llProgressBar.visibility = View.GONE
                            btAccept.isEnabled = true
//                            btReject.isEnabled = true
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                llProgressBar.visibility = View.GONE
                                btAccept.isEnabled = true
//                                btReject.isEnabled = true

                                EventBus.getDefault().post(GoHomeEvent())

                                AlertDialog.Builder(this@ProposeAndExchangeDataActivity)
                                    .setMessage(
                                        resources.getString(
                                            R.string.txt_exchange_successful_without_name
                                        )
                                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    .setPositiveButton(
                                        android.R.string.ok,
                                        DialogInterface.OnClickListener { dialog, which ->
                                            onBackPressed()
                                        }) // A null listener allows the button to dismiss the dialog and take no further action.
                                    .show()
                            }
                        }
                    })
            }
        }, mPresentationExchange, requestedAttributes).execute(
            "",
            mConnectionId
        )
    }

    private fun saveConnection(
        invitation: Invitation
    ) {
        if (ConnectionUtils.checkIfConnectionAvailable(invitation.recipientKeys!![0])) {
            Toast.makeText(
                this,
                resources.getString(R.string.err_connection_already_added),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
                    ConnectionProgressDailogFragment.newInstance(
                        true,
                        invitation,
                        ""
                    )
                connectionSuccessDialogFragment.show(supportFragmentManager, "fragment_edit_name")
            }, 200)
        }
    }

    override fun onSuccess(proposal: String, orgId: String) {
        llProgressBar.visibility = View.VISIBLE
        btAccept.isEnabled = false
//        btReject.isEnabled = false
        if (mPresentationExchange != null) {
            exchangeData(UUID.randomUUID().toString())
        } else {
            sendProposalWithOrgId(orgId)
        }
    }

    override fun onExistingConnection(connectionId: String) {
        val connectionList = SearchUtils.searchWallet(WalletRecordType.CONNECTION,"{\"request_id\":\"$connectionId\"}")

        if (connectionList.totalCount?:0>0){
            llProgressBar.visibility = View.VISIBLE
            btAccept.isEnabled = false
//                    btReject.isEnabled = false
            if (mPresentationExchange != null) {
                exchangeData(UUID.randomUUID().toString())
            } else {
                sendProposal(connectionId)
            }
        }
    }
}