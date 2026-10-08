package io.miragon.blueprint.adapter.inbound.rest

import mu.KotlinLogging
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

/**
 * Cross-cutting web error handling for the REST adapter: turns domain and input errors into RFC 9457
 * `application/problem+json` responses (Spring serialises [ProblemDetail] as such automatically).
 *
 * This is web configuration, hence the `Configuration` suffix whitelisted for `adapter.inbound.rest`
 * and its home here rather than in a new top-level `config` package (which the architecture tests
 * would reject).
 */
@RestControllerAdvice
class GlobalExceptionConfiguration {

    private val log = KotlinLogging.logger {}

    /** Invalid input — a bad UUID, a blank value object, an unknown status filter. */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ProblemDetail {
        log.debug { "Bad request: ${exception.message}" }
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.message)
    }

    /** Unknown aggregate — the services signal a missing application with [IllegalStateException]. */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(exception: IllegalStateException): ProblemDetail {
        log.debug { "Unprocessable: ${exception.message}" }
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.message)
    }

    /** Action not available — the process token is no longer at the expected wait state. */
    @ExceptionHandler(MismatchingMessageCorrelationException::class)
    fun handleMismatchingCorrelation(exception: MismatchingMessageCorrelationException): ProblemDetail {
        log.debug { "Mismatching correlation: ${exception.message}" }
        return problem(HttpStatus.CONFLICT, "Action not available in the current state", exception.message)
    }

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail =
        ProblemDetail.forStatus(status).apply {
            this.title = title
            this.detail = detail
            this.type = URI.create("https://miravelo.example/problems/${status.value()}")
        }
}
