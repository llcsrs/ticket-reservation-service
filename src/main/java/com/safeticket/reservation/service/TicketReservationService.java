package com.safeticket.reservation.service;

import com.safeticket.reservation.domain.Reservation;
import com.safeticket.reservation.domain.ReservationStatus;
import com.safeticket.reservation.dto.ReservationRequest;
import com.safeticket.reservation.dto.ReservationResponse;
import com.safeticket.reservation.exception.SeatAlreadyReservedException;
import com.safeticket.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketReservationService {

    private final StringRedisTemplate redisTemplate;
    private final ReservationRepository reservationRepository;

    private static final String LOCK_PREFIX = "seat:lock:";
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(10);

    /**
     * Tenta reservar um assento utilizando um Distributed Lock no Redis.
     * Se o lock for obtido, salva a intenção de reserva no banco como PENDING.
     *
     * Arquitetura:
     * - Uso de Redis (setIfAbsent / SETNX) garante que requisições concorrentes 
     *   para o mesmo assento não gerem double-booking.
     * - TTL de 10 minutos garante que se o usuário não pagar, o assento é liberado.
     * - O método é @Transactional para garantir que a inserção no banco seja atômica.
     */
    @Transactional
    public ReservationResponse reserveSeat(ReservationRequest request) {
        String lockKey = LOCK_PREFIX + request.eventId() + ":" + request.seatNumber();
        String lockValue = request.userId().toString();

        log.info("Attempting to reserve seat {} for event {} by user {}", 
                 request.seatNumber(), request.eventId(), request.userId());

        // SETNX: Seta o valor apenas se a chave não existir. É atômico.
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, LOCK_TIMEOUT);

        if (Boolean.FALSE.equals(lockAcquired)) {
            log.warn("Seat {} for event {} is already reserved or locked", 
                     request.seatNumber(), request.eventId());
            throw new SeatAlreadyReservedException("Assento já encontra-se reservado ou em processo de compra.");
        }

        // Lock obtido com sucesso. Persistir a intenção no banco de dados.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(LOCK_TIMEOUT);

        Reservation reservation = Reservation.builder()
                .eventId(request.eventId())
                .userId(request.userId())
                .seatNumber(request.seatNumber())
                .status(ReservationStatus.PENDING)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Reservation {} created successfully for user {}", savedReservation.getId(), request.userId());

        return new ReservationResponse(
                savedReservation.getId(),
                savedReservation.getStatus(),
                "Reserva garantida por 10 minutos. Efetue o pagamento.",
                savedReservation.getExpiresAt()
        );
    }
}
