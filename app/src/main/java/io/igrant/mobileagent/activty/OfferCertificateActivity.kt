package io.igrant.mobileagent.activty

import android.content.DialogInterface
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
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
import io.igrant.mobileagent.adapter.CertificateAttributeAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ReceiveExchangeRequestEvent
import io.igrant.mobileagent.events.ReceiveOfferEvent
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.handlers.PoolHandler
import io.igrant.mobileagent.indy.LedgerNetworkType
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.Notification
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.certificateOffer.OfferAttach
import io.igrant.mobileagent.models.certificateOffer.OfferData
import io.igrant.mobileagent.models.certificateOffer.RequestOffer
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.credentialExchange.CredentialExchange
import io.igrant.mobileagent.models.credentialExchange.CredentialRequest
import io.igrant.mobileagent.models.credentialExchange.CredentialRequestMetadata
import io.igrant.mobileagent.models.credentialExchange.Thread
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.tasks.LoadLibIndyTask
import io.igrant.mobileagent.tasks.OpenWalletTask
import io.igrant.mobileagent.tasks.PoolTask
import io.igrant.mobileagent.utils.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class OfferCertificateActivity : BaseActivity() {

    private var goToHome: Boolean = false
    private var name: String = ""
    private lateinit var mConnectionId: String
    private var mCertificateOffer: CertificateOffer? = null
    private var record: Record? = null

    private lateinit var toolbar: Toolbar

    private lateinit var btAccept: Button
    private lateinit var tvHead: TextView
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    private lateinit var adapter: CertificateAttributeAdapter

    companion object {
        const val EXTRA_CERTIFICATE_PREVIEW =
            "io.igrant.mobileagent.activty.OfferCertificateActivity.certificate"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_certificate)
        initViews()
        checkPool()
        initListener()
        getIntentData()
        initValues()
        setUpToolbar()
        setUpAdapter()
    }

    private fun checkPool() {
        if (PoolManager.getPool == null) {
            goToHome = true
            llProgressBar.visibility = View.VISIBLE
            initLibIndy()
        }
    }

    private fun initLibIndy() {
        LoadLibIndyTask(object : CommonHandler {
            override fun taskCompleted() {
                loadPool()
            }

            override fun taskStarted() {

            }
        }, applicationContext).execute()
    }

    private fun openWallet() {
        OpenWalletTask(object : CommonHandler {
            override fun taskCompleted() {
                llProgressBar.visibility = View.GONE
                checkExistanceOfRecord()
            }

            override fun taskStarted() {

            }
        }).execute()
    }

    private fun checkExistanceOfRecord() {

        try {
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"certificateId\":\"${record?.id}\"}"
            )
            if (searchResponse.totalCount ?: 0 == 0) {
                onBackPressed()
            }
        } catch (e: Exception) {
        }

    }

    private fun loadPool() {
        PoolTask(object : PoolHandler {
            override fun taskCompleted(pool: Pool) {
                PoolManager.setPool(pool)
                openWallet()
            }

            override fun taskStarted() {

            }
        }, LedgerNetworkType.getSelectedNetwork(this)).execute()
    }

    private fun initValues() {
        tvHead.text = name.toUpperCase()
    }

    private fun initListener() {

        btAccept.setOnClickListener {
            llProgressBar.visibility = View.VISIBLE
            btAccept.isEnabled = false
            RequestCertificateTask(object : RequestCertificateHandler {
                override fun taskCompleted(requestBody: RequestBody?, endPoint: String?) {

                    if (requestBody == null) {
                        Toast.makeText(
                            this@OfferCertificateActivity,
                            resources.getString(R.string.err_ledger_missmatch),
                            Toast.LENGTH_SHORT
                        ).show()
                        llProgressBar.visibility = View.GONE
                        btAccept.isEnabled = true
                    } else {
                        ApiManager.api.getService()
                            ?.postDataWithoutData(endPoint ?: "", requestBody)
                            ?.enqueue(object : Callback<ResponseBody> {
                                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                    llProgressBar.visibility = View.GONE
                                    btAccept.isEnabled = true
                                }

                                override fun onResponse(
                                    call: Call<ResponseBody>,
                                    response: Response<ResponseBody>
                                ) {
                                    llProgressBar.visibility = View.GONE
                                    btAccept.isEnabled = false

                                    val tagJson = "{\n" +
                                            "  \"type\":\"${MessageTypes.TYPE_OFFER_CREDENTIAL}\",\n" +
                                            "  \"connectionId\":\"${mConnectionId}\",\n" +
                                            "  \"stat\":\"Processed\"\n" +
                                            "}"

                                    WalletRecord.updateTags(
                                        WalletManager.getWallet,
                                        WalletRecordType.MESSAGE_RECORDS,
                                        record?.id ?: "",
                                        tagJson
                                    )
                                    EventBus.getDefault().post(ReceiveOfferEvent(mConnectionId))
                                    EventBus.getDefault()
                                        .post(ReceiveExchangeRequestEvent())

                                    Toast.makeText(
                                        this@OfferCertificateActivity, resources.getString(
                                            R.string.txt_data_request_accepted_successfully
                                        ), Toast.LENGTH_SHORT
                                    ).show()

//                                AlertDialog.Builder(this@OfferCertificateActivity)
//                                    .setMessage(
//                                        resources.getString(
//                                            R.string.txt_data_request_accepted_successfully
//                                        )
//                                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
//                                    // The dialog is automatically dismissed when a dialog button is clicked.
//                                    .setPositiveButton(
//                                        android.R.string.ok,
//                                        DialogInterface.OnClickListener { dialog, which ->
                                    onBackPressed()
//                                        }) // A null listener allows the button to dismiss the dialog and take no further action.
//                                    .show()

                                }
                            })
                    }
                }

                override fun taskStarted() {

                }
            }, mCertificateOffer!!, mConnectionId).execute()
        }
    }

    override fun onBackPressed() {
        if (goToHome) {
            val intent = Intent(this@OfferCertificateActivity, InitializeActivity::class.java)
            startActivity(intent)
        }
        super.onBackPressed()
    }

    private fun setUpAdapter() {
        adapter = CertificateAttributeAdapter(
            mCertificateOffer!!.credentialPreview!!.attributes ?: ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btAccept = findViewById(R.id.btAccept)
        tvHead = findViewById(R.id.tvHead)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.title_offer_detail)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_delete -> {

                AlertDialog.Builder(this@OfferCertificateActivity)
                    .setTitle(resources.getString(R.string.txt_confirmation))
                    .setMessage(
                        resources.getString(
                            R.string.txt_offer_credential_delete_confirmation
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        android.R.string.ok,
                        DialogInterface.OnClickListener { dialog, which ->
                            try {
                                WalletRecord.delete(
                                    WalletManager.getWallet,
                                    WalletRecordType.MESSAGE_RECORDS,
                                    mCertificateOffer?.id ?: ""
                                ).get()

                                val credentialExchangeResponse =
                                    SearchUtils.searchWallet(
                                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                        "{\"thread_id\": \"${mCertificateOffer?.id}\"}"
                                    )

                                if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                                    WalletRecord.delete(
                                        WalletManager.getWallet,
                                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                        "${credentialExchangeResponse.records?.get(0)?.id}"
                                    ).get()
                                }

                                EventBus.getDefault()
                                    .post(ReceiveExchangeRequestEvent())

                                onBackPressed()
                            } catch (e: Exception) {
                            }
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

    private fun getIntentData() {
        record = intent.extras!!.get(EXTRA_CERTIFICATE_PREVIEW) as Record

        val notification = WalletManager.getGson.fromJson(record!!.value, Notification::class.java)
        name = notification.presentation?.presentationRequest?.name ?: ""
        mCertificateOffer = notification.certificateOffer
        mConnectionId = notification.connection?.requestId ?: ""
        checkExistanceOfRecord()
    }

    class RequestCertificateTask(
        private val commonHandler: RequestCertificateHandler,
        private val mCertificateOffer: CertificateOffer,
        private val mConnectionId: String
    ) :
        AsyncTask<Void, Void, Void>() {

        private var serviceEndPoint: String? = null
        private var typedBytes: RequestBody? = null

        override fun doInBackground(vararg p0: Void?): Void? {
            val credentialExchangeResponse =
                SearchUtils.searchWallet(
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "{\"thread_id\": \"${mCertificateOffer.id}\"}"
                )

            var credentialExchangeData = CredentialExchange()
            if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                credentialExchangeData = WalletManager.getGson.fromJson(
                    credentialExchangeResponse.records?.get(0)?.value,
                    CredentialExchange::class.java
                )
            }

            Pool.setProtocolVersion(2)

            val credDef =
                Ledger.buildGetCredDefRequest(
                    null,
                    credentialExchangeData.credentialOffer?.credDefId ?: ""
                ).get()

            val credDefResponse = Ledger.submitRequest(PoolManager.getPool, credDef).get()

            try {
                val parsedCredDefResponse = Ledger.parseGetCredDefResponse(credDefResponse).get()

                val resultObject =
                    SearchUtils.searchWallet(
                        WalletRecordType.CONNECTION,
                        "{\n" +
                                "  \"request_id\":\"$mConnectionId\"\n" +
                                "}"
                    )
                val connectionObject = WalletManager.getGson.fromJson(
                    resultObject.records?.get(0)?.value,
                    MediatorConnectionObject::class.java
                )
                val proverDid = connectionObject.myDid

                //get cred offer json:
                //offer credential base64 parameter value decoded value
                val credOfferJson = Base64.decode(
                    mCertificateOffer.offersAttach?.get(0)?.data?.base64,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))

                val proverResponse = Anoncreds.proverCreateCredentialReq(
                    WalletManager.getWallet,
                    proverDid,
                    credOfferJson,
                    parsedCredDefResponse.objectJson,
                    "IGrantMobileAgent-000001"
                ).get()

                val credentialRequest =
                    WalletManager.getGson.fromJson(
                        proverResponse.credentialRequestJson,
                        CredentialRequest::class.java
                    )
                val credentialRequestMetaData =
                    WalletManager.getGson.fromJson(
                        proverResponse.credentialRequestMetadataJson,
                        CredentialRequestMetadata::class.java
                    )
                credentialExchangeData.state = CredentialExchangeStates.CREDENTIAL_REQUEST_SENT
                credentialExchangeData.credentialRequest = credentialRequest
                credentialExchangeData.credentialRequestMetadata = credentialRequestMetaData

                WalletRecord.updateValue(
                    WalletManager.getWallet,
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "${credentialExchangeResponse.records?.get(0)?.id}",
                    WalletManager.getGson.toJson(credentialExchangeData)
                )

                //creating model for sending
                val thread = Thread()
                thread.thid = mCertificateOffer.id ?: ""

                val v = Base64.encodeToString(
                    proverResponse.credentialRequestJson.toByteArray(),
                    Base64.NO_WRAP
                )
                v.replace("\\n", "")
                val offerData = OfferData()
                offerData.base64 = v

                val requestAttach = OfferAttach()
                requestAttach.id = "libindy-cred-request-0"
                requestAttach.mimeType = "application/json"
                requestAttach.data = offerData
                thread.thid = mCertificateOffer.id ?: ""

                val requestAttachList = ArrayList<OfferAttach>()
                requestAttachList.add(requestAttach)

                val certificateOffer = RequestOffer()
                certificateOffer.type =
                    "${DidCommPrefixUtils.getType()}/issue-credential/1.0/request-credential"
                certificateOffer.id = UUID.randomUUID().toString()
                certificateOffer.thread = thread
                certificateOffer.offersAttach = requestAttachList

                val metaString =
                    Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
                val metaObject = JSONObject(metaString)
                val publicKey = metaObject.getString("verkey")

                val didDocObject =
                    SearchUtils.searchWallet(
                        WalletRecordType.DID_DOC,
                        "{\n" +
                                "  \"did\":\"${connectionObject.theirDid}\"\n" +
                                "}"
                    )

                val didDoc =
                    WalletManager.getGson.fromJson(
                        didDocObject.records?.get(0)?.value,
                        DidDoc::class.java
                    )

                val packedMessage = PackingUtils.packMessage(
                    didDoc, publicKey,
                    WalletManager.getGson.toJson(certificateOffer)
                )

                typedBytes = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(packedMessage)
                    }
                }

                val connectionInvitaitonObject =
                    SearchUtils.searchWallet(
                        WalletRecordType.CONNECTION_INVITATION,
                        "{\n" +
                                "  \"connection_id\":\"${resultObject.records?.get(0)?.id}\"\n" +
                                "}"
                    )

                serviceEndPoint =
                    JSONObject(
                        connectionInvitaitonObject.records?.get(0)?.value ?: ""
                    ).getString("serviceEndpoint")
            } catch (e: Exception) {
                return null
            }

            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
            commonHandler.taskStarted()
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            commonHandler.taskCompleted(typedBytes, serviceEndPoint)
        }
    }

    interface RequestCertificateHandler {
        fun taskCompleted(requestBody: RequestBody?, endPoint: String?)
        fun taskStarted()
    }
}