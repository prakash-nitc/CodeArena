package com.codearena.codearena.config;

import com.codearena.codearena.security.JwtAuthenticationFilter;
import com.codearena.codearena.security.JwtService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The real security configuration (Phase 6) — it replaces the temporary
 * permit-all chain from Phases 2–5.
 *
 * <p>Key choices:
 * <ul>
 *   <li><strong>Stateless</strong> ({@code SessionCreationPolicy.STATELESS}) — no
 *       HTTP session; identity comes from the JWT on every request.</li>
 *   <li><strong>CSRF disabled</strong> — CSRF protects cookie/session-based
 *       browsers; a stateless token API doesn't need it.</li>
 *   <li>The {@link JwtAuthenticationFilter} runs before the username/password
 *       filter so a valid token authenticates the request.</li>
 *   <li>Custom {@code AuthenticationEntryPoint}/{@code AccessDeniedHandler}
 *       return JSON 401/403 instead of HTML.</li>
 * </ul>
 *
 * <p>Authorization rules (first match wins):
 * <ul>
 *   <li>{@code /api/auth/**} and the H2 console — public</li>
 *   <li>{@code GET} on ping/problems — public (anyone can browse problems)</li>
 *   <li>{@code DELETE /api/problems/**} — {@code ADMIN} only</li>
 *   <li>everything else (e.g. {@code POST}/{@code PUT}) — any authenticated user</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   UserDetailsService userDetailsService,
                                                   AuthenticationEntryPoint authenticationEntryPoint,
                                                   AccessDeniedHandler accessDeniedHandler) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(PathRequest.toH2Console()).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ping", "/api/problems", "/api/problems/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/problems/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
