package com.scheduling.repository;

import com.scheduling.entity.Slot;
import com.scheduling.entity.Slot.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByScheduleDate_Id(Long scheduleDateId);
    List<Slot> findByStatus(SlotStatus status);
    List<Slot> findByScheduleDate_IdAndStatus(Long scheduleDateId, SlotStatus status);
}
