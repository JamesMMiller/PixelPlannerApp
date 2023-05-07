package com.jamesmmiller.pixelplanner

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

// ColumnAdapter.kt
class ColumnAdapter(private val columns: List<Column>, private val onAddTicket: (Column) -> Unit) :
    RecyclerView.Adapter<ColumnAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val columnTitle: TextView = itemView.findViewById(R.id.columnTitle)
        val ticketsRecyclerView: RecyclerView = itemView.findViewById(R.id.ticketsRecyclerView)
        val addTicketButton: FloatingActionButton = itemView.findViewById(R.id.addTicketButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.column_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val column = columns[position]
        holder.columnTitle.text = column.title

        val ticketAdapter = TicketAdapter(column.tickets)
        holder.ticketsRecyclerView.adapter = ticketAdapter
        holder.ticketsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)

        holder.addTicketButton.setOnClickListener {
            onAddTicket(column)
        }
    }

    override fun getItemCount(): Int {
        return columns.size
    }
}


// TicketAdapter.kt
class TicketAdapter(private val tickets: List<Ticket>) : RecyclerView.Adapter<TicketAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ticketTitle: TextView = itemView.findViewById(R.id.ticketTitle)
        val ticketDescription: TextView = itemView.findViewById(R.id.ticketDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ticket_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ticket = tickets[position]
        holder.ticketTitle.text = ticket.title
        holder.ticketDescription.text = ticket.description
    }

    override fun getItemCount(): Int {
        return tickets.size
    }
}

