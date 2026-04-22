export const environment = {
  production: false,
  apiUrl: 'http://localhost:8097/api',
  keycloak: {
    url: 'http://localhost:8080/', // L'URL de votre serveur Keycloak
    realm: 'contentieux-realm', // Le nom du realm que vous avez créé
    clientId: 'contentieux-client2' // L'ID du client Keycloak
  }
};
