package com.medsetu.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vitals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(name = "bp_systolic")
    private Integer bpSystolic;

    @Column(name = "bp_diastolic")
    private Integer bpDiastolic;

    @Column(name = "blood_sugar", precision = 6, scale = 2)
    private BigDecimal bloodSugar;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(precision = 4, scale = 1)
    private BigDecimal temperature;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
