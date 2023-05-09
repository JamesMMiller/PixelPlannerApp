package com.jamesmmiller.pixelplanner

import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Column(val id: UUID, val title: String, val tickets: MutableList<Ticket> = mutableListOf())

data class Ticket(val id: UUID, var title: String, var description: String, var dueDate: Instant? = null, var warningTime: Duration? = null, val completed: Boolean = false)
