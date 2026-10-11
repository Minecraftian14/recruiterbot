package dev.keyup.recruiterbot.web

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

object ApiProblemWriter {
    fun write(response: HttpServletResponse, status: HttpStatus, detail: String, code: String) {
        val requestId = MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY).orEmpty()
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(
            """{"type":"about:blank","title":"${status.reasonPhrase}","status":${status.value()},"detail":"$detail","code":"$code","requestId":"$requestId"}""",
        )
    }
}
