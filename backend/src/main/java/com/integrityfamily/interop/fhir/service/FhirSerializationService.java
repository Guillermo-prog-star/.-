package com.integrityfamily.interop.fhir.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

/**
 * Serializa recursos FHIR a JSON usando el {@code FhirContext} compartido
 * (ver {@code FhirConfig}) — el parser de HAPI no es thread-safe para
 * reutilizar entre hilos, así que se crea uno nuevo por llamada; es barato
 * comparado con crear el FhirContext mismo.
 */
@Service
@RequiredArgsConstructor
public class FhirSerializationService {

    private final FhirContext fhirContext;

    public String toJson(Resource resource) {
        IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);
        return parser.encodeResourceToString(resource);
    }
}
