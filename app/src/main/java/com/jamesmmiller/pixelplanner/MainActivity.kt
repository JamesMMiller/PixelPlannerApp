package com.jamesmmiller.pixelplanner

import android.app.Dialog
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
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
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue


interface TicketDragDropListener {
    fun onTicketMoved(fromColumn: Column?, fromPosition: Int, toColumn: Column?, toPosition: Int)
}

class MainActivity : AppCompatActivity(), TicketDragDropListener {


    private lateinit var boardRecyclerView: RecyclerView
    private val nsdManager by lazy { getSystemService(Context.NSD_SERVICE) as NsdManager }


    @RequiresApi(Build.VERSION_CODES.O)
    private val now = Instant.now().plusSeconds(700)

    private val webserverList = mutableListOf<NsdServiceInfo>()

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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    private lateinit var progressDialog: Dialog

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_webserver -> {
                showAddServerDialog()
                true
            }
            R.id.upload_board -> {
                showUploadDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val servicesToResolve = ConcurrentLinkedQueue<NsdServiceInfo>()
    private val resolvedServices = ConcurrentLinkedQueue<NsdServiceInfo>()

    private fun showAddServerDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.add_server_dialog, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val alertDialog = builder.show()

        val searchButton = dialogView.findViewById<Button>(R.id.search_button)
        val addButton = dialogView.findViewById<Button>(R.id.add_button)
        val serverList = dialogView.findViewById<ListView>(R.id.server_list)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        var discoveryListener: NsdManager.DiscoveryListener? = null

        val serverListAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice)
        serverList.adapter = serverListAdapter
        serverList.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        searchButton.setOnClickListener {
            // Clear any old data
            serverListAdapter.clear()

            // Show spinner and disable button
            searchButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            // Start network discovery
            startNetworkDiscovery(serverListAdapter, searchButton, progressBar)
        }

        addButton.setOnClickListener {
            val checkedItems = serverList.checkedItemPositions
            for (i in 0 until checkedItems.size()) {
                if (checkedItems.valueAt(i)) {
                    val server = serverListAdapter.getItem(checkedItems.keyAt(i))
                    val info = resolvedServices.firstOrNull{ it.serviceName == server }
                    if (info != null) {
                        webserverList.add(info)
                    } else {
                        // Handle the case where no matching service was found
                        // (e.g., show a message to the user or log an error)
                        Log.e("WEBSERVER", "No matching service found for $server in $resolvedServices")
                    }
                }
            }
            alertDialog.dismiss()
        }


        serverList.setOnItemClickListener { _, _, _, _ ->
            // Enable the "add" button once at least one item is checked
            addButton.isEnabled = serverList.checkedItemCount > 0
        }

        alertDialog.setOnCancelListener {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        }
    }

    private fun startNetworkDiscovery(serverListAdapter: ArrayAdapter<String>, searchButton: Button, progressBar: ProgressBar) {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("NSD", "Service discovery started")
                servicesToResolve.clear()
                resolvedServices.clear()
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("NSD", "Service found, serviceInfo: $serviceInfo")

                // Add service to queue
                servicesToResolve.add(serviceInfo)

                // Add delay before starting resolution
                Handler(Looper.getMainLooper()).postDelayed({
                    resolveNextService(serverListAdapter)
                }, 1000) // Delay the resolve by 1 second
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.e("NSD", "service lost: $serviceInfo")
                runOnUiThread {
                    serverListAdapter.remove(serviceInfo.serviceName)
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i("NSD", "Discovery stopped: $serviceType")
                runOnUiThread {
                    searchButton.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
                startNetworkDiscovery(serverListAdapter, searchButton, progressBar) // Restart the discovery
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("NSD", "Discovery failed: Error code:$errorCode")
                nsdManager.stopServiceDiscovery(this)
                startNetworkDiscovery(serverListAdapter, searchButton, progressBar) // Restart the discovery
            }
        }

        nsdManager.discoverServices(
            "_http._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )

        // Stop discovery after 30 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            nsdManager.stopServiceDiscovery(discoveryListener)
        }, 30000) // Delay the stop by 30 seconds
    }

    private fun resolveNextService(serverListAdapter: ArrayAdapter<String>) {
        if (servicesToResolve.isNotEmpty()) {
            val serviceInfo = servicesToResolve.poll()

            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    Log.d("NSD", "Service resolved: $serviceInfo")
                    runOnUiThread {
                        serverListAdapter.add(serviceInfo.serviceName)
                    }
                    resolvedServices.add(serviceInfo)
                    // Resolve next service
                    resolveNextService(serverListAdapter)
                }

                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e("NSD", "Resolve failed: $errorCode")
                    // Even if the resolve failed, try to resolve next service
                    resolveNextService(serverListAdapter)
                }
            })
        }
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
    private fun showUploadDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.upload_dialog, null)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val alertDialog = builder.show()

        val uploadButton = dialogView.findViewById<Button>(R.id.upload_columns_button)
        val columnList = dialogView.findViewById<ListView>(R.id.columns_list)
        val serverList = dialogView.findViewById<ListView>(R.id.webserver_list)

        val columnListAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, columns.map { it.title })
        columnList.adapter = columnListAdapter
        columnList.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        val serverListAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, webserverList.map { it.serviceName })
        serverList.adapter = serverListAdapter
        serverList.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        uploadButton.setOnClickListener {
            val selectedColumns = mutableListOf<Column>()
            val selectedWebservers = mutableListOf<NsdServiceInfo>()

            for (i in 0 until columnList.count) {
                if (columnList.isItemChecked(i)) {
                    val columnTitle = columnListAdapter.getItem(i)!!
                    val column = columns.find { it.title == columnTitle }!!
                    selectedColumns.add(column)
                }
            }

            for (i in 0 until serverList.count) {
                if (serverList.isItemChecked(i)) {
                    val serviceName = serverListAdapter.getItem(i)!!
                    val webserver = webserverList.first { it.serviceName == serviceName }
                    selectedWebservers.add(webserver)
                }
            }

            for ((index, column) in selectedColumns.withIndex()) {
                val isFirst = index == 0
                val isLast = index == selectedColumns.lastIndex

                selectedWebservers.forEach { webserver ->
                    postColumn(column, isFirst, isLast, webserver)
                }
            }

            alertDialog.dismiss()
        }
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
            dueDateTimeInput.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                dueDateTimeInput.setText("")
            }
        }

        setWarningTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            warningTimeLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
            warningTimeInput.setText("")
        }

        setupWarningTimeUnitSpinner(warningTimeUnitSpinner)

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

        setupDialogListeners(
            dialog,
            titleInput,
            descriptionInput,
            dueDateTimeInput,
            warningTimeInput,
            warningTimeUnitSpinner,
            setDueDateSwitch,
            setWarningTimeSwitch,
            ticketToEdit,
            onTicketAction
        )

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun postColumn(column: Column, isFirst: Boolean, isLast: Boolean, webserver: NsdServiceInfo) {
        Log.d("postColumn", "Posting column ${column.title} to ${webserver.serviceName}")
        Log.d("postColumn", webserver.toString())
        val url = "http:/${webserver.host}/column"

        Log.d("postColumn", "Url: $url")

        val client = OkHttpClient()

        val columnJson = JSONObject().apply {
            put("id", column.id.toString())
            put("title", column.title)
            put("first", isFirst)
            put("last", isLast)
            put("tickets", JSONArray().apply {
                column.tickets.forEach { ticket ->
                    this.put(JSONObject().apply {
                        put("id", ticket.id.toString())
                        put("title", ticket.title)
                        put("description", ticket.description)
                        put("dueDate", ticket.dueDate?.epochSecond)
                        put("warningTime", ticket.warningTime?.seconds)
                        put("completed", ticket.completed)
                    })
                }
            })
        }

        Log.d("columnJson", columnJson.toString())

        val requestBody = columnJson.toString().toRequestBody("application/json".toMediaType())

        Log.d("requestBody", requestBody.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        Log.d("request", request.toString())

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                Log.d("response", response.toString())
            }

            override fun onFailure(call: Call, e: IOException) {
                Log.d("Error response", e.toString())
            }
        })
    }


    private fun setupWarningTimeUnitSpinner(warningTimeUnitSpinner: Spinner) {
        val timeUnits = arrayOf("Minutes", "Hours", "Days")
        val spinnerAdapter =
            ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, timeUnits)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        warningTimeUnitSpinner.adapter = spinnerAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDialogListeners(
        dialog: AlertDialog,
        titleInput: EditText,
        descriptionInput: EditText,
        dueDateTimeInput: EditText,
        warningTimeInput: EditText,
        warningTimeUnitSpinner: Spinner,
        setDueDateSwitch: Switch,
        setWarningTimeSwitch: Switch,
        ticketToEdit: Ticket?,
        onTicketAction: (String, String, Instant?, Duration?, Boolean) -> Unit
    ) {
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled =
                (titleInput.text.toString().trim().isNotBlank()
                        && descriptionInput.text.toString().trim().isNotBlank())
            positiveButton.setOnClickListener {
                val title = titleInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                val dueDate: Instant? = if (setDueDateSwitch.isChecked) {
                    val dueDateTimeString = dueDateTimeInput.text.toString()
                    val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                    val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                    val parsedDate = try {
                        isoDateFormat.parse(dueDateTimeString)
                    } catch (e: ParseException) {
                        inputDateFormat.parse(dueDateTimeString)
                    }
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

        titleInput.onTextChange { updatePositiveButtonState(dialog, titleInput) }
        dueDateTimeInput.onTextChange {
            updateWarningTimeSwitchVisibility(dueDateTimeInput, setWarningTimeSwitch)
        }
    }

    private fun updatePositiveButtonState(
        dialog: AlertDialog,
        titleInput: EditText
    ) {
        val title = titleInput.text.toString().trim()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = title.isNotBlank()
    }

    private fun updateWarningTimeSwitchVisibility(dueDateTimeInput: EditText, setWarningTimeSwitch: Switch) {
        val dueDateTimeString = dueDateTimeInput.text.toString()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val parsedDate = dateFormat.parse(dueDateTimeString)
        setWarningTimeSwitch.visibility = if (parsedDate == null) View.GONE else View.VISIBLE
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

        ticket.warningTime?.let { _ ->
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date.from(ticket.warningInstant()))
            warningTimeView.text = "Warning Time: $formattedDate"
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

