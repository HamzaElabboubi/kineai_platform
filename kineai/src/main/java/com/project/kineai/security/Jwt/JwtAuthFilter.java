package com.project.kineai.security.Jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraire le token du header
        String token = extractToken(request);

        // 2. Valider et authentifier
        if (token != null && jwtUtils.validateToken(token)) {
            {
                String email = jwtUtils.getEmailFromToken(token);
                UserDetails userDetails = userDetailsService
                        .loadUserByUsername(email);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                auth.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                // 3. Mettre dans le contexte Spring Security
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Utilisateur authentifié : {}", email);
            }

        }
            filterChain.doFilter(request, response);
    }


        // ── Extraire token du header Authorization ─
        private String extractToken(HttpServletRequest request){
            String header = request.getHeader("Authorization");
            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return null;
        }
    }
