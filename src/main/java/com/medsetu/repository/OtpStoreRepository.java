package com.medsetu.repository;

import com.medsetu.entity.OtpStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpStoreRepository extends JpaRepository<OtpStore, Long> {
    Optional<OtpStore> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);
}
