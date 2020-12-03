package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.mobileagent.listeners.ConnectionClickListener
import io.igrant.mobileagent.R
import org.json.JSONArray
import org.json.JSONObject

class ConnectionListAdapter(
    val connectionRecords: JSONArray,
    val listener: ConnectionClickListener
) :
    RecyclerView.Adapter<ConnectionListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvConnectionName: TextView = itemView.findViewById<View>(R.id.tvConnection) as TextView
        var ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_connection,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return connectionRecords.length()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = JSONObject(connectionRecords.getJSONObject(position).getString("value"))

        holder.tvConnectionName.setOnClickListener {
            listener.onConnectionClick(data.getString("request_id"))
        }
        try {
            Glide
                .with(holder.ivLogo.context)
                .load(data.getString("their_image_url"))
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(holder.ivLogo)
        } catch (e: Exception) {
        }
        if (data.getString("their_label") != "")
            holder.tvConnectionName.text = data.getString("their_label")
    }
}