package io.igrant.mobileagent.activty

import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
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
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import io.igrant.mobileagent.utils.CredentialExchangeStates
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
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
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

    private lateinit var toolbar: Toolbar
    private lateinit var btReject: Button
    private lateinit var btAccept: Button
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar:LinearLayout

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
        btReject.setOnClickListener { onBackPressed() }

        btAccept.setOnClickListener {
            val gson = Gson()

            llProgressBar.visibility = View.VISIBLE
            val search = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                "{\"thread_id\": \"${mCertificateOffer?.id}\"}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val credentialExchange =
                WalletSearch.searchFetchNextRecords(
                    WalletManager.getWallet,
                    search,
                    100
                ).get()

            val credentialExchangeResponse =
                gson.fromJson(credentialExchange, SearchResponse::class.java)
            var credentialExchangeData: CredentialExchange = CredentialExchange()
            if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                credentialExchangeData = gson.fromJson(
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

            //get prover did
            val searchConnection = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.CONNECTION,
                "{\n" +
                        "  \"request_id\":\"$mConnectionId\"\n" +
                        "}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val connection =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchConnection, 100)
                    .get()

            val resultObject = gson.fromJson(connection, SearchResponse::class.java)
            val connectionObject = gson.fromJson(
                resultObject.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
            val proverDid = connectionObject.myDid
            //get cred offer json: //offer credential base64 parameter value decoded value
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
                gson.fromJson(proverResponse.credentialRequestJson, CredentialRequest::class.java)
            val credentialRequestMetaData =
                gson.fromJson(
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
                gson.toJson(credentialExchangeData)
            )

            //creating model for sending
            val thread = Thread()
            thread.thid = mCertificateOffer?.id ?: ""

//            val test = "{\"prover_did\":\"5UExfQDojJijLj8jGP6QXN\",\"cred_def_id\":\"V9DMzk7seuY2p8HunWByLP:3:CL:342:default\",\"blinded_ms\":{\"u\":\"77375274852976695547753617098432772549828655179881657273184470439608678414739989105574637842226275460533919514958284167057676287235469382370484775088355684935044342837962263304043076780284237767288294048720430416186508386633112797440124418469382744299678649044903129786263482062143818825665944485811338293615477777762732192583414781585930140644912645089163665577100865479236529067667850952934660345533375848288295677731577559249345324567933976656408029826178506026844284623507684172413072463510195494267281851397211440117245229373962323326017491059484866078775397190365802390752674996027764147866149624902788843231807\",\"ur\":null,\"hidden_attributes\":[\"master_secret\"],\"committed_attributes\":{}},\"blinded_ms_correctness_proof\":{\"c\":\"73823886387725862586876241041783250482352049808506954090284483778213448160178\",\"v_dash_cap\":\"935991108803215211683050517192484217671824733663961684848796934376911874709137345876290538032437746484411543588029017327486133662372375060280951404273033284705416059578007294487241426075553600939852306776729994700794062560764419690022893985840643635795205052848085099849914369781889545121136340702458842450976245635784395290645815335262787155356701037194006999801910552927342639491174881299945058493932383864637303706177485582003578314655820622204321716235554956206839335083112365495195895975395259667930361567538334872546976684576479604836717005035162889504996142498026823712828053354798256953362554299147444719988503988027905081292991709926576542060444429276660623686156435934337735705016613419020559420078973784326\",\"m_caps\":{\"master_secret\":\"10792619785299083536781002478084613079843837285619106415559816465198409452442486114589442604359951359049591799665235688983996703511724359215192745077588301196412298279717156354795\"},\"r_caps\":{}},\"nonce\":\"543779694111493123581672\"}"
//            val testJson = JSONObject(test)
            val v = Base64.encodeToString(proverResponse.credentialRequestJson.toByteArray(), Base64.NO_WRAP)
            Log.d("milna", "initListener: ${proverResponse.credentialRequestJson}")
            Log.d("milna", "initListener: $v")
            v.replace("\\n","")
            Log.d("milna", "initListener: $v")
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

            val metaString = Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            //get prover did
            val searchDidDoc = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.DID_KEY,
                "{\n" +
                        "  \"did\":\"${connectionObject.theirDid}\"\n" +
                        "}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val didKey =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDidDoc, 100)
                    .get()

            val searchResponse = gson.fromJson(didKey,SearchResponse::class.java)

            var packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"${searchResponse.records?.get(0)?.value ?:""}\"]",
                publicKey,
                gson.toJson(certificateOffer).toByteArray()
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

            val searchConnectionInvitation = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.CONNECTION_INVITATION,
                "{\n" +
                        "  \"connection_id\":\"${resultObject.records?.get(0)?.id}\"\n" +
                        "}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val connectionInvitation =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchConnectionInvitation, 100)
                    .get()

            val connectionInvitaitonObject = gson.fromJson(connectionInvitation,SearchResponse::class.java)

            val serviceEndPoint = JSONObject(connectionInvitaitonObject.records?.get(0)?.value).getString("serviceEndpoint")

            ApiManager.api.getService()
                ?.postData(serviceEndPoint, typedBytes)
                ?.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        llProgressBar.visibility = View.GONE
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        llProgressBar.visibility = View.GONE
                        if (response.code() == 200 && response.body() != null) {

                        }
                    }
                })
            //todo pack the message and send to the connection endpoint
            //todo encode proverResponse to base 64 and append

            onBackPressed()
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
        supportActionBar!!.title = ""
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
        mCertificateOffer = intent.extras!!.get(EXTRA_CERTIFICATE_PREVIEW) as CertificateOffer
        mConnectionId = intent.extras!!.get(EXTRA_CONNECTION_ID) as String
    }
}