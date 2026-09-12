package com.integrityfamily.interop.terminology;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Concept Map de códigos propios de Integrity → terminología estándar
 * (SNOMED CT, LOINC, CIE-10).
 *
 * DELIBERADAMENTE VACÍO en esta fase. Mapear códigos clínicos requiere
 * terminología licenciada y validación de un profesional de codificación
 * clínica/informática en salud — no es una decisión que deba tomar
 * unilateralmente un cambio de código. El Banco de Trayectorias de Riesgo
 * (V75/V97) incluye 7 trayectorias con protocolo de seguridad obligatorio
 * (violencia intrafamiliar, ideación suicida, autolesiones, trastorno de
 * alimentación, consumo problemático...), donde un código mal mapeado tiene
 * consecuencias reales, no solo un bug. Además, el ICF (Índice de Cohesión
 * Familiar) es un instrumento propio de Integrity sin equivalente reconocido
 * en terminología internacional — no hay "código correcto" que mapear
 * todavía para varios de sus componentes.
 *
 * El servicio SÍ está listo para usarse: agregar una entrada real (revisada
 * por un profesional de codificación clínica) es una línea con
 * {@link #register}. El resto del pipeline ({@code ObservationFhirMapper},
 * {@code FhirBundleAssembler}) ya consume el resultado de {@link #lookup}
 * automáticamente — no requiere ningún otro cambio.
 */
@Service
public class TerminologyService {

    private final Map<String, ConceptMapping> conceptMap = new HashMap<>();

    public void register(ConceptMapping mapping) {
        conceptMap.put(mapping.integrityCode(), mapping);
    }

    public Optional<ConceptMapping> lookup(String integrityCode) {
        return Optional.ofNullable(conceptMap.get(integrityCode));
    }
}
