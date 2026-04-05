#!/bin/bash

# Colores para la terminal (Estándar ANSI)
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BOLD='\033[1m'

echo -e "${BOLD}>>> [NODO ARMENIA] Auditoría de Integridad del Sistema <<<${NC}\n"

# 1. Verificar Base de Datos (MySQL)
echo -n "1. Base de Datos (MySQL): "
if docker exec if-mysql mysqladmin ping -h localhost -proot123 &>/dev/null; then
    echo -e "${GREEN}Conectada y Saludable${NC}"
else
    echo -e "${RED}Error de Conexión${NC}"
fi

# 2. Verificar Mensajería (RabbitMQ)
echo -n "2. RabbitMQ (Mensajería): "
if docker exec if-rabbit rabbitmqctl status &>/dev/null; then
    echo -e "${GREEN}Listo y Operativo${NC}"
else
    echo -e "${RED}Fuera de Línea${NC}"
fi

# 3. Verificar Backend (Spring Boot)
echo -n "3. Backend (Spring Boot): "
# Se asume que tienes un endpoint /api/health o el root responde 200/401
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/auth/login || echo "000")
if [ "$HTTP_CODE" -ne "000" ]; then
    echo -e "${GREEN}Cerebro en línea (HTTP $HTTP_CODE)${NC}"
else
    echo -e "${RED}Backend no responde${NC}"
fi

# 4. Verificar IA Claude (Anthropic)
echo -n "4. IA Claude (Anthropic): "
# Verificamos si la variable de entorno está cargada en el contenedor del backend
API_KEY_CHECK=$(docker exec if-backend printenv ANTHROPIC_API_KEY)
if [ -n "$API_KEY_CHECK" ]; then
    echo -e "${GREEN}IA Claude en línea (API Key Cargada)${NC}"
else
    echo -e "${RED}Error: API Key faltante en el Backend${NC}"
fi

# 5. Verificar Frontend (Nginx)
echo -n "5. Frontend (Nginx):    "
FRONT_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4200 || echo "000")
if [ "$FRONT_CODE" == "200" ]; then
    echo -e "${GREEN}Interfaz lista en puerto 4200${NC}"
else
    echo -e "${RED}Frontend inaccesible${NC}"
fi

echo -e "\n${BOLD}>>> Fin de la Auditoría - Nodo Armenia <<<${NC}"