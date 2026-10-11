package dev.keyup.recruiterbot.usage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.sql.DriverManager
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class UsageStatisticsIntegrationTest @Autowired constructor(
    private val jdbc: JdbcTemplate,
    private val service: UsageStatisticsService,
    private val recorder: ApiUsageRecorder,
) {
    @Test
    fun `usage report aggregates saved events and applies inclusive UTC dates`() {
        jdbc.update("DELETE FROM api_usage_events")
        jdbc.update(
            "INSERT INTO api_usage_events (completed_at_ms, request_id, http_method, route_template, status_code, duration_ms) VALUES (?, ?, ?, ?, ?, ?)",
            LocalDate.parse("2026-02-04").atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            "req-1", "GET", "/jobs/{id}/matches", 200, 12,
        )
        jdbc.update(
            "INSERT INTO api_usage_events (completed_at_ms, request_id, http_method, route_template, status_code, duration_ms) VALUES (?, ?, ?, ?, ?, ?)",
            LocalDate.parse("2026-02-04").atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            "req-2", "GET", "/jobs/{id}/matches", 404, 24,
        )
        val report = service.report(LocalDate.parse("2026-02-04"), LocalDate.parse("2026-02-04"))
        assertEquals(2, report.totalRequests)
        assertEquals(1, report.errorCount)
        assertEquals(50.0, report.errorRatePercent)
        assertEquals(24, report.latencyMillis.p95)
        assertEquals(1, report.routeBreakdown.single().errorCount)
    }

    @Test
    fun `usage report rejects inverted or overlong date ranges`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.report(LocalDate.parse("2026-03-02"), LocalDate.parse("2026-03-01"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            service.report(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-02-01"))
        }
    }

    @Test
    fun `usage event remains readable after opening a new SQLite connection`() {
        recorder.record("persistence-reopen", "GET", "/jobs/{id}/matches", 200, 17)
        DriverManager.getConnection("jdbc:sqlite:./build/recruiterbot-test.db").use { connection ->
            connection.prepareStatement("SELECT route_template FROM api_usage_events WHERE request_id = ?").use { statement ->
                statement.setString(1, "persistence-reopen")
                statement.executeQuery().use { rows ->
                    org.junit.jupiter.api.Assertions.assertTrue(rows.next())
                    assertEquals("/jobs/{id}/matches", rows.getString("route_template"))
                }
            }
        }
    }
}
