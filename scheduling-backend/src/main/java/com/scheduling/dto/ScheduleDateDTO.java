package com.scheduling.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleDateDTO {
    private Long id;

    @NotNull(message = "Demo date is required")
    private LocalDate demoDate;
}
