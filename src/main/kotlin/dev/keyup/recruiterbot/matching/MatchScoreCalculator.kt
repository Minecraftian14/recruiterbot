package dev.keyup.recruiterbot.matching

import dev.keyup.recruiterbot.candidate.Candidate
import dev.keyup.recruiterbot.job.Job
import java.math.BigDecimal
import java.math.RoundingMode

/** Pure deterministic 70/20/10 score. Availability is a soft penalty. */
class MatchScoreCalculator {
    fun calculate(candidate: Candidate, job: Job): MatchScoreBreakdown {
        val matchedSkills = candidate.skills.intersect(job.requiredSkills).toSortedSet()
        val matchedCulture = candidate.traits.intersect(job.cultureKeywords).toSortedSet()
        val skillFit = if (job.requiredSkills.isEmpty()) 1.0 else matchedSkills.size.toDouble() / job.requiredSkills.size
        val experienceFit = if (job.minimumExperience == 0) {
            1.0
        } else {
            (candidate.yearsExperience.toDouble() / job.minimumExperience).coerceIn(0.0, 1.0)
        }
        val cultureFit = if (job.cultureKeywords.isEmpty()) 1.0 else matchedCulture.size.toDouble() / job.cultureKeywords.size
        val rawScore = (70.0 * skillFit) + (20.0 * experienceFit) + (10.0 * cultureFit) - candidate.availability.penalty
        val roundedScore = BigDecimal.valueOf(rawScore.coerceIn(0.0, 100.0))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        return MatchScoreBreakdown(
            matchedSkills = matchedSkills,
            requiredSkillCount = job.requiredSkills.size,
            candidateYears = candidate.yearsExperience,
            minimumYears = job.minimumExperience,
            matchedCulture = matchedCulture,
            cultureKeywordCount = job.cultureKeywords.size,
            availabilityPenalty = candidate.availability.penalty,
            score = roundedScore,
        )
    }
}
