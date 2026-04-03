package com.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDTO {
    private Long id;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    @NotNull(message = "Slot ID is required")
    private Long slotId;

    // Response-only fields
    private String teamProjectName;
    private String teamLeaderName;
    private String teamEmail;
    private String slotDate;
    private String slotStartTime;
    private String slotEndTime;
    private String status;
    private LocalDateTime bookedAt;
}
