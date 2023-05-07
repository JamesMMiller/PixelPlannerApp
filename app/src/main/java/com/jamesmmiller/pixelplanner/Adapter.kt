package com.jamesmmiller.pixelplanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// ColumnAdapter.kt
class ColumnAdapter(private val columns: List<Column>, private val onAddColumn: () -> Unit, private val onAddTicket: (Column) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ADD_COLUMN = 0
        private const val VIEW_TYPE_COLUMN = 1
    }

    inner class AddColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val columnTitle: TextView = itemView.findViewById(R.id.columnTitle)
        val ticketsRecyclerView: RecyclerView = itemView.findViewById(R.id.ticketsRecyclerView)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == columns.size) VIEW_TYPE_ADD_COLUMN else VIEW_TYPE_COLUMN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ADD_COLUMN) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.add_column_item, parent, false)
            AddColumnViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.column_item, parent, false)
            ColumnViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ColumnViewHolder) {
            val column = columns[position]
            holder.columnTitle.text = column.title

            val ticketAdapter = TicketAdapter(column.tickets) {
                onAddTicket(column)
            }
            holder.ticketsRecyclerView.adapter = ticketAdapter
            holder.ticketsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        } else if (holder is AddColumnViewHolder) {
            holder.itemView.setOnClickListener {
                onAddColumn()
            }
        }
    }

    override fun getItemCount(): Int {
        return columns.size + 1
    }
}



// TicketAdapter.kt
class TicketAdapter(private val tickets: List<Ticket>, private val onAddTicket: () -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ADD_TICKET = 0
        private const val VIEW_TYPE_TICKET = 1
    }

    inner class AddTicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ticketTitle: TextView = itemView.findViewById(R.id.ticketTitle)
        val ticketDescription: TextView = itemView.findViewById(R.id.ticketDescription)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD_TICKET else VIEW_TYPE_TICKET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ADD_TICKET) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.add_ticket_item, parent, false)
            AddTicketViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_item, parent, false)
            TicketViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TicketViewHolder) {
            val ticket = tickets[position - 1]
            holder.ticketTitle.text = ticket.title
            holder.ticketDescription.text = ticket.description
        } else if (holder is AddTicketViewHolder) {
            holder.itemView.setOnClickListener {
                onAddTicket()
            }
        }
    }

    override fun getItemCount(): Int {
        return tickets.size + 1
    }
}


