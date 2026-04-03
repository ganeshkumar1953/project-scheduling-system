package com.scheduling.service;

import com.scheduling.dto.BookingDTO;
import com.scheduling.dto.TeamDTO;
import com.scheduling.entity.Team;
import com.scheduling.repository.BookingRepository;
import com.scheduling.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final TeamRepository teamRepository;
    private final BookingRepository bookingRepository;
    private final AdminService adminService;

    public TeamDTO registerTeam(TeamDTO dto) {
        if (teamRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Team with this email already exists: " + dto.getEmail());
        }
        Team team = Team.builder()
                .projectName(dto.getProjectName())
                .members(dto.getMembers())
                .leaderName(dto.getLeaderName())
                .email(dto.getEmail())
                .description(dto.getDescription())
                .build();
        return adminService.mapToDTO(teamRepository.save(team));
    }

    public TeamDTO getTeamByEmail(String email) {
        Team team = teamRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Team not found for email: " + email));
        return adminService.mapToDTO(team);
    }

    public List<BookingDTO> getBookingsByEmail(String email) {
        return bookingRepository.findByTeam_Email(email).stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }
}
