package com.jamesmmiller.pixelplanner

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Column(val id: UUID, val title: String, val tickets: MutableList<Ticket> = mutableListOf())

data class Ticket(val id: UUID, val title: String, val description: String, val dueDate: Instant? = null, val warningTime: Duration? = null, val completed: Boolean = false)
