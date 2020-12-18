package io.igrant.mobileagent.activty

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_INVITATION
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_PROPOSAL
import io.igrant.mobileagent.adapter.RequestListAdapter
import io.igrant.mobileagent.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.presentationExchange.CredentialValue
import io.igrant.mobileagent.models.presentationExchange.ExchangeAttributes
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.utils.*
import org.json.JSONObject

class RequestActivity : BaseActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var llProgressBar: LinearLayout
    private lateinit var fabScan: FloatingActionButton

    private lateinit var adapter: RequestListAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()

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
        sendProposal(proofRequest, invitation)
    }

    private fun sendProposal(
        proofRequest: JSONObject,
        invitation: Invitation
    ) {

        val intent = Intent(this, ProposeAndExchangeDataActivity::class.java)
        intent.putExtra(EXTRA_PRESENTATION_PROPOSAL, proofRequest.toString())
        intent.putExtra(EXTRA_PRESENTATION_INVITATION, invitation)
        startActivity(intent)
    }

    private fun setUpAdapter() {
        adapter = RequestListAdapter(connectionMessageList, object : ConnectionMessageListener {
            override fun onConnectionMessageClick(record: Record,name:String) {
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
}