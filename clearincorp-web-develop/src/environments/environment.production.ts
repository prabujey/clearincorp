// src/environments/environment.production.ts
// SECURITY: All secrets MUST be injected at build time via CI/CD pipeline.
// Never commit actual production secrets to source control.
export const environment = {
    production: true,
    name: 'production',
    version: '20250217.01',
    apiBaseUrl: 'https://clearincorp.com/api',
    wsBaseUrl: 'https://clearincorp.com/ws',
    defaultBlobUrl: '',
    // CRITICAL: This placeholder must be replaced during production build with actual secret
    cryptoSecretKey: '__CRYPTO_SECRET_KEY_PLACEHOLDER__',

    // Firebase Configuration (alternative to AWS Cognito)
    // CRITICAL: These must be replaced during production build
    firebase: {
      apiKey: '__FIREBASE_API_KEY_PLACEHOLDER__',
      authDomain: 'clearincorp-prod.firebaseapp.com',
      projectId: 'clearincorp-prod',
      storageBucket: 'clearincorp-prod.appspot.com',
      messagingSenderId: '__FIREBASE_MESSAGING_SENDER_ID__',
      appId: '__FIREBASE_APP_ID__',
    },
  };
