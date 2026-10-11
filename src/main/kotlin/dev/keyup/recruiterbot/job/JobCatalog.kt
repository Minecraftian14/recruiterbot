package dev.keyup.recruiterbot.job

interface JobCatalog {
    fun findById(id: Long): Job?
    fun findAll(): List<Job>
    fun create(job: NewJob): Job
}
