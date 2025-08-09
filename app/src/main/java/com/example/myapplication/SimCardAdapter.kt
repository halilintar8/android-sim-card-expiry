package com.example.myapplication

import android.annotation.SuppressLint
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
import com.example.myapplication.data.SimCard
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
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) onEditClick(position)
            }
            btnDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) onDeleteClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sim_card, parent, false)
        return SimCardViewHolder(view)
    }

    @SuppressLint("NewApi")
    override fun onBindViewHolder(holder: SimCardViewHolder, position: Int) {
        val simCard = simCards[position]

        holder.tvNumber.text = simCard.id.toString()
        holder.tvName.text = "name : ${simCard.name}"
        holder.tvSimNumber.text = "sim card no. : ${simCard.simCardNumber}"

        val normalizedDate = normalizeDate(simCard.expiredDate)
        val baseLabel = "expired date : $normalizedDate"

        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val expiredDate = LocalDate.parse(normalizedDate, formatter)
            val today = LocalDate.now()

            val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiredDate)

            val isExpiredSoon = daysUntilExpiry <= 7

            if (isExpiredSoon) {
                val expiredText = "$baseLabel (expired)"
                val spannable = SpannableString(expiredText)
                val highlightStart = expiredText.indexOf(normalizedDate)
                val highlightEnd = expiredText.length

                spannable.setSpan(
                    ForegroundColorSpan(Color.RED),
                    highlightStart,
                    highlightEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                holder.tvExpiredDate.text = spannable
            } else {
                holder.tvExpiredDate.text = baseLabel
            }

        } catch (e: Exception) {
            holder.tvExpiredDate.text = baseLabel
        }
    }

    override fun getItemCount(): Int = simCards.size

    // Ensure date format is "yyyy-MM-dd"
    private fun normalizeDate(raw: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-M-d", Locale.US)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = inputFormat.parse(raw)
            outputFormat.format(parsed ?: return raw)
        } catch (e: Exception) {
            raw
        }
    }

    fun addSimCard(simCard: SimCard) {
        simCards.add(simCard)
        notifyItemInserted(simCards.size - 1)
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
