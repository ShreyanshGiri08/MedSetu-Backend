package com.medsetu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private DoctorSlot slot;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ONLINE','OFFLINE') DEFAULT 'OFFLINE'")
    private AppointmentType type = AppointmentType.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING','CONFIRMED','COMPLETED','CANCELLED','NO_SHOW') DEFAULT 'PENDING'")
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum AppointmentType {
        ONLINE, OFFLINE
    }

    public enum AppointmentStatus {
        PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
    }
}
