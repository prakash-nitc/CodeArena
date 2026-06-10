package com.codearena.codearena.model;

/**
 * Programming language a submission is written in.
 *
 * <p>An enum keeps the accepted set fixed and type-safe. When a real code
 * execution engine arrives (Phase 8), each value would map to a compiler/runtime
 * image; for now it is just metadata stored with the submission.
 */
public enum Language {
    JAVA,
    PYTHON,
    CPP,
    JAVASCRIPT
}
