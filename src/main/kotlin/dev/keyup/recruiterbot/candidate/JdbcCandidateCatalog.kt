package dev.keyup.recruiterbot.candidate

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Repository
class JdbcCandidateCatalog(private val jdbc: JdbcTemplate) : CandidateCatalog {
    @Transactional
    override fun create(candidate: NewCandidate): Candidate {
        val skills = candidate.skills.canonicalTokens()
        val traits = candidate.traits.canonicalTokens()
        jdbc.update(
            "INSERT INTO candidates(name, years_experience, availability, quirk) VALUES (?, ?, ?, ?)",
            candidate.name.trim(), candidate.yearsExperience, candidate.availability.name, candidate.quirk.trim(),
        )
        val id = requireNotNull(jdbc.queryForObject<Long>("SELECT last_insert_rowid()"))
        skills.forEach { jdbc.update("INSERT INTO candidate_skills(candidate_id, skill) VALUES (?, ?)", id, it) }
        traits.forEach { jdbc.update("INSERT INTO candidate_traits(candidate_id, trait) VALUES (?, ?)", id, it) }
        return Candidate(
            id,
            candidate.name.trim(),
            candidate.yearsExperience,
            candidate.availability,
            skills,
            traits,
            candidate.quirk.trim()
        )
    }

    override fun findById(id: Long): Candidate? = jdbc.query(
        SELECT_BY_ID,
        { rs, _ ->
            CandidateRow(
                rs.getLong("id"), rs.getString("name"), rs.getInt("years_experience"),
                CandidateAvailability.valueOf(rs.getString("availability")), rs.getString("quirk"),
            )
        },
        id,
    ).singleOrNull()?.let { row ->
        val skills = jdbc.query(SKILLS_FOR_CANDIDATE, { rs, _ -> rs.getString("skill") }, id).toSortedSet()
        val traits = jdbc.query(TRAITS_FOR_CANDIDATE, { rs, _ -> rs.getString("trait") }, id).toSortedSet()
        row.toCandidate(skills, traits)
    }

    override fun findAll(): List<Candidate> = jdbc.query(
        SELECT_ALL,
    ) { rs, _ ->
        CandidateRow(
            rs.getLong("id"), rs.getString("name"), rs.getInt("years_experience"),
            CandidateAvailability.valueOf(rs.getString("availability")), rs.getString("quirk"),
        )
    }.let { rows ->
        val skills = jdbc.query(
            "SELECT candidate_id, skill FROM candidate_skills ORDER BY candidate_id, skill",
        ) { rs, _ -> rs.getLong("candidate_id") to rs.getString("skill") }.groupBy({ it.first }, { it.second })
        val traits = jdbc.query(
            "SELECT candidate_id, trait FROM candidate_traits ORDER BY candidate_id, trait",
        ) { rs, _ -> rs.getLong("candidate_id") to rs.getString("trait") }.groupBy({ it.first }, { it.second })
        rows.map { row ->
            row.toCandidate(
                skills[row.id].orEmpty().toSortedSet(),
                traits[row.id].orEmpty().toSortedSet()
            )
        }
    }

    private fun CandidateRow.toCandidate(skills: Set<String>, traits: Set<String>) =
        Candidate(id, name, yearsExperience, availability, skills, traits, quirk)

    private data class CandidateRow(
        val id: Long,
        val name: String,
        val yearsExperience: Int,
        val availability: CandidateAvailability,
        val quirk: String,
    )

    private companion object {
        const val SELECT_BY_ID = """
            SELECT id, name, years_experience, availability, quirk
            FROM candidates WHERE id = ?
        """
        const val SELECT_ALL = """
            SELECT id, name, years_experience, availability, quirk
            FROM candidates ORDER BY id
        """
        const val SKILLS_FOR_CANDIDATE = """
            SELECT skill FROM candidate_skills WHERE candidate_id = ? ORDER BY skill
        """
        const val TRAITS_FOR_CANDIDATE = """
            SELECT trait FROM candidate_traits WHERE candidate_id = ? ORDER BY trait
        """
    }
}

private fun Set<String>.canonicalTokens(): Set<String> =
    map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSortedSet()
