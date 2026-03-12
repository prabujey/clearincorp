// src/environments/environment.production.ts
// SECURITY: All secrets MUST be injected at build time via Vercel Environment Variables.
// Configure these in Vercel Dashboard > Project Settings > Environment Variables
//
// Required Environment Variables in Vercel:
// - NG_APP_API_BASE_URL (e.g., https://clearincorp-api.railway.app)
// - NG_APP_WS_BASE_URL (e.g., wss://clearincorp-api.railway.app/ws)
// - NG_APP_CRYPTO_SECRET_KEY (generate with: openssl rand -base64 32)
// - NG_APP_FIREBASE_API_KEY (from Firebase Console)
// - NG_APP_FIREBASE_PROJECT_ID (from Firebase Console)
//
// These placeholders are replaced during the Vercel build process.

export const environment = {
    production: true,
    name: 'production',
    version: '20250319.01',

    // API Configuration - Set in Vercel Environment Variables
    apiBaseUrl: '__NG_APP_API_BASE_URL__',
    wsBaseUrl: '__NG_APP_WS_BASE_URL__',
    defaultBlobUrl: '',

    // Encryption Key - Set in Vercel Environment Variables
    cryptoSecretKey: '__NG_APP_CRYPTO_SECRET_KEY__',

    // Firebase Configuration - Set in Vercel Environment Variables
    firebase: {
      apiKey: '__NG_APP_FIREBASE_API_KEY__',
      authDomain: '__NG_APP_FIREBASE_AUTH_DOMAIN__',
      projectId: '__NG_APP_FIREBASE_PROJECT_ID__',
      storageBucket: '__NG_APP_FIREBASE_STORAGE_BUCKET__',
      messagingSenderId: '__NG_APP_FIREBASE_MESSAGING_SENDER_ID__',
      appId: '__NG_APP_FIREBASE_APP_ID__',
    },
  };
