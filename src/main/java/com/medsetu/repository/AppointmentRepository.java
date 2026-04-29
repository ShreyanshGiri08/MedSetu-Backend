package com.medsetu.repository;

import com.medsetu.entity.Appointment;
import com.medsetu.entity.Appointment.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    List<Appointment> findByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId, AppointmentStatus status);

    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND DATE(a.slot.slotDate) = :date ORDER BY a.slot.startTime")
    List<Appointment> findByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countTodaysAppointments();

    boolean existsByPatientIdAndDoctorId(Long patientId, Long doctorId);
}
