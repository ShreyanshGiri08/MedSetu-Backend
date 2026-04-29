package com.medsetu.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(length = 100)
    private String specialization;

    @Column(length = 100)
    private String qualification;

    @Column(name = "experience_years", columnDefinition = "INT DEFAULT 0")
    private Integer experienceYears = 0;

    @Column(name = "consultation_fees", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal consultationFees = BigDecimal.ZERO;

    @Column(name = "hospital_name", length = 150)
    private String hospitalName;

    @Column(name = "profile_pic_url", length = 500)
    private String profilePicUrl;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "avg_rating", precision = 3, scale = 2, columnDefinition = "DECIMAL(3,2) DEFAULT 0")
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "is_approved", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isApproved = false;
}
