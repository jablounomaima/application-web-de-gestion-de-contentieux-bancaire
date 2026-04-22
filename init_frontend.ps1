Write-Host "========================================================"
Write-Host "  Initialisation du projet Frontend Angular 19"
Write-Host "========================================================"

Write-Host "1. Creation du projet Angular 'contentieux-frontend'..."
npx -y @angular/cli@19 new contentieux-frontend --standalone=true --routing=true --style=css --skip-git

Write-Host "2. Installation des dependances (Keycloak, JWT)..."
Set-Location -Path "contentieux-frontend"
npm install keycloak-angular keycloak-js jwt-decode

Write-Host "========================================================"
Write-Host "  Installation terminee avec succes !"
Write-Host "  Veuillez me dire 'c'est fait' pour que je continue."
Write-Host "========================================================"
