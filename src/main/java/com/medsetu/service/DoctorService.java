package com.medsetu.service;

import com.medsetu.entity.Doctor;
import com.medsetu.entity.User;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.UnauthorizedException;
import com.medsetu.repository.DoctorRepository;
import com.medsetu.repository.ReviewRepository;
import com.medsetu.repository.UserRepository;
import com.medsetu.util.PaginationUtil;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final PaginationUtil paginationUtil;
    private final FileStorageService fileStorageService;

    public DoctorService(DoctorRepository doctorRepository,
                         UserRepository userRepository,
                         ReviewRepository reviewRepository,
                         PaginationUtil paginationUtil,
                         FileStorageService fileStorageService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.paginationUtil = paginationUtil;
        this.fileStorageService = fileStorageService;
    }

    public Map<String, Object> listApprovedDoctors(String specialization, String hospitalName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("avgRating").descending());
        Page<Doctor> doctors = doctorRepository.findApprovedDoctors(specialization, hospitalName, pageable);
        Page<Map<String, Object>> mapped = doctors.map(this::mapDoctorToResponse);
        return paginationUtil.buildPageResponse(mapped);
    }

    public Map<String, Object> getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + doctorId));
        Map<String, Object> result = mapDoctorToResponse(doctor);
        result.put("reviewCount", reviewRepository.countByDoctorId(doctorId));
        return result;
    }

    @Transactional
    public Map<String, Object> updateDoctorProfile(Long userId, Map<String, Object> updates) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));

        if (updates.containsKey("specialization")) doctor.setSpecialization((String) updates.get("specialization"));
        if (updates.containsKey("qualification")) doctor.setQualification((String) updates.get("qualification"));
        if (updates.containsKey("experienceYears")) doctor.setExperienceYears((Integer) updates.get("experienceYears"));
        if (updates.containsKey("consultationFees")) doctor.setConsultationFees(new BigDecimal(updates.get("consultationFees").toString()));
        if (updates.containsKey("hospitalName")) doctor.setHospitalName((String) updates.get("hospitalName"));
        if (updates.containsKey("bio")) doctor.setBio((String) updates.get("bio"));
        if (updates.containsKey("profilePicUrl")) doctor.setProfilePicUrl((String) updates.get("profilePicUrl"));

        doctorRepository.save(doctor);
        return mapDoctorToResponse(doctor);
    }

    @Transactional
    public Map<String, Object> uploadProfilePicture(Long userId, MultipartFile file) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
        String url = fileStorageService.getDownloadUrl(fileStorageService.store(file));
        doctor.setProfilePicUrl(url);
        doctorRepository.save(doctor);
        return mapDoctorToResponse(doctor);
    }

    @Transactional
    public void approveDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        doctor.setIsApproved(true);

        User user = doctor.getUser();
        user.setIsActive(true);
        userRepository.save(user);
        doctorRepository.save(doctor);
    }

    @Transactional
    public void rejectDoctor(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        doctor.setIsApproved(false);

        User user = doctor.getUser();
        user.setIsActive(false);
        userRepository.save(user);
        doctorRepository.save(doctor);
    }

    private Map<String, Object> mapDoctorToResponse(Doctor doctor) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", doctor.getId());
        result.put("specialization", doctor.getSpecialization());
        result.put("qualification", doctor.getQualification());
        result.put("experienceYears", doctor.getExperienceYears());
        result.put("consultationFees", doctor.getConsultationFees());
        result.put("hospitalName", doctor.getHospitalName());
        result.put("bio", doctor.getBio());
        result.put("profilePicUrl", doctor.getProfilePicUrl());
        result.put("avgRating", doctor.getAvgRating());
        result.put("isApproved", doctor.getIsApproved());

        User user = doctor.getUser();
        if (user != null) {
            result.put("userId", user.getId());
            result.put("name", user.getName());
            result.put("email", user.getEmail());
            result.put("phone", user.getPhone());
        }
        return result;
    }
}
