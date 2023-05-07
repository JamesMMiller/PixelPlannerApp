package com.jamesmmiller.pixelplanner

// Column.kt
data class Column(val id: String, val title: String, val tickets: MutableList<Ticket> = mutableListOf())

// Ticket.kt
data class Ticket(val id: String, val title: String, val description: String)
