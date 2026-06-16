#!/usr/bin/env bash
# ============================================================
#  Integrity Family — MySQL Restore
#  Uso: ./scripts/restore-mysql.sh <archivo_backup>
#
#  Restaura un dump (plain SQL o .sql.gz) en integrity_family
#  ⚠️  DESTRUCTIVO: sobreescribe la BD actual
# ============================================================
set -euo pipefail

DB_CONTAINER="integrity-db"
DB_NAME="integrity_family"
DB_USER="root"
DB_PASS="root123"

if [[ $# -lt 1 ]]; then
  echo "Uso: $0 <archivo_backup.sql|.sql.gz>"
  echo ""
  echo "Backups disponibles:"
  ls -lh "$(dirname "$0")/../backups"/if_backup_* 2>/dev/null | awk '{print "  " $5 "  " $9}' || echo "  (ninguno)"
  exit 1
fi

BACKUP_FILE="$1"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "❌  Archivo no encontrado: $BACKUP_FILE"
  exit 1
fi

echo "⚠️  RESTORE — Integrity Family MySQL"
echo "   Archivo : $BACKUP_FILE"
echo "   BD      : $DB_NAME @ $DB_CONTAINER"
echo ""
read -rp "¿Confirmas que quieres SOBREESCRIBIR la BD actual? [s/N] " CONFIRM
if [[ "$CONFIRM" != "s" && "$CONFIRM" != "S" ]]; then
  echo "Operación cancelada."
  exit 0
fi

if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "❌  El contenedor '$DB_CONTAINER' no está activo."
  exit 1
fi

echo "⏳  Restaurando..."

if [[ "$BACKUP_FILE" == *.gz ]]; then
  gunzip -c "$BACKUP_FILE" | docker exec -i "$DB_CONTAINER" \
    mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME"
else
  docker exec -i "$DB_CONTAINER" \
    mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" < "$BACKUP_FILE"
fi

echo "✅  Restore completado exitosamente."
