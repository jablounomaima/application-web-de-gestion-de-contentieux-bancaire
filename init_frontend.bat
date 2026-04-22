@echo off
echo ========================================================
echo   Initialisation du projet Frontend Angular 19
echo ========================================================
echo.
echo 1. Creation du projet Angular "contentieux-frontend"...
call npx -y @angular/cli@19 new contentieux-frontend --standalone=true --routing=true --style=css --skip-git
echo.
echo 2. Installation des dependances (Keycloak, JWT)...
cd contentieux-frontend
call npm install keycloak-angular keycloak-js jwt-decode
echo.
echo ========================================================
echo   Installation terminee avec succes !
echo   Vous pouvez fermer cette fenetre et le dire a l'assistant.
echo ========================================================
pause

