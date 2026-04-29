package com.medsetu.service;

import com.medsetu.entity.*;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               UserRepository userRepository,
                               AppointmentRepository appointmentRepository,
                               NotificationService notificationService) {
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> createPrescription(Long doctorUserId, Long patientId,
                                                   Long appointmentId, String notes,
                                                   List<Map<String, String>> medicines) {
        User doctor = userRepository.findById(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        Appointment appointment = null;
        if (appointmentId != null) {
            appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));
        }

        Prescription prescription = Prescription.builder()
                .doctor(doctor)
                .patient(patient)
                .appointment(appointment)
                .notes(notes)
                .build();

        prescription = prescriptionRepository.save(prescription);

        for (Map<String, String> med : medicines) {
            PrescriptionMedicine medicine = PrescriptionMedicine.builder()
                    .prescription(prescription)
                    .medicineName(med.get("medicineName"))
                    .dosage(med.getOrDefault("dosage", null))
                    .frequency(med.getOrDefault("frequency", null))
                    .duration(med.getOrDefault("duration", null))
                    .build();
            prescription.getMedicines().add(medicine);
        }

        prescription = prescriptionRepository.save(prescription);

        notificationService.createNotification(patientId,
                "Dr. " + doctor.getName() + " has issued a new prescription for you.",
                "PRESCRIPTION");

        return mapPrescription(prescription);
    }

    public List<Map<String, Object>> getPatientPrescriptions(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::mapPrescriptionSummary).collect(Collectors.toList());
    }

    public Map<String, Object> getPrescriptionDetail(Long prescriptionId, Long userId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found."));

        boolean isPatient = prescription.getPatient().getId().equals(userId);
        boolean isDoctor = prescription.getDoctor().getId().equals(userId);
        if (!isPatient && !isDoctor) {
            throw new ValidationException("Access denied.");
        }

        return mapPrescription(prescription);
    }

    private Map<String, Object> mapPrescriptionSummary(Prescription p) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", p.getId());
        result.put("notes", p.getNotes());
        result.put("createdAt", p.getCreatedAt());
        result.put("medicineCount", p.getMedicines().size());

        if (p.getDoctor() != null) {
            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", p.getDoctor().getId());
            doctor.put("name", p.getDoctor().getName());
            result.put("doctor", doctor);
        }
        return result;
    }

    private Map<String, Object> mapPrescription(Prescription p) {
        Map<String, Object> result = mapPrescriptionSummary(p);

        List<Map<String, Object>> medicineList = p.getMedicines().stream().map(m -> {
            Map<String, Object> med = new HashMap<>();
            med.put("id", m.getId());
            med.put("medicineName", m.getMedicineName());
            med.put("dosage", m.getDosage());
            med.put("frequency", m.getFrequency());
            med.put("duration", m.getDuration());
            return med;
        }).collect(Collectors.toList());

        result.put("medicines", medicineList);

        if (p.getPatient() != null) {
            Map<String, Object> patient = new HashMap<>();
            patient.put("id", p.getPatient().getId());
            patient.put("name", p.getPatient().getName());
            result.put("patient", patient);
        }
        return result;
    }
}
