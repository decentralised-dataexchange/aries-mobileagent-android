package io.igrant.mobileagent.activty

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.CertificateAttributeAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.certificateOffer.Attributes
import io.igrant.mobileagent.models.presentationExchange.CredentialValue
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.presentationExchange.RequestCredential
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.PresentationExchangeStates
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.ledger.LedgerResults
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ExchangeDataActivity : BaseActivity() {

    private lateinit var mConnectionId: String
    private var record: Record? = null

    private var mPresentationExchange: PresentationExchange? = null

    private lateinit var toolbar: Toolbar
    private lateinit var tvDesc: TextView
    private lateinit var tvHead: TextView
    private lateinit var btReject: Button
    private lateinit var btAccept: Button
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    private lateinit var adapter: CertificateAttributeAdapter

    private var attributelist: ArrayList<Attributes> = ArrayList()

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

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

    private fun initValues() {
        tvDesc.text = mPresentationExchange?.comment ?: ""
        tvHead.text = mPresentationExchange?.presentationRequest?.name ?: ""

        val searchHandle = CredentialsSearchForProofReq.open(
            WalletManager.getWallet,
            WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
            "{}"
        ).get()

        requestedAttributes = HashMap()
        attributelist.clear()
        var credentialValue = CredentialValue()
        mPresentationExchange?.presentationRequest?.requestedAttributes?.forEach { (key, value) ->

            val searchResult = searchHandle.fetchNextCredentials(key, 100).get()

            val referent =
                JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                    .getString("referent")

            credentialValue = CredentialValue()
            credentialValue.credId = referent
            credentialValue.revealed = true

            requestedAttributes[key] = credentialValue;

            val data = JSONObject(JSONArray(searchResult)[0].toString()).getJSONObject("cred_info")
                .getJSONObject("attrs").getString(value.name)

            val attributes: Attributes = Attributes()
            attributes.name = value.name
            attributes.value = data

            attributelist.add(attributes)

        }

        searchHandle.closeSearch()

        adapter = CertificateAttributeAdapter(
            attributelist
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun getIntentData() {
        record = intent.extras!!.get(EXTRA_PRESENTATION_RECORD) as Record
        mPresentationExchange =
            WalletManager.getGson.fromJson(record!!.value, PresentationExchange::class.java)
        mConnectionId = mPresentationExchange?.connectionId ?: ""
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar);
        supportActionBar!!.title = resources.getString(R.string.title_exchange_data_detail)
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
        btReject = findViewById(R.id.btReject)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun initListener() {
        btReject.setOnClickListener {
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.MESSAGE_RECORDS,
                "${mPresentationExchange?.threadId ?: ""}"
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

            onBackPressed()
        }

        btAccept.setOnClickListener {
            llProgressBar.visibility = View.VISIBLE
            btAccept.isEnabled = false
            btReject.isEnabled = false

            Pool.setProtocolVersion(2)

            var schemaParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
            var credParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
            requestedAttributes.forEach() { (key, value) ->
                val proverCred =
                    Anoncreds.proverGetCredential(WalletManager.getWallet, value.credId).get()
                val schemaResponse =
                    Ledger.buildGetSchemaRequest(
                        null,
                        JSONObject(proverCred).getString("schema_id")
                    )
                        .get()
                val requestResponse =
                    Ledger.submitRequest(PoolManager.getPool, schemaResponse).get()
                val schemaParsed = Ledger.parseGetSchemaResponse(requestResponse).get()
                if (!schemaParsedList.contains(schemaParsed))
                    schemaParsedList.add(schemaParsed)

                val credDefResponse =
                    Ledger.buildGetCredDefRequest(
                        null,
                        JSONObject(proverCred).getString("cred_def_id")
                    )
                        .get()

                val credDefSubmitResponse =
                    Ledger.submitRequest(PoolManager.getPool, credDefResponse).get()

                val creedDefParsed = Ledger.parseGetCredDefResponse(credDefSubmitResponse).get()
                if (!credParsedList.contains(creedDefParsed))
                    credParsedList.add(creedDefParsed)
            }

            val requestCredential = RequestCredential()
            requestCredential.requestedAttributes = requestedAttributes
            requestCredential.requestedPredicates = Object()
            requestCredential.selfAttestedAttributes = Object()


            val schemaMap = convertArrayListToHashMap(schemaParsedList)
            var schema = "{\n"
            schemaMap?.forEach { (s, jsonObject) ->
                schema += " \"${s}\": $jsonObject ,\n"
            }
            schema = schema.substring(0, schema.length - 2);
            schema += "}"

            val credMap = convertArrayListToHashMap(credParsedList)
            var credDef = "{\n"
            credMap?.forEach { (s, jsonObject) ->
                credDef += " \"${s}\": $jsonObject ,\n"
            }
            credDef = credDef.substring(0, credDef.length - 2);
            credDef += "}"

            val proverProofResponse = Anoncreds.proverCreateProof(
                WalletManager.getWallet,
                WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
                WalletManager.getGson.toJson(requestCredential),
                "IGrantMobileAgent-000001",
                schema,
                credDef,
                "{}"
            ).get()

            mPresentationExchange?.presentation = JSONObject(proverProofResponse)
            mPresentationExchange?.state = PresentationExchangeStates.PRESENTATION_SENT

            Log.d(TAG, "initListener: ${WalletManager.getGson.toJson(mPresentationExchange)}")

            WalletRecord.updateValue(
                WalletManager.getWallet, WalletRecordType.PRESENTATION_EXCHANGE_V10,
                record?.id, WalletManager.getGson.toJson(mPresentationExchange)
            )

            val connectionObjectRecord =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\n" +
                            "  \"request_id\":\"${mConnectionId}\"\n" +
                            "}"
                )

            val connectionObject = WalletManager.getGson.fromJson(
                connectionObjectRecord.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
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

            val base64 = Base64.encodeToString(
                proverProofResponse.toByteArray(),
                Base64.NO_WRAP
            )

            val data = "{\n" +
                    "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/present-proof/1.0/presentation\",\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"~thread\": {\n" +
                    "    \"thid\": \"${mPresentationExchange?.threadId}\"\n" +
                    "  },\n" +
                    "  \"presentations~attach\": [\n" +
                    "    {\n" +
                    "      \"@id\": \"libindy-presentation-0\",\n" +
                    "      \"mime-type\": \"application/json\",\n" +
                    "      \"data\": {\n" +
                    "        \"base64\": \"$base64\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"comment\": \"auto-presented for proof request nonce=1234567890\"\n" +
                    "}"
            val connectionInvitaitonObject =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION_INVITATION,
                    "{\n" +
                            "  \"connection_id\":\"${connectionObjectRecord.records?.get(0)?.id}\"\n" +
                            "}"
                )

            var packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"${searchResponse.records?.get(0)?.value ?: ""}\"]",
                publicKey,
                data.toByteArray()
            ).get()

            val typedBytes = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedMessage)
                }
            }

            ApiManager.api.getService()
                ?.postData(
                    JSONObject(connectionInvitaitonObject.records?.get(0)?.value).getString("serviceEndpoint"),
                    typedBytes
                )
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
    }

    private fun convertArrayListToHashMap(arrayList: ArrayList<LedgerResults.ParseResponseResult>): HashMap<String, JSONObject>? {
        val hashMap: HashMap<String, JSONObject> = HashMap()
        for (str in arrayList) {
            hashMap[str.id] = JSONObject(str.objectJson)
        }
        return hashMap
    }
}