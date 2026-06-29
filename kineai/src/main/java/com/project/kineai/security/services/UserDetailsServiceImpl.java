package com.project.kineai.security.services;

import com.project.kineai.model.entity.User;
import com.project.kineai.model.enums.Role;
import com.project.kineai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // 1. Charger l'utilisateur actif
        User user = userRepository
                .findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilisateur non trouvé ou inactif : " + email));

        // 2. Vérifier kiné validé — RG-38
        if (user.getRole() == Role.KINE) {
            boolean validated = user.getKinesitherapeute() != null
                    && user.getKinesitherapeute().getValidated();
            if (!validated) {
                throw new UsernameNotFoundException(
                        "Compte kiné en attente de validation " +
                                "par l'administrateur");
            }
        }

        // 3. Construire UserDetails
        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.getActive())     // ✅ inverse de active
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(List.of(new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().name())))
                .build();
    }
}