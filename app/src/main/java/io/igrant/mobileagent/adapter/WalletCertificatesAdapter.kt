package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.WalletListener
import io.igrant.mobileagent.models.certificate.Certificate

class WalletCertificatesAdapter(
    private val credentialList: ArrayList<Certificate>,
    val listener: WalletListener
) : RecyclerView.Adapter<WalletCertificatesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvCertificateName: TextView =
            itemView.findViewById<View>(R.id.tvCertificateName) as TextView
        var ivDelete: ImageView = itemView.findViewById<View>(R.id.ivDelete) as ImageView
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
        val certificate = credentialList[position]
        val lst = certificate.schemaId?.split(":")
        holder.tvCertificateName.text = lst?.get(2) ?: ""
        holder.ivDelete.setOnClickListener {
            listener.onDelete(certificate.referent?:"",position)
        }
    }
}