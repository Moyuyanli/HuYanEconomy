package cn.chahuyun.economy.entity.common

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Centralized UTC clock and presentation helpers for new entities. */
object EntityTime {
    private val clock: Clock = Clock.systemUTC()

    fun now(): Instant = Instant.now(clock)

    fun Instant.toDisplay(zone: ZoneId, formatter: DateTimeFormatter): String =
        atZone(zone).format(formatter)
}
