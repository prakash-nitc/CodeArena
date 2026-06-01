package com.codearena.codearena.repository;

import com.codearena.codearena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User}s.
 *
 * <p>{@code findByUsername} is used by the {@code UserDetailsService} during
 * authentication; {@code existsByUsername} guards registration so we can reject
 * a duplicate before attempting to save.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
