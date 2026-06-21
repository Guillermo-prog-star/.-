#!/usr/bin/env bash
# ============================================================
#  Integrity Family — Verificación de Restore (NO destructivo)
#  Uso: ./scripts/verify-restore.sh [archivo_backup]
#
#  Restaura en BD temporal, compara registros con producción,
#  luego elimina la BD temporal. No toca integrity_family.
# ============================================================
set -euo pipefail

DB_CONTAINER="integrity-db"
DB_PROD="integrity_family"
DB_TEST="integrity_family_verify_restore"
DB_USER="root"
DB_PASS="root123"

# ── Seleccionar backup ───────────────────────────────────────
if [[ $# -ge 1 ]]; then
  BACKUP_FILE="$1"
else
  BACKUP_FILE=$(ls -1t "$(dirname "$0")/../backups"/if_backup_*.sql.gz 2>/dev/null | head -1)
  if [[ -z "$BACKUP_FILE" ]]; then
    echo "No se encontró ningún backup en /backups/"
    exit 1
  fi
fi

echo "================================================="
echo "  Integrity Family — Verificacion de Restore"
echo "================================================="
echo "  Backup : $BACKUP_FILE"
echo "  BD temp: $DB_TEST"
echo ""

# ── Verificar contenedor ─────────────────────────────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "El contenedor '$DB_CONTAINER' no esta activo."
  echo "Ejecuta: docker compose up -d db"
  exit 1
fi

# ── Crear BD temporal ────────────────────────────────────────
echo "[1/4] Creando BD temporal..."
docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" \
  -e "DROP DATABASE IF EXISTS \`$DB_TEST\`; CREATE DATABASE \`$DB_TEST\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# ── Restaurar backup en BD temporal ─────────────────────────
echo "[2/4] Restaurando backup en BD temporal..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$DB_CONTAINER" \
  mysql -u"$DB_USER" -p"$DB_PASS" "$DB_TEST"

# ── Comparar registros ───────────────────────────────────────
echo "[3/4] Comparando registros prod vs restore..."
echo ""

TABLAS="families family_members evaluations improvement_plans plan_tasks task_evidences family_documentaries family_chapter_progress"

TODOS_OK=true
printf "%-35s %10s %10s %8s\n" "Tabla" "Produccion" "Restore" "Estado"
printf "%-35s %10s %10s %8s\n" "-----" "----------" "-------" "------"

for TABLA in $TABLAS; do
  PROD=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -sN \
    -e "SELECT COUNT(*) FROM \`$DB_PROD\`.\`$TABLA\`;" 2>/dev/null || echo "N/A")
  REST=$(docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" -sN \
    -e "SELECT COUNT(*) FROM \`$DB_TEST\`.\`$TABLA\`;" 2>/dev/null || echo "N/A")

  if [[ "$PROD" == "$REST" ]]; then
    STATUS="OK"
  else
    STATUS="DIFERENTE"
    TODOS_OK=false
  fi

  printf "%-35s %10s %10s %8s\n" "$TABLA" "$PROD" "$REST" "$STATUS"
done

echo ""

# ── Eliminar BD temporal ─────────────────────────────────────
echo "[4/4] Eliminando BD temporal..."
docker exec "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" \
  -e "DROP DATABASE IF EXISTS \`$DB_TEST\`;"

# ── Resultado final ──────────────────────────────────────────
echo ""
echo "================================================="
BACKUP_SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
BACKUP_DATE=$(basename "$BACKUP_FILE" | grep -oP '\d{8}_\d{6}')

if $TODOS_OK; then
  echo "  RESULTADO: RESTORE VERIFICADO CORRECTAMENTE"
  echo "  Backup  : $(basename $BACKUP_FILE) ($BACKUP_SIZE)"
  echo "  Fecha   : $BACKUP_DATE"
  echo "  Tablas  : 8/8 coinciden"
  echo ""
  echo "  Actualiza docs/backup-restore.md con esta verificacion."
else
  echo "  RESULTADO: DIFERENCIAS DETECTADAS - REVISAR"
fi
echo "================================================="
