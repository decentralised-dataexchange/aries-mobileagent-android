package io.igrant.mobileagent.activty

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.CertificateAttributeAdapter
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ReceiveCertificateEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.connection.Connection
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.TextUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class CertificateDetailActivity : BaseActivity() {

    private lateinit var adapter: CertificateAttributeAdapter
    private var wallet: WalletModel? = null

    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var tvHead: TextView
    private lateinit var tvRemove: TextView
    private lateinit var ivCoverUrl: ImageView
    private lateinit var ivLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView

    companion object {
        const val EXTRA_WALLET_DETAIL = "CertificateDetailActivity.wallet"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_detail)
        getIntentData()
        initViews()
        initValues()
        initListener()
        setUpToolbar()
        setUpAdapter()
        getConnectionDetail()
    }

    private fun getIntentData() {
        val wal = intent.extras!!.getString(EXTRA_WALLET_DETAIL)
        wallet = WalletManager.getGson.fromJson(wal, WalletModel::class.java)
    }

    private fun getConnectionDetail() {
        val orgData =
            "{ \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/igrantio-operator/1.0/organization-info\", \"@id\": \"${wallet?.connection?.requestId ?: ""}\" , \"~transport\": {" +
                    "\"return_route\": \"all\"}\n}"


        val didDoc =
            SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${wallet?.connection?.theirDid}\"}"
            )

        if (didDoc.totalCount ?: 0 > 0) {
            val didDocObj = WalletManager.getGson.fromJson(
                didDoc.records?.get(0)?.value,
                DidDoc::class.java
            )

            val serviceEndPoint = didDocObj.service?.get(0)?.serviceEndpoint ?: ""
            val publicKey = didDocObj.publicKey?.get(0)?.publicKeyBase58

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, wallet?.connection?.myDid).get()
            val metaObject = JSONObject(metaString)
            val key = metaObject.getString("verkey")

            val orgDetailPacked = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"${publicKey ?: ""}\"]",
                key,
                orgData.toByteArray()
            ).get()

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
        }
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
            .placeholder(R.drawable.images)
            .into(ivCoverUrl)

        tvDescription.text = connectionData?.description
        TextUtils.makeTextViewResizable(tvDescription, 3, "See More", true);
        tvName.text = connectionData?.name
        tvLocation.text = connectionData?.location
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        tvHead = findViewById(R.id.tvHead)
        tvRemove = findViewById(R.id.tvRemove)
        ivCoverUrl = findViewById(R.id.ivCoverUrl)
        ivLogo = findViewById(R.id.ivLogo)
        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
        tvDescription = findViewById(R.id.tvDescription)
    }

    private fun initValues() {
        try {
            tvHead.text = ((wallet?.rawCredential?.schemaId ?: "").split(":")[2]).toUpperCase()
        } catch (e: Exception) {
        }
    }

    private fun initListener() {
        tvRemove.setOnClickListener {
            AlertDialog.Builder(this@CertificateDetailActivity)
                .setTitle(resources.getString(R.string.txt_confirmation))
                .setMessage(
                    resources.getString(
                        R.string.txt_certificate_delete_confirmation
                    )
                ) // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, which ->
                        try {
                            Anoncreds.proverDeleteCredential(
                                WalletManager.getWallet,
                                wallet?.credentialId
                            )
                                .get()
                            WalletRecord.delete(
                                WalletManager.getWallet,
                                WalletRecordType.WALLET,
                                wallet?.credentialId
                            )
                            EventBus.getDefault().post(ReceiveCertificateEvent())
                            finish()
                        } catch (e: Exception) {
                        }
                    }) // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(
                    android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, which ->

                    })
                .show()
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
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

    private fun setUpAdapter() {
        adapter = CertificateAttributeAdapter(
            wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

}