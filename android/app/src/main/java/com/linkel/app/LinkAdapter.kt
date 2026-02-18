package com.linkel.app

import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.linkel.app.api.Link
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class LinkAdapter(
    private val currentUsername: String,
    private val onLinkClick: (Link) -> Unit,
    private val onDeleteClick: (Link) -> Unit
) : ListAdapter<Link, LinkAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
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

        // Avatar: colored circle with the first letter of the username
        val initial = link.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.tvAvatar.text = initial
        val avatarColor = AVATAR_COLORS[
            link.username.hashCode().and(0x7FFFFFFF) % AVATAR_COLORS.size
        ]
        val circle = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(avatarColor)
        }
        holder.tvAvatar.background = circle

        holder.tvUsername.text = "@${link.username}"
        holder.tvTimestamp.text = relativeTime(link.createdAt)
        holder.tvTitle.text = link.title

        if (link.description.isNullOrBlank()) {
            holder.tvDescription.visibility = View.GONE
        } else {
            holder.tvDescription.visibility = View.VISIBLE
            holder.tvDescription.text = link.description
        }

        // Show domain name instead of the raw URL to keep cards tidy
        holder.tvUrl.text = "\uD83D\uDD17 ${extractDomain(link.url)}"

        holder.btnDelete.visibility =
            if (link.username == currentUsername) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onLinkClick(link) }
        holder.btnDelete.setOnClickListener { onDeleteClick(link) }
    }

    /** Return the host portion of [url], stripping a leading "www." if present. */
    private fun extractDomain(url: String): String = try {
        val host = Uri.parse(url).host ?: return url
        host.removePrefix("www.")
    } catch (_: Exception) {
        url
    }

    /**
     * Convert a SQLite timestamp ("2024-05-10 14:32:00") to a human-friendly
     * relative string: "just now", "5m ago", "3h ago", "2d ago", or "May 10".
     */
    private fun relativeTime(raw: String): String = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val date: Date = sdf.parse(raw) ?: return raw
        val diffMs = Date().time - date.time
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
        val days = TimeUnit.MILLISECONDS.toDays(diffMs)
        when {
            mins < 1   -> "just now"
            mins < 60  -> "${mins}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7   -> "${days}d ago"
            else       -> SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    } catch (_: Exception) {
        raw
    }

    companion object {
        /** Material palette colors used for user avatars. */
        private val AVATAR_COLORS = intArrayOf(
            0xFFE53935.toInt(), // Red
            0xFFD81B60.toInt(), // Pink
            0xFF8E24AA.toInt(), // Purple
            0xFF3949AB.toInt(), // Indigo
            0xFF1E88E5.toInt(), // Blue
            0xFF00897B.toInt(), // Teal
            0xFF43A047.toInt(), // Green
            0xFFF4511E.toInt(), // Deep Orange
            0xFF6D4C41.toInt(), // Brown
            0xFF546E7A.toInt(), // Blue Grey
        )

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Link>() {
            override fun areItemsTheSame(old: Link, new: Link) = old.id == new.id
            override fun areContentsTheSame(old: Link, new: Link) = old == new
        }
    }
}
