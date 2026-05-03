# ETAPA 1: Construcción nativa
FROM node:20-alpine AS build-stage
WORKDIR /app

COPY package*.json ./
RUN npm ci
COPY . .

# [SDD] Optimización de Estructura para Linux (Docker)
RUN if [ -d "src/app/features/shell/Services" ]; then mv src/app/features/shell/Services src/app/features/shell/services; fi && \
    if [ -d "src/app/features/dashboard/Services" ]; then mv src/app/features/dashboard/Services src/app/features/dashboard/services; fi

# Generar el bundle de producción optimizado
RUN npx ng build --configuration production

# ETAPA 2: Servidor de ejecución (FIX: nombre de imagen corregido)
FROM node:20-alpine AS runtime-stage
WORKDIR /app
COPY --from=build-stage /app/server.js .
COPY --from=build-stage /app/dist ./dist
EXPOSE 4200
CMD ["node", "server.js"]