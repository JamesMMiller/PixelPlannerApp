package com.jamesmmiller.pixelplanner

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.UUID

interface TicketDragDropListener {
    fun onTicketMoved(fromColumn: Column?, fromPosition: Int, toColumn: Column?, toPosition: Int)
}

class MainActivity : AppCompatActivity(), TicketDragDropListener {


    private lateinit var boardRecyclerView: RecyclerView
    //private lateinit var addColumnButton: FloatingActionButton

//    private val columns = mutableListOf<Column>(
//        Column(
//            UUID.randomUUID(), "To Do", mutableListOf(
//            Ticket(UUID.randomUUID(), "Ticket 1", "Ticket 1 description"),
//            Ticket(UUID.randomUUID(), "Ticket 2", "Ticket 2 description"),
//            Ticket(UUID.randomUUID(), "Ticket 3", "Ticket 3 description"))
//    ),
//        Column(UUID.randomUUID(), "In Progress", mutableListOf(
//            Ticket(UUID.randomUUID(), "Ticket 4", "Ticket 4 description"),
//            Ticket(UUID.randomUUID(), "Ticket 5", "Ticket 5 description"),
//            Ticket(UUID.randomUUID(), "Ticket 6", "Ticket 6 description"))
//        ),
//        Column(UUID.randomUUID(), "Done", mutableListOf(
//            Ticket(UUID.randomUUID(), "Ticket 7", "Ticket 7 description"),
//            Ticket(UUID.randomUUID(), "Ticket 8", "Ticket 8 description"),
//            Ticket(UUID.randomUUID(), "Ticket 9", "Ticket 9 description"))
//        )
//    )

    @RequiresApi(Build.VERSION_CODES.O)
    private val now = Instant.now().plusSeconds(700)

    @RequiresApi(Build.VERSION_CODES.O)
    private val columns =  mutableListOf<Column>(
        Column(
            UUID.randomUUID(), "To Do", mutableListOf(
            Ticket(UUID.randomUUID(), "Ticket 1", "Ticket 1 description", now, Duration.ofSeconds(300), false),
            Ticket(UUID.randomUUID(), "Ticket 2", "Ticket 2 description"),
            Ticket(UUID.randomUUID(), "Ticket 3", "Ticket 3 description" ))
    ),
    Column(UUID.randomUUID(), "In Progress", mutableListOf(
        Ticket(UUID.randomUUID(), "Ticket 4", "Ticket 4 description"),
        Ticket(UUID.randomUUID(), "Ticket 5", "Ticket 5 description"),
        Ticket(UUID.randomUUID(), "Ticket 6", "Ticket 6 description"))
    ),
    Column(UUID.randomUUID(), "Done", mutableListOf(
        Ticket(UUID.randomUUID(), "Ticket 7", "Ticket 7 description", null, null, true),
        Ticket(UUID.randomUUID(), "Ticket 8", "Ticket 8 description", null, null, true),
        Ticket(UUID.randomUUID(), "Ticket 9", "Ticket 9 description", null, null, true))
    )
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        boardRecyclerView = findViewById(R.id.boardRecyclerView)

        val layoutManager = GridLayoutManager(this, 4)
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
    val onAddTicket2: (Column) -> Unit = { column ->
        // Add a new ticket to the column
        val newTicket = Ticket(UUID.randomUUID(), "Ticket 10", "Ticket 10 description")
        println("newTicket: $newTicket")
        column.tickets.add(newTicket)
        println("column.tickets: ${column.tickets}")
        (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
        boardRecyclerView.adapter?.notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val onAddTicket: (Column) -> Unit = { column ->
        showAddTicketDialog { ticketTitle, ticketDescription, dueDate, warnigTime ->
            val newTicket = Ticket(UUID.randomUUID(), ticketTitle, ticketDescription, dueDate, warnigTime)
            column.tickets.add(newTicket)
            (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
            boardRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showAddTicketDialog(onTicketCreated: (String, String, Instant?, Duration?) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ticket, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.ticketTitleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.ticketDescriptionInput)
        val dueDatePicker = dialogView.findViewById<DatePicker>(R.id.ticketDueDatePicker)
        val dueTimePicker = dialogView.findViewById<TimePicker>(R.id.ticketDueTimePicker)
        val warningTimePicker = dialogView.findViewById<NumberPicker>(R.id.ticketWarningTimePicker)
        val setDueDateSwitch = dialogView.findViewById<Switch>(R.id.ticketSetDueDateSwitch)
        val setWarningTimeSwitch = dialogView.findViewById<Switch>(R.id.ticketSetWarningTimeSwitch)

        dueDatePicker.visibility = View.GONE
        dueTimePicker.visibility = View.GONE
        warningTimePicker.visibility = View.GONE

        warningTimePicker.minValue = 1
        warningTimePicker.maxValue = 48 // You can set an appropriate maximum value

        setDueDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dueDatePicker.visibility = View.VISIBLE
                dueTimePicker.visibility = View.VISIBLE
            } else {
                dueDatePicker.visibility = View.GONE
                dueTimePicker.visibility = View.GONE
                setWarningTimeSwitch.isChecked = false
            }
        }

        setWarningTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            warningTimePicker.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Ticket")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = false
            positiveButton.setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isNotBlank() && description.isNotBlank()) {
                    val dueDate: Instant? = if (setDueDateSwitch.isChecked) {
                        val calendar = Calendar.getInstance()
                        calendar.set(
                            dueDatePicker.year,
                            dueDatePicker.month,
                            dueDatePicker.dayOfMonth,
                            dueTimePicker.hour,
                            dueTimePicker.minute
                        )
                        calendar.toInstant()
                    } else {
                        null
                    }

                    val warningTime: Duration? = if (setWarningTimeSwitch.isChecked) {
                        Duration.ofHours(warningTimePicker.value.toLong())
                    } else {
                        null
                    }

                    onTicketCreated(title, description, dueDate, warningTime)
                    dialog.dismiss()
                }
            }
        }

        titleInput.onTextChange { updatePositiveButtonState(dialog, titleInput, descriptionInput) }
        descriptionInput.onTextChange { updatePositiveButtonState(dialog, titleInput, descriptionInput) }

        dialog.show()
    }

    private fun updatePositiveButtonState(dialog: AlertDialog, titleInput: EditText, descriptionInput: EditText) {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = title.isNotBlank() && description.isNotBlank()
    }


    private inline fun EditText.onTextChange(crossinline onTextChanged: () -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                onTextChanged()
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }



    @RequiresApi(Build.VERSION_CODES.O)
    val onAddColumn : () -> Unit = {
        showAddColumnDialog { columnTitle ->
            val newColumn = Column(UUID.randomUUID(), columnTitle)
            columns.add(newColumn)
            (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
            boardRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun showAddColumnDialog(onColumnCreated: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_column, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.columnTitleInput)

        AlertDialog.Builder(this)
            .setTitle("Add Column")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()

                if (title.isNotBlank()) {
                    onColumnCreated(title)
                } else {
                    Toast.makeText(this, "Column title is required.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
}
