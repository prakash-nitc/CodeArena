package com.codearena.codearena.service;

import com.codearena.codearena.dto.LeaderboardEntry;
import com.codearena.codearena.dto.SubmissionRequest;
import com.codearena.codearena.dto.SubmissionResponse;
import com.codearena.codearena.exception.ProblemNotFoundException;
import com.codearena.codearena.exception.SubmissionNotFoundException;
import com.codearena.codearena.model.Problem;
import com.codearena.codearena.model.Role;
import com.codearena.codearena.model.Submission;
import com.codearena.codearena.model.SubmissionStatus;
import com.codearena.codearena.model.User;
import com.codearena.codearena.repository.LeaderboardRow;
import com.codearena.codearena.repository.ProblemRepository;
import com.codearena.codearena.repository.SubmissionRepository;
import com.codearena.codearena.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for submissions and the leaderboard.
 *
 * <p>Every method is {@code @Transactional}: submissions carry lazy
 * {@code problem}/{@code user} associations, and with Open-Session-In-View
 * disabled (Phase 4) the persistence context must stay open while we map an
 * entity to its DTO. The transaction provides that window.
 *
 * <p><strong>Authorization beyond roles:</strong> a user may only read their own
 * submissions; admins may read anyone's. This <em>ownership</em> check lives here
 * (the service knows who owns what) and throws {@link AccessDeniedException},
 * which surfaces as a 403.
 */
@Service
@Transactional
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final UserRepository userRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                             ProblemRepository problemRepository,
                             UserRepository userRepository) {
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
        this.userRepository = userRepository;
    }

    /** Records a new submission (status {@code PENDING}) by {@code username} for a problem. */
    public SubmissionResponse create(Long problemId, SubmissionRequest request, String username) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new ProblemNotFoundException(problemId));
        User user = requireUser(username);

        Submission submission = Submission.builder()
                .problem(problem)
                .user(user)
                .language(request.getLanguage())
                .sourceCode(request.getSourceCode())
                .status(SubmissionStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        return toResponse(submissionRepository.save(submission));
    }

    /** Fetches one submission, enforcing that the requester owns it (or is an admin). */
    @Transactional(readOnly = true)
    public SubmissionResponse getById(Long submissionId, String username) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        requireOwnerOrAdmin(submission, username);
        return toResponse(submission);
    }

    /**
     * Lists submissions for a problem. Admins see all submissions; a regular user
     * sees only their own.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> listForProblem(Long problemId, String username) {
        if (!problemRepository.existsById(problemId)) {
            throw new ProblemNotFoundException(problemId);
        }
        User requester = requireUser(username);
        List<Submission> submissions = requester.getRole() == Role.ADMIN
                ? submissionRepository.findByProblemIdOrderByCreatedAtDesc(problemId)
                : submissionRepository.findByProblemIdAndUserUsernameOrderByCreatedAtDesc(problemId, username);
        return submissions.stream().map(this::toResponse).toList();
    }

    /** Lists the requester's own submissions, newest first. */
    @Transactional(readOnly = true)
    public List<SubmissionResponse> listMine(String username) {
        return submissionRepository.findByUserUsernameOrderByCreatedAtDesc(username).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Admin-only "judge": moves a submission to a new status. (Access is also
     * restricted to ADMIN at the URL level in SecurityConfig.)
     */
    public SubmissionResponse updateStatus(Long submissionId, SubmissionStatus status) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new SubmissionNotFoundException(submissionId));
        submission.setStatus(status);
        return toResponse(submissionRepository.save(submission));
    }

    /** Builds the leaderboard: users ranked by distinct problems accepted. */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> leaderboard() {
        List<LeaderboardRow> rows = submissionRepository.leaderboard();
        List<LeaderboardEntry> entries = new ArrayList<>(rows.size());
        int rank = 1;
        for (LeaderboardRow row : rows) {
            entries.add(LeaderboardEntry.builder()
                    .rank(rank++)
                    .username(row.getUsername())
                    .solvedCount(row.getSolvedCount())
                    .build());
        }
        return entries;
    }

    // ------------------------------------------------------------------

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private void requireOwnerOrAdmin(Submission submission, String username) {
        boolean isOwner = submission.getUser().getUsername().equals(username);
        if (isOwner) {
            return;
        }
        User requester = requireUser(username);
        if (requester.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("You may only access your own submissions");
        }
    }

    private SubmissionResponse toResponse(Submission submission) {
        return SubmissionResponse.builder()
                .id(submission.getId())
                .problemId(submission.getProblem().getId())
                .problemTitle(submission.getProblem().getTitle())
                .username(submission.getUser().getUsername())
                .language(submission.getLanguage())
                .status(submission.getStatus())
                .sourceCode(submission.getSourceCode())
                .createdAt(submission.getCreatedAt())
                .build();
    }
}
