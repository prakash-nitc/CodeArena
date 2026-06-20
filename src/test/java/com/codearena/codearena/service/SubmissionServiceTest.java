package com.codearena.codearena.service;

import com.codearena.codearena.dto.LeaderboardEntry;
import com.codearena.codearena.dto.SubmissionRequest;
import com.codearena.codearena.dto.SubmissionResponse;
import com.codearena.codearena.exception.ProblemNotFoundException;
import com.codearena.codearena.exception.SubmissionNotFoundException;
import com.codearena.codearena.model.Difficulty;
import com.codearena.codearena.model.Language;
import com.codearena.codearena.model.Problem;
import com.codearena.codearena.model.Role;
import com.codearena.codearena.model.Submission;
import com.codearena.codearena.model.SubmissionStatus;
import com.codearena.codearena.model.User;
import com.codearena.codearena.repository.LeaderboardRow;
import com.codearena.codearena.repository.ProblemRepository;
import com.codearena.codearena.repository.SubmissionRepository;
import com.codearena.codearena.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SubmissionService} — no Spring, repositories mocked.
 * Focuses on the create flow, the ownership rule, and leaderboard ranking.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ProblemRepository problemRepository;
    @Mock
    private UserRepository userRepository;

    private SubmissionService service;

    private Problem problem;
    private User alice;
    private User bob;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new SubmissionService(submissionRepository, problemRepository, userRepository);
        Instant now = Instant.now();
        problem = Problem.builder().id(1L).title("Two Sum").difficulty(Difficulty.EASY)
                .tags(List.of()).createdAt(now).build();
        alice = User.builder().id(10L).username("alice").password("x").role(Role.USER).createdAt(now).build();
        bob = User.builder().id(11L).username("bob").password("x").role(Role.USER).createdAt(now).build();
        admin = User.builder().id(1L).username("admin").password("x").role(Role.ADMIN).createdAt(now).build();
    }

    private Submission submissionOwnedBy(Long id, User owner) {
        return Submission.builder().id(id).problem(problem).user(owner)
                .language(Language.JAVA).sourceCode("code").status(SubmissionStatus.PENDING)
                .createdAt(Instant.now()).build();
    }

    @Test
    void create_savesPendingSubmission() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(problem));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> {
            Submission s = inv.getArgument(0);
            s.setId(100L);
            return s;
        });

        SubmissionResponse response = service.create(1L, new SubmissionRequest(Language.JAVA, "code"), "alice");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(response.getProblemId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("alice");
    }

    @Test
    void create_throwsWhenProblemMissing() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(999L, new SubmissionRequest(Language.JAVA, "code"), "alice"))
                .isInstanceOf(ProblemNotFoundException.class);
    }

    @Test
    void getById_returnsForOwner() {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submissionOwnedBy(100L, alice)));

        assertThat(service.getById(100L, "alice").getId()).isEqualTo(100L);
    }

    @Test
    void getById_deniesForOtherNonAdmin() {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submissionOwnedBy(100L, alice)));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> service.getById(100L, "bob"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_allowsAdminForOthersSubmission() {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submissionOwnedBy(100L, alice)));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThat(service.getById(100L, "admin").getUsername()).isEqualTo("alice");
    }

    @Test
    void getById_throwsWhenMissing() {
        when(submissionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(404L, "alice"))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    @Test
    void updateStatus_changesStatus() {
        when(submissionRepository.findById(100L)).thenReturn(Optional.of(submissionOwnedBy(100L, alice)));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.updateStatus(100L, SubmissionStatus.ACCEPTED).getStatus())
                .isEqualTo(SubmissionStatus.ACCEPTED);
    }

    @Test
    void leaderboard_assignsRanksInOrder() {
        LeaderboardRow first = mock(LeaderboardRow.class);
        when(first.getUsername()).thenReturn("alice");
        when(first.getSolvedCount()).thenReturn(3L);
        LeaderboardRow second = mock(LeaderboardRow.class);
        when(second.getUsername()).thenReturn("bob");
        when(second.getSolvedCount()).thenReturn(1L);
        when(submissionRepository.leaderboard()).thenReturn(List.of(first, second));

        List<LeaderboardEntry> board = service.leaderboard();

        assertThat(board).hasSize(2);
        assertThat(board.get(0).getRank()).isEqualTo(1);
        assertThat(board.get(0).getUsername()).isEqualTo("alice");
        assertThat(board.get(0).getSolvedCount()).isEqualTo(3L);
        assertThat(board.get(1).getRank()).isEqualTo(2);
        assertThat(board.get(1).getUsername()).isEqualTo("bob");
    }
}
