package dev.keyup.recruiterbot.web

import dev.keyup.recruiterbot.usage.ApiUsageRecorder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RequestCorrelationFilterTest {
    @Test
    fun `safe IDs are echoed and failed routes are stored as templates`() {
        var recorded: List<Any>? = null
        val filter = RequestCorrelationFilter(object : ApiUsageRecorder {
            override fun record(requestId: String, method: String, routeTemplate: String, statusCode: Int, durationMillis: Long) {
                recorded = listOf(requestId, method, routeTemplate, statusCode, durationMillis)
            }
        })
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/jobs/43/matches"
            addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "smoke.safe-01")
        }
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, rawResponse -> (rawResponse as HttpServletResponse).status = 401 }

        filter.doFilter(request, response, chain)

        assertEquals("smoke.safe-01", response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
        assertEquals(listOf("smoke.safe-01", "GET", "/jobs/{id}/matches", 401), recorded?.take(4))
        assertNull(MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY))
    }

    @Test
    fun `unsafe IDs are replaced with a generated UUID`() {
        val filter = RequestCorrelationFilter(object : ApiUsageRecorder {
            override fun record(requestId: String, method: String, routeTemplate: String, statusCode: Int, durationMillis: Long) = Unit
        })
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/unknown"
            addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "bad id\n")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> })

        assertTrue(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER).orEmpty().matches(Regex("[0-9a-f-]{36}")))
    }
}
