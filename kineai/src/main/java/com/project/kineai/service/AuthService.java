package com.project.kineai.service;

import com.project.kineai.dto.request.CreateKineRequest;
import com.project.kineai.dto.request.CreatePatientRequest;
import com.project.kineai.dto.request.LoginRequest;
import com.project.kineai.dto.response.AuthResponse;
import com.project.kineai.mapper.UserMapper;
import com.project.kineai.exception.BusinessException;
import com.project.kineai.model.entity.Kinesitherapeute;
import com.project.kineai.model.entity.Patient;
import com.project.kineai.model.entity.User;
import com.project.kineai.model.enums.Level;
import com.project.kineai.model.enums.Role;
import com.project.kineai.repository.KineRepository;
import com.project.kineai.repository.PatientRepository;
import com.project.kineai.repository.UserRepository;
import com.project.kineai.security.Jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final KineRepository kineRepository;
    private  final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    //-------Login-------------------
    public AuthResponse Login(LoginRequest login) {
        // 1. Vérifier credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        login.getEmail(),
                        login.getPassword()
                ));

        // 2. Charger utilisateur
        User user = userRepository
                .findByEmailAndActiveTrue(login.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "Compte inactif ou introuvable"));

        // ✅ NOUVEAU — Vérifier validation kiné AVANT
        // de construire la réponse
        if (user.getRole() == Role.KINE) {
            Kinesitherapeute kine = user.getKinesitherapeute();
            if (kine == null || !kine.getValidated()) {
                throw new RuntimeException(
                        "Votre compte est en attente de validation"
                                + " par l'administrateur. Vous recevrez"
                                + " un accès dès qu'il sera validé.");
            }
        }

        // 3. Construire AuthResponse selon le role
        AuthResponse response = switch (user.getRole()) {
            case Role.PATIENT -> userMapper.toAuthResponse(
                    user, user.getPatient());
            case Role.KINE -> userMapper.toAuthResponse(
                    user, user.getKinesitherapeute());
            case Role.ADMIN -> AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .role(user.getRole().name())
                    .fullName("Administrateur")
                    .build();
        };

        // 4. Ajouter tokens JWT
        response.setAccessToken(jwtUtils.generateAccessToken(
                user.getEmail(), user.getRole().name()));
        response.setRefreshToken(jwtUtils.generateRefreshToken(
                user.getEmail()));

        log.info("Connexion réussie : {}", user.getEmail());
        return response;
    }

    //---- Inscription Patient---------
    @Transactional
    public AuthResponse registerPatient(CreatePatientRequest request){
        // 1. Vérifier email unique
        if(userRepository.existsByEmail(request.getEmail())){
            throw new BusinessException("Email déjà utilisé");
        }
        // 2. Vérifier que le kiné existe
        Kinesitherapeute kinesitherapeute = kineRepository.findById(request.getKineId())
                .orElseThrow(() -> new BusinessException("Kinésithérapeute introuvable"));

        // 3. Créer User
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.PATIENT)
                .build();
        user = userRepository.save(user);

        // 4. Créer Patient
        Patient patient = Patient.builder()
                .user(user)
                .fullName(request.getFullName())
                .age(request.getAge())
                .phone(request.getPhone())
                .pathology(request.getPathology())
                .kine(kinesitherapeute)
                .level(Level.DEBUTANT)
                .build();
        patientRepository.save(patient);

        log.info("Patient inscrit : {}", user.getEmail());
        return buildAuthResponse(user, patient.getFullName());
    }

    //------- 5. Inscription Kiné------
        @Transactional
        public AuthResponse registerKine(CreateKineRequest request) {
            // 1. Vérifier email unique
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("Email déjà utilisé");
            }

            // 2. Créer User
            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.KINE)
                    .build();
            user = userRepository.save(user);

            // 3. Créer Kiné — validated = false (RG-38)
            Kinesitherapeute kine = Kinesitherapeute.builder()
                    .user(user)
                    .fullName(request.getFullName())
                    .speciality(request.getSpeciality())
                    .validated(false)
                    .build();
            kineRepository.save(kine);

            log.info("Kiné inscrit (en attente validation) : {}", user.getEmail());
            return buildAuthResponse(user, kine.getFullName());
        }
    // ── Refresh Token ─────────────────────────
    public AuthResponse refresh(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("Refresh token invalide ou expiré");
        }

        String email = jwtUtils.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        return buildAuthResponse(user,
                user.getPatient() != null
                        ? user.getPatient().getFullName()
                        : user.getKinesitherapeute() != null
                        ? user.getKinesitherapeute().getFullName()
                        : "Administrateur");
    }

    // ── Helper ────────────────────────────────
    private AuthResponse buildAuthResponse(User user, String fullName) {
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(
                        user.getEmail(), user.getRole().name()))
                .refreshToken(jwtUtils.generateRefreshToken(user.getEmail()))
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(fullName)
                .build();
    }
    }

