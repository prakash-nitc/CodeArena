package com.codearena.codearena.model;

/**
 * Represents how hard a coding problem is.
 *
 * <p>Modelled as an enum so the set of valid values is fixed and type-safe:
 * a {@code Difficulty} can only ever be one of these three constants, which
 * prevents typos like {@code "esay"} from sneaking into the data.
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
