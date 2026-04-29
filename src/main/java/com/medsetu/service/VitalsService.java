package com.medsetu.service;

import com.medsetu.entity.Vitals;
import com.medsetu.entity.User;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.repository.UserRepository;
import com.medsetu.repository.VitalsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VitalsService {

    private final VitalsRepository vitalsRepository;
    private final UserRepository userRepository;

    public VitalsService(VitalsRepository vitalsRepository, UserRepository userRepository) {
        this.vitalsRepository = vitalsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Map<String, Object> logVitals(Long patientId, Map<String, Object> data) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        Vitals vitals = Vitals.builder()
                .patient(patient)
                .bpSystolic(data.containsKey("bpSystolic") ? Integer.parseInt(data.get("bpSystolic").toString()) : null)
                .bpDiastolic(data.containsKey("bpDiastolic") ? Integer.parseInt(data.get("bpDiastolic").toString()) : null)
                .bloodSugar(data.containsKey("bloodSugar") ? new BigDecimal(data.get("bloodSugar").toString()) : null)
                .weight(data.containsKey("weight") ? new BigDecimal(data.get("weight").toString()) : null)
                .heartRate(data.containsKey("heartRate") ? Integer.parseInt(data.get("heartRate").toString()) : null)
                .temperature(data.containsKey("temperature") ? new BigDecimal(data.get("temperature").toString()) : null)
                .build();

        vitals = vitalsRepository.save(vitals);
        return mapVitals(vitals);
    }

    public List<Map<String, Object>> getAllVitals(Long patientId) {
        return vitalsRepository.findByPatientIdOrderByRecordedAtDesc(patientId)
                .stream().map(this::mapVitals).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getVitalsLast30Days(Long patientId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        return vitalsRepository.findByPatientIdAndRecordedAtAfterOrderByRecordedAt(patientId, since)
                .stream().map(this::mapVitals).collect(Collectors.toList());
    }

    private Map<String, Object> mapVitals(Vitals v) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", v.getId());
        result.put("bpSystolic", v.getBpSystolic());
        result.put("bpDiastolic", v.getBpDiastolic());
        result.put("bloodSugar", v.getBloodSugar());
        result.put("weight", v.getWeight());
        result.put("heartRate", v.getHeartRate());
        result.put("temperature", v.getTemperature());
        result.put("recordedAt", v.getRecordedAt());
        return result;
    }
}
