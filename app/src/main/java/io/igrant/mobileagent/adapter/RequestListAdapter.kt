package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.walletSearch.Record

class RequestListAdapter(
    private val mList: ArrayList<Record>,
    private val mListener: ConnectionMessageListener
) : RecyclerView.Adapter<RequestListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvVersion: TextView = itemView.findViewById<View>(R.id.tvVersion) as TextView
        var cvOffer: CardView = itemView.findViewById<View>(R.id.cvOffer) as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_connection_messaages,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = WalletManager.getGson.fromJson(mList[position].value, PresentationExchange::class.java)
        holder.tvName.text = message.presentationRequest?.name?:""
        holder.tvVersion.text = message.presentationRequest?.version?:""

        holder.cvOffer.setOnClickListener {
            mListener.onConnectionMessageClick(mList[position])
        }
    }
}