package dev.keyup.recruiterbot.matching

import dev.keyup.recruiterbot.candidate.Candidate
import dev.keyup.recruiterbot.candidate.CandidateAvailability
import dev.keyup.recruiterbot.candidate.CandidateCatalog
import dev.keyup.recruiterbot.candidate.NewCandidate
import dev.keyup.recruiterbot.job.Job
import dev.keyup.recruiterbot.job.JobCatalog
import dev.keyup.recruiterbot.job.NewJob
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MatchingServiceTest {
    private val candidate = Candidate(1, "Alex", 5, CandidateAvailability.IMMEDIATE, setOf("sql"), setOf("curious"), "")
    private val job = Job(9, "Analyst", 3, setOf("sql", "kotlin"), setOf("curious"), "")
    private val service = MatchingService(
        object : CandidateCatalog {
            override fun findById(id: Long) = candidate.takeIf { it.id == id }
            override fun findAll() = listOf(candidate)
            override fun create(candidate: NewCandidate): Candidate = error("This test catalog is read-only")
        },
        object : JobCatalog {
            override fun findById(id: Long) = job.takeIf { it.id == id }
            override fun findAll() = listOf(job)
            override fun create(job: NewJob): Job = error("This test catalog is read-only")
        },
        SimpleMeterRegistry(),
    )

    @Test
    fun `same candidate job pair has the same score in both directions`() {
        val fromJob = service.candidatesForJob(job.id).matches.single().breakdown.score
        val fromCandidate = service.jobsForCandidate(candidate.id).matches.single().breakdown.score
        assertEquals(fromJob, fromCandidate)
    }

    @Test
    fun `unknown resources are distinguished from empty rankings`() {
        assertThrows(MatchingResourceNotFound::class.java) { service.candidatesForJob(404) }
        assertThrows(MatchingResourceNotFound::class.java) { service.jobsForCandidate(404) }
    }
}
