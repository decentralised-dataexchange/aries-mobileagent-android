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
import io.igrant.mobileagent.activty.InitializeActivity
import io.igrant.mobileagent.activty.OfferCertificateActivity
import io.igrant.mobileagent.adapter.ConnectionMessageAdapter
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.*

class ConnectionMessagesFragment : BaseFragment() {

    private var mConnectionId: String = ""

    private lateinit var connectionMessageAdapter: ConnectionMessageAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()

    //views
    private lateinit var rvConnectionMessages: RecyclerView
    private lateinit var llErrorMessage: LinearLayout

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mConnectionId = requireArguments().getString(EXTRA_CONNECTION_DATA, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connection_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initListener()
        setUpAdapter()
        setUpConnectionMessagesList()
    }

    private fun setUpAdapter() {
        connectionMessageAdapter =
            ConnectionMessageAdapter(connectionMessageList, object : ConnectionMessageListener {
                override fun onConnectionMessageClick(record: Record) {
                    val intent: Intent = Intent(context, OfferCertificateActivity::class.java)
                    intent.putExtra(
                        OfferCertificateActivity.EXTRA_CERTIFICATE_PREVIEW,
                        record
                    )
                    intent.putExtra(
                        OfferCertificateActivity.EXTRA_CONNECTION_ID,
                        mConnectionId
                    )
                    startActivity(intent)
                    Handler(Looper.getMainLooper()).postDelayed({
                        NavigationUtils.popBack(parentFragmentManager)
                    }, 500)

                }

            })
        rvConnectionMessages.adapter = connectionMessageAdapter
    }

    private fun initViews(view: View) {
        rvConnectionMessages = view.findViewById(R.id.rvConnectionMessages)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
    }

    private fun initListener() {

    }

    private fun setUpConnectionMessagesList() {
        val connectionMessageResponse =
            SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"connectionId\": \"${mConnectionId}\"," +
                        "\"type\":\"${MessageTypes.TYPE_OFFER_CREDENTIAL}\""+
                        "}"
            )
        if (connectionMessageResponse.totalCount ?: 0 > 0) {
            llErrorMessage.visibility = View.GONE
            connectionMessageList.clear()
            connectionMessageList.addAll(connectionMessageResponse.records ?: ArrayList())
            connectionMessageAdapter.notifyDataSetChanged()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val EXTRA_CONNECTION_DATA =
            "io.igrant.mobileagent.fragment.ConnectionMessagesFragment.connection"

        fun newInstance(connection: String?): ConnectionMessagesFragment {
            val args = Bundle()
            args.putString(EXTRA_CONNECTION_DATA, connection)
            val fragment = ConnectionMessagesFragment()
            fragment.arguments = args
            return fragment
        }
    }
}