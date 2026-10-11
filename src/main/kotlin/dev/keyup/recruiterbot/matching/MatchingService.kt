package dev.keyup.recruiterbot.matching

import dev.keyup.recruiterbot.candidate.CandidateCatalog
import dev.keyup.recruiterbot.job.JobCatalog
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class MatchingService(
    private val candidates: CandidateCatalog,
    private val jobs: JobCatalog,
    private val meters: MeterRegistry,
) {
    private val calculator = MatchScoreCalculator()
    private val reasonBuilder = MatchReasonBuilder()

    fun candidatesForJob(jobId: Long): JobRanking {
        meters.counter("recruiterbot.match.requests", "direction", "job").increment()
        val started = System.nanoTime()
        try {
            val job = jobs.findById(jobId) ?: throw MatchingResourceNotFound("Job", jobId)
            val matches = candidates.findAll().map { candidate ->
                val breakdown = calculator.calculate(candidate, job)
                CandidateMatch(candidate, breakdown, reasonBuilder.build(breakdown))
            }.sortedWith(
                compareByDescending<CandidateMatch> { it.breakdown.score }
                    .thenByDescending { it.breakdown.matchedSkills.size }
                    .thenByDescending { it.breakdown.matchedCulture.size }
                    .thenBy { it.candidate.name.lowercase() }
                    .thenBy { it.candidate.id },
            )
            meters.counter("recruiterbot.match.results", "direction", "job").increment(matches.size.toDouble())
            return JobRanking(job, matches)
        } finally {
            Timer.builder("recruiterbot.match.calculation").tag("direction", "job").register(meters)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS)
        }
    }

    fun jobsForCandidate(candidateId: Long): CandidateRanking {
        meters.counter("recruiterbot.match.requests", "direction", "candidate").increment()
        val started = System.nanoTime()
        try {
            val candidate = candidates.findById(candidateId)
                ?: throw MatchingResourceNotFound("Candidate", candidateId)
            val matches = jobs.findAll().map { job ->
                val breakdown = calculator.calculate(candidate, job)
                JobMatch(job, breakdown, reasonBuilder.build(breakdown))
            }.sortedWith(
                compareByDescending<JobMatch> { it.breakdown.score }
                    .thenByDescending { it.breakdown.matchedSkills.size }
                    .thenByDescending { it.breakdown.matchedCulture.size }
                    .thenBy { it.job.title.lowercase() }
                    .thenBy { it.job.id },
            )
            meters.counter("recruiterbot.match.results", "direction", "candidate").increment(matches.size.toDouble())
            return CandidateRanking(candidate, matches)
        } finally {
            Timer.builder("recruiterbot.match.calculation").tag("direction", "candidate").register(meters)
                .record(System.nanoTime() - started, TimeUnit.NANOSECONDS)
        }
    }
}

class MatchingResourceNotFound(val resource: String, val id: Long) : RuntimeException("$resource $id was not found")
