package dev.keyup.recruiterbot.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatchingApiIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbc: JdbcTemplate,
) {
    @Test
    fun `job match endpoint returns ranked candidates with reasons`() {
        val before = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ?",
            "/jobs/{id}/matches",
        ) ?: 0L
        mockMvc.perform(get("/jobs/1/matches").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isOk)
            .andExpect(header().exists("X-Request-ID"))
            .andExpect(jsonPath("$.jobId").value(1))
            .andExpect(jsonPath("$.matches.length()").value(15))
            .andExpect(jsonPath("$.matches[0].score").isNumber)
            .andExpect(jsonPath("$.matches[0].reason").isString)
        val after = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ?",
            "/jobs/{id}/matches",
        ) ?: 0L
        org.junit.jupiter.api.Assertions.assertEquals(before + 1, after)
    }

    @Test
    fun `candidate match endpoint returns ranked jobs`() {
        mockMvc.perform(get("/candidates/1/matches").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.candidateId").value(1))
            .andExpect(jsonPath("$.matches.length()").value(6))
            .andExpect(jsonPath("$.matches[0].score").isNumber)
    }

    @Test
    fun `missing IDs and invalid IDs use the shared error format`() {
        mockMvc.perform(get("/jobs/999/matches").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("resource_not_found"))
            .andExpect(jsonPath("$.requestId").isString)

        mockMvc.perform(get("/jobs/0/matches").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("invalid_request"))
    }

    @Test
    fun `authentication and documentation access follow the policy`() {
        val beforeUnauthorized = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ? AND status_code = ?",
            "/jobs/{id}/matches",
            401,
        ) ?: 0L
        mockMvc.perform(get("/jobs/1/matches"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("unauthorized"))
        val afterUnauthorized = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ? AND status_code = ?",
            "/jobs/{id}/matches",
            401,
        ) ?: 0L
        org.junit.jupiter.api.Assertions.assertEquals(beforeUnauthorized + 1, afterUnauthorized)

        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.paths['/jobs/{id}/matches'].get").exists())
            .andExpect(jsonPath("$.components.securitySchemes.basicAuth.scheme").value("basic"))

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().is3xxRedirection)
    }

    @Test
    fun `usage and operational metrics require admin role`() {
        val usageEventsBefore = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ?",
            "/api/v1/admin/usage",
        ) ?: 0L
        mockMvc.perform(get("/api/v1/admin/usage?from=2026-10-11&to=2026-10-11").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("forbidden"))
        mockMvc.perform(get("/api/v1/admin/usage?from=2026-10-11&to=2026-10-11").with(httpBasic("test-admin", "test-admin-password")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalRequests").isNumber)
        mockMvc.perform(
            get("/api/v1/admin/usage?from=not-a-date&to=2026-10-11")
                .with(httpBasic("test-admin", "test-admin-password")),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("invalid_request"))

        mockMvc.perform(get("/actuator/metrics").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isForbidden)
        mockMvc.perform(get("/actuator/metrics").with(httpBasic("test-admin", "test-admin-password")))
            .andExpect(status().isOk)
        mockMvc.perform(get("/jobs/1/matches").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isOk)
        mockMvc.perform(
            get("/actuator/metrics/recruiterbot.match.requests")
                .with(httpBasic("test-admin", "test-admin-password")),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("recruiterbot.match.requests"))
            .andExpect(jsonPath("$.availableTags[0].tag").value("direction"))
        val usageEventsAfter = jdbc.queryForObject<Long>(
            "SELECT COUNT(*) FROM api_usage_events WHERE route_template = ?",
            "/api/v1/admin/usage",
        ) ?: 0L
        org.junit.jupiter.api.Assertions.assertEquals(usageEventsBefore, usageEventsAfter)
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }
}
