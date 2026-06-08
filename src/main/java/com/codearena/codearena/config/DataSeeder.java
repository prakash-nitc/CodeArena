package com.codearena.codearena.config;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Role;
import com.codearena.codearena.model.User;
import com.codearena.codearena.repository.ProblemRepository;
import com.codearena.codearena.repository.UserRepository;
import com.codearena.codearena.service.ProblemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Seeds sample data on startup so the API is usable immediately.
 *
 * <p>A {@link CommandLineRunner} runs once, after the context and database are
 * ready. Each seed is guarded by a {@code count() == 0} check, so it is
 * idempotent against a persistent database (it won't duplicate on restart).
 *
 * <p>Phase 6 adds two default accounts so the secured endpoints can be tried
 * out. These credentials are <strong>for local development only</strong>:
 * <ul>
 *   <li>{@code admin} / {@code admin123} — role {@code ADMIN}</li>
 *   <li>{@code user} / {@code user123} — role {@code USER}</li>
 * </ul>
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProblemService problemService;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(ProblemService problemService,
                      ProblemRepository problemRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder) {
        this.problemService = problemService;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedProblems();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        Instant now = Instant.now();
        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ADMIN)
                .createdAt(now)
                .build());
        userRepository.save(User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .role(Role.USER)
                .createdAt(now)
                .build());
    }

    private void seedProblems() {
        if (problemRepository.count() > 0) {
            return;
        }
        problemService.create(new ProblemRequest(
                "Two Sum",
                "Given an array of integers and a target, return indices of the two numbers that add up to the target.",
                Difficulty.EASY,
                List.of("array", "hash-table")));
        problemService.create(new ProblemRequest(
                "Longest Substring Without Repeating Characters",
                "Given a string, find the length of the longest substring without repeating characters.",
                Difficulty.MEDIUM,
                List.of("string", "sliding-window")));
    }
}
