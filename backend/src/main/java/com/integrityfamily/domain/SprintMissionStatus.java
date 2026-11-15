package com.integrityfamily.domain;

/**
 * Estado de una misión de sprint. Se persiste como texto en
 * {@code sprint_missions.status} (VARCHAR(30)).
 *
 * <p>{@code CHANGED_BY_AGREEMENT} y {@code POSTPONED} no son incumplimiento:
 * la familia decidió cambiar o dejar para después lo previsto. Se excluyen del
 * denominador de cumplimiento y se muestran aparte de completadas y pendientes.
 */
public enum SprintMissionStatus {
    PENDING,
    COMPLETED,
    CHANGED_BY_AGREEMENT,
    POSTPONED;

    public boolean is(String raw) {
        return name().equals(raw);
    }

    /** Las misiones que siguen comprometidas (completadas o pendientes). */
    public static boolean countsAsCommitted(String raw) {
        return !CHANGED_BY_AGREEMENT.is(raw) && !POSTPONED.is(raw);
    }
}
