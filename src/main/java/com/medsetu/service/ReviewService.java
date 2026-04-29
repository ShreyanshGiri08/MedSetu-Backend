package com.medsetu.service;

import com.medsetu.entity.*;
import com.medsetu.exception.DuplicateResourceException;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public ReviewService(ReviewRepository reviewRepository,
                         UserRepository userRepository,
                         DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public Map<String, Object> submitReview(Long patientId, Long doctorId, Long appointmentId,
                                             int rating, String comment) {
        if (appointmentId != null && reviewRepository.existsByPatientIdAndAppointmentId(patientId, appointmentId)) {
            throw new DuplicateResourceException("You have already reviewed this appointment.");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        }

        Review review = Review.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .rating(rating)
                .comment(comment)
                .build();

        review = reviewRepository.save(review);

        // Recalculate doctor's avg rating
        Double avgRating = doctorRepository.calculateAvgRating(doctorId);
        if (avgRating != null) {
            doctor.setAvgRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
            doctorRepository.save(doctor);
        }

        return mapReview(review);
    }

    public List<Map<String, Object>> getDoctorReviews(Long doctorId) {
        return reviewRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream().map(this::mapReview).collect(Collectors.toList());
    }

    private Map<String, Object> mapReview(Review r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("rating", r.getRating());
        result.put("comment", r.getComment());
        result.put("createdAt", r.getCreatedAt());
        if (r.getPatient() != null) {
            result.put("patientName", r.getPatient().getName());
            result.put("patientId", r.getPatient().getId());
        }
        return result;
    }
}
