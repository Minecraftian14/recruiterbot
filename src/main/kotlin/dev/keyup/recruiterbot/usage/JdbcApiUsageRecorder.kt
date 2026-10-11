package dev.keyup.recruiterbot.usage

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.time.Clock

@Repository
class JdbcApiUsageRecorder(private val jdbc: JdbcTemplate) : ApiUsageRecorder {
    private val clock = Clock.systemUTC()

    override fun record(
        requestId: String,
        method: String,
        routeTemplate: String,
        statusCode: Int,
        durationMillis: Long
    ) {
        jdbc.update(
            INSERT_EVENT,
            clock.millis(), requestId, method, routeTemplate, statusCode, durationMillis.coerceAtLeast(0),
        )
    }

    private companion object {
        const val INSERT_EVENT = """
            INSERT INTO api_usage_events (completed_at_ms, request_id, http_method, route_template, status_code, duration_ms)
            VALUES (?, ?, ?, ?, ?, ?)
        """
    }
}
