package com.integrityfamily.sistemas.catalog;

import com.integrityfamily.sistemas.dto.SistemasDtos.CatalogResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.ScreenLink;
import com.integrityfamily.sistemas.dto.SistemasDtos.SystemView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SistemasCatalog — los diez sistemas y sus pantallas")
class SistemasCatalogTest {

    private static final Set<String> KNOWN_ROUTES = Set.of(
            "/logbook", "/my-space", "/sprint", "/transformation/weekly-plan", "/crisis",
            "/plans", "/checklist", "/documentation", "/documentary-maker", "/legado",
            "/biography", "/chat", "/family-council", "/transformation/route",
            "/sistemas/revision/diaria", "/sistemas/revision/semanal", "/sistemas/revision/cambio",
            "/sistemas/revision/mensual", "/sistemas/revision/anual"
    );

    @Test
    @DisplayName("hay dos niveles, diez sistemas y solo el 8 se dispara por un cambio")
    void twoLevelsAndTenSystems() {
        CatalogResponse catalog = SistemasCatalog.catalog();

        assertThat(catalog.levels()).hasSize(2);
        assertThat(catalog.levels().get(0).title()).contains("Estructura");
        assertThat(catalog.levels().get(1).title()).contains("Cómo hacer que los sistemas funcionen");
        List<SystemView> systems = catalog.levels().stream().flatMap(level -> level.systems().stream()).toList();
        assertThat(systems).extracting(SystemView::number).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertThat(systems).extracting(SystemView::name)
                .containsExactly("Captura", "Calendario", "Tareas", "Conocimiento", "Comunicación",
                        "Revisión diaria", "Planificación semanal", "Ajuste por cambio",
                        "Revisión mensual", "Revisión anual");
        assertThat(systems).filteredOn(SystemView::eventTriggered).extracting(SystemView::number).containsExactly(8);
        assertThat(catalog.checkQuestions()).containsExactly(
                "¿Hiciste al menos 3 de las 4 revisiones semanales?",
                "¿Llegó algún compromiso a la fecha sin estar registrado?",
                "¿La captura se vació al menos una vez por semana?"
        );
        assertThat(catalog.firstMonth()).hasSize(4);
    }

    @Test
    @DisplayName("cada enlace apunta a una pantalla que ya existe")
    void linksStayInsideExistingScreens() {
        List<ScreenLink> links = SistemasCatalog.catalog().levels().stream()
                .flatMap(level -> level.systems().stream())
                .flatMap(system -> system.links().stream())
                .toList();

        assertThat(links).isNotEmpty();
        assertThat(links).allSatisfy(link -> {
            assertThat(link.label()).isNotBlank();
            assertThat(KNOWN_ROUTES).contains(link.route());
        });
    }
}
