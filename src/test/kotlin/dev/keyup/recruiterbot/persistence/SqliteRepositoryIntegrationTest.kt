package dev.keyup.recruiterbot.persistence

import dev.keyup.recruiterbot.candidate.CandidateCatalog
import dev.keyup.recruiterbot.job.JobCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class SqliteRepositoryIntegrationTest @Autowired constructor(
    private val candidates: CandidateCatalog,
    private val jobs: JobCatalog,
    private val jdbc: JdbcTemplate,
) {
    @Test
    fun `migrations seed all records and map normalized relationships`() {
        assertEquals(15, candidates.findAll().size)
        assertEquals(6, jobs.findAll().size)
        assertEquals(50, jdbc.queryForObject("SELECT COUNT(*) FROM candidate_skills", Int::class.java))
        assertEquals(30, jdbc.queryForObject("SELECT COUNT(*) FROM candidate_traits", Int::class.java))
        assertEquals(16, jdbc.queryForObject("SELECT COUNT(*) FROM job_required_skills", Int::class.java))
        assertEquals(12, jdbc.queryForObject("SELECT COUNT(*) FROM job_culture_keywords", Int::class.java))
        assertEquals(setOf("deduction", "forensics", "pattern-recognition"), candidates.findById(1)?.skills)
        assertEquals(setOf("analytical", "autonomous"), jobs.findById(1)?.cultureKeywords)
        assertNull(candidates.findById(999))
        assertNull(jobs.findById(999))
    }

    @Test
    fun `foreign keys are enabled on application connections`() {
        assertNotNull(candidates.findById(1))
        val failure = assertThrows(DataAccessException::class.java) {
            jdbc.update("INSERT INTO candidate_skills (candidate_id, skill) VALUES (?, ?)", 999, "orphan")
        }
        org.junit.jupiter.api.Assertions.assertTrue(failure.message.orEmpty().contains("FOREIGN KEY"))
    }
}
