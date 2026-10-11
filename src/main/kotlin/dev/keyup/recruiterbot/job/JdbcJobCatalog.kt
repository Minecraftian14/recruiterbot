package dev.keyup.recruiterbot.job

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class JdbcJobCatalog(private val jdbc: JdbcTemplate) : JobCatalog {
    override fun findById(id: Long): Job? = jdbc.query(
        SELECT_BY_ID,
        RowMapper { rs, _ ->
            JobRow(rs.getLong("id"), rs.getString("title"), rs.getInt("minimum_experience"), rs.getString("tagline"))
        },
        id,
        ).singleOrNull()?.let { row ->
            val skills = jdbc.query(SKILLS_FOR_JOB, RowMapper { rs, _ -> rs.getString("skill") }, id).toSortedSet()
            val keywords = jdbc.query(CULTURE_FOR_JOB, RowMapper { rs, _ -> rs.getString("keyword") }, id).toSortedSet()
            row.toJob(skills, keywords)
        }

    override fun findAll(): List<Job> = jdbc.query(
        SELECT_ALL,
        RowMapper { rs, _ ->
            JobRow(rs.getLong("id"), rs.getString("title"), rs.getInt("minimum_experience"), rs.getString("tagline"))
        },
    ).let { rows ->
        val skills = jdbc.query(
            "SELECT job_id, skill FROM job_required_skills ORDER BY job_id, skill",
            RowMapper { rs, _ -> rs.getLong("job_id") to rs.getString("skill") },
        ).groupBy({ it.first }, { it.second })
        val keywords = jdbc.query(
            "SELECT job_id, keyword FROM job_culture_keywords ORDER BY job_id, keyword",
            RowMapper { rs, _ -> rs.getLong("job_id") to rs.getString("keyword") },
        ).groupBy({ it.first }, { it.second })
        rows.map { row -> row.toJob(skills[row.id].orEmpty().toSortedSet(), keywords[row.id].orEmpty().toSortedSet()) }
    }

    private fun JobRow.toJob(skills: Set<String>, keywords: Set<String>) =
        Job(id, title, minimumExperience, skills, keywords, tagline)

    private data class JobRow(val id: Long, val title: String, val minimumExperience: Int, val tagline: String)

    private companion object {
        const val SELECT_BY_ID = """
            SELECT id, title, minimum_experience, tagline FROM jobs WHERE id = ?
        """
        const val SELECT_ALL = """
            SELECT id, title, minimum_experience, tagline FROM jobs ORDER BY id
        """
        const val SKILLS_FOR_JOB = """
            SELECT skill FROM job_required_skills WHERE job_id = ? ORDER BY skill
        """
        const val CULTURE_FOR_JOB = """
            SELECT keyword FROM job_culture_keywords WHERE job_id = ? ORDER BY keyword
        """
    }
}
