package com.medsetu.service;

import com.medsetu.entity.Doctor;
import com.medsetu.entity.DoctorSlot;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.DoctorRepository;
import com.medsetu.repository.DoctorSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SlotService {

    private final DoctorSlotRepository slotRepository;
    private final DoctorRepository doctorRepository;

    public SlotService(DoctorSlotRepository slotRepository, DoctorRepository doctorRepository) {
        this.slotRepository = slotRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional
    public Map<String, Object> createSlot(Long userId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));

        if (!startTime.isBefore(endTime)) {
            throw new ValidationException("Start time must be before end time.");
        }

        boolean overlaps = slotRepository.existsOverlappingSlot(
                doctor.getId(), date, startTime, endTime, -1L);

        if (overlaps) {
            throw new ValidationException("This time slot overlaps with an existing slot.");
        }

        DoctorSlot slot = DoctorSlot.builder()
                .doctor(doctor)
                .slotDate(date)
                .startTime(startTime)
                .endTime(endTime)
                .isBooked(false)
                .build();

        slot = slotRepository.save(slot);
        return mapSlot(slot);
    }

    public List<Map<String, Object>> getAvailableSlots(Long doctorId, LocalDate date) {
        return slotRepository.findByDoctorIdAndSlotDateAndIsBookedFalse(doctorId, date)
                .stream()
                .map(this::mapSlot)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSlot(Long slotId, Long userId) {
        DoctorSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found."));

        if (!slot.getDoctor().getUser().getId().equals(userId)) {
            throw new ValidationException("You can only delete your own slots.");
        }

        if (Boolean.TRUE.equals(slot.getIsBooked())) {
            throw new ValidationException("Cannot delete a slot that is already booked.");
        }

        slotRepository.delete(slot);
    }

    public List<Map<String, Object>> getAllSlotsForDoctor(Long doctorId, LocalDate date) {
        return slotRepository.findByDoctorIdAndSlotDate(doctorId, date)
                .stream()
                .map(this::mapSlot)
                .collect(Collectors.toList());
    }

    /** Used by the authenticated doctor to see their own slots — resolves userId → doctorId internally. */
    public List<Map<String, Object>> getMySlots(Long userId, LocalDate date) {
        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
        return slotRepository.findByDoctorIdAndSlotDate(doctor.getId(), date)
                .stream()
                .map(this::mapSlot)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapSlot(DoctorSlot slot) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", slot.getId());
        result.put("doctorId", slot.getDoctor().getId());
        result.put("slotDate", slot.getSlotDate());
        result.put("startTime", slot.getStartTime());
        result.put("endTime", slot.getEndTime());
        result.put("isBooked", slot.getIsBooked());
        return result;
    }
}
