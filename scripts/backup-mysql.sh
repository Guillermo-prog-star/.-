#!/usr/bin/env bash
# ============================================================
#  Integrity Family — MySQL Backup
#  Uso: ./scripts/backup-mysql.sh [--compress] [--keep N]
#
#  Crea un dump completo de integrity_family en /backups/
#  Retiene los últimos N backups (default: 7)
# ============================================================
set -euo pipefail

# ── Configuración ───────────────────────────────────────────
DB_CONTAINER="integrity-db"
DB_NAME="integrity_family"
DB_USER="root"
DB_PASS="root123"
BACKUP_DIR="$(cd "$(dirname "$0")/.." && pwd)/backups"
KEEP=7
COMPRESS=false

# ── Argumentos ──────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case $1 in
    --compress) COMPRESS=true; shift ;;
    --keep)     KEEP="$2"; shift 2 ;;
    *) echo "Uso: $0 [--compress] [--keep N]"; exit 1 ;;
  esac
done

# ── Preparar directorio ─────────────────────────────────────
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
FILENAME="if_backup_${TIMESTAMP}.sql"
FILEPATH="$BACKUP_DIR/$FILENAME"

echo "🗄  Integrity Family — MySQL Backup"
echo "   Base de datos : $DB_NAME"
echo "   Destino       : $FILEPATH"
echo "   Compresión    : $COMPRESS"
echo ""

# ── Verificar que el contenedor está corriendo ───────────────
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo "❌  El contenedor '$DB_CONTAINER' no está activo."
  echo "    Ejecuta: docker compose up -d"
  exit 1
fi

# ── Dump ────────────────────────────────────────────────────
echo "⏳  Generando dump..."
docker exec "$DB_CONTAINER" \
  mysqldump \
    -u"$DB_USER" -p"$DB_PASS" \
    --single-transaction \
    --routines \
    --triggers \
    --set-gtid-purged=OFF \
    "$DB_NAME" > "$FILEPATH"

# ── Compresión opcional ─────────────────────────────────────
if $COMPRESS; then
  gzip "$FILEPATH"
  FILEPATH="${FILEPATH}.gz"
  FILENAME="${FILENAME}.gz"
  echo "📦  Archivo comprimido: $FILENAME"
fi

SIZE=$(du -sh "$FILEPATH" | cut -f1)
echo "✅  Backup completado: $FILENAME ($SIZE)"

# ── Rotación: mantener solo los últimos $KEEP backups ───────
BACKUP_COUNT=$(ls -1 "$BACKUP_DIR"/if_backup_* 2>/dev/null | wc -l)
if (( BACKUP_COUNT > KEEP )); then
  TO_DELETE=$(( BACKUP_COUNT - KEEP ))
  echo "🔄  Rotando backups — eliminando los $TO_DELETE más antiguos..."
  ls -1t "$BACKUP_DIR"/if_backup_* | tail -n "$TO_DELETE" | xargs rm -f
fi

echo ""
echo "📁  Backups disponibles:"
ls -lh "$BACKUP_DIR"/if_backup_* 2>/dev/null | awk '{print "   " $5 "  " $9}' || echo "   (ninguno)"
