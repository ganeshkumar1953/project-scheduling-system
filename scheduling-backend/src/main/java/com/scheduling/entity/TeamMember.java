package com.scheduling.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_member")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Team team;

    @Column(nullable = false)
    private String memberName;
}
