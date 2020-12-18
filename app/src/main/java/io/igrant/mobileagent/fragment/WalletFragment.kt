package io.igrant.mobileagent.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.CertificateDetailActivity
import io.igrant.mobileagent.activty.CertificateDetailActivity.Companion.EXTRA_WALLET_DETAIL
import io.igrant.mobileagent.activty.RequestActivity
import io.igrant.mobileagent.adapter.WalletCertificatesAdapter
import io.igrant.mobileagent.events.ReceiveOfferEvent
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.WalletListener
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import io.igrant.mobileagent.utils.WalletRecordType.Companion.WALLET
import kotlinx.android.synthetic.main.fragment_wallet.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletRecord

class WalletFragment : BaseFragment() {

    lateinit var tvAddCertificate: TextView
    lateinit var etSearchWallet: EditText
    lateinit var rvCertificates: RecyclerView
    lateinit var llErrorMessage: LinearLayout

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
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ReceiveOfferEvent) {
        setUpCertificateList()
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
//            llErrorMessage.visibility = View.GONE
//            credentialList.clear()
//            credentialList.addAll(searchResponse.records ?: ArrayList())
//            walletCertificateAdapter.notifyDataSetChanged()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }

    }

    private fun initListener() {
        tvAddCertificate.setOnClickListener {
            NavigationUtils.showConnectionListFragment(fragmentManager = parentFragmentManager)
        }

        tvExchangeData.setOnClickListener {
            startActivity(
                Intent(
                    context,
                    RequestActivity::class.java
                )
            )
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
        tvAddCertificate = view.findViewById(R.id.tvAddCertificate)
        etSearchWallet = view.findViewById(R.id.etSearch)
        rvCertificates = view.findViewById(R.id.rvCertificates)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
    }

    companion object {
        private const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }
    }
}