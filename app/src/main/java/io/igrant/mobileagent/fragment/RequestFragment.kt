package io.igrant.mobileagent.fragment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.ExchangeDataActivity
import io.igrant.mobileagent.activty.InitializeActivity
import io.igrant.mobileagent.adapter.RequestListAdapter
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.MessageTypes
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType

class RequestFragment : BaseFragment() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var llErrorMessage: LinearLayout

    private lateinit var adapter: RequestListAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()

    private var toolbarTitle: String? = ""

    override fun onResume() {
        toolbarTitle = (activity as InitializeActivity?)?.getActionBarTitle()
        (activity as InitializeActivity?)?.setActionBarTitle(getString(R.string.title_offer_list))
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as InitializeActivity?)?.setActionBarTitle(toolbarTitle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initListener()
        setUpAdapter()
        setUpConnectionMessagesList()
    }

    private fun initViews(view: View) {
        rvRequests = view.findViewById(R.id.rvRequests)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
    }

    private fun initListener() {

    }

    private fun setUpAdapter() {
        adapter = RequestListAdapter(connectionMessageList, object : ConnectionMessageListener {
            override fun onConnectionMessageClick(record: Record) {
                val intent: Intent = Intent(context, ExchangeDataActivity::class.java)
                intent.putExtra(
                    ExchangeDataActivity.EXTRA_PRESENTATION_RECORD,
                    record
                )

                startActivity(intent)

                Handler(Looper.getMainLooper()).postDelayed({
                    NavigationUtils.popBack(parentFragmentManager)
                }, 500)
            }

        })
        rvRequests.adapter = adapter
    }

    private fun setUpConnectionMessagesList() {
        val connectionMessageResponse =
            SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{" +
                        "\"type\":\"${MessageTypes.TYPE_REQUEST_PRESENTATION}\"" +
                        "}"
            )
        if (connectionMessageResponse.totalCount ?: 0 > 0) {
            llErrorMessage.visibility = View.GONE
            connectionMessageList.clear()
            connectionMessageList.addAll(connectionMessageResponse.records ?: ArrayList())
            adapter.notifyDataSetChanged()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    companion object {

        fun newInstance(): RequestFragment {

            return RequestFragment()
        }
    }
}