package dev.keyup.recruiterbot.matching

class MatchReasonBuilder {
    fun build(breakdown: MatchScoreBreakdown): String {
        val skillText = "${breakdown.matchedSkills.size}/${breakdown.requiredSkillCount} required skills"
        val experienceText = "${breakdown.candidateYears} years vs ${breakdown.minimumYears} minimum"
        val cultureText = "${breakdown.matchedCulture.size}/${breakdown.cultureKeywordCount} culture keywords"
        val availabilityText = when (breakdown.availabilityPenalty) {
            0.0 -> "immediately available"
            5.0 -> "available in two weeks (-5 points)"
            else -> "not looking (-${breakdown.availabilityPenalty.toInt()} points)"
        }
        return "$skillText; $experienceText; $cultureText; $availabilityText."
    }
}
