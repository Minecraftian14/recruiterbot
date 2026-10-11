package dev.keyup.recruiterbot.web

import dev.keyup.recruiterbot.usage.ApiUsageRecorder
import jakarta.servlet.FilterChain
import jakarta.servlet.Filter
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestCorrelationFilter(private val usageRecorder: ApiUsageRecorder) : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, filterChain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse
        val requestId = httpRequest.getHeader(REQUEST_ID_HEADER)
            ?.takeIf { REQUEST_ID_PATTERN.matches(it) }
            ?: UUID.randomUUID().toString()
        val started = System.nanoTime()
        MDC.put(REQUEST_ID_MDC_KEY, requestId)
        httpResponse.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            val durationMs = ((System.nanoTime() - started) / 1_000_000).coerceAtLeast(0)
            val route = httpRequest.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)?.toString()
                ?: safeKnownRoute(httpRequest.requestURI)
            log.info(
                "request completed method={} route={} status={} durationMs={} requestId={}",
                httpRequest.method,
                route ?: "unmapped",
                httpResponse.status,
                durationMs,
                requestId,
            )
            if (route != null && isRecordable(route)) {
                runCatching {
                    usageRecorder.record(requestId, httpRequest.method, route, httpResponse.status, durationMs)
                }.onFailure { error -> log.warn("Could not persist API usage event requestId={}", requestId, error) }
            }
            MDC.remove(REQUEST_ID_MDC_KEY)
        }
    }

    private fun isRecordable(route: String): Boolean =
        !route.startsWith("/actuator/") &&
            !route.startsWith("/swagger-ui") &&
            !route.startsWith("/v3/api-docs") &&
            route != "/api/v1/admin/usage"

    private fun safeKnownRoute(path: String): String? = when {
        JOB_MATCH_PATH.matches(path) -> "/jobs/{id}/matches"
        CANDIDATE_MATCH_PATH.matches(path) -> "/candidates/{id}/matches"
        path == "/api/v1/admin/usage" -> path
        else -> null
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
        private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")
        private val JOB_MATCH_PATH = Regex("/jobs/-?\\d+/matches")
        private val CANDIDATE_MATCH_PATH = Regex("/candidates/-?\\d+/matches")
        private val log = LoggerFactory.getLogger(RequestCorrelationFilter::class.java)
    }
}
