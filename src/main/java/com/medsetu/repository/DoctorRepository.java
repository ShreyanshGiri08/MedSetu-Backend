package com.medsetu.repository;

import com.medsetu.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    @Query("SELECT d FROM Doctor d WHERE d.isApproved = true " +
           "AND (:specialization IS NULL OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :specialization, '%'))) " +
           "AND (:hospitalName IS NULL OR LOWER(d.hospitalName) LIKE LOWER(CONCAT('%', :hospitalName, '%')))")
    Page<Doctor> findApprovedDoctors(
            @Param("specialization") String specialization,
            @Param("hospitalName") String hospitalName,
            Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.doctor.id = :doctorId")
    Double calculateAvgRating(@Param("doctorId") Long doctorId);
}
