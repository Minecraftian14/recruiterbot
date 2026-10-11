package dev.keyup.recruiterbot.matching

import jakarta.validation.constraints.Positive
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class MatchingController(
    private val matchingService: MatchingService,
) {
    @GetMapping("/jobs/{id}/matches")
    @Operation(
        summary = "Rank candidates for a job",
        description = "Returns all candidates sorted by the shared match score. The score includes skill, experience, culture, and availability components; the reason describes those components.",
        security = [SecurityRequirement(name = "basicAuth")],
        responses = [ApiResponse(responseCode = "404", description = "No job exists with this ID")],
    )
    fun candidatesForJob(@PathVariable @Positive id: Long): JobMatchesResponse {
        val ranking = matchingService.candidatesForJob(id)
        val matches = ranking.matches.map {
            CandidateMatchDto(it.candidate.id, it.candidate.name, it.breakdown.score, it.reason)
        }
        return JobMatchesResponse(ranking.job.id, ranking.job.title, matches)
    }

    @GetMapping("/candidates/{id}/matches")
    @Operation(
        summary = "Rank jobs for a candidate",
        description = "Returns all jobs sorted by the same candidate/job score used by the job-first endpoint.",
        security = [SecurityRequirement(name = "basicAuth")],
        responses = [ApiResponse(responseCode = "404", description = "No candidate exists with this ID")],
    )
    fun jobsForCandidate(@PathVariable @Positive id: Long): CandidateMatchesResponse {
        val ranking = matchingService.jobsForCandidate(id)
        val matches = ranking.matches.map {
            JobMatchDto(it.job.id, it.job.title, it.breakdown.score, it.reason)
        }
        return CandidateMatchesResponse(ranking.candidate.id, ranking.candidate.name, matches)
    }
}
