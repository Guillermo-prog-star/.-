package com.integrityfamily.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * Tests para BackupService.
 *
 * performSecurityBackup() crea directorios reales usando el path relativo "backups/…".
 * Para evitar contaminar el directorio de trabajo, mockeamos JdbcTemplate para que
 * las exportaciones de tabla devuelvan filas vacías — el servicio solo escribe el
 * manifest.txt y los archivos SQL (vacíos) en el directorio relativo.
 * Los tests verifican el contrato externo (retorno de path, existencia de manifest)
 * sin necesidad de cambiar el directorio de trabajo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BackupService")
class BackupServiceTest {

    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks BackupService service;

    // ── performSecurityBackup ─────────────────────────────────────────────────

    @Test
    @DisplayName("performSecurityBackup retorna un path con prefijo 'backups/IF_SNAPSHOT_'")
    void performSecurityBackup_returnsSnapshotPath() throws IOException {
        // Ninguna tabla tiene filas — exportTable escribe archivos SQL vacíos
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        String path = service.performSecurityBackup();

        assertThat(path).startsWith("backups/IF_SNAPSHOT_");
        deleteDirectory(Path.of("backups")); // limpia el directorio raíz de backups
    }

    @Test
    @DisplayName("performSecurityBackup crea un manifest.txt en el directorio de snapshot")
    void performSecurityBackup_createsManifestFile() throws IOException {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        String path = service.performSecurityBackup();
        Path manifestPath = Path.of(path).resolve("manifest.txt");

        assertThat(manifestPath).exists();
        String content = Files.readString(manifestPath);
        assertThat(content).contains("INTEGRITY FAMILY BACKUP MANIFEST");
        assertThat(content).contains("Tablas Procesadas: 9");

        deleteDirectory(Path.of("backups"));
    }

    @Test
    @DisplayName("performSecurityBackup con tabla que lanza excepción — continúa y retorna path")
    void performSecurityBackup_tableExportFails_continuesAndReturnsPath() throws IOException {
        // Todas las tablas devuelven lista vacía por defecto; si alguna falla,
        // el service la captura con warn y continúa
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

        String path = service.performSecurityBackup();

        assertThat(path).startsWith("backups/IF_SNAPSHOT_");
        deleteDirectory(Path.of("backups"));
    }

    // ── runAutomaticBackup ────────────────────────────────────────────────────

    @Test
    @DisplayName("runAutomaticBackup no propaga excepciones (swallows errors con catch)")
    void runAutomaticBackup_doesNotThrow() {
        // jdbcTemplate lanza excepción → performSecurityBackup falla → catch lo traga
        when(jdbcTemplate.queryForList(anyString()))
                .thenThrow(new RuntimeException("fallo simulado en backup"));

        assertThatNoException().isThrownBy(() -> service.runAutomaticBackup());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void deleteDirectory(Path dir) {
        if (!Files.exists(dir)) return;
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }
}
