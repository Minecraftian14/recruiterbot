package dev.keyup.recruiterbot.usage

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

@Service
class UsageStatisticsService(private val jdbc: JdbcTemplate) {
    fun report(from: LocalDate, to: LocalDate): UsageStatisticsResponse {
        require(!from.isAfter(to)) { "The from date must not be after the to date." }
        require(ChronoUnit.DAYS.between(from, to) <= MAX_INCLUSIVE_DAYS - 1) {
            "The requested date range must be at most $MAX_INCLUSIVE_DAYS days."
        }
        val startMillis = from.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endExclusiveMillis = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val events = jdbc.query(
            SELECT_EVENTS_IN_RANGE,
            { rs, _ ->
                UsageRow(rs.getString("route_template"), rs.getInt("status_code"), rs.getLong("duration_ms"))
            },
            startMillis,
            endExclusiveMillis,
        )
        val statuses = events.groupingBy { it.statusCode }.eachCount().toSortedMap()
            .map { (status, count) -> StatusUsage(status, count.toLong()) }
        val routes = events.groupBy { it.routeTemplate }.toSortedMap()
            .map { (route, rows) ->
                RouteUsage(
                    route,
                    rows.size.toLong(),
                    rows.count { it.statusCode >= 400 }.toLong()
                )
            }
        val durations = events.map { it.durationMillis }.sorted()
        val errorCount = events.count { it.statusCode >= 400 }.toLong()
        val latency = if (durations.isEmpty()) {
            LatencyUsage(0.0, 0, 0)
        } else {
            val p95Index = (ceil(durations.size * 0.95).toInt() - 1).coerceAtLeast(0)
            LatencyUsage(durations.average(), durations[p95Index], durations.last())
        }
        val errorRate = if (events.isEmpty()) 0.0 else errorCount * 100.0 / events.size
        return UsageStatisticsResponse(
            from.toString(),
            to.toString(),
            events.size.toLong(),
            errorCount,
            errorRate,
            statuses,
            routes,
            latency
        )
    }

    private data class UsageRow(val routeTemplate: String, val statusCode: Int, val durationMillis: Long)

    private companion object {
        const val MAX_INCLUSIVE_DAYS = 31L
        const val SELECT_EVENTS_IN_RANGE = """
            SELECT route_template, status_code, duration_ms
            FROM api_usage_events
            WHERE completed_at_ms >= ? AND completed_at_ms < ?
            ORDER BY completed_at_ms, id
        """
    }
}
