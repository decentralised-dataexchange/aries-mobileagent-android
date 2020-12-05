package io.igrant.mobileagent.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.WalletCertificatesAdapter
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.WalletListener
import io.igrant.mobileagent.models.certificate.Certificate
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearch
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

class WalletFragment : BaseFragment() {

    lateinit var tvAddCertificate: TextView
    lateinit var etSearchWallet: EditText
    lateinit var rvCertificates: RecyclerView
    lateinit var llErrorMessage: LinearLayout

    lateinit var walletCertificateAdapter: WalletCertificatesAdapter

    private var certificateList: ArrayList<Certificate> = ArrayList()
    private var certificateListCopy: ArrayList<Certificate> = ArrayList()
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

    private fun setUpCertificateList() {

        val credentialExchangeSearch = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.CREDENTIAL_EXCHANGE_V10,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val credentialExchangeResponse =
            WalletSearch.searchFetchNextRecords(
                WalletManager.getWallet,
                credentialExchangeSearch,
                100
            ).get()

        Log.d(TAG, "credentialExchangeResult: $credentialExchangeResponse")
        WalletManager.closeSearchHandle(credentialExchangeSearch)

        val credSearch = CredentialsSearch.open(WalletManager.getWallet, "{}").get()

        val data = credSearch.fetchNextCredentials(100).get()

        certificateList.clear()
        certificateList.addAll(WalletManager.getGson.fromJson(data, Array<Certificate>::class.java))
        certificateListCopy.clear()
        certificateListCopy.addAll(
            WalletManager.getGson.fromJson(
                data,
                Array<Certificate>::class.java
            )
        )
        walletCertificateAdapter =
            WalletCertificatesAdapter(certificateList, object : WalletListener {
                override fun onDelete(id: String) {
                    WalletRecord.delete(
                        WalletManager.getWallet,
                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                        id
                    ).get()
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
        var tempList: ArrayList<Certificate> = ArrayList()
        for (certificate in certificateListCopy) {
            var lst = certificate.schemaId?.split(":")
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