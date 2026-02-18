package com.linkel.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.linkel.app.api.Link

class LinkAdapter(
    private val currentUsername: String,
    private val onLinkClick: (Link) -> Unit,
    private val onDeleteClick: (Link) -> Unit
) : ListAdapter<Link, LinkAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvUrl: TextView = view.findViewById(R.id.tvUrl)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_link, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val link = getItem(position)

        holder.tvTitle.text = link.title
        holder.tvUrl.text = link.url
        holder.tvUsername.text = "@${link.username}"
        holder.tvTimestamp.text = formatTimestamp(link.createdAt)

        if (link.description.isNullOrBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = link.description
        }

        holder.btnDelete.visibility =
            if (link.username == currentUsername) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onLinkClick(link) }
        holder.btnDelete.setOnClickListener { onDeleteClick(link) }
    }

    private fun formatTimestamp(raw: String): String {
        // raw format from SQLite: "2024-05-10 14:32:00"
        return try {
            val parts = raw.split(" ")
            "${parts[0]}  ${parts[1].substring(0, 5)}"
        } catch (_: Exception) {
            raw
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Link>() {
            override fun areItemsTheSame(old: Link, new: Link) = old.id == new.id
            override fun areContentsTheSame(old: Link, new: Link) = old == new
        }
    }
}
