package com.medsetu.repository;

import com.medsetu.entity.MedicineReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<MedicineReminder, Long> {
    List<MedicineReminder> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<MedicineReminder> findByIsActiveTrueAndTimeOfDayBetween(LocalTime from, LocalTime to);
}
