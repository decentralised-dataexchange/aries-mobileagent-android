package io.igrant.mobileagent.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.WalletCertificatesAdapter
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

class WalletFragment : BaseFragment() {

    lateinit var tvAddCertificate: TextView
    lateinit var rvCertificates: RecyclerView
    lateinit var llErrorMessage: LinearLayout

    lateinit var walletCertificateAdapter: WalletCertificatesAdapter

    var credentialList: ArrayList<Record> = ArrayList()
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

//        val credSearch = CredentialsSearch.open(WalletManager.getWallet, "{}").get()
//
//        val data = credSearch.fetchNextCredentials(100).get()
//        Log.d(TAG, "certificate list : $data")

        walletCertificateAdapter = WalletCertificatesAdapter(credentialList)
        rvCertificates.adapter = walletCertificateAdapter

        val gson = Gson()

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

        val searchResponse = gson.fromJson(credentialExchangeResponse, SearchResponse::class.java)

        if (searchResponse.totalCount ?: 0 > 0) {
            llErrorMessage.visibility = View.GONE
            credentialList.clear()
            credentialList.addAll(searchResponse.records ?: ArrayList())
            walletCertificateAdapter.notifyDataSetChanged()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }

    }

    private fun initListener() {
        tvAddCertificate.setOnClickListener {
            NavigationUtils.showConnectionListFragment(fragmentManager = parentFragmentManager)
        }
    }

    private fun initViews(view: View) {
        tvAddCertificate = view.findViewById(R.id.tvAddCertificate)
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