package com.xaxage.be.domain.model;

/**
 * One way a Pokémon learns a move in one version group.
 *
 * @param method       how the move is learned, e.g. "level-up", "machine", "egg" or "tutor"
 * @param level        the level it is learned at when levelling up, otherwise {@code null}
 * @param versionGroup the version group (games) this way of learning applies to
 */
public record LearnMethod(String method, Integer level, String versionGroup) {
}
