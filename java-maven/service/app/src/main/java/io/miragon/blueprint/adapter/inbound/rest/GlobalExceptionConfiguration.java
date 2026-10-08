package io.miragon.blueprint.adapter.inbound.rest;

import java.net.URI;
import org.cibseven.bpm.engine.MismatchingMessageCorrelationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Cross-cutting web error handling for the REST adapter: turns domain and input errors into RFC 9457
 * {@code application/problem+json} responses (Spring serialises {@link ProblemDetail} as such automatically).
 *
 * <p>This is web configuration, hence the {@code Configuration} suffix whitelisted for {@code adapter.inbound.rest}
 * and its home here rather than in a new top-level {@code config} package (which the architecture tests
 * would reject).
 */
@RestControllerAdvice
public class GlobalExceptionConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionConfiguration.class);

    /** Invalid input — a bad UUID, a blank value object, an unknown status filter. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        log.debug("Bad request: {}", exception.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
    }

    /** Unknown aggregate — the services signal a missing application with {@link IllegalStateException}. */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException exception) {
        log.debug("Unprocessable: {}", exception.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    /** Action not available — the process token is no longer at the expected wait state. */
    @ExceptionHandler(MismatchingMessageCorrelationException.class)
    public ProblemDetail handleMismatchingCorrelation(MismatchingMessageCorrelationException exception) {
        log.debug("Mismatching correlation: {}", exception.getMessage());
        return problem(HttpStatus.CONFLICT, "Action not available in the current state", exception.getMessage());
    }

    /**
     * @param detail the problem detail; {@code null} when the exception carries no message
     */
    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setType(URI.create("https://miravelo.example/problems/" + status.value()));
        return problem;
    }
}
