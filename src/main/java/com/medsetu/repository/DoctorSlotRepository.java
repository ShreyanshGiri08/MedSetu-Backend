package com.medsetu.repository;

import com.medsetu.entity.DoctorSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface DoctorSlotRepository extends JpaRepository<DoctorSlot, Long> {

    List<DoctorSlot> findByDoctorIdAndSlotDateAndIsBookedFalse(Long doctorId, LocalDate slotDate);

    List<DoctorSlot> findByDoctorIdAndSlotDate(Long doctorId, LocalDate slotDate);

    @Query("SELECT COUNT(s) > 0 FROM DoctorSlot s WHERE s.doctor.id = :doctorId " +
           "AND s.slotDate = :date " +
           "AND s.id != :excludeId " +
           "AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    boolean existsOverlappingSlot(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId);
}
