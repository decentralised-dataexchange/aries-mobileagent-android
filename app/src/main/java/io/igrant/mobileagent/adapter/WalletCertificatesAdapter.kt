package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.mobileagent.R
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.WalletListener
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.models.walletSearch.Record

class WalletCertificatesAdapter(
    private val credentialList: ArrayList<Record>,
    private val listener: WalletListener
) : RecyclerView.Adapter<WalletCertificatesAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvCertificateName: TextView =
            itemView.findViewById<View>(R.id.tvCertificateName) as TextView
        var tvCompanyName: TextView =
            itemView.findViewById<View>(R.id.tvCompanyName) as TextView
        var ivDelete: ImageView = itemView.findViewById<View>(R.id.ivDelete) as ImageView
        var ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        var cvItem: CardView = itemView.findViewById(R.id.cvItem)
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
        val certificate =
            WalletManager.getGson.fromJson(credentialList[position].value, WalletModel::class.java)
        val lst = certificate.rawCredential?.schemaId?.split(":")
        holder.tvCertificateName.text = (lst?.get(2) ?: "").toUpperCase()
        holder.ivDelete.setOnClickListener {
            listener.onDelete(certificate.credentialId ?: "", position)
        }
        holder.tvCompanyName.text = certificate.connection?.theirLabel ?: ""
        try {
            Glide
                .with(holder.ivLogo.context)
                .load(certificate.connection?.theirImageUrl ?: "")
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(holder.ivLogo)
        } catch (e: Exception) {
        }
        holder.cvItem.setOnClickListener {
            listener.onItemClick(certificate)
        }
    }
}