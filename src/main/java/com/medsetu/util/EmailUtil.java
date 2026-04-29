package com.medsetu.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailUtil {

    private final JavaMailSender mailSender;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPlainText(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(message);
    }

    public void sendHtml(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }

    public void sendOtpEmail(String to, String otp) throws MessagingException {
        String html = """
                <div style="font-family:Poppins,sans-serif;max-width:500px;margin:auto;padding:30px;border-radius:12px;background:#f0f8ff;border:1px solid #caf0f8">
                  <h2 style="color:#0077b6;text-align:center">MedSetu OTP Verification</h2>
                  <p style="color:#333">Your One-Time Password (OTP) is:</p>
                  <div style="font-size:2.5rem;font-weight:700;color:#0a5cc2;text-align:center;letter-spacing:8px;padding:20px;background:white;border-radius:8px;margin:20px 0">%s</div>
                  <p style="color:#666;font-size:0.9rem">This OTP expires in 10 minutes. Do not share it with anyone.</p>
                  <hr style="border-color:#caf0f8"/>
                  <p style="color:#999;font-size:0.8rem;text-align:center">— The MedSetu Team</p>
                </div>
                """.formatted(otp);
        sendHtml(to, "MedSetu — Your OTP Code", html);
    }

    public void sendAppointmentConfirmation(String to, String patientName, String doctorName, String dateTime) throws MessagingException {
        String html = """
                <div style="font-family:Poppins,sans-serif;max-width:500px;margin:auto;padding:30px;border-radius:12px;background:#f0f8ff;border:1px solid #caf0f8">
                  <h2 style="color:#0077b6;text-align:center">Appointment Confirmed ✅</h2>
                  <p style="color:#333">Dear <strong>%s</strong>,</p>
                  <p>Your appointment with <strong>%s</strong> has been confirmed.</p>
                  <p><strong>Date & Time:</strong> %s</p>
                  <p style="color:#666;font-size:0.9rem">Please arrive 10 minutes early. If you need to cancel, please do so at least 2 hours before.</p>
                  <hr style="border-color:#caf0f8"/>
                  <p style="color:#999;font-size:0.8rem;text-align:center">— The MedSetu Team</p>
                </div>
                """.formatted(patientName, doctorName, dateTime);
        sendHtml(to, "MedSetu — Appointment Confirmed", html);
    }
}
