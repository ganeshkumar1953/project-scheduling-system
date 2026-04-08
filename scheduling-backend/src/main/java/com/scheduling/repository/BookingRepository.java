package com.scheduling.repository;

import com.scheduling.entity.Booking;
import com.scheduling.entity.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByTeam_Id(Long teamId);
    List<Booking> findBySlot_Id(Long slotId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByTeam_Email(String email);
    boolean existsByTeam_IdAndSlot_IdAndStatusNot(Long teamId, Long slotId, BookingStatus status);

    /**
     * Find the first waitlisted booking for a specific slot, ordered by bookedAt (FIFO).
     * Used for waitlist promotion when a confirmed booking is cancelled.
     */
    Optional<Booking> findFirstBySlot_IdAndStatusOrderByBookedAtAsc(Long slotId, BookingStatus status);
}
