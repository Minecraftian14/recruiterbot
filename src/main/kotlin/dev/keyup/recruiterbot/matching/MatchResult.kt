package dev.keyup.recruiterbot.matching

import dev.keyup.recruiterbot.candidate.Candidate
import dev.keyup.recruiterbot.job.Job

data class CandidateMatch(val candidate: Candidate, val breakdown: MatchScoreBreakdown, val reason: String)
data class JobMatch(val job: Job, val breakdown: MatchScoreBreakdown, val reason: String)
data class JobRanking(val job: Job, val matches: List<CandidateMatch>)
data class CandidateRanking(val candidate: Candidate, val matches: List<JobMatch>)
