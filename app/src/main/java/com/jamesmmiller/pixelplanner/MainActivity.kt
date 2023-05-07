package com.jamesmmiller.pixelplanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {


    private lateinit var columnsRecyclerView: RecyclerView
    //private lateinit var addColumnButton: FloatingActionButton
    private val columns = mutableListOf<Column>(
        Column("1", "To Do", mutableListOf(
            Ticket("1", "Ticket 1", "Ticket 1 description"),
            Ticket("2", "Ticket 2", "Ticket 2 description"),
            Ticket("3", "Ticket 3", "Ticket 3 description"))
    ),
        Column("2", "In Progress", mutableListOf(
            Ticket("4", "Ticket 4", "Ticket 4 description"),
            Ticket("5", "Ticket 5", "Ticket 5 description"),
            Ticket("6", "Ticket 6", "Ticket 6 description"))
        ),
        Column("3", "Done", mutableListOf(
            Ticket("7", "Ticket 7", "Ticket 7 description"),
            Ticket("8", "Ticket 8", "Ticket 8 description"),
            Ticket("9", "Ticket 9", "Ticket 9 description"))
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        columnsRecyclerView = findViewById(R.id.columnsRecyclerView)
        // addColumnButton = findViewById(R.id.addColumnButton)

        columnsRecyclerView.adapter = ColumnAdapter(columns, onAddColumn, onAddTicket)

        columnsRecyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)

//        addColumnButton.setOnClickListener {
//            // Add a new column
//        }
    }

    //val onAddTicket: (com.jamesmmiller.pixelplanner.Column) -> Unit
    val onAddTicket: (Column) -> Unit = { column ->
        // Add a new ticket to the column
        val newTicket = Ticket("10", "Ticket 10", "Ticket 10 description")
        column.tickets.add(newTicket)
        columnsRecyclerView.adapter?.notifyDataSetChanged()
    }

    val onAddColumn: () -> Unit = {
        // Add a new column
        val newColumn = Column("4", "New Column")
        columns.add(newColumn)
        columnsRecyclerView.adapter?.notifyDataSetChanged()
    }
}
