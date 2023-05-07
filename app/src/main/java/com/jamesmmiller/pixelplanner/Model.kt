package com.jamesmmiller.pixelplanner

import java.util.UUID


data class Board(
    val id: String,
    val name: String,
    val columns: MutableList<Column>
)

// Column.kt
data class Column(val id: UUID, val title: String, val tickets: MutableList<Ticket> = mutableListOf())

// Ticket.kt
data class Ticket(val id: UUID, val title: String, val description: String) {
}
