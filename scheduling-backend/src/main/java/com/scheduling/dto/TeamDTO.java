package com.scheduling.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDTO {
    private Long id;

    @NotBlank(message = "Project name is required")
    private String projectName;

    @NotNull(message = "Number of members is required")
    @Min(value = 1, message = "Must have at least 1 member")
    private Integer members;

    @NotBlank(message = "Leader name is required")
    private String leaderName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String description;
}
