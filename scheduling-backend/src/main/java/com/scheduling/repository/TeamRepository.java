package com.scheduling.repository;

import com.scheduling.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByProjectName(String projectName);
}
