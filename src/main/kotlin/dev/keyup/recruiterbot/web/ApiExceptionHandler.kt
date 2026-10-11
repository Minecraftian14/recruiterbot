package dev.keyup.recruiterbot.web

import dev.keyup.recruiterbot.matching.MatchingResourceNotFound
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import java.net.URI

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MatchingResourceNotFound::class)
    fun notFound(error: MatchingResourceNotFound, request: HttpServletRequest) =
        problem(HttpStatus.NOT_FOUND, error.message ?: "Resource not found", "resource_not_found", request)

    @ExceptionHandler(
        ConstraintViolationException::class,
        MethodArgumentNotValidException::class,
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
        HttpMessageNotReadableException::class,
    )
    fun badRequest(error: Exception, request: HttpServletRequest) =
        problem(HttpStatus.BAD_REQUEST, "The request contains an invalid value.", "invalid_request", request)

    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgument(error: IllegalArgumentException, request: HttpServletRequest) =
        problem(HttpStatus.BAD_REQUEST, error.message ?: "The request is invalid.", "invalid_request", request)

    @ExceptionHandler(Exception::class)
    fun unexpected(error: Exception, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        logger.error("Unhandled request failure requestId={}", MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY), error)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", "internal_error", request)
    }

    private fun problem(status: HttpStatus, detail: String, code: String, request: HttpServletRequest): ResponseEntity<ProblemDetail> {
        val body = ProblemDetail.forStatusAndDetail(status, detail)
        body.type = URI.create("about:blank")
        body.instance = URI.create(request.requestURI)
        body.setProperty("code", code)
        body.setProperty("requestId", MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY))
        return ResponseEntity.status(status).body(body)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
