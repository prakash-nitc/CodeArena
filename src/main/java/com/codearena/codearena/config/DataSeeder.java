package com.codearena.codearena.config;

import com.codearena.codearena.dto.ProblemRequest;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.repository.ProblemRepository;
import com.codearena.codearena.service.ProblemService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a couple of example problems on startup so the API has data to show.
 *
 * <p>A {@link CommandLineRunner} runs once, <em>after</em> the application
 * context (and the database) is fully initialised — the correct place to write
 * seed data. (Doing this in the service's {@code @PostConstruct}, as Phase 2/3
 * did, runs too early and is fragile once a real transactional datastore is
 * involved.)
 *
 * <p>The {@code count() > 0} guard makes seeding idempotent: against a
 * persistent PostgreSQL database it won't duplicate rows on every restart. For
 * the in-memory H2 default the table always starts empty, so it always seeds.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ProblemService problemService;
    private final ProblemRepository problemRepository;

    public DataSeeder(ProblemService problemService, ProblemRepository problemRepository) {
        this.problemService = problemService;
        this.problemRepository = problemRepository;
    }

    @Override
    public void run(String... args) {
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
