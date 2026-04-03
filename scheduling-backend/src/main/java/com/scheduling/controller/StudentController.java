package com.scheduling.controller;

import com.scheduling.dto.BookingDTO;
import com.scheduling.dto.TeamDTO;
import com.scheduling.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping("/register")
    public ResponseEntity<TeamDTO> registerTeam(@Valid @RequestBody TeamDTO dto) {
        return ResponseEntity.ok(studentService.registerTeam(dto));
    }

    @GetMapping("/{email}/team")
    public ResponseEntity<TeamDTO> getTeam(@PathVariable String email) {
        return ResponseEntity.ok(studentService.getTeamByEmail(email));
    }

    @GetMapping("/{email}/bookings")
    public ResponseEntity<List<BookingDTO>> getBookings(@PathVariable String email) {
        return ResponseEntity.ok(studentService.getBookingsByEmail(email));
    }
}
