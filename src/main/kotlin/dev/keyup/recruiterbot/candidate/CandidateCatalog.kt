package dev.keyup.recruiterbot.candidate

interface CandidateCatalog {
    fun findById(id: Long): Candidate?
    fun findAll(): List<Candidate>
    fun create(candidate: NewCandidate): Candidate
}
