package dev.keyup.recruiterbot.candidate

enum class CandidateAvailability(val penalty: Double) {
    IMMEDIATE(0.0),
    TWO_WEEKS(5.0),
    NOT_LOOKING(15.0),
}

data class Candidate(
    val id: Long,
    val name: String,
    val yearsExperience: Int,
    val availability: CandidateAvailability,
    val skills: Set<String>,
    val traits: Set<String>,
    val quirk: String,
)
