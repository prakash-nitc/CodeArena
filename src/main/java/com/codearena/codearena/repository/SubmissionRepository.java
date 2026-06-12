package com.codearena.codearena.repository;

import com.codearena.codearena.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Submission}s.
 *
 * <p>The {@code findBy...} methods are derived queries that navigate nested
 * properties: {@code ProblemId} → {@code problem.id} and {@code UserUsername} →
 * {@code user.username}. They power the "submissions for a problem" and
 * "my submissions" views (newest first).
 *
 * <p>{@link #leaderboard()} is an explicit JPQL aggregation: it counts the
 * distinct problems each user has had <em>accepted</em>, grouped per user and
 * ranked highest first. Counting <em>distinct problems</em> (not raw accepted
 * submissions) means re-solving the same problem doesn't inflate a score.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByProblemIdOrderByCreatedAtDesc(Long problemId);

    List<Submission> findByProblemIdAndUserUsernameOrderByCreatedAtDesc(Long problemId, String username);

    List<Submission> findByUserUsernameOrderByCreatedAtDesc(String username);

    @Query("""
            select s.user.username as username, count(distinct s.problem.id) as solvedCount
            from Submission s
            where s.status = com.codearena.codearena.model.SubmissionStatus.ACCEPTED
            group by s.user.username
            order by count(distinct s.problem.id) desc, s.user.username asc
            """)
    List<LeaderboardRow> leaderboard();
}
