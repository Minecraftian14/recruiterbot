package dev.keyup.recruiterbot.job

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@Validated
@RestController
class JobController(private val catalog: JobCatalog) {
    @GetMapping("/jobs")
    @Operation(security = [SecurityRequirement(name = "basicAuth")])
    fun list(): JobListResponse = JobListResponse(catalog.findAll())

    @PostMapping("/jobs")
    @Operation(security = [SecurityRequirement(name = "basicAuth")])
    fun create(@Valid @RequestBody request: CreateJobRequest): ResponseEntity<Job> {
        require(request.requiredSkills.none(String::isBlank) && request.cultureKeywords.none(String::isBlank)) {
            "Skills and culture keywords must not contain blank values."
        }
        val job = catalog.create(NewJob(request.title, request.minimumExperience, request.requiredSkills.toSet(), request.cultureKeywords.toSet(), request.tagline))
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(job.id).toUri()
        return ResponseEntity.created(location).body(job)
    }
}

data class JobListResponse(val jobs: List<Job>)

data class CreateJobRequest(
    @field:NotBlank @field:Size(max = 160) val title: String,
    @field:PositiveOrZero val minimumExperience: Int,
    @field:Size(max = 50) val requiredSkills: List<@NotBlank @Size(max = 80) String> = emptyList(),
    @field:Size(max = 50) val cultureKeywords: List<@NotBlank @Size(max = 80) String> = emptyList(),
    @field:Size(max = 500) val tagline: String = "",
)
