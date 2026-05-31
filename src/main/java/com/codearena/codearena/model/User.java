package com.codearena.codearena.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity for an application user.
 *
 * <p>Note the {@code password} field stores a <strong>BCrypt hash</strong>, never
 * the plaintext password — hashing happens in {@code AuthService} before save.
 * {@code username} is unique so two accounts can't share a name.
 *
 * <p>This entity deliberately does <em>not</em> implement Spring Security's
 * {@code UserDetails} interface. Keeping persistence (this entity) separate from
 * the security adapter ({@code AppUserDetailsService} builds a {@code UserDetails}
 * from it) avoids mixing two concerns in one class.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** BCrypt hash of the password — not the plaintext. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
