package io.igrant.mobileagent.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.CertificateDetailActivity
import io.igrant.mobileagent.activty.CertificateDetailActivity.Companion.EXTRA_WALLET_DETAIL
import io.igrant.mobileagent.activty.ConnectionListActivity
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_INVITATION
import io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_PROPOSAL
import io.igrant.mobileagent.activty.RequestActivity
import io.igrant.mobileagent.adapter.WalletCertificatesAdapter
import io.igrant.mobileagent.events.ReceiveCertificateEvent
import io.igrant.mobileagent.events.ReceiveOfferEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.WalletListener
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.utils.ConnectionUtils
import io.igrant.mobileagent.utils.PermissionUtils
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType.Companion.WALLET
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject

class WalletFragment : BaseFragment() {

    lateinit var tvDataWallet: TextView
    lateinit var etSearchWallet: EditText
    lateinit var rvCertificates: RecyclerView
    lateinit var llErrorMessage: LinearLayout
    lateinit var ivAdd:ImageView

    lateinit var walletCertificateAdapter: WalletCertificatesAdapter

    private var certificateList: ArrayList<Record> = ArrayList()
    private var certificateListCopy: ArrayList<Record> = ArrayList()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        initViews(view)
        initListener()
        setUpCertificateList()

        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ReceiveCertificateEvent) {
        setUpCertificateList()
    }

    override fun onDestroy() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    private fun setUpCertificateList() {
        val walletSearch = SearchUtils.searchWallet(WALLET, "{}")

        certificateList.clear()
        certificateList.addAll(walletSearch.records ?: ArrayList())
        certificateListCopy.clear()
        certificateListCopy.addAll(walletSearch.records ?: ArrayList())
        walletCertificateAdapter =
            WalletCertificatesAdapter(certificateList, object : WalletListener {
                override fun onDelete(id: String, position: Int) {
                    try {
                        Anoncreds.proverDeleteCredential(WalletManager.getWallet, id).get()
                        WalletRecord.delete(WalletManager.getWallet, WALLET, id)
                        walletCertificateAdapter.notifyItemRemoved(position)
                        val walletSearch = SearchUtils.searchWallet(WALLET, "{}")
                        certificateList.clear()
                        certificateList.addAll(walletSearch.records ?: ArrayList())
                        certificateListCopy.clear()
                        certificateListCopy.addAll(walletSearch.records ?: ArrayList())
                    } catch (e: Exception) {
                    }
                }

                override fun onItemClick(wallet: WalletModel) {
                    val intent = Intent(context, CertificateDetailActivity::class.java)
                    val wal = WalletManager.getGson.toJson(wallet)
                    intent.putExtra(EXTRA_WALLET_DETAIL, wal)
                    startActivity(intent)
                }
            })
        rvCertificates.adapter = walletCertificateAdapter

        if (certificateList.size > 0) {
            llErrorMessage.visibility = View.GONE
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }

    }

    private fun initListener() {
        ivAdd.setOnClickListener {
            val intent = Intent(context,
                ConnectionListActivity::class.java)
            startActivity(intent)
        }

        tvExchangeData.setOnClickListener {

            if (PermissionUtils.hasPermissions(
                    requireActivity(),
                    PERMISSIONS
                )
            ) {
                val i = Intent(requireActivity(), QrCodeActivity::class.java)
                startActivityForResult(
                    i,
                    REQUEST_CODE_SCAN_INVITATION
                )
            }else{
                requestPermissions(PERMISSIONS, PICK_IMAGE_REQUEST)
            }
        }

        etSearchWallet.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s)
            }
        })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val i = Intent(requireActivity(), QrCodeActivity::class.java)
            startActivityForResult(
                i,
                REQUEST_CODE_SCAN_INVITATION
            )
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN_INVITATION) {
            if (data == null) return

            try {
                val uri: Uri =
                    Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
                val v: String = uri.getQueryParameter("qr_p") ?: ""
                if (v != "") {
                    val json =
                        Base64.decode(
                            v,
                            Base64.URL_SAFE
                        ).toString(charset("UTF-8"))
                    val data = JSONObject(json)
                    if (data.getString("invitation_url") != "") {
                        val invitation: String =
                            Uri.parse(data.getString("invitation_url")).getQueryParameter("c_i")
                                ?: ""
                        val proofRequest = data.getJSONObject("proof_request")
                        saveConnectionAndExchangeData(invitation, proofRequest)
                    } else {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.err_unexpected),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(R.string.err_unexpected),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.err_unexpected),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveConnectionAndExchangeData(
        data: String,
        proofRequest: JSONObject
    ) {
        var invitation: Invitation? = null
        try {
            val json =
                Base64.decode(
                    data,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))

            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }
        if (invitation != null)
            sendProposal(proofRequest, invitation)
        else
            Toast.makeText(
                context,
                resources.getString(R.string.err_unexpected),
                Toast.LENGTH_SHORT
            ).show()
    }

    private fun sendProposal(
        proofRequest: JSONObject,
        invitation: Invitation
    ) {
        val intent = Intent(requireContext(), ProposeAndExchangeDataActivity::class.java)
        intent.putExtra(EXTRA_PRESENTATION_PROPOSAL, proofRequest.toString())
        intent.putExtra(EXTRA_PRESENTATION_INVITATION, invitation)
        startActivity(intent)
    }

    private fun filterList(s: CharSequence?) {
        val tempList: ArrayList<Record> = ArrayList()
        for (certificate in certificateListCopy) {
            val walletModel =
                WalletManager.getGson.fromJson(certificate.value, WalletModel::class.java)
            val lst = walletModel.rawCredential?.schemaId?.split(":")
            val text = lst?.get(2) ?: ""
            if (text.contains(s ?: "", ignoreCase = true)) {
                tempList.add(certificate)
            }
        }

        certificateList.clear()
        certificateList.addAll(tempList)

        walletCertificateAdapter.notifyDataSetChanged()
    }

    private fun initViews(view: View) {
        tvDataWallet = view.findViewById(R.id.tvDataWallet)
        etSearchWallet = view.findViewById(R.id.etSearch)
        rvCertificates = view.findViewById(R.id.rvCertificates)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
        ivAdd = view.findViewById(R.id.ivAdd)
    }

    companion object {
        private const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }

        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202
    }
}