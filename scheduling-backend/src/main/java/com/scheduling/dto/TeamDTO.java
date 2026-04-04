package com.scheduling.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

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
    @Max(value = 5, message = "Maximum 5 members allowed")
    private Integer members;

    @NotBlank(message = "Leader name is required")
    private String leaderName;

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@gmail\\.com$", message = "Only Gmail addresses are allowed (e.g. name@gmail.com)")
    private String email;

    private String description;

    private List<String> memberNames;
}
