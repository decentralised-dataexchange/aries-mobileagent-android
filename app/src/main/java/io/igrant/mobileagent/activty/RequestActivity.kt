package io.igrant.mobileagent.activty

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_PROPOSAL
import io.igrant.mobileagent.adapter.RequestListAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.presentationExchange.*
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.utils.*
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
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

class RequestActivity : BaseActivity(),ConnectionProgressDailogFragment.OnConnectionSuccess {

    private lateinit var rvRequests: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var llProgressBar: LinearLayout
    private lateinit var fabScan: FloatingActionButton

    private lateinit var adapter: RequestListAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()

    private var attributelist: ArrayList<ExchangeAttributes> = ArrayList()

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

    private var isInsufficientData = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)
        initViews()
        initToolbar()
        initListener()
        setUpAdapter()
        setUpConnectionMessagesList()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_requests)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViews() {
        rvRequests = findViewById(R.id.rvRequests)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        llProgressBar = findViewById(R.id.llProgressBar)
        fabScan = findViewById(R.id.fabScan)
    }

    private fun initListener() {
        fabScan.setOnClickListener {
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN_INVITATION) {
            if (data == null) return

            val uri: Uri =
                Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
            val v: String = uri.getQueryParameter("qr_p") ?: ""
            val json =
                Base64.decode(
                    v,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))
            val data = JSONObject(json)
            if (data.getString("invitation_url") != "") {
                val invitation: String =
                    Uri.parse(data.getString("invitation_url")).getQueryParameter("c_i") ?: ""
                val proofRequest = data.getJSONObject("proof_request")
                saveConnectionAndExchangeData(invitation, proofRequest)
            } else {
                Toast.makeText(this, "Unexpected error", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveConnectionAndExchangeData(
        data: String,
        proofRequest: JSONObject
    ) {
        val json =
            Base64.decode(
                data,
                Base64.URL_SAFE
            ).toString(charset("UTF-8"))

        val gson = Gson()
        val invitation: Invitation = gson.fromJson(json, Invitation::class.java)
        val connection =
            ConnectionUtils.getConnectionWithInvitationKey(invitation.recipientKeys!![0])
        if (connection != null) {
            sendProposal(proofRequest, connection.requestId ?: "")
        } else {
            saveConnection(invitation,proofRequest.toString())
        }
    }

    private fun sendProposal(
        proofRequest: JSONObject,
        connectioId: String
    ) {

        val intent = Intent(this,ProposeAndExchangeDataActivity::class.java)
        intent.putExtra(EXTRA_PRESENTATION_PROPOSAL,proofRequest.toString())
        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_CONNECTION_ID,connectioId)
        startActivity(intent)

//        val presentationRequest =
//            WalletManager.getGson.fromJson(proofRequest.toString(), PresentationRequest::class.java)
//
//        Log.d(
//            TAG,
//            "sendProposal: \n ${proofRequest.toString()} \n ${WalletManager.getGson.toJson(
//                presentationRequest
//            )}"
//        )
//        val searchHandle = CredentialsSearchForProofReq.open(
//            WalletManager.getWallet,
//            proofRequest.toString(),
//            "{}"
//        ).get()
//
//        requestedAttributes = HashMap()
//        attributelist.clear()
//        var credentialValue: CredentialValue
//
//        presentationRequest?.requestedAttributes?.forEach { (key, value) ->
//
//            val searchResult = searchHandle.fetchNextCredentials(key, 100).get()
//
//            if (JSONArray(searchResult).length() > 0) {
//                val referent =
//                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
//                        .getString("referent")
//
//                credentialValue = CredentialValue()
//                credentialValue.credId = referent
//                credentialValue.revealed = true
//
//                requestedAttributes[key] = credentialValue
//
//                val data =
//                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
//                        .getJSONObject("attrs").getString(value.name ?: "")
//
//                val attributes = ExchangeAttributes()
//                attributes.name = value.name
//                attributes.value = data
//                attributes.credDefId =
//                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
//                        .getString("cred_def_id")
//                attributes.referent =
//                    JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
//                        .getString("referent")
//
//                attributelist.add(attributes)
//            } else {
//                isInsufficientData = true
//            }
//        }
//
//        val presentationProposal = PresentationProposal()
//        presentationProposal.type =
//            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/presentation-preview"
//        presentationProposal.attributes = attributelist
//        presentationProposal.predicates = ArrayList()
//
//        var presentationProposalData = PresentationProposalData()
//        presentationProposalData.type =
//            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/propose-presentation"
//        presentationProposalData.id = UUID.randomUUID().toString()
//        presentationProposalData.comment = "Proposing credentials"
//        presentationProposalData.presentationProposal = presentationProposal
//
//        var recipientKey = ""
//        var serviceEndPoint = ""
//        val didDocSearch = SearchUtils.searchWallet(DID_DOC, "{\"did\":\"$theirDid\"}")
//        if (didDocSearch.totalCount ?: 0 > 0) {
//            val didDoc = WalletManager.getGson.fromJson(
//                didDocSearch.records?.get(0)?.value,
//                DidDoc::class.java
//            )
//            recipientKey = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
//            serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
//        }
//
//        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
//        val metaObject = JSONObject(metaString)
//        val senderKey = metaObject.getString("verkey")
//        val packed = Crypto.packMessage(
//            WalletManager.getWallet,
//            "[\"$recipientKey\"]",
//            senderKey,
//            WalletManager.getGson.toJson(presentationProposalData).toByteArray()
//        ).get()
//        val typedBytes = object : RequestBody() {
//            override fun contentType(): MediaType? {
//                return "application/ssi-agent-wire".toMediaTypeOrNull()
//            }
//
//            @Throws(IOException::class)
//            override fun writeTo(sink: BufferedSink) {
//                sink.write(packed)
//            }
//        }
//
//        ApiManager.api.getService()?.postDataWithoutData(serviceEndPoint, typedBytes)
//            ?.enqueue(object : Callback<ResponseBody> {
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//
//                }
//
//                override fun onResponse(
//                    call: Call<ResponseBody>,
//                    response: Response<ResponseBody>
//                ) {
//
//                }
//            })

    }

    private fun saveConnection(
        invitation: Invitation,
        proposal: String
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
                        "You are not connected to ${invitation.label}. Please connect before sharing data.",
                        invitation,
                        proposal
                    )
                connectionSuccessDialogFragment.show(supportFragmentManager, "fragment_edit_name")
            }, 200)
        }
    }

    private fun setUpAdapter() {
        adapter = RequestListAdapter(connectionMessageList, object : ConnectionMessageListener {
            override fun onConnectionMessageClick(record: Record) {
                val intent: Intent = Intent(this@RequestActivity, ExchangeDataActivity::class.java)
                intent.putExtra(
                    ExchangeDataActivity.EXTRA_PRESENTATION_RECORD,
                    record
                )

                startActivity(intent)

                finish()
            }

        })
        rvRequests.adapter = adapter
    }

    private fun setUpConnectionMessagesList() {
        val connectionMessageResponse =
            SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{" +
                        "\"type\":\"${MessageTypes.TYPE_REQUEST_PRESENTATION}\"" +
                        "}"
            )
        if (connectionMessageResponse.totalCount ?: 0 > 0) {
            llErrorMessage.visibility = View.GONE
            connectionMessageList.clear()
            connectionMessageList.addAll(connectionMessageResponse.records ?: ArrayList())
            adapter.notifyDataSetChanged()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val TAG = "InitializeActivity"
        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202

        var deviceId = ""
    }

    override fun onSuccess(proposal: String, connectionId: String) {
        Log.d(TAG, "onSuccess: ")
        val intent = Intent(this,ProposeAndExchangeDataActivity::class.java)
        intent.putExtra(EXTRA_PRESENTATION_PROPOSAL,proposal)
        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_CONNECTION_ID,connectionId)
        startActivity(intent)
    }
}