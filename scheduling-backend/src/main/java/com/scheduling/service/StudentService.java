package com.scheduling.service;

import com.scheduling.dto.BookingDTO;
import com.scheduling.dto.TeamDTO;
import com.scheduling.entity.Team;
import com.scheduling.entity.TeamMember;
import com.scheduling.repository.BookingRepository;
import com.scheduling.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final TeamRepository teamRepository;
    private final BookingRepository bookingRepository;
    private final AdminService adminService;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@gmail\\.com$"
    );

    @Transactional
    public TeamDTO registerTeam(TeamDTO dto) {
        // Validate email format explicitly
        if (dto.getEmail() == null || !EMAIL_PATTERN.matcher(dto.getEmail().trim()).matches()) {
            throw new IllegalArgumentException("Only Gmail addresses are allowed (e.g. name@gmail.com)");
        }

        if (teamRepository.existsByEmail(dto.getEmail().trim())) {
            throw new IllegalArgumentException("Email already registered. Use a different Gmail address.");
        }

        if (dto.getProjectName() != null && teamRepository.existsByProjectName(dto.getProjectName().trim())) {
            throw new IllegalArgumentException("A team with this project name already exists.");
        }

        // Validate member names
        List<String> memberNames = dto.getMemberNames();
        if (memberNames == null || memberNames.isEmpty()) {
            throw new IllegalArgumentException("At least 1 team member name is required.");
        }
        if (memberNames.size() > 5) {
            throw new IllegalArgumentException("Maximum 5 team members allowed.");
        }
        // Filter out blank names
        List<String> validNames = memberNames.stream()
                .map(String::trim)
                .filter(n -> !n.isEmpty())
                .collect(Collectors.toList());
        if (validNames.isEmpty()) {
            throw new IllegalArgumentException("At least 1 valid team member name is required.");
        }
        if (validNames.size() > 5) {
            throw new IllegalArgumentException("Maximum 5 team members allowed.");
        }

        Team team = Team.builder()
                .projectName(dto.getProjectName().trim())
                .members(validNames.size())
                .leaderName(dto.getLeaderName().trim())
                .email(dto.getEmail().trim())
                .description(dto.getDescription() != null ? dto.getDescription().trim() : null)
                .build();

        // Create team member entities
        for (String name : validNames) {
            TeamMember member = TeamMember.builder()
                    .team(team)
                    .memberName(name)
                    .build();
            team.getTeamMembers().add(member);
        }

        return adminService.mapToDTO(teamRepository.save(team));
    }

    public TeamDTO getTeamByEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email format.");
        }
        Team team = teamRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Team not found for email: " + email));
        return adminService.mapToDTO(team);
    }

    public List<BookingDTO> getBookingsByEmail(String email) {
        return bookingRepository.findByTeam_Email(email.trim()).stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }
}
