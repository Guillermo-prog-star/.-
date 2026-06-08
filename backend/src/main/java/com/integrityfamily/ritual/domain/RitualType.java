package com.integrityfamily.ritual.domain;

public enum RitualType {
    CUMPLEANOS,         // Cumpleaños de un miembro
    DOMINGO_FAMILIAR,   // Cada domingo — reflexión semanal
    ANIVERSARIO,        // Aniversario de la creación de la familia en IF
    LOGRO_CELEBRADO,    // Tasa de misiones >= 80 %
    CRISIS_SUPERADA,    // Días desde la última crisis >= 30
    FIN_DE_MES,         // Último día del mes
    SIN_ACTIVIDAD,      // 14+ días sin ningún evento registrado
    RACHA_POSITIVA,     // 7 días seguidos de daily/evidencias
    PRIMER_ANO,         // Primer año completo en la plataforma
    META_ALCANZADA      // Hito de plan completado
}
