package dev.keyup.recruiterbot.matching

data class JobMatchesResponse(val jobId: Long, val jobTitle: String, val matches: List<CandidateMatchDto>)
data class CandidateMatchesResponse(val candidateId: Long, val candidateName: String, val matches: List<JobMatchDto>)

data class CandidateMatchDto(
    val candidateId: Long,
    val candidateName: String,
    val score: Double,
    val reason: String,
)

data class JobMatchDto(
    val jobId: Long,
    val jobTitle: String,
    val score: Double,
    val reason: String,
)
