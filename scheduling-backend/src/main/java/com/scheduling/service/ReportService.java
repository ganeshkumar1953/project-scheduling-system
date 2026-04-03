package com.scheduling.service;

import com.scheduling.dto.BookingDTO;
import com.scheduling.dto.SlotDTO;
import com.scheduling.repository.BookingRepository;
import com.scheduling.repository.SlotRepository;
import com.scheduling.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final SlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final TeamRepository teamRepository;
    private final AdminService adminService;

    public List<SlotDTO> getFullSchedule() {
        return slotRepository.findAll().stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<BookingDTO> getBookingSummary() {
        return bookingRepository.findAll().stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }

    public String exportScheduleCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Slot ID,Date,Start Time,End Time,Status\n");
        slotRepository.findAll().forEach(slot ->
                sb.append(slot.getId()).append(",")
                  .append(slot.getScheduleDate().getDemoDate()).append(",")
                  .append(slot.getStartTime()).append(",")
                  .append(slot.getEndTime()).append(",")
                  .append(slot.getStatus()).append("\n")
        );
        return sb.toString();
    }

    public String exportTeamsCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Team ID,Project Name,Members,Leader Name,Email,Description\n");
        teamRepository.findAll().forEach(team ->
                sb.append(team.getId()).append(",")
                  .append(team.getProjectName()).append(",")
                  .append(team.getMembers()).append(",")
                  .append(team.getLeaderName()).append(",")
                  .append(team.getEmail()).append(",")
                  .append(team.getDescription() != null ? team.getDescription().replace(",", ";") : "")
                  .append("\n")
        );
        return sb.toString();
    }
}
