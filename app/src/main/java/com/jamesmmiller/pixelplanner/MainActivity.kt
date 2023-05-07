package com.jamesmmiller.pixelplanner

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.UUID

interface TicketDragDropListener {
    fun onTicketMoved(fromColumn: Column?, fromPosition: Int, toColumn: Column?, toPosition: Int)
}

class MainActivity : AppCompatActivity(), TicketDragDropListener {


    private lateinit var boardRecyclerView: RecyclerView
    //private lateinit var addColumnButton: FloatingActionButton
    private val columns = mutableListOf<Column>(
        Column(
            UUID.randomUUID(), "To Do", mutableListOf(
            Ticket(UUID.randomUUID(), "Ticket 1", "Ticket 1 description"),
            Ticket(UUID.randomUUID(), "Ticket 2", "Ticket 2 description"),
            Ticket(UUID.randomUUID(), "Ticket 3", "Ticket 3 description"))
    ),
        Column(UUID.randomUUID(), "In Progress", mutableListOf(
            Ticket(UUID.randomUUID(), "Ticket 4", "Ticket 4 description"),
            Ticket(UUID.randomUUID(), "Ticket 5", "Ticket 5 description"),
            Ticket(UUID.randomUUID(), "Ticket 6", "Ticket 6 description"))
        ),
        Column(UUID.randomUUID(), "Done", mutableListOf(
            Ticket(UUID.randomUUID(), "Ticket 7", "Ticket 7 description"),
            Ticket(UUID.randomUUID(), "Ticket 8", "Ticket 8 description"),
            Ticket(UUID.randomUUID(), "Ticket 9", "Ticket 9 description"))
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        boardRecyclerView = findViewById(R.id.boardRecyclerView)

        val layoutManager = GridLayoutManager(this, columns.size + 1)
        boardRecyclerView.layoutManager = layoutManager
        boardRecyclerView.adapter = BoardAdapter(columns, layoutManager,
            onAddColumn,
            onAddTicket = { column ->
                onAddTicket(column)
            }
        )
        val callback = BoardItemTouchHelperCallback(boardRecyclerView.adapter as BoardAdapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(boardRecyclerView)

    }

    //val onAddTicket: (com.jamesmmiller.pixelplanner.Column) -> Unit
    val onAddTicket: (Column) -> Unit = { column ->
        // Add a new ticket to the column
        val newTicket = Ticket(UUID.randomUUID(), "Ticket 10", "Ticket 10 description")
        println("newTicket: $newTicket")
        column.tickets.add(newTicket)
        println("column.tickets: ${column.tickets}")
        (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
        boardRecyclerView.adapter?.notifyDataSetChanged()
    }

    val onAddColumn: () -> Unit = {
        // Add a new column
        val newColumn = Column(UUID.randomUUID(), "New Column")
        println("newColumn: $newColumn")
        columns.add(newColumn)
        println("columns: $columns")
        (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
        boardRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun onTicketMoved(
        fromColumn: Column?,
        fromPosition: Int,
        toColumn: Column?,
        toPosition: Int
    ) {
        if (fromColumn != null && toColumn != null) {
            val ticketToMove = fromColumn.tickets[fromPosition]

            // Remove the ticket from its original column
            fromColumn.tickets.removeAt(fromPosition)

            // Add the ticket to the new column at the desired position
            toColumn.tickets.add(toPosition, ticketToMove)

            // Update the adapter to reflect the changes
            boardRecyclerView.adapter?.notifyDataSetChanged()
        } else {
            Log.e("MainActivity", "Invalid column(s) provided in onTicketMoved")
        }
    }



//    val ticketTouchHelperCallback: TicketTouchHelperCallback = TicketTouchHelperCallback { column, fromPos, toPos ->
//        val columnObj = columns[column]
////        val toColumnObj = columns[toColumn]
//        val movedTicket = columnObj.tickets.removeAt(fromPos)
//        columnObj.tickets.add(toPos, movedTicket)
//
//        columnsRecyclerView.adapter?.notifyDataSetChanged()
//    }
}
