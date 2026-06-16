@echo off
echo Iniciando contenedor MySQL sin phpMyAdmin...
docker compose up -d db
echo MySQL se ha iniciado exitosamente en el puerto 3307 del Host (mapeado a 3306 en el contenedor).

