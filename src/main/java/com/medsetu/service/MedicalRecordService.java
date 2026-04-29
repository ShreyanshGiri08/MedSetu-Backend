package com.medsetu.service;

import com.medsetu.entity.MedicalRecord;
import com.medsetu.entity.User;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.MedicalRecordRepository;
import com.medsetu.repository.UserRepository;
import com.medsetu.util.FileUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final FileUtil fileUtil;

    public MedicalRecordService(MedicalRecordRepository recordRepository,
                                UserRepository userRepository,
                                FileUtil fileUtil) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.fileUtil = fileUtil;
    }

    @Transactional
    public Map<String, Object> uploadRecord(Long patientId, MultipartFile file, String title,
                                            String fileType, String recordDate) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        String fileUrl;
        try {
            fileUrl = fileUtil.saveFile(file);
        } catch (IOException e) {
            throw new ValidationException("Failed to save file: " + e.getMessage());
        }

        MedicalRecord.FileType type = null;
        if (fileType != null) {
            try {
                type = MedicalRecord.FileType.valueOf(fileType.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .title(title)
                .fileUrl(fileUrl)
                .fileType(type)
                .recordDate(recordDate != null ? LocalDate.parse(recordDate) : null)
                .build();

        record = recordRepository.save(record);
        return mapRecord(record);
    }

    public List<Map<String, Object>> getPatientRecords(Long patientId) {
        return recordRepository.findByPatientIdOrderByUploadedAtDesc(patientId)
                .stream().map(this::mapRecord).collect(Collectors.toList());
    }

    public MedicalRecord getRecordById(Long id, Long patientId) {
        MedicalRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found."));
        if (!record.getPatient().getId().equals(patientId)) {
            throw new ResourceNotFoundException("Record not found.");
        }
        return record;
    }

    @Transactional
    public void deleteRecord(Long id, Long patientId) {
        MedicalRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found."));
        if (!record.getPatient().getId().equals(patientId)) {
            throw new ValidationException("You can only delete your own records.");
        }
        fileUtil.deleteFile(record.getFileUrl());
        recordRepository.delete(record);
    }

    private Map<String, Object> mapRecord(MedicalRecord r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("title", r.getTitle());
        result.put("fileUrl", r.getFileUrl());
        result.put("fileType", r.getFileType());
        result.put("recordDate", r.getRecordDate());
        result.put("uploadedAt", r.getUploadedAt());
        return result;
    }
}
