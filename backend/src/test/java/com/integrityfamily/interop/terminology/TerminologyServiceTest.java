package com.integrityfamily.interop.terminology;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TerminologyService")
class TerminologyServiceTest {

    @Test
    @DisplayName("código no registrado → Optional vacío, no excepción")
    void shouldReturnEmpty_forUnknownCode() {
        TerminologyService service = new TerminologyService();

        assertThat(service.lookup("ICF")).isEmpty();
    }

    @Test
    @DisplayName("código registrado → lookup lo devuelve completo")
    void shouldReturnRegisteredMapping() {
        TerminologyService service = new TerminologyService();
        ConceptMapping mapping = new ConceptMapping("ICF", "http://example.org/test-system", "999999", "Ejemplo", "1.0");
        service.register(mapping);

        assertThat(service.lookup("ICF")).contains(mapping);
    }

    @Test
    @DisplayName("sin ningún register() → el mapa está vacío por diseño (sin códigos clínicos sin validar)")
    void shouldStartEmptyByDesign() {
        TerminologyService service = new TerminologyService();

        assertThat(service.lookup("VIOLENCIA_INTRAFAMILIAR")).isEmpty();
        assertThat(service.lookup("IDEACION_SUICIDA")).isEmpty();
    }
}
