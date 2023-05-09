package com.jamesmmiller.pixelplanner

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BoardAdapter(
    private val columns: List<Column>,
    private val layoutManager: GridLayoutManager,
    private val onAddColumn: () -> Unit,
    private val onAddTicket: (Column) -> Unit,
    private val onTicketSelected: (Ticket) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ADD_COLUMN = 0
        private const val VIEW_TYPE_COLUMN = 1
        private const val VIEW_TYPE_ADD_TICKET = 2
        private const val VIEW_TYPE_TICKET = 3
    }

    var items = generateItems(columns)

    fun updateItems() {
        items = generateItems(columns)
    }


    inner class AddColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    inner class ColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val columnTitle: TextView = itemView.findViewById(R.id.columnTitle)
    }

    inner class AddTicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    inner class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ticketTitle: TextView = itemView.findViewById(R.id.ticketTitle)
    }

    data class AddTicketPlaceholder(val column: Column)
    object AddColumnPlaceholder

    private fun generateItems(columns: List<Column>): MutableList<Any> {
        val items = mutableListOf<Any>()

        for (column in columns) {
            items.add(column)

            for (ticket in column.tickets) {
                items.add(ticket)
            }

            items.add(AddTicketPlaceholder(column))
        }

        items.add(AddColumnPlaceholder)

        return items
    }

    init {
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (items[position] is Column || items[position] is AddColumnPlaceholder) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Column -> VIEW_TYPE_COLUMN
            is Ticket -> VIEW_TYPE_TICKET
            is AddTicketPlaceholder -> VIEW_TYPE_ADD_TICKET
            is AddColumnPlaceholder -> VIEW_TYPE_ADD_COLUMN
            else -> throw IllegalStateException("Unknown item type")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_ADD_COLUMN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_column_item, parent, false)
                AddColumnViewHolder(view)
            }

            VIEW_TYPE_COLUMN -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.column_item, parent, false)
                ColumnViewHolder(view)
            }

            VIEW_TYPE_ADD_TICKET -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.add_ticket_item, parent, false)
                AddTicketViewHolder(view)
            }

            VIEW_TYPE_TICKET -> {
                val view =
                    LayoutInflater.from(parent.context).inflate(R.layout.ticket_item, parent, false)
                TicketViewHolder(view)
            }

            else -> throw IllegalStateException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Column -> {
                // If the item is a Column, cast the holder to ColumnViewHolder and update the UI.
                (holder as ColumnViewHolder).apply {
                    columnTitle.text = item.title
                }
            }
            is Ticket -> {
                // If the item is a Ticket, cast the holder to TicketViewHolder and update the UI.
                (holder as TicketViewHolder).apply {
                    ticketTitle.text = item.title
                    itemView.setOnClickListener {
                        onTicketSelected(item)
                    }
                }
            }
            is AddTicketPlaceholder -> {
                // If the item is an AddTicketPlaceholder, cast the holder to AddTicketViewHolder,
                // and set the click listener to call the onAddTicket callback with the associated column.
                (holder as AddTicketViewHolder).apply {
                    itemView.setOnClickListener {
                        onAddTicket(item.column)
                    }
                }
            }
            is AddColumnPlaceholder -> {
                // If the item is an AddColumnPlaceholder, cast the holder to AddColumnViewHolder,
                // and set the click listener to call the onAddColumn callback.
                (holder as AddColumnViewHolder).apply {
                    itemView.setOnClickListener {
                        Log.d("BoardAdapter", "Add Column card clicked")
                        onAddColumn()
                    }
                }
            }
        }
    }

    fun onTicketMoved(fromPosition: Int, toPosition: Int) {
        val fromItem = items[fromPosition]
        val toItem = items[toPosition]

        if (fromItem is Ticket && toItem is Ticket) {
            items.removeAt(fromPosition)
            items.add(toPosition, fromItem)

            val fromColumn = columns.firstOrNull { fromItem in it.tickets }
            val toColumn = columns.firstOrNull { toItem in it.tickets }

            if (fromColumn != null && toColumn != null) {
                fromColumn.tickets.remove(fromItem)
                toColumn.tickets.add(toPosition - items.indexOf(toColumn) - 1, fromItem)
            }

            notifyItemMoved(fromPosition, toPosition)
        }
    }



}


