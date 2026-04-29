package com.medsetu.service;

import com.medsetu.entity.*;
import com.medsetu.entity.Appointment.AppointmentStatus;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              DoctorSlotRepository slotRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> bookAppointment(Long patientId, Long doctorId, Long slotId, String type, String notes) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));

        DoctorSlot slot = null;
        if (slotId != null) {
            slot = slotRepository.findById(slotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found."));
            if (Boolean.TRUE.equals(slot.getIsBooked())) {
                throw new ValidationException("This slot is already booked.");
            }
            slot.setIsBooked(true);
            slotRepository.save(slot);
        }

        Appointment.AppointmentType appointmentType = type != null
                ? Appointment.AppointmentType.valueOf(type.toUpperCase())
                : Appointment.AppointmentType.OFFLINE;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .slot(slot)
                .type(appointmentType)
                .status(AppointmentStatus.PENDING)
                .notes(notes)
                .build();

        appointment = appointmentRepository.save(appointment);

        // Notify both patient and doctor
        notificationService.createNotification(patientId, "Your appointment with Dr. " + doctor.getUser().getName() + " has been booked.", "APPOINTMENT");
        notificationService.createNotification(doctor.getUser().getId(), "New appointment booked by " + patient.getName() + ".", "APPOINTMENT");

        return mapAppointment(appointment);
    }

    public List<Map<String, Object>> getPatientAppointments(Long patientId, String status) {
        List<Appointment> appointments;
        if (status != null) {
            appointments = appointmentRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(
                    patientId, AppointmentStatus.valueOf(status.toUpperCase()));
        } else {
            appointments = appointmentRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        }
        return appointments.stream().map(this::mapAppointment).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getDoctorAppointments(Long doctorUserId, String date) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
        List<Appointment> appointments;
        if (date != null) {
            appointments = appointmentRepository.findByDoctorIdAndDate(doctor.getId(), LocalDate.parse(date));
        } else {
            appointments = appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctor.getId());
        }
        return appointments.stream().map(this::mapAppointment).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> cancelAppointment(Long appointmentId, Long userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        boolean isPatient = appointment.getPatient().getId().equals(userId);
        boolean isDoctor = appointment.getDoctor().getUser().getId().equals(userId);

        if (!isPatient && !isDoctor) {
            throw new ValidationException("You are not authorized to cancel this appointment.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);

        if (appointment.getSlot() != null) {
            DoctorSlot slot = appointment.getSlot();
            slot.setIsBooked(false);
            slotRepository.save(slot);
        }

        appointment = appointmentRepository.save(appointment);
        return mapAppointment(appointment);
    }

    @Transactional
    public Map<String, Object> rescheduleAppointment(Long appointmentId, Long newSlotId, Long userId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (!appointment.getPatient().getId().equals(userId)) {
            throw new ValidationException("Only the patient can reschedule an appointment.");
        }

        // Free old slot
        if (appointment.getSlot() != null) {
            DoctorSlot oldSlot = appointment.getSlot();
            oldSlot.setIsBooked(false);
            slotRepository.save(oldSlot);
        }

        // Book new slot
        DoctorSlot newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new ResourceNotFoundException("New slot not found."));
        if (Boolean.TRUE.equals(newSlot.getIsBooked())) {
            throw new ValidationException("The new slot is already booked.");
        }
        newSlot.setIsBooked(true);
        slotRepository.save(newSlot);

        appointment.setSlot(newSlot);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment = appointmentRepository.save(appointment);
        return mapAppointment(appointment);
    }

    @Transactional
    public Map<String, Object> updateAppointmentStatus(Long appointmentId, String status, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found."));

        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new ValidationException("You are not authorized to update this appointment.");
        }

        appointment.setStatus(AppointmentStatus.valueOf(status.toUpperCase()));
        appointment = appointmentRepository.save(appointment);
        return mapAppointment(appointment);
    }

    private Map<String, Object> mapAppointment(Appointment a) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", a.getId());
        result.put("type", a.getType());
        result.put("status", a.getStatus());
        result.put("notes", a.getNotes());
        result.put("createdAt", a.getCreatedAt());

        if (a.getPatient() != null) {
            Map<String, Object> patient = new HashMap<>();
            patient.put("id", a.getPatient().getId());
            patient.put("name", a.getPatient().getName());
            result.put("patient", patient);
        }

        if (a.getDoctor() != null) {
            Map<String, Object> doctor = new HashMap<>();
            doctor.put("id", a.getDoctor().getId());
            doctor.put("name", a.getDoctor().getUser() != null ? a.getDoctor().getUser().getName() : null);
            doctor.put("specialization", a.getDoctor().getSpecialization());
            result.put("doctor", doctor);
        }

        if (a.getSlot() != null) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("id", a.getSlot().getId());
            slot.put("slotDate", a.getSlot().getSlotDate());
            slot.put("startTime", a.getSlot().getStartTime());
            slot.put("endTime", a.getSlot().getEndTime());
            result.put("slot", slot);
        }

        return result;
    }
}
