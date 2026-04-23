// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8098',   // ✅ même port que server.port
  keycloak: {
    url: 'http://localhost:8080',
    realm: 'contentieux-realm',
    clientId: 'contentieux-client2'  // ✅ même client-id que application.properties
  }
};