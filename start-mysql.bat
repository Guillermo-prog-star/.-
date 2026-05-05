@echo off
echo Iniciando contenedor MySQL sin phpMyAdmin...
docker compose up -d if-mysql
echo MySQL se ha iniciado exitosamente en el puerto 3306.
pause
