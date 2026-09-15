package com.integrityfamily.interop.terminology;

/**
 * Un vínculo entre un código propio de Integrity y su equivalente en una
 * terminología estándar. Se guardan ambos lados a propósito (nunca se
 * reemplaza el código de Integrity por el estándar) — así una familia sigue
 * siendo interpretable dentro de Integrity aunque el mapeo cambie o se
 * retire.
 */
public record ConceptMapping(
        String integrityCode,
        String targetSystem,
        String targetCode,
        String targetDisplay,
        String targetVersion
) {}
