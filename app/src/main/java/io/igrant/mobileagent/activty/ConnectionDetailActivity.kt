package io.igrant.mobileagent.activty

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.ConnectionMessageAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ReceiveOfferEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.Notification
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.certificateOffer.Attributes
import io.igrant.mobileagent.models.connection.Certificate
import io.igrant.mobileagent.models.connection.Connection
import io.igrant.mobileagent.models.connection.ConnectionCerListResponse
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.credentialExchange.RawCredential
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.*
import io.igrant.mobileagent.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.mobileagent.utils.WalletRecordType.Companion.DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class ConnectionDetailActivity : BaseActivity() {

    private var connectionCertList: ConnectionCerListResponse? = null
    private var mConnectionId: String = ""

    private lateinit var connectionMessageAdapter: ConnectionMessageAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()
    private var dataCertificateTypes: ArrayList<Certificate> = ArrayList()

    //views
    private lateinit var rvConnectionMessages: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var ivCoverUrl: ImageView
    private lateinit var ivLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRemove: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_detail)
        initViews()
        getIntentData()
        setUpToolbar()
        initListener()
        checkIfIgrantSupportedConnection()
        setUpAdapter()
        setUpConnectionMessagesList()
    }

    private fun checkIfIgrantSupportedConnection() {

        val connection = SearchUtils.searchWallet(CONNECTION, "{\"request_id\":\"$mConnectionId\"}")

        if (connection.totalCount ?: 0 > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value ?: "",
                MediatorConnectionObject::class.java
            )

            if (connectionObject.isIGrantEnabled == true) {
                getConnectionDetail(connectionObject)
            } else {
                setDefaultValues(connectionObject)
            }
        }
    }

    private fun setDefaultValues(connectionObject: MediatorConnectionObject) {
        Glide
            .with(ivLogo.context)
            .load(connectionObject.theirImageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvName.text = connectionObject.theirLabel ?: ""
        tvLocation.text = connectionObject.location ?: ""
    }

    private fun getConnectionDetail(connectionObject: MediatorConnectionObject) {
        val orgData =
            "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"$mConnectionId\" , \"~transport\": {" +
                    "\"return_route\": \"all\"}\n}"

        val cerData =
            "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/list-data-certificate-types\", \"@id\": \"$mConnectionId\" , \"~transport\": {" +
                    "\"return_route\": \"all\"}\n}"

        val didDoc =
            SearchUtils.searchWallet(DID_DOC, "{\"did\":\"${connectionObject.theirDid}\"}")

        if (didDoc.totalCount ?: 0 > 0) {
            val didDocObj = WalletManager.getGson.fromJson(
                didDoc.records?.get(0)?.value,
                DidDoc::class.java
            )

            val serviceEndPoint = didDocObj.service?.get(0)?.serviceEndpoint ?: ""
//                val publicKey = didDocObj.publicKey?.get(0)?.publicKeyBase58

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
            val metaObject = JSONObject(metaString)
            val key = metaObject.getString("verkey")

            val orgDetailPacked = PackingUtils.packMessage(didDocObj, key, orgData,"")

            val orgDetailTypedArray = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(orgDetailPacked)
                }
            }
            ApiManager.api.getService()?.postData(serviceEndPoint, orgDetailTypedArray)
                ?.enqueue(object :
                    Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        Log.d("https", "onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        if (response.code() == 200 && response.body() != null) {
                            val unpack =
                                Crypto.unpackMessage(
                                    WalletManager.getWallet,
                                    WalletManager.getGson.toJson(response.body()).toString()
                                        .toByteArray()
                                ).get()

                            Log.d(
                                "milan",
                                "onResponse: ${JSONObject(String(unpack)).getString("message")}"
                            )
                            val connectionData = WalletManager.getGson.fromJson(
                                JSONObject(String(unpack)).getString("message"),
                                Connection::class.java
                            )
                            initDataValues(connectionData)
                        }
                    }
                })

            val orgCerListPacked = PackingUtils.packMessage(didDocObj, key, cerData,"")

            val orgCerListTypedArray = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(orgCerListPacked)
                }
            }
            ApiManager.api.getService()?.postData(serviceEndPoint, orgCerListTypedArray)
                ?.enqueue(object :
                    Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        Log.d("https", "onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        if (response.code() == 200 && response.body() != null) {
                            val unpack =
                                Crypto.unpackMessage(
                                    WalletManager.getWallet,
                                    WalletManager.getGson.toJson(response.body()).toString()
                                        .toByteArray()
                                ).get()

                            Log.d(
                                "milan",
                                "onResponse: ${JSONObject(String(unpack)).getString("message")}"
                            )
                            val certificateList = WalletManager.getGson.fromJson(
                                JSONObject(String(unpack)).getString("message"),
                                ConnectionCerListResponse::class.java
                            )
                            connectionCertList = certificateList
                            initList()
                        }
                    }
                })
        }
    }

    private fun initList() {
        val tempList: ArrayList<Certificate> = ArrayList()
        var tempCer: Certificate
        for (certificate in connectionCertList?.dataCertificateTypes ?: ArrayList()) {
            tempCer = certificate
            for (cer in connectionMessageList) {
                val gson = Gson()
                val notification = gson.fromJson(cer.value, Notification::class.java)
                val message = notification.certificateOffer

                val schema = gson.fromJson(
                    Base64.decode(message?.offersAttach?.get(0)?.data?.base64, Base64.URL_SAFE)
                        .toString(charset("UTF-8")), RawCredential::class.java
                ).schemaId

                if (certificate.schemaId == schema ?: "") {
                    tempCer.record = cer
                }
            }

//            val walletModelTag = "{" +
//                    "\"connection_id\":\"${mConnectionId}\"," +
//                    "\"schema_id\":\"${certificate.schemaId ?: ""}\"" +
//                    "}"

//            val walletSearch = SearchUtils.searchWallet(WalletRecordType.WALLET, walletModelTag)


//            if (walletSearch.records != null && walletSearch.totalCount ?: 0 > 0) {
//                try {
//                    val certificate =
//                        WalletManager.getGson.fromJson(
//                            walletSearch.records!![0].value,
//                            WalletModel::class.java
//                        )
//                    tempCer.attributeList =
//                        certificate.credentialProposalDict?.credentialProposal?.attributes!!
//                } catch (e: Exception) {
//                }
//            } else {
            var attributeList: ArrayList<Attributes> = ArrayList()
            var attribute: Attributes
            for (string in certificate.schemaAttributes) {
                attribute = Attributes()
                attribute.name = string
                attribute.value = ""

                attributeList.add(attribute)
            }
            tempCer.attributeList = attributeList
//            }

            tempList.add(tempCer)
        }

        dataCertificateTypes.clear()
        dataCertificateTypes.addAll(tempList)
        llErrorMessage.visibility = if (dataCertificateTypes.size > 0) View.GONE else View.VISIBLE
        connectionMessageAdapter.notifyDataSetChanged()
    }

    private fun initDataValues(connectionData: Connection?) {
        Glide
            .with(ivLogo.context)
            .load(connectionData?.logoImageUrl)
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        Glide
            .with(ivCoverUrl.context)
            .load(connectionData?.coverImageUrl)
            .centerCrop()
            .placeholder(R.drawable.default_cover_image)
            .into(ivCoverUrl)

        tvDescription.text = connectionData?.description
        TextUtils.makeTextViewResizable(
            tvDescription,
            3,
            resources.getString(R.string.txt_read_more),
            true
        );
        tvName.text = connectionData?.name
        tvLocation.text = connectionData?.location
    }

    private fun initViews() {
        rvConnectionMessages = findViewById(R.id.rvConnectionMessages)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        ivCoverUrl = findViewById(R.id.ivCoverUrl)
        ivLogo = findViewById(R.id.ivLogo)
        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
        tvDescription = findViewById(R.id.tvDescription)
        tvRemove = findViewById(R.id.tvRemove)
    }

    private fun getIntentData() {
        if (intent.extras != null) {
            mConnectionId = intent.getStringExtra(EXTRA_CONNECTION_DATA) ?: ""
        }
    }

    private fun setUpToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolBarCommon)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back_bg)
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

    private fun initListener() {
        tvRemove.setOnClickListener {
            DeleteUtils.deleteConnection(mConnectionId)
            finish()
        }
    }

    private fun setUpAdapter() {
        connectionMessageAdapter =
            ConnectionMessageAdapter(dataCertificateTypes, object : ConnectionMessageListener {
                override fun onConnectionMessageClick(record: Record, name: String) {
                    val intent =
                        Intent(this@ConnectionDetailActivity, OfferCertificateActivity::class.java)
                    intent.putExtra(
                        OfferCertificateActivity.EXTRA_CERTIFICATE_PREVIEW,
                        record
                    )
                    startActivity(intent)
                    Handler(Looper.getMainLooper()).postDelayed({
                        finish()
                    }, 100)

                }

            })
        rvConnectionMessages.adapter = connectionMessageAdapter
    }

    private fun setUpConnectionMessagesList() {
        val connectionMessageResponse =
            SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"connectionId\": \"${mConnectionId}\"," +
                        "\"type\":\"${MessageTypes.TYPE_OFFER_CREDENTIAL}\",\n" +
                        "\"stat\":\"Active\"\n" +
                        "}"
            )
        if (connectionMessageResponse.totalCount ?: 0 > 0) {
            connectionMessageList.clear()
            connectionMessageList.addAll(connectionMessageResponse.records ?: ArrayList())
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ReceiveOfferEvent) {
        setUpConnectionMessagesList()
        if (connectionCertList != null)
            initList()
    }

    override fun onStart() {
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
        super.onStart()
    }

    override fun onStop() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onStop()
    }


    companion object {
        const val EXTRA_CONNECTION_DATA =
            "io.igrant.mobileagent.fragment.ConnectionMessagesFragment.connection"
    }
}