# Integrity Family — Backup y Restauración

**Última actualización:** 2026-06-20

---

## Backup automático

- **Tarea:** `IntegrityFamily-DailyBackup` (Windows Programador de Tareas)
- **Horario:** 2:00 AM diario
- **Retención:** 30 backups (~30 días)
- **Ubicación:** `C:\Proyectos\if-full\backups\`
- **Formato:** `.sql.gz` (comprimido)
- **Log:** `C:\Proyectos\if-full\backups\backup.log`

### Verificar que la tarea existe
```powershell
Get-ScheduledTask -TaskName "IntegrityFamily-DailyBackup"
```

### Ejecutar backup manual
```bash
# Desde C:\Proyectos\if-full
./scripts/backup-mysql.sh --compress --keep 30
```

---

## Restauración

### Requisito previo
```bash
docker compose up -d db
```

### Restaurar desde backup
```bash
./scripts/restore-mysql.sh backups/if_backup_YYYYMMDD_HHmmss.sql.gz
```

### Verificación post-restore (ejecutar en MySQL)
```sql
SELECT 'families'       AS tabla, COUNT(*) AS registros FROM families
UNION ALL
SELECT 'family_members',          COUNT(*) FROM family_members
UNION ALL
SELECT 'evaluations',             COUNT(*) FROM evaluations
UNION ALL
SELECT 'improvement_plans',       COUNT(*) FROM improvement_plans
UNION ALL
SELECT 'plan_tasks',              COUNT(*) FROM plan_tasks
UNION ALL
SELECT 'task_evidences',          COUNT(*) FROM task_evidences
UNION ALL
SELECT 'family_documentaries',    COUNT(*) FROM family_documentaries
UNION ALL
SELECT 'family_chapter_progress', COUNT(*) FROM family_chapter_progress;
```

---

## Historial de verificaciones

| Fecha | Backup | Resultado | Notas |
|-------|--------|-----------|-------|
| 2026-06-16 | if_backup_20260616_140811.sql.gz (9.6 MB) | OK | 8 tablas verificadas, 100% coincidencia |
| 2026-06-20 | if_backup_20260620_202317.sql.gz (9.7 MB) | OK | Primer backup automático |

**Próxima verificación recomendada:** 2026-07-20

---

## Objetivo de recuperación

- **RPO (pérdida máxima de datos):** 24 horas
- **RTO (tiempo máximo de restauración):** 2 horas
- **Meta:** Restaurar todo el sistema desde cero en menos de 2 horas
