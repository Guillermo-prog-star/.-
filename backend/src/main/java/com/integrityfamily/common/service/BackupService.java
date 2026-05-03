package com.integrityfamily.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * SDD: PROTOCOLO SENTINEL AUTOMÃƒÂTICO
     * Se ejecuta cada dÃƒÂ­a a las 2:00 AM para asegurar la integridad de la Fase Alfa.
     */
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 2 * * *")
    public void runAutomaticBackup() {
        log.info("Ã°Å¸â€¢â€™ [SENTINEL-AUTO] Iniciando ciclo de respaldo programado...");
        try {
            String path = performSecurityBackup();
            log.info("Ã¢Å“â€¦ [SENTINEL-AUTO] Respaldo diario completado en: {}", path);
            rotateBackups();
        } catch (Exception e) {
            log.error("Ã¢ÂÅ’ [SENTINEL-AUTO] Falla crÃƒÂ­tica en el auto-respaldo: {}", e.getMessage());
        }
    }

    public String performSecurityBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupDir = "backups/IF_SNAPSHOT_" + timestamp;
        
        Path path = Paths.get(backupDir);
        Files.createDirectories(path);

        log.info("Ã°Å¸â€ºÂ¡Ã¯Â¸Â [BACKUP] Resguardando integridad del Nodo en: {}", backupDir);

        // 1. Exportar Tablas CrÃƒÂ­ticas (Estado del Sistema)
        String[] tables = {
            "users", "families", "family_members", "evaluations", 
            "evaluation_dimension_scores", "risk_snapshots", 
            "admin_alerts", "beta_feedback", "chat_messages"
        };

        for (String table : tables) {
            try {
                exportTable(backupDir + "/" + table + ".sql", table);
            } catch (Exception e) {
                log.warn("Ã¢Å¡Â Ã¯Â¸Â [BACKUP-WARN] No se pudo respaldar la tabla {}: {}", table, e.getMessage());
            }
        }

        // 2. Informe de Integridad (MetaData)
        String manifest = "INTEGRITY FAMILY BACKUP MANIFEST\n" +
                "==================================\n" +
                "Timestamp: " + LocalDateTime.now() + "\n" +
                "Fase: ProducciÃƒÂ³n Alfa (Sentinel Active)\n" +
                "Tablas Procesadas: " + tables.length + "\n" +
                "Arquitecto Responsable: William\n";
        
        Files.writeString(path.resolve("manifest.txt"), manifest);
        return backupDir;
    }

    /**
     * Mantenimiento de Disco: Mantiene solo los ÃƒÂºltimos 7 respaldos.
     */
    private void rotateBackups() {
        try {
            Path backupsPath = Paths.get("backups");
            if (!Files.exists(backupsPath)) return;

            List<Path> dirs = Files.list(backupsPath)
                    .filter(Files::isDirectory)
                    .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                    .toList();

            if (dirs.size() > 7) {
                for (int i = 7; i < dirs.size(); i++) {
                    log.info("Ã°Å¸Â§Â¹ [BACKUP-ROTATE] Depurando respaldo antiguo: {}", dirs.get(i).getFileName());
                    deleteDirectory(dirs.get(i));
                }
            }
        } catch (IOException e) {
            log.error("Ã¢Å¡Â Ã¯Â¸Â [BACKUP-ROTATE] Error al rotar respaldos: {}", e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(java.util.Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        log.error("Falla al borrar {}", p);
                    }
                });
    }

    private void exportTable(String filePath, String tableName) throws IOException {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName);
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("-- Backup for table: " + tableName + "\n");
            for (Map<String, Object> row : rows) {
                StringBuilder columns = new StringBuilder();
                StringBuilder values = new StringBuilder();
                row.forEach((k, v) -> {
                    columns.append(k).append(",");
                    if (v == null) {
                        values.append("NULL,");
                    } else if (v instanceof String) {
                        values.append("'").append(v.toString().replace("'", "''")).append("',");
                    } else {
                        values.append(v).append(",");
                    }
                });
                
                String sql = String.format("INSERT INTO %s (%s) VALUES (%s);\n",
                        tableName,
                        columns.substring(0, columns.length() - 1),
                        values.substring(0, values.length() - 1));
                writer.write(sql);
            }
        }
    }
}


