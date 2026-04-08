package com.scheduling.controller;

import com.scheduling.dto.BookingDTO;
import com.scheduling.dto.SlotDTO;
import com.scheduling.entity.Booking;
import com.scheduling.entity.Booking.BookingStatus;
import com.scheduling.entity.Slot;
import com.scheduling.entity.Slot.SlotStatus;
import com.scheduling.entity.Team;
import com.scheduling.repository.BookingRepository;
import com.scheduling.repository.SlotRepository;
import com.scheduling.repository.TeamRepository;
import com.scheduling.service.AdminService;
import com.scheduling.service.EmailService;
import com.scheduling.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final TeamRepository teamRepository;
    private final AdminService adminService;
    private final ScheduleService scheduleService;
    private final EmailService emailService;

    @GetMapping("/available")
    public ResponseEntity<List<SlotDTO>> getAvailableSlots() {
        return ResponseEntity.ok(scheduleService.getAvailableSlots());
    }

    @GetMapping("/all")
    public ResponseEntity<List<SlotDTO>> getAllSlots() {
        return ResponseEntity.ok(scheduleService.getAllSlots());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<BookingDTO> bookSlot(@Valid @RequestBody BookingDTO dto) {
        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + dto.getTeamId()));
        Slot slot = slotRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + dto.getSlotId()));

        // Prevent double-confirmed booking for same team across ANY slots
        boolean alreadyConfirmed = bookingRepository.findByTeam_Id(team.getId()).stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED);
        if (alreadyConfirmed) {
            throw new IllegalArgumentException("Team already has a confirmed booking. Cancel or reschedule first.");
        }

        // Prevent double-booking (or double-waitlisting) the exact same slot
        if (bookingRepository.existsByTeam_IdAndSlot_IdAndStatusNot(team.getId(), slot.getId(), BookingStatus.CANCELLED)) {
             throw new IllegalArgumentException("You have already booked or waitlisted this slot.");
        }

        BookingStatus status;
        if (slot.getStatus() == SlotStatus.AVAILABLE) {
            slot.setStatus(SlotStatus.BOOKED);
            slotRepository.save(slot);
            status = BookingStatus.CONFIRMED;
        } else if (slot.getStatus() == SlotStatus.BOOKED) {
            status = BookingStatus.WAITLISTED;
        } else {
            throw new IllegalArgumentException("Slot is cancelled and not available for booking.");
        }

        Booking booking = Booking.builder()
                .team(team)
                .slot(slot)
                .status(status)
                .bookedAt(LocalDateTime.now())
                .build();
        BookingDTO result = adminService.mapToDTO(bookingRepository.save(booking));

        // Send email notification (async, non-blocking)
        try {
            String slotTime = slot.getStartTime() + " – " + slot.getEndTime();
            String date = slot.getScheduleDate().getDemoDate().toString();
            emailService.sendBookingConfirmation(
                    team.getEmail(),
                    team.getProjectName(),
                    date,
                    slotTime,
                    status.name()
            );
        } catch (Exception e) {
            // Email failure must never break booking flow
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/reschedule")
    @Transactional
    public ResponseEntity<BookingDTO> reschedule(@PathVariable Long id, @RequestBody BookingDTO dto) {
        Booking existing = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        if (existing.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot reschedule a cancelled booking.");
        }

        Slot newSlot = slotRepository.findById(dto.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("New slot not found: " + dto.getSlotId()));

        // Use primitive comparison to avoid JPA proxy / Long boxing issues
        long existingSlotId = existing.getSlot().getId().longValue();
        long newSlotIdVal = newSlot.getId().longValue();
        if (existingSlotId == newSlotIdVal) {
             throw new IllegalArgumentException("Cannot reschedule to the exact same slot.");
        }

        if (bookingRepository.existsByTeam_IdAndSlot_IdAndStatusNot(existing.getTeam().getId(), newSlot.getId(), BookingStatus.CANCELLED)) {
             throw new IllegalArgumentException("You have already booked or waitlisted this new slot.");
        }

        // Release old slot if this was a confirmed booking
        Slot oldSlot = existing.getSlot();
        boolean wasConfirmed = existing.getStatus() == BookingStatus.CONFIRMED;
        if (wasConfirmed) {
            oldSlot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(oldSlot);
            // Promote first waitlisted for old slot (FIFO by bookedAt, excluding current booking)
            bookingRepository.findFirstBySlot_IdAndStatusOrderByBookedAtAsc(oldSlot.getId(), BookingStatus.WAITLISTED)
                    .filter(b -> !b.getId().equals(id))
                    .ifPresent(promoted -> {
                        promoted.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(promoted);
                        oldSlot.setStatus(SlotStatus.BOOKED);
                        slotRepository.save(oldSlot);

                        // Send promotion email (async, non-blocking)
                        try {
                            Team promotedTeam = promoted.getTeam();
                            String pSlotTime = oldSlot.getStartTime() + " – " + oldSlot.getEndTime();
                            String pDate = oldSlot.getScheduleDate().getDemoDate().toString();
                            emailService.sendBookingConfirmation(
                                    promotedTeam.getEmail(), promotedTeam.getProjectName(),
                                    pDate, pSlotTime, "CONFIRMED (promoted from waitlist)");
                        } catch (Exception e) {
                            // Email failure must never break reschedule flow
                        }
                    });
        }

        // Assign new slot
        BookingStatus newStatus;
        if (newSlot.getStatus() == SlotStatus.AVAILABLE) {
            newSlot.setStatus(SlotStatus.BOOKED);
            slotRepository.save(newSlot);
            newStatus = BookingStatus.CONFIRMED;
        } else if (newSlot.getStatus() == SlotStatus.BOOKED) {
            newStatus = BookingStatus.WAITLISTED;
        } else {
            throw new IllegalArgumentException("Selected slot is not available.");
        }

        existing.setSlot(newSlot);
        existing.setStatus(newStatus);
        existing.setBookedAt(LocalDateTime.now());
        BookingDTO result = adminService.mapToDTO(bookingRepository.save(existing));

        // Send reschedule email notification (async, non-blocking)
        try {
            Team team = existing.getTeam();
            String slotTime = newSlot.getStartTime() + " – " + newSlot.getEndTime();
            String date = newSlot.getScheduleDate().getDemoDate().toString();
            emailService.sendBookingConfirmation(
                    team.getEmail(),
                    team.getProjectName(),
                    date,
                    slotTime,
                    "RESCHEDULED to " + newStatus.name()
            );
        } catch (Exception e) {
            // Email failure must never break reschedule flow
        }

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + id));
        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Send cancellation email (async, non-blocking)
        try {
            Team cancelledTeam = booking.getTeam();
            Slot cancelledSlot = booking.getSlot();
            String slotTime = cancelledSlot.getStartTime() + " – " + cancelledSlot.getEndTime();
            String date = cancelledSlot.getScheduleDate().getDemoDate().toString();
            emailService.sendBookingConfirmation(
                    cancelledTeam.getEmail(), cancelledTeam.getProjectName(), date, slotTime, "CANCELLED");
        } catch (Exception e) {
            // Email failure must never break cancellation flow
        }

        if (wasConfirmed) {
            Slot slot = booking.getSlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            slotRepository.save(slot);
            // Promote first waitlisted booking for this slot (FIFO by bookedAt)
            bookingRepository.findFirstBySlot_IdAndStatusOrderByBookedAtAsc(slot.getId(), BookingStatus.WAITLISTED)
                    .ifPresent(promoted -> {
                        promoted.setStatus(BookingStatus.CONFIRMED);
                        bookingRepository.save(promoted);
                        slot.setStatus(SlotStatus.BOOKED);
                        slotRepository.save(slot);

                        // Send promotion email (async, non-blocking)
                        try {
                            Team promotedTeam = promoted.getTeam();
                            String slotTime = slot.getStartTime() + " – " + slot.getEndTime();
                            String date = slot.getScheduleDate().getDemoDate().toString();
                            emailService.sendBookingConfirmation(
                                    promotedTeam.getEmail(), promotedTeam.getProjectName(),
                                    date, slotTime, "CONFIRMED (promoted from waitlist)");
                        } catch (Exception e) {
                            // Email failure must never break cancellation flow
                        }
                    });
        }
        return ResponseEntity.noContent().build();
    }
}
