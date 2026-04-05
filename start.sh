#!/bin/bash
# start.sh — Levanta Integrity Family MVP completo

set -e

echo "╔════════════════════════════════════════╗"
echo "║      INTEGRITY FAMILY MVP v1.0         ║"
echo "╚════════════════════════════════════════╝"

# Verificar API key
if [ -z "$ANTHROPIC_API_KEY" ]; then
  echo ""
  echo "⚠️  ANTHROPIC_API_KEY no está configurada."
  echo "   El sistema funcionará pero sin análisis IA real."
  echo "   Para configurarla: export ANTHROPIC_API_KEY=sk-ant-..."
  echo ""
fi

# Ir a infra
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/infra"

echo "🐳 Levantando contenedores..."
docker compose up --build -d

echo ""
echo "⏳ Esperando que los servicios estén listos..."
sleep 15

echo ""
echo "✅ Sistema iniciado:"
echo "   Frontend:  http://localhost:4200"
echo "   Backend:   http://localhost:8080/swagger-ui.html"
echo "   RabbitMQ:  http://localhost:15672 (guest/guest)"
echo ""
echo "   Email:     admin@integrityfamily.com"
echo "   Password:  Admin123*"
echo ""
echo "Para detener: docker compose -f infra/docker-compose.yml down"
