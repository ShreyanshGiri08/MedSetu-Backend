package com.medsetu.repository;

import com.medsetu.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);
    boolean existsByPatientIdAndAppointmentId(Long patientId, Long appointmentId);
    long countByDoctorId(Long doctorId);
}
