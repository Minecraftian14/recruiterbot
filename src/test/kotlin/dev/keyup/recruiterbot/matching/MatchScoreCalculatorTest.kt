package dev.keyup.recruiterbot.matching

import dev.keyup.recruiterbot.candidate.Candidate
import dev.keyup.recruiterbot.candidate.CandidateAvailability
import dev.keyup.recruiterbot.job.Job
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MatchScoreCalculatorTest {
    private val calculator = MatchScoreCalculator()
    private val candidate = Candidate(
        1, "Test", 4, CandidateAvailability.TWO_WEEKS,
        setOf("deduction", "forensics"), setOf("analytical"), "",
    )
    private val job = Job(1, "Detective", 4, setOf("deduction", "pattern-recognition"), setOf("analytical", "autonomous"), "")

    @Test
    fun `partial fit applies weights and availability penalty`() {
        val result = calculator.calculate(candidate, job)
        assertEquals(55.0, result.score)
        assertEquals(setOf("deduction"), result.matchedSkills)
        assertEquals(setOf("analytical"), result.matchedCulture)
    }

    @Test
    fun `experience is capped and no minimum is full fit`() {
        val noMinimum = job.copy(minimumExperience = 0)
        assertEquals(55.0, calculator.calculate(candidate, noMinimum).score)
        assertEquals(
            95.0,
            calculator.calculate(
                candidate.copy(yearsExperience = 99),
                job.copy(minimumExperience = 1, requiredSkills = emptySet(), cultureKeywords = emptySet()),
            ).score,
        )
    }

    @Test
    fun `no overlap and below minimum experience still produce a soft score`() {
        val result = calculator.calculate(
            candidate.copy(skills = emptySet(), traits = emptySet(), yearsExperience = 2),
            job.copy(minimumExperience = 4),
        )
        assertEquals(0, result.matchedSkills.size)
        assertEquals(0, result.matchedCulture.size)
        assertEquals(5.0, result.score)
    }

    @Test
    fun `HALF UP rounding uses two decimal places`() {
        val oneSkill = candidate.copy(availability = CandidateAvailability.IMMEDIATE, yearsExperience = 0, skills = setOf("skill-1"), traits = emptySet())
        val manyRequirements = job.copy(
            minimumExperience = 0,
            requiredSkills = (1..16).map { "skill-$it" }.toSet(),
            cultureKeywords = setOf("no-overlap"),
        )
        assertEquals(24.38, calculator.calculate(oneSkill, manyRequirements).score)
    }

    @Test
    fun `score is clamped and rounds to two decimals`() {
        val high = candidate.copy(availability = CandidateAvailability.IMMEDIATE, yearsExperience = 50)
        assertEquals(100.0, calculator.calculate(high, job.copy(requiredSkills = high.skills, cultureKeywords = high.traits)).score)
        val reason = MatchReasonBuilder().build(calculator.calculate(candidate, job))
        assertTrue(reason.contains("4 years vs 4 minimum"))
        assertTrue(reason.contains("available in two weeks"))
    }
}
