package com.integrityfamily.report.service;

import com.integrityfamily.report.dto.TransformationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Smoke tests para PdfReportService (iText PDF).
 * No requiere mocks: la clase es pura (sin dependencias inyectadas).
 */
@DisplayName("PdfReportService")
class PdfReportServiceTest {

    private final PdfReportService service = new PdfReportService();

    private TransformationSummary summary(String familyName) {
        return new TransformationSummary(
                1L, familyName, 40.0, 72.5, 80.0, 65.0,
                3L, 15L, "M3", Collections.emptyList()
        );
    }

    @Test
    @DisplayName("genera un byte array de PDF no vacio con datos validos")
    void generateTransformationReport_validInputs_returnsNonEmptyPdf() {
        byte[] pdf = service.generateTransformationReport(summary("Los García"), "Análisis de integridad.");

        assertThat(pdf).isNotNull().isNotEmpty();
        // El header de PDF siempre empieza con %PDF-
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("el PDF generado supera 1 KB (contiene contenido real de iText)")
    void generateTransformationReport_contentHasMinimumSize() {
        byte[] pdf = service.generateTransformationReport(
                summary("Familia Prueba"),
                "Narrativa de IA con detalles extensos sobre la evolución familiar.");

        assertThat(pdf.length).isGreaterThan(1_000);
    }

    @Test
    @DisplayName("lanza excepcion cuando summary es null (NPE propagada o envuelta)")
    void generateTransformationReport_nullSummary_throwsRuntimeException() {
        // El servicio no guarda la NPE en su catch (ocurre antes del try),
        // así que esperamos cualquier RuntimeException (NPE extiende RuntimeException).
        assertThatThrownBy(() -> service.generateTransformationReport(null, "narrativa"))
                .isInstanceOf(RuntimeException.class);
    }
}
