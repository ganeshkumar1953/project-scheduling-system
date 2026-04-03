package com.scheduling.controller;

import com.scheduling.dto.*;
import com.scheduling.service.AdminService;
import com.scheduling.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    // ── Demo Dates ────────────────────────────────────────────────────────

    @PostMapping("/dates")
    public ResponseEntity<ScheduleDateDTO> addDemoDate(@Valid @RequestBody ScheduleDateDTO dto) {
        return ResponseEntity.ok(adminService.addDemoDate(dto));
    }

    @GetMapping("/dates")
    public ResponseEntity<List<ScheduleDateDTO>> getAllDates() {
        return ResponseEntity.ok(adminService.getAllDemoDates());
    }

    @DeleteMapping("/dates/{id}")
    public ResponseEntity<Void> deleteDemoDate(@PathVariable Long id) {
        adminService.deleteDemoDate(id);
        return ResponseEntity.noContent().build();
    }

    // ── Slots ─────────────────────────────────────────────────────────────

    @PostMapping("/slots")
    public ResponseEntity<SlotDTO> createSlot(@Valid @RequestBody SlotDTO dto) {
        return ResponseEntity.ok(adminService.createSlot(dto));
    }

    @PutMapping("/slots/{id}")
    public ResponseEntity<SlotDTO> editSlot(@PathVariable Long id, @RequestBody SlotDTO dto) {
        return ResponseEntity.ok(adminService.editSlot(id, dto));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        adminService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotDTO>> getSlots(@RequestParam(required = false) Long dateId) {
        return ResponseEntity.ok(adminService.getAllSlots(dateId));
    }

    // ── Teams & Bookings ──────────────────────────────────────────────────

    @GetMapping("/teams")
    public ResponseEntity<List<TeamDTO>> getAllTeams() {
        return ResponseEntity.ok(adminService.getAllTeams());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        return ResponseEntity.ok(adminService.getAllBookings());
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable Long id) {
        adminService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookings/waitlist")
    public ResponseEntity<List<BookingDTO>> getWaitingList() {
        return ResponseEntity.ok(adminService.getWaitingList());
    }

    // ── Reports ───────────────────────────────────────────────────────────

    @GetMapping("/reports/schedule")
    public ResponseEntity<List<SlotDTO>> getFullSchedule() {
        return ResponseEntity.ok(reportService.getFullSchedule());
    }

    @GetMapping("/reports/summary")
    public ResponseEntity<List<BookingDTO>> getBookingSummary() {
        return ResponseEntity.ok(reportService.getBookingSummary());
    }

    @GetMapping("/reports/export/csv")
    public ResponseEntity<String> exportScheduleCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=schedule.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.exportScheduleCsv());
    }

    @GetMapping("/reports/export/teams-csv")
    public ResponseEntity<String> exportTeamsCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=teams.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportService.exportTeamsCsv());
    }
}
