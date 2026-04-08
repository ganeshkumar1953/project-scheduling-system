package com.scheduling.service;

import com.scheduling.dto.*;
import com.scheduling.entity.*;
import com.scheduling.entity.Booking.BookingStatus;
import com.scheduling.entity.Slot.SlotStatus;
import com.scheduling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final ScheduleDateRepository scheduleDateRepository;
    private final SlotRepository slotRepository;
    private final TeamRepository teamRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    // ── Demo Date Management ──────────────────────────────────────────────

    public ScheduleDateDTO addDemoDate(ScheduleDateDTO dto) {
        if (scheduleDateRepository.existsByDemoDate(dto.getDemoDate())) {
            throw new IllegalArgumentException("Demo date already exists: " + dto.getDemoDate());
        }
        ScheduleDate saved = scheduleDateRepository.save(
                ScheduleDate.builder().demoDate(dto.getDemoDate()).build()
        );
        return mapToDTO(saved);
    }

    public List<ScheduleDateDTO> getAllDemoDates() {
        return scheduleDateRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDemoDate(Long id) {
        ScheduleDate date = scheduleDateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demo date not found: " + id));
        List<Slot> slots = slotRepository.findByScheduleDate_Id(id);
        for (Slot slot : slots) {
            bookingRepository.findBySlot_Id(slot.getId()).forEach(bookingRepository::delete);
            slotRepository.delete(slot);
        }
        scheduleDateRepository.delete(date);
    }

    // ── Slot Management ───────────────────────────────────────────────────

    public SlotDTO createSlot(SlotDTO dto) {
        ScheduleDate scheduleDate = scheduleDateRepository.findById(dto.getScheduleDateId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule date not found: " + dto.getScheduleDateId()));
        Slot slot = Slot.builder()
                .scheduleDate(scheduleDate)
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();
        return mapToDTO(slotRepository.save(slot));
    }

    public SlotDTO editSlot(Long id, SlotDTO dto) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + id));
        if (dto.getStartTime() != null) slot.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) slot.setEndTime(dto.getEndTime());
        if (dto.getScheduleDateId() != null) {
            ScheduleDate sd = scheduleDateRepository.findById(dto.getScheduleDateId())
                    .orElseThrow(() -> new IllegalArgumentException("Schedule date not found"));
            slot.setScheduleDate(sd);
        }
        return mapToDTO(slotRepository.save(slot));
    }

    @Transactional
    public void deleteSlot(Long id) {
        Slot slot = slotRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + id));
        bookingRepository.findBySlot_Id(id).forEach(bookingRepository::delete);
        slotRepository.delete(slot);
    }

    public List<SlotDTO> getAllSlots(Long dateId) {
        List<Slot> slots = (dateId != null)
                ? slotRepository.findByScheduleDate_Id(dateId)
                : slotRepository.findAll();
        return slots.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ── Teams & Bookings ──────────────────────────────────────────────────

    public List<TeamDTO> getAllTeams() {
        return teamRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        boolean wasConfirmed = booking.getStatus() == BookingStatus.CONFIRMED;
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Send cancellation email (async, non-blocking)
        try {
            Team team = booking.getTeam();
            Slot slot = booking.getSlot();
            String slotTime = slot.getStartTime() + " – " + slot.getEndTime();
            String date = slot.getScheduleDate().getDemoDate().toString();
            emailService.sendBookingConfirmation(
                    team.getEmail(), team.getProjectName(), date, slotTime, "CANCELLED");
        } catch (Exception e) {
            log.error("Failed to send cancellation email for booking {}: {}", bookingId, e.getMessage());
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
                            log.error("Failed to send promotion email for booking {}: {}", promoted.getId(), e.getMessage());
                        }
                    });
        }
    }

    public List<BookingDTO> getWaitingList() {
        return bookingRepository.findByStatus(BookingStatus.WAITLISTED).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    public ScheduleDateDTO mapToDTO(ScheduleDate sd) {
        return ScheduleDateDTO.builder().id(sd.getId()).demoDate(sd.getDemoDate()).build();
    }

    public SlotDTO mapToDTO(Slot slot) {
        return SlotDTO.builder()
                .id(slot.getId())
                .scheduleDateId(slot.getScheduleDate().getId())
                .demoDate(slot.getScheduleDate().getDemoDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(slot.getStatus().name())
                .build();
    }

    public TeamDTO mapToDTO(Team team) {
        List<String> memberNames = team.getTeamMembers() != null
                ? team.getTeamMembers().stream()
                    .map(TeamMember::getMemberName)
                    .collect(Collectors.toList())
                : List.of();

        return TeamDTO.builder()
                .id(team.getId())
                .projectName(team.getProjectName())
                .members(team.getMembers())
                .leaderName(team.getLeaderName())
                .email(team.getEmail())
                .description(team.getDescription())
                .memberNames(memberNames)
                .build();
    }

    public BookingDTO mapToDTO(Booking booking) {
        return BookingDTO.builder()
                .id(booking.getId())
                .teamId(booking.getTeam().getId())
                .slotId(booking.getSlot().getId())
                .teamProjectName(booking.getTeam().getProjectName())
                .teamLeaderName(booking.getTeam().getLeaderName())
                .teamEmail(booking.getTeam().getEmail())
                .slotDate(booking.getSlot().getScheduleDate().getDemoDate().toString())
                .slotStartTime(booking.getSlot().getStartTime().toString())
                .slotEndTime(booking.getSlot().getEndTime().toString())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .build();
    }
}
