# ETAPA 1: Construcción nativa
FROM node:18-alpine AS build-stage
WORKDIR /app
COPY package*.json ./
RUN yarn install --frozen-lockfile
COPY . .
RUN ./node_modules/.bin/ng build --configuration production

# ETAPA 2: Servidor de ejecución
FROM node:18-alpine AS runtime-stage
WORKDIR /app
# Copiamos el servidor y los archivos compilados de la etapa anterior
COPY --from=build-stage /app/server.js .
COPY --from=build-stage /app/dist ./dist
EXPOSE 4200
CMD ["node", "server.js"]