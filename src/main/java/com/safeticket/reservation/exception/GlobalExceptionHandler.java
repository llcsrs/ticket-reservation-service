package com.safeticket.reservation.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatAlreadyReservedException.class)
    public ProblemDetail handleSeatAlreadyReservedException(SeatAlreadyReservedException ex) {
        // Usando ProblemDetail, padrão do Spring 3 (RFC 7807) para erros em APIs REST
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Seat Already Reserved");
        problemDetail.setType(URI.create("https://safeticket.com/errors/seat-reserved"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
