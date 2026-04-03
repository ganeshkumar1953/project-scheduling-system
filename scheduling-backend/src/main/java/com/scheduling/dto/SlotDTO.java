package com.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotDTO {
    private Long id;

    @NotNull(message = "Schedule date ID is required")
    private Long scheduleDateId;

    private LocalDate demoDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private String status;
}
