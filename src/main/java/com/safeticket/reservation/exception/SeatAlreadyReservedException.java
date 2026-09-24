package com.safeticket.reservation.exception;

public class SeatAlreadyReservedException extends RuntimeException {
    public SeatAlreadyReservedException(String message) {
        super(message);
    }
}
