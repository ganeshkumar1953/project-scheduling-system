package com.scheduling.repository;

import com.scheduling.entity.ScheduleDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ScheduleDateRepository extends JpaRepository<ScheduleDate, Long> {
    Optional<ScheduleDate> findByDemoDate(LocalDate demoDate);
    boolean existsByDemoDate(LocalDate demoDate);
}
