package com.safeticket.reservation.dto;

import com.safeticket.reservation.domain.ReservationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationResponse(
    UUID reservationId,
    ReservationStatus status,
    String message,
    LocalDateTime expiresAt
) {}
