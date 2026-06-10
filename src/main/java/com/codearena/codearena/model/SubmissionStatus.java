package com.codearena.codearena.model;

/**
 * Lifecycle/verdict of a submission.
 *
 * <p>New submissions start as {@link #PENDING}. Because the code execution
 * engine (Phase 8) is out of scope, judging is simulated: an administrator moves
 * a submission to {@link #ACCEPTED} or {@link #REJECTED}. The leaderboard counts
 * only {@code ACCEPTED} submissions.
 */
public enum SubmissionStatus {
    /** Submitted, awaiting judging. */
    PENDING,

    /** Judged correct. */
    ACCEPTED,

    /** Judged incorrect. */
    REJECTED
}
