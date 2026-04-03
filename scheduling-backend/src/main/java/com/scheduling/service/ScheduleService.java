package com.scheduling.service;

import com.scheduling.dto.SlotDTO;
import com.scheduling.entity.Slot.SlotStatus;
import com.scheduling.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final SlotRepository slotRepository;
    private final AdminService adminService;

    public List<SlotDTO> getAvailableSlots() {
        return slotRepository.findByStatus(SlotStatus.AVAILABLE).stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SlotDTO> getSlotsByDate(Long dateId) {
        return slotRepository.findByScheduleDate_Id(dateId).stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<SlotDTO> getAllSlots() {
        return slotRepository.findAll().stream()
                .map(adminService::mapToDTO)
                .collect(Collectors.toList());
    }
}
