// src/environments/environment.development.ts
// SECURITY: cryptoSecretKey should be set via CI/CD build process.
// Do NOT commit actual secret keys to source control.
export const environment = {
  production: false,
  name: "development",
  version: "20250319.01",
  apiBaseUrl: 'http://localhost:8080',
  wsBaseUrl: 'https://dev.clearincorp.com/ws',
  defaultBlobUrl: "",
  // IMPORTANT: Replace this placeholder at build time with actual secret from secure vault
  cryptoSecretKey: process.env['NG_APP_CRYPTO_SECRET_KEY'] || '__CRYPTO_SECRET_KEY_PLACEHOLDER__',

  // Firebase Configuration (alternative to AWS Cognito)
  // Get these values from Firebase Console > Project Settings > General > Your apps
  firebase: {
    apiKey: process.env['NG_APP_FIREBASE_API_KEY'] || '__FIREBASE_API_KEY_PLACEHOLDER__',
    authDomain: process.env['NG_APP_FIREBASE_AUTH_DOMAIN'] || 'your-project.firebaseapp.com',
    projectId: process.env['NG_APP_FIREBASE_PROJECT_ID'] || 'your-project-id',
    storageBucket: process.env['NG_APP_FIREBASE_STORAGE_BUCKET'] || 'your-project.appspot.com',
    messagingSenderId: process.env['NG_APP_FIREBASE_MESSAGING_SENDER_ID'] || '',
    appId: process.env['NG_APP_FIREBASE_APP_ID'] || '',
  },
};
