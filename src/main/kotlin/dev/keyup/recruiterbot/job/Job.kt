package dev.keyup.recruiterbot.job

data class Job(
    val id: Long,
    val title: String,
    val minimumExperience: Int,
    val requiredSkills: Set<String>,
    val cultureKeywords: Set<String>,
    val tagline: String,
)

data class NewJob(
    val title: String,
    val minimumExperience: Int,
    val requiredSkills: Set<String>,
    val cultureKeywords: Set<String>,
    val tagline: String,
)
