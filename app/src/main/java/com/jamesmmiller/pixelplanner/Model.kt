package com.jamesmmiller.pixelplanner

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class Column(val id: UUID, val title: String, val tickets: MutableList<Ticket> = mutableListOf())

data class Ticket(val id: UUID, var title: String, var description: String, var dueDate: Instant? = null, var warningTime: Duration? = null, val completed: Boolean = false){
    @RequiresApi(Build.VERSION_CODES.O)
    fun isAfterWarningTime(): Boolean? {
        return warningTime?.let {
            Instant.now().isAfter(warningInstant())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun warningInstant(): Instant? {
        return dueDate?.let { due ->
            warningTime?.let { warning ->
                due.minus(warning)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun isOverdue(): Boolean {
        return dueDate?.let { due ->
            val currentTime = Instant.now()
            currentTime.isAfter(due)
        } ?: false
    }
}
