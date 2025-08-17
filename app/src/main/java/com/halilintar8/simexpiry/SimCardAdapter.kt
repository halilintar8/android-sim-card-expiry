package com.halilintar8.simexpiry

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.halilintar8.simexpiry.data.SimCard
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class SimCardAdapter(
    private val simCards: MutableList<SimCard>,
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SimCardAdapter.SimCardViewHolder>() {

    inner class SimCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNumber: TextView = itemView.findViewById(R.id.tvNumber)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvSimNumber: TextView = itemView.findViewById(R.id.tvSimNumber)
        val tvExpiredDate: TextView = itemView.findViewById(R.id.tvExpiredDate)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        init {
            btnEdit.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onEditClick(pos)
            }
            btnDelete.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onDeleteClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sim_card, parent, false)
        return SimCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: SimCardViewHolder, position: Int) {
        val simCard = simCards[position]
        val context = holder.itemView.context

        // Sequential numbering starting from 1
        holder.tvNumber.text = (position + 1).toString()

        holder.tvName.text = "Name: ${simCard.name}"
        holder.tvSimNumber.text = "Sim card no.: ${simCard.simCardNumber}"

        val normalizedDate = normalizeDate(simCard.expiredDate)
        val baseLabel = "Expired date: $normalizedDate"

        val spannableText = try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val expiredDate = LocalDate.parse(normalizedDate, formatter)
            val today = LocalDate.now()
            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiredDate).toInt()
            val reminderDays = getReminderDays(context)

            when {
                daysUntilExpiry < 0 -> {
                    colorText("$baseLabel (expired)", normalizedDate, Color.RED)
                }
                daysUntilExpiry in 0..reminderDays -> {
                    colorText("$baseLabel (expiring in $daysUntilExpiry days)", normalizedDate, Color.MAGENTA)
                }
                else -> SpannableString(baseLabel)
            }
        } catch (e: Exception) {
            SpannableString(baseLabel)
        }

        holder.tvExpiredDate.text = spannableText
    }

    override fun getItemCount(): Int = simCards.size

    private fun normalizeDate(raw: String): String = try {
        val inputFormat = SimpleDateFormat("yyyy-M-d", Locale.US)
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        inputFormat.parse(raw)?.let { outputFormat.format(it) } ?: raw
    } catch (e: Exception) {
        raw
    }

    private fun colorText(fullText: String, highlight: String, color: Int): SpannableString {
        val spannable = SpannableString(fullText)
        val start = fullText.indexOf(highlight)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(color),
                start,
                fullText.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun getReminderDays(context: Context): Int {
        val prefs = context.getSharedPreferences("sim_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("reminder_days", 7)
    }

    // --- Public helper methods ---
    fun addSimCard(simCard: SimCard) {
        simCards.add(simCard)
        notifyItemInserted(simCards.lastIndex)
    }

    fun updateSimCard(position: Int, updatedSimCard: SimCard) {
        if (position in simCards.indices) {
            simCards[position] = updatedSimCard
            notifyItemChanged(position)
        }
    }

    fun removeSimCard(position: Int) {
        if (position in simCards.indices) {
            simCards.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
