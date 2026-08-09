package com.turntalienn.mytrade.runner.analytics;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = PortfolioAnalyticsController.class)
class AnalyticsExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
    ProblemDetail handleInvalidDomainInput(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid analytics request");
        return problem;
    }
}
