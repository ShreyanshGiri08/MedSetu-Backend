package com.medsetu.service;

import com.medsetu.dto.request.ForgotPasswordRequest;
import com.medsetu.dto.request.LoginRequest;
import com.medsetu.dto.request.RegisterRequest;
import com.medsetu.dto.request.ResetPasswordRequest;
import com.medsetu.dto.response.LoginResponse;
import com.medsetu.entity.Doctor;
import com.medsetu.entity.OtpStore;
import com.medsetu.entity.User;
import com.medsetu.exception.DuplicateResourceException;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.UnauthorizedException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.DoctorRepository;
import com.medsetu.repository.OtpStoreRepository;
import com.medsetu.repository.UserRepository;
import com.medsetu.util.EmailUtil;
import com.medsetu.util.JwtUtil;
import com.medsetu.util.OtpUtil;
import jakarta.mail.MessagingException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final OtpStoreRepository otpStoreRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpUtil otpUtil;
    private final EmailUtil emailUtil;

    public AuthService(UserRepository userRepository,
                       DoctorRepository doctorRepository,
                       OtpStoreRepository otpStoreRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       OtpUtil otpUtil,
                       EmailUtil emailUtil) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.otpStoreRepository = otpStoreRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpUtil = otpUtil;
        this.emailUtil = emailUtil;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid role. Must be PATIENT, DOCTOR, or ADMIN.");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // If registering as doctor, create a doctor profile
        if (role == User.Role.DOCTOR) {
            Doctor doctor = Doctor.builder()
                    .user(user)
                    .isApproved(false)
                    .build();
            doctorRepository.save(doctor);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new UnauthorizedException("Your account has been deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + request.getEmail()));

        String otpCode = otpUtil.generateOtp();

        OtpStore otpStore = OtpStore.builder()
                .email(request.getEmail())
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .build();

        otpStoreRepository.save(otpStore);

        try {
            emailUtil.sendOtpEmail(request.getEmail(), otpCode);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email. Please try again.");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        OtpStore otpStore = otpStoreRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new ValidationException("No valid OTP found. Please request a new one."));

        if (otpStore.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("OTP has expired. Please request a new one.");
        }

        if (!otpStore.getOtpCode().equals(request.getOtp())) {
            throw new ValidationException("Invalid OTP.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpStore.setIsUsed(true);
        otpStoreRepository.save(otpStore);
    }

    public LoginResponse refreshToken(String currentToken) {
        if (!jwtUtil.validateToken(currentToken)) {
            throw new UnauthorizedException("Invalid or expired token.");
        }

        String email = jwtUtil.extractEmail(currentToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String newToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return LoginResponse.builder()
                .token(newToken)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
