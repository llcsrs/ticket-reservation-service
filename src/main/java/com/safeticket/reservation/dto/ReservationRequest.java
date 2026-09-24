package com.safeticket.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReservationRequest(
    @NotNull(message = "Event ID is required") UUID eventId,
    @NotNull(message = "User ID is required") UUID userId,
    @NotBlank(message = "Seat number is required") String seatNumber
) {}
