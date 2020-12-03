package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.igrant.mobileagent.R
import io.igrant.mobileagent.models.credentialExchange.CredentialExchange
import io.igrant.mobileagent.models.walletSearch.Record

class WalletCertificatesAdapter(private val credentialList: ArrayList<Record>) : RecyclerView.Adapter<WalletCertificatesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvCertificateName: TextView = itemView.findViewById<View>(R.id.tvCertificateName) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_certificate, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return credentialList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val gson= Gson()
        val credentialExchange =
            gson.fromJson(credentialList[position].value, CredentialExchange::class.java)

        var lst = credentialExchange.credentialProposalDict?.schemaId?.split(":")
        holder.tvCertificateName.text = lst?.get(2) ?: ""
    }
}