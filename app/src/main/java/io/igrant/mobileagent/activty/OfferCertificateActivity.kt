package io.igrant.mobileagent.activty

import android.os.AsyncTask
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.CertificateAttributeAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.certificateOffer.OfferAttach
import io.igrant.mobileagent.models.certificateOffer.OfferData
import io.igrant.mobileagent.models.certificateOffer.RequestOffer
import io.igrant.mobileagent.models.credentialExchange.CredentialExchange
import io.igrant.mobileagent.models.credentialExchange.CredentialRequest
import io.igrant.mobileagent.models.credentialExchange.CredentialRequestMetadata
import io.igrant.mobileagent.models.credentialExchange.Thread
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.CredentialExchangeStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.crypto.Crypto
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

    private lateinit var mConnectionId: String
    private var mCertificateOffer: CertificateOffer? = null
    private var record: Record? = null

    private lateinit var toolbar: Toolbar
    private lateinit var btReject: Button
    private lateinit var btAccept: Button
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    private lateinit var adapter: CertificateAttributeAdapter

    companion object {
        const val EXTRA_CERTIFICATE_PREVIEW =
            "io.igrant.mobileagent.activty.OfferCertificateActivity.certificate"
        const val EXTRA_CONNECTION_ID =
            "io.igrant.mobileagent.activty.OfferCertificateActivity.connectionId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_certificate)
        initViews()
        initListener()
        getIntentData()
        setUpToolbar()
        setUpAdapter()
    }

    private fun initListener() {
        btReject.setOnClickListener {
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.MESSAGE_RECORDS,
                "${mCertificateOffer?.id ?: ""}"
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

            onBackPressed()
        }

        btAccept.setOnClickListener {
            llProgressBar.visibility = View.VISIBLE
            btAccept.isEnabled = false
            btReject.isEnabled = false
            RequestCertificateTask(object : RequestCertificateHandler {
                override fun taskCompleted(requestBody: RequestBody, endPoint: String) {

//                    val tagJson = "{\n" +
//                            "  \"type\":\"${record?.tags?.get("type")}\",\n" +
//                            "  \"connectionId\":\"${record?.tags?.get("connectionId")}\",\n" +
//                            "}"
//                    WalletRecord.updateTags(
//                        WalletManager.getWallet,
//                        WalletRecordType.MESSAGE_RECORDS,
//                        record!!.id,
//                        tagJson
//                    )

                    ApiManager.api.getService()
                        ?.postData(endPoint, requestBody)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                llProgressBar.visibility = View.GONE
                                btAccept.isEnabled = true
                                btReject.isEnabled = false
                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                llProgressBar.visibility = View.GONE
                                btAccept.isEnabled = false
                                btReject.isEnabled = true
                                onBackPressed()
                                if (response.code() == 200 && response.body() != null) {

                                }
                            }
                        })
                }

                override fun taskStarted() {

                }
            }, mCertificateOffer!!, mConnectionId).execute()
        }
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
        btReject = findViewById(R.id.btReject)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar);
        supportActionBar!!.title = resources.getString(R.string.title_offer_detail)
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

    private fun getIntentData() {
        record = intent.extras!!.get(EXTRA_CERTIFICATE_PREVIEW) as Record
        mCertificateOffer =
            WalletManager.getGson.fromJson(record!!.value, CertificateOffer::class.java)
        mConnectionId = intent.extras!!.get(EXTRA_CONNECTION_ID) as String
    }

    class RequestCertificateTask(
        private val commonHandler: RequestCertificateHandler,
        private val mCertificateOffer: CertificateOffer,
        private val mConnectionId: String
    ) :
        AsyncTask<Void, Void, Void>() {

        private lateinit var serviceEndPoint: String
        private var typedBytes: RequestBody? = null

        override fun doInBackground(vararg p0: Void?): Void? {
            val credentialExchangeResponse =
                SearchUtils.searchWallet(
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "{\"thread_id\": \"${mCertificateOffer?.id}\"}"
                )

            var credentialExchangeData: CredentialExchange = CredentialExchange()
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
                mCertificateOffer?.offersAttach?.get(0)?.data?.base64,
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
            thread.thid = mCertificateOffer?.id ?: ""

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
            thread.thid = mCertificateOffer?.id ?: ""

            val requestAttachList = ArrayList<OfferAttach>()
            requestAttachList.add(requestAttach)

            val certificateOffer = RequestOffer()
            certificateOffer.type =
                "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/issue-credential/1.0/request-credential"
            certificateOffer.id = UUID.randomUUID().toString()
            certificateOffer.thread = thread
            certificateOffer.offersAttach = requestAttachList

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            //get prover did
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.DID_KEY,
                "{\n" +
                        "  \"did\":\"${connectionObject.theirDid}\"\n" +
                        "}"
            )

            var packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"${searchResponse.records?.get(0)?.value ?: ""}\"]",
                publicKey,
                WalletManager.getGson.toJson(certificateOffer).toByteArray()
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

            val connectionInvitaitonObject =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION_INVITATION,
                    "{\n" +
                            "  \"connection_id\":\"${resultObject.records?.get(0)?.id}\"\n" +
                            "}"
                )

            serviceEndPoint =
                JSONObject(connectionInvitaitonObject.records?.get(0)?.value).getString("serviceEndpoint")

            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
            commonHandler.taskStarted()
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)
            commonHandler.taskCompleted(typedBytes!!, serviceEndPoint)
        }
    }

    interface RequestCertificateHandler {
        fun taskCompleted(requestBody: RequestBody, endPoint: String)
        fun taskStarted()
    }
}