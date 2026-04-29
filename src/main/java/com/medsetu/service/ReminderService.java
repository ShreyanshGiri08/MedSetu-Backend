package com.medsetu.service;

import com.medsetu.entity.MedicineReminder;
import com.medsetu.entity.User;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.ReminderRepository;
import com.medsetu.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ReminderService(ReminderRepository reminderRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Map<String, Object> createReminder(Long patientId, String medicineName, String dosage,
                                               String timeOfDay, String repeatDays) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        MedicineReminder reminder = MedicineReminder.builder()
                .patient(patient)
                .medicineName(medicineName)
                .dosage(dosage)
                .timeOfDay(LocalTime.parse(timeOfDay))
                .repeatDays(repeatDays)
                .isActive(true)
                .build();

        reminder = reminderRepository.save(reminder);
        return mapReminder(reminder);
    }

    public List<Map<String, Object>> getPatientReminders(Long patientId) {
        return reminderRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(this::mapReminder).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> toggleReminder(Long reminderId, Long patientId) {
        MedicineReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found."));
        if (!reminder.getPatient().getId().equals(patientId)) {
            throw new ValidationException("You can only modify your own reminders.");
        }
        reminder.setIsActive(!reminder.getIsActive());
        reminder = reminderRepository.save(reminder);
        return mapReminder(reminder);
    }

    @Transactional
    public void deleteReminder(Long reminderId, Long patientId) {
        MedicineReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder not found."));
        if (!reminder.getPatient().getId().equals(patientId)) {
            throw new ValidationException("You can only delete your own reminders.");
        }
        reminderRepository.delete(reminder);
    }

    /**
     * Run every minute — check reminders matching current time within 1-minute window
     * and send notification if today's day is in repeatDays.
     */
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processReminders() {
        LocalTime now = LocalTime.now();
        LocalTime from = now.minusSeconds(30);
        LocalTime to = now.plusSeconds(30);

        String todayShort = DayOfWeek.from(java.time.LocalDate.now())
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase(); // MON, TUE...

        List<MedicineReminder> dueReminders = reminderRepository
                .findByIsActiveTrueAndTimeOfDayBetween(from, to);

        for (MedicineReminder reminder : dueReminders) {
            String repeatDays = reminder.getRepeatDays();
            if (repeatDays == null || repeatDays.isBlank() ||
                    Arrays.stream(repeatDays.split(",")).anyMatch(d -> d.trim().equalsIgnoreCase(todayShort))) {
                notificationService.createNotification(
                        reminder.getPatient().getId(),
                        "Medicine Reminder: Take " + reminder.getMedicineName() +
                                (reminder.getDosage() != null ? " — " + reminder.getDosage() : ""),
                        "REMINDER");
            }
        }
    }

    private Map<String, Object> mapReminder(MedicineReminder r) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", r.getId());
        result.put("medicineName", r.getMedicineName());
        result.put("dosage", r.getDosage());
        result.put("timeOfDay", r.getTimeOfDay());
        result.put("repeatDays", r.getRepeatDays());
        result.put("isActive", r.getIsActive());
        result.put("createdAt", r.getCreatedAt());
        return result;
    }
}
