package dev.keyup.recruiterbot.matching

data class MatchScoreBreakdown(
    val matchedSkills: Set<String>,
    val requiredSkillCount: Int,
    val candidateYears: Int,
    val minimumYears: Int,
    val matchedCulture: Set<String>,
    val cultureKeywordCount: Int,
    val availabilityPenalty: Double,
    val score: Double,
)
