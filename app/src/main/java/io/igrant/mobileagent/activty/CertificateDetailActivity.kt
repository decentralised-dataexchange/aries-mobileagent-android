package io.igrant.mobileagent.activty

import android.content.DialogInterface
import android.os.Bundle
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
import io.igrant.mobileagent.events.ReceiveCertificateEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.connection.Connection
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.utils.TextUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletRecord

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
        if (wallet?.organization == null) {
            initDataValues(wallet?.organization)
        } else {
            initDataValueWithConnection(wallet?.connection)
        }
    }

    private fun initDataValueWithConnection(connectionData: MediatorConnectionObject?) {
        Glide
            .with(ivLogo.context)
            .load(connectionData?.theirImageUrl)
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvName.text = connectionData?.theirLabel
        tvLocation.text = connectionData?.location ?: "Nil"
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

    private fun setUpAdapter() {
        adapter = CertificateAttributeAdapter(
            wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

}