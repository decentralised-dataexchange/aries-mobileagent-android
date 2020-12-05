package io.igrant.mobileagent.adapter

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.credentialExchange.RawCredential
import io.igrant.mobileagent.models.walletSearch.Record

class ConnectionMessageAdapter(
    val mList: ArrayList<Record>,
    val mListener: ConnectionMessageListener
) :
    RecyclerView.Adapter<ConnectionMessageAdapter.ViewHolder>() {
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
        val gson = Gson()
        val message = gson.fromJson(mList[position].value, CertificateOffer::class.java)

        var schema = gson.fromJson(
            Base64.decode(message.offersAttach?.get(0)?.data?.base64, Base64.URL_SAFE)
                .toString(charset("UTF-8")), RawCredential::class.java
        ).schemaId
        var lst = schema?.split(":")
        holder.tvName.text = lst?.get(2) ?: ""

        holder.tvVersion.text = "Version : ${lst?.get(3) ?: ""}"

        holder.cvOffer.setOnClickListener {
            mListener.onConnectionMessageClick(mList[position])
        }
    }
}