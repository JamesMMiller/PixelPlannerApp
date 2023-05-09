package com.jamesmmiller.pixelplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class DateTimePickerDialog(
    private val context: Context,
    private val onDateTimeSelected: (Calendar) -> Unit
) {
    private val calendar = Calendar.getInstance()

    fun show() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                showTimePickerDialog()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                onDateTimeSelected(calendar)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
}


interface TicketDragDropListener {
    fun onTicketMoved(fromColumn: Column?, fromPosition: Int, toColumn: Column?, toPosition: Int)
}

class MainActivity : AppCompatActivity(), TicketDragDropListener {


    private lateinit var boardRecyclerView: RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    private val now = Instant.now().plusSeconds(700)

    @RequiresApi(Build.VERSION_CODES.O)
    private val columns = mutableListOf<Column>(
        Column(
            UUID.randomUUID(), "To Do", mutableListOf(
                Ticket(
                    UUID.randomUUID(),
                    "Ticket 1",
                    "Ticket 1 description",
                    now,
                    Duration.ofSeconds(300),
                    false
                ),
                Ticket(UUID.randomUUID(), "Ticket 2", "Ticket 2 description"),
                Ticket(UUID.randomUUID(), "Ticket 3", "Ticket 3 description")
            )
        ),
        Column(
            UUID.randomUUID(), "In Progress", mutableListOf(
                Ticket(UUID.randomUUID(), "Ticket 4", "Ticket 4 description"),
                Ticket(UUID.randomUUID(), "Ticket 5", "Ticket 5 description"),
                Ticket(UUID.randomUUID(), "Ticket 6", "Ticket 6 description")
            )
        ),
        Column(
            UUID.randomUUID(), "Done", mutableListOf(
                Ticket(UUID.randomUUID(), "Ticket 7", "Ticket 7 description", null, null, true),
                Ticket(UUID.randomUUID(), "Ticket 8", "Ticket 8 description", null, null, true),
                Ticket(UUID.randomUUID(), "Ticket 9", "Ticket 9 description", null, null, true)
            )
        )
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        boardRecyclerView = findViewById(R.id.boardRecyclerView)

        val layoutManager = GridLayoutManager(this, 4)
        boardRecyclerView.layoutManager = layoutManager
        boardRecyclerView.adapter = BoardAdapter(
            columns, layoutManager,
            onAddColumn,
            onAddTicket = { column ->
                onAddTicket(column)
            },
            onTicketSelected
        )
        val callback = BoardItemTouchHelperCallback(boardRecyclerView.adapter as BoardAdapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(boardRecyclerView)

    }

    @RequiresApi(Build.VERSION_CODES.O)
    val onTicketClick: (Ticket) -> Unit = { ticket ->
        showTicketDialog(
            ticketToEdit = ticket,
            onTicketAction = { title, description, dueDate, warningTime, _ ->
                // Update the ticket
                ticket.title = title
                ticket.description = description
                ticket.dueDate = dueDate
                ticket.warningTime = warningTime

                (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
                boardRecyclerView.adapter?.notifyDataSetChanged()
            }
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    val onAddTicket: (Column) -> Unit = { column ->
        showTicketDialog { ticketTitle, ticketDescription, dueDate, warnigTime, completed->
            val newTicket =
                Ticket(UUID.randomUUID(), ticketTitle, ticketDescription, dueDate, warnigTime, completed)
            column.tickets.add(newTicket)
            (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
            boardRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    val onTicketSelected: (Ticket) -> Unit = { ticket ->
        showTicketDetailsDialog(
            ticket,
            onTicketDeleted = {
                // Remove the ticket from the corresponding column
                columns.find { column ->
                    column.tickets.map { t -> t.id }.contains(ticket.id)
                }?.tickets?.remove(ticket)
                (boardRecyclerView.adapter as? BoardAdapter)?.updateItems()
                boardRecyclerView.adapter?.notifyDataSetChanged()

            }
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTicketDialog(
        ticketToEdit: Ticket? = null,
        onTicketAction: (String, String, Instant?, Duration?, Boolean) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_ticket, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.ticketTitleInput)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.ticketDescriptionInput)
        val dueDateTimeInput = dialogView.findViewById<EditText>(R.id.ticketDueDateTimeInput)
        val warningTimeLayout = dialogView.findViewById<LinearLayout>(R.id.warningTimeLayout)
        val warningTimeInput = dialogView.findViewById<EditText>(R.id.ticketWarningTimeInput)
        val warningTimeUnitSpinner =
            dialogView.findViewById<Spinner>(R.id.ticketWarningTimeUnitSpinner)
        val setDueDateSwitch = dialogView.findViewById<Switch>(R.id.ticketSetDueDateSwitch)
        val setWarningTimeSwitch = dialogView.findViewById<Switch>(R.id.ticketSetWarningTimeSwitch)

        dueDateTimeInput.visibility = View.GONE

        ticketToEdit?.dueDate?.let {
            dueDateTimeInput.setText(it.toString())
        }
        ticketToEdit?.title?.let {
            titleInput.setText(it)
        }
        ticketToEdit?.description?.let {
            descriptionInput.setText(it)
        }
        ticketToEdit?.warningTime?.let {
            setWarningTimeSwitch.isChecked = true
            warningTimeLayout.visibility = View.VISIBLE
            warningTimeInput.setText(it.seconds.toString())
        }
        ticketToEdit?.dueDate?.let {
            setDueDateSwitch.isChecked = true
            dueDateTimeInput.visibility = View.VISIBLE
        }



        setDueDateSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dueDateTimeInput.visibility = View.VISIBLE
            } else {
                dueDateTimeInput.visibility = View.GONE
                dueDateTimeInput.setText("")
            }
        }



        setWarningTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                warningTimeLayout.visibility = View.VISIBLE
            } else {
                warningTimeLayout.visibility = View.GONE
                warningTimeInput.setText("")
            }
        }

        val timeUnits = arrayOf("Minutes", "Hours", "Days")
        val spinnerAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timeUnits)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        warningTimeUnitSpinner.adapter = spinnerAdapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Ticket")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dueDateTimeInput.setOnClickListener {
            val dateTimePickerDialog = DateTimePickerDialog(this) { calendar ->
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                dueDateTimeInput.setText(dateFormat.format(calendar.time))
            }
            dateTimePickerDialog.show()
        }


        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled =
                (titleInput.text.toString().trim().isNotBlank()
                        && descriptionInput.text.toString().trim().isNotBlank())
            positiveButton.setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (title.isNotBlank() && description.isNotBlank()) {
                    val dueDate: Instant? = if (setDueDateSwitch.isChecked) {
                        val dueDateTimeString = dueDateTimeInput.text.toString()
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val parsedDate = dateFormat.parse(dueDateTimeString)
                        parsedDate?.toInstant()
                    } else {
                        null
                    }

                    val warningTime: Duration? = if (setWarningTimeSwitch.isChecked) {
                        val inputValue = warningTimeInput.text.toString().toIntOrNull()
                        val selectedUnit = warningTimeUnitSpinner.selectedItem.toString()

                        inputValue?.let { value ->
                            when (selectedUnit) {
                                "Minutes" -> Duration.ofMinutes(value.toLong())
                                "Hours" -> Duration.ofHours(value.toLong())
                                "Days" -> Duration.ofDays(value.toLong())
                                else -> null
                            }
                        }
                    } else {
                        null
                    }

                    val completed = ticketToEdit?.completed ?: false
                    onTicketAction(title, description, dueDate, warningTime, completed)
                    dialog.dismiss()
                }
            }
        }


        titleInput.onTextChange { updatePositiveButtonState(dialog, titleInput, descriptionInput) }
        descriptionInput.onTextChange {
            updatePositiveButtonState(
                dialog,
                titleInput,
                descriptionInput
            )
        }
        dueDateTimeInput.onTextChange{

            val dueDateTimeString = dueDateTimeInput.text.toString()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val parsedDate = dateFormat.parse(dueDateTimeString)
            if (parsedDate == null) {
                setWarningTimeSwitch.visibility = View.GONE
            } else {
                setWarningTimeSwitch.visibility = View.VISIBLE
            }
        }

        dialog.show()
    }


    private fun updatePositiveButtonState(
        dialog: AlertDialog,
        titleInput: EditText,
        descriptionInput: EditText
    ) {
        val title = titleInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            title.isNotBlank() && description.isNotBlank()
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
    val onAddColumn: () -> Unit = {
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showTicketDetailsDialog(
        ticket: Ticket,
        onTicketDeleted: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_ticket_details, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val titleView = dialogView.findViewById<TextView>(R.id.ticketTitle)
        val descriptionView = dialogView.findViewById<TextView>(R.id.ticketDescription)
        val dueDateView = dialogView.findViewById<TextView>(R.id.ticketDueDate)
        val warningTimeView = dialogView.findViewById<TextView>(R.id.ticketWarningTime)
        val editTicketButton = dialogView.findViewById<Button>(R.id.editTicketButton)
        val deleteTicketButton = dialogView.findViewById<Button>(R.id.deleteTicketButton)

        titleView.text = ticket.title
        descriptionView.text = ticket.description

        ticket.dueDate?.let { dueDate ->
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date.from(dueDate))
            dueDateView.text = "Due Date: $formattedDate"
        }

        ticket.warningTime?.let { warningTime ->
            warningTimeView.text = "Warning Time: ${warningTime.toHours()} Minutes"
        }

        editTicketButton.setOnClickListener {
            showTicketDialog(
                ticketToEdit = ticket,
                onTicketAction = { title, description, dueDate, warningTime, _ ->
                    // Update the ticket
                    ticket.title = title
                    ticket.description = description
                    ticket.dueDate = dueDate
                    ticket.warningTime = warningTime

                    boardRecyclerView.adapter?.notifyDataSetChanged()
                }
            )
            dialog.dismiss()
        }


        deleteTicketButton.setOnClickListener {
            onTicketDeleted()
            dialog.dismiss()
        }

        dialog.show()
    }
}

