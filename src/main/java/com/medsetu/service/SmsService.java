package com.medsetu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS Service — Twilio is optional.
 * If credentials are blank or contain placeholder text, all SMS calls are silently skipped.
 */
@Service
public class SmsService {

    private final boolean enabled;
    private final String fromNumber;

    public SmsService(
            @Value("${twilio.account.sid:}") String accountSid,
            @Value("${twilio.auth.token:}") String authToken,
            @Value("${twilio.from.number:}") String fromNumber) {

        this.fromNumber = fromNumber;

        boolean ready = !accountSid.isBlank()
                && !authToken.isBlank()
                && !fromNumber.isBlank()
                && !accountSid.startsWith("YOUR_")
                && !fromNumber.startsWith("+YOUR_");

        if (ready) {
            boolean initOk;
            try {
                com.twilio.Twilio.init(accountSid, authToken);
                initOk = true;
                System.out.println("✅ Twilio SMS service initialised.");
            } catch (Exception e) {
                System.err.println("⚠️  Twilio init failed — SMS disabled: " + e.getMessage());
                initOk = false;
            }
            this.enabled = initOk;
        } else {
            this.enabled = false;
            System.out.println("ℹ️  Twilio not configured — SMS features disabled.");
        }
    }

    public void sendSms(String toNumber, String messageBody) {
        if (!enabled || toNumber == null || toNumber.isBlank()) {
            System.out.println("SMS skipped → " + messageBody);
            return;
        }
        try {
            com.twilio.rest.api.v2010.account.Message.creator(
                    new com.twilio.type.PhoneNumber(toNumber),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    messageBody)
                    .create();
        } catch (Exception e) {
            System.err.println("SMS send failed: " + e.getMessage());
        }
    }

    public void sendOtpSms(String toNumber, String otp) {
        sendSms(toNumber, "MedSetu OTP: " + otp + ". Valid for 10 minutes. Do not share.");
    }

    public void sendReminderSms(String toNumber, String medicineName, String dosage) {
        sendSms(toNumber, "MedSetu Reminder: Please take " + medicineName
                + (dosage != null ? " — " + dosage : "") + " now.");
    }
}
