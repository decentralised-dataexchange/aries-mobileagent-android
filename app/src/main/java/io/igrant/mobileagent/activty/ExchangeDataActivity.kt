package io.igrant.mobileagent.activty

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
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
import io.igrant.mobileagent.events.ReceiveCertificateEvent
import io.igrant.mobileagent.events.ReceiveExchangeRequestEvent
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.Notification
import io.igrant.mobileagent.models.presentationExchange.CredentialValue
import io.igrant.mobileagent.models.presentationExchange.ExchangeAttributes
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.tasks.ExchangeDataTask
import io.igrant.mobileagent.utils.MessageTypes
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ExchangeDataActivity : BaseActivity() {

    private var connection: MediatorConnectionObject? = null
    private lateinit var mConnectionId: String
    private var record: Record? = null

    private var mPresentationExchange: PresentationExchange? = null

    private lateinit var toolbar: Toolbar
    private lateinit var tvDesc: TextView
    private lateinit var tvHead: TextView

    //    private lateinit var btReject: Button
    private lateinit var btAccept: Button
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    private lateinit var adapter: ExchangeRequestAttributeAdapter

    private var attributelist: ArrayList<ExchangeAttributes> = ArrayList()

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

    private var isInsufficientData = false

    companion object {
        private const val TAG = "ExchangeDataActivity"
        const val EXTRA_PRESENTATION_RECORD =
            "io.igrant.mobileagent.activty.ExchangeDataActivity.record"
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    private fun initValues() {
        if (connection != null) {
            tvDesc.text =
                resources.getString(
                    R.string.txt_exchange_data_desc,
                    connection?.theirLabel ?: resources.getString(R.string.txt_organisations)
                )
        }

        tvHead.text = (mPresentationExchange?.presentationRequest?.name ?: "").toUpperCase()

        val searchHandle = CredentialsSearchForProofReq.open(
            WalletManager.getWallet,
            WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
            "{}"
        ).get()

        requestedAttributes = HashMap()
        attributelist.clear()
        var credentialValue: CredentialValue
        mPresentationExchange?.presentationRequest?.requestedAttributes?.forEach { (key, value) ->

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
        record = intent.extras!!.get(EXTRA_PRESENTATION_RECORD) as Record
        val notification = WalletManager.getGson.fromJson(record!!.value, Notification::class.java)
        mPresentationExchange = notification.presentation
        connection = notification.connection
        mConnectionId = mPresentationExchange?.connectionId ?: ""
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_exchange_data)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_delete -> {
                AlertDialog.Builder(this@ExchangeDataActivity)
                    .setTitle(resources.getString(R.string.txt_confirmation))
                    .setMessage(
                        resources.getString(
                            R.string.txt_exchange_request_delete_confirmation
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { dialog, which ->
                            WalletRecord.delete(
                                WalletManager.getWallet,
                                WalletRecordType.MESSAGE_RECORDS,
                                mPresentationExchange?.threadId ?: ""
                            ).get()

                            val credentialExchangeResponse =
                                SearchUtils.searchWallet(
                                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                    "{\"thread_id\": \"${mPresentationExchange?.threadId}\"}"
                                )

                            if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                                WalletRecord.delete(
                                    WalletManager.getWallet,
                                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                    "${credentialExchangeResponse.records?.get(0)?.id}"
                                ).get()
                            }

                            EventBus.getDefault()
                                .post(ReceiveExchangeRequestEvent(mConnectionId))

                            onBackPressed()
                        }) // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(
                        android.R.string.cancel,
                        DialogInterface.OnClickListener { dialog, which ->

                        })
                    .show()


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
//        btReject.setOnClickListener {
//
//        }

        btAccept.setOnClickListener {
            if (!isInsufficientData) {
                llProgressBar.visibility = View.VISIBLE
                btAccept.isEnabled = false
//                btReject.isEnabled = false

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
//                                    btReject.isEnabled = true
                                }

                                override fun onResponse(
                                    call: Call<ResponseBody>,
                                    response: Response<ResponseBody>
                                ) {
                                    if (response.code() == 200 && response.body() != null) {
                                        llProgressBar.visibility = View.GONE
                                        btAccept.isEnabled = true
//                                        btReject.isEnabled = true

                                        val tagJson = "{\n" +
                                                "  \"type\":\"${MessageTypes.TYPE_REQUEST_PRESENTATION}\",\n" +
                                                "  \"connectionId\":\"${mConnectionId}\",\n" +
                                                "  \"stat\":\"Processed\"\n" +
                                                "}"
                                        WalletRecord.updateTags(
                                            WalletManager.getWallet,
                                            WalletRecordType.MESSAGE_RECORDS,
                                            record?.id ?: "",
                                            tagJson
                                        )

                                        EventBus.getDefault()
                                            .post(ReceiveExchangeRequestEvent(mConnectionId))

                                        AlertDialog.Builder(this@ExchangeDataActivity)
                                            .setMessage(
                                                resources.getString(
                                                    R.string.txt_exchange_successful,
                                                    connection?.theirLabel ?: ""
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
                }, mPresentationExchange, requestedAttributes).execute(record?.id, mConnectionId)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.err_insufficient_data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}