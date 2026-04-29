package com.medsetu.repository;

import com.medsetu.entity.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VitalsRepository extends JpaRepository<Vitals, Long> {
    List<Vitals> findByPatientIdOrderByRecordedAtDesc(Long patientId);
    List<Vitals> findByPatientIdAndRecordedAtAfterOrderByRecordedAt(Long patientId, LocalDateTime since);
}
