package dev.keyup.recruiterbot.usage

import org.springframework.format.annotation.DateTimeFormat
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/usage")
class UsageStatisticsController(private val service: UsageStatisticsService) {
    @GetMapping
    @Operation(
        summary = "Read persistent API usage statistics",
        description = "Returns counts and latency aggregates for the inclusive UTC date range. The maximum range is 31 days. This endpoint is admin-only and is excluded from its own report.",
        security = [SecurityRequirement(name = "basicAuth")],
    )
    fun report(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate,
    ): UsageStatisticsResponse = service.report(from, to)
}
