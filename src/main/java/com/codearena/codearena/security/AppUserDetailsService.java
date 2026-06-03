package com.codearena.codearena.security;

import com.codearena.codearena.model.User;
import com.codearena.codearena.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bridges our {@link User} entity to Spring Security's authentication machinery.
 *
 * <p>Spring Security knows nothing about our {@code User} table; it asks a
 * {@link UserDetailsService} to load a user by name and hand back a
 * {@link UserDetails} (username, password hash, authorities). The
 * {@code AuthenticationManager} then compares the submitted password against
 * that hash, and the {@code JwtAuthenticationFilter} uses this to rebuild the
 * principal from a token.
 *
 * <p>The role becomes an authority with the {@code ROLE_} prefix that
 * {@code hasRole(...)} expects (so {@code ADMIN} → {@code ROLE_ADMIN}).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build();
    }
}
