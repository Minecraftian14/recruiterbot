package dev.keyup.recruiterbot.candidate

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI

@Validated
@RestController
class CandidateController(private val catalog: CandidateCatalog) {
    @GetMapping("/candidates")
    fun list(): CandidateListResponse = CandidateListResponse(catalog.findAll())

    @PostMapping("/candidates")
    fun create(@Valid @RequestBody request: CreateCandidateRequest): ResponseEntity<Candidate> {
        require(request.skills.none(String::isBlank) && request.traits.none(String::isBlank)) { "Skills and traits must not contain blank values." }
        val candidate = catalog.create(
            NewCandidate(request.name, request.yearsExperience, request.availability, request.skills.toSet(), request.traits.toSet(), request.quirk),
        )
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(candidate.id).toUri()
        return ResponseEntity.created(location).body(candidate)
    }
}

data class CandidateListResponse(val candidates: List<Candidate>)

data class CreateCandidateRequest(
    @field:NotBlank @field:Size(max = 160) val name: String,
    @field:PositiveOrZero val yearsExperience: Int,
    val availability: CandidateAvailability,
    @field:Size(max = 50) val skills: List<@NotBlank @Size(max = 80) String> = emptyList(),
    @field:Size(max = 50) val traits: List<@NotBlank @Size(max = 80) String> = emptyList(),
    @field:Size(max = 500) val quirk: String = "",
)
