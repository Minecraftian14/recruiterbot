package dev.keyup.recruiterbot.web

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbc: JdbcTemplate,
) {
    @AfterEach
    fun removeCreatedFixtures() {
        jdbc.update("DELETE FROM candidates WHERE name = ?", "API Candidate")
        jdbc.update("DELETE FROM jobs WHERE title = ?", "API Role")
    }

    @Test
    fun `candidate can be created with canonical tokens and listed`() {
        val response = mockMvc.perform(
            post("/candidates").with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"  API Candidate ","yearsExperience":3,"availability":"IMMEDIATE","skills":[" Kotlin ","KOTLIN"],"traits":[" Curious "],"quirk":"  "}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/candidates/")))
            .andExpect(jsonPath("$.name").value("API Candidate"))
            .andExpect(jsonPath("$.skills[0]").value("kotlin"))
            .andReturn()
        val id = Regex("\"id\":(\\d+)").find(response.response.contentAsString)?.groupValues?.get(1)?.toLong()
        requireNotNull(id)
        mockMvc.perform(get("/candidates").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.candidates[?(@.id == $id)]").exists())
    }

    @Test
    fun `job can be created and listed`() {
        val response = mockMvc.perform(
            post("/jobs").with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"  API Role ","minimumExperience":2,"requiredSkills":[" Kotlin "],"cultureKeywords":[" Teamwork "],"tagline":" Ship well "}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/jobs/")))
            .andExpect(jsonPath("$.title").value("API Role"))
            .andExpect(jsonPath("$.requiredSkills[0]").value("kotlin"))
            .andReturn()
        val id = Regex("\"id\":(\\d+)").find(response.response.contentAsString)?.groupValues?.get(1)?.toLong()
        requireNotNull(id)
        val listed = mockMvc.perform(get("/jobs").with(httpBasic("test-user", "test-password")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobs[?(@.id == $id)]").exists())
            .andReturn().response.contentAsString
        assertTrue(listed.contains("API Role"))
    }

    @Test
    fun `invalid creation requests return structured validation errors`() {
        mockMvc.perform(
            post("/candidates").with(httpBasic("test-user", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":" ","yearsExperience":-1,"availability":"IMMEDIATE"}"""),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("invalid_request"))
    }
}
