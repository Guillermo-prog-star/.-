#!/bin/bash
# start.sh — Levanta Integrity Family MVP completo

set -e

echo "╔════════════════════════════════════════╗"
echo "║      INTEGRITY FAMILY MVP v1.0         ║"
echo "╚════════════════════════════════════════╝"

# Cargar variables del .env si existe
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
  echo "✅ Variables de entorno cargadas desde .env"
fi

# Verificar API key
if [ -z "$ANTHROPIC_API_KEY" ]; then
  echo "⚠️  ANTHROPIC_API_KEY no detectada. Usando modo simulación."
fi

# Usar la carpeta raíz (donde está el docker-compose.yml)
DOCKER_DIR="."

echo "🐳 Levantando servicios..."
docker compose -f $DOCKER_DIR/docker-compose.yml up --build -d

echo ""
echo "⏳ Esperando a que los servicios estén listos..."
# Los healthchecks en docker-compose se encargan de la sincronización real.
# Solo damos un pequeño margen para que el log se asiente.
sleep 5

echo ""
echo "✅ Sistema iniciado satisfactoriamente:"
echo "   Frontend:  http://localhost:4200"
echo "   Backend:   http://localhost:8080/swagger-ui/index.html"
echo "   RabbitMQ:  http://localhost:15672 (guest/guest)"
echo "   MySQL:     localhost:3307"

echo ""
echo "Para ver logs en tiempo real:"
echo "docker compose logs -f"

