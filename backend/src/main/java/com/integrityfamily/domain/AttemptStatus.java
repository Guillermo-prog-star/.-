package com.integrityfamily.domain;

/**
 * Estado del intento de la conducta practicada en un check-in diario del
 * sprint (V114). Separa "no hubo oportunidad" de "no se intentó" y de
 * "intentado sin resultado favorable": son situaciones distintas y un día
 * sin oportunidad no cuenta como fracaso. null = no respondido.
 */
public enum AttemptStatus {
    NO_OPPORTUNITY,
    NOT_ATTEMPTED,
    ATTEMPTED_UNFAVORABLE,
    ATTEMPTED_FAVORABLE
}
