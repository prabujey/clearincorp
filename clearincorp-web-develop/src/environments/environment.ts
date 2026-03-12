// src/environments/environment.ts
// SECURITY: All secrets should be set via CI/CD build process using environment substitution.
// Do NOT commit actual secret keys to source control.
// For local development, create a .env.local file (gitignored) with your dev keys.
export const environment = {
    production: false,
    name: 'local',
    version: '20250319.01',
    // IMPORTANT: Replace this placeholder at build time with actual secret from secure vault
    cryptoSecretKey: process.env['NG_APP_CRYPTO_SECRET_KEY'] || '__CRYPTO_SECRET_KEY_PLACEHOLDER__',
    apiBaseUrl: 'https://65h5fmmu0c.execute-api.us-east-1.amazonaws.com/llcDev',
    defaultBlobUrl: '',

    // Firebase Configuration (alternative to AWS Cognito)
    // Get these values from Firebase Console > Project Settings > General > Your apps
    // SECURITY: Only apiKey and authDomain are safe to expose. Others are for reference.
    firebase: {
      apiKey: process.env['NG_APP_FIREBASE_API_KEY'] || '__FIREBASE_API_KEY_PLACEHOLDER__',
      authDomain: process.env['NG_APP_FIREBASE_AUTH_DOMAIN'] || 'your-project.firebaseapp.com',
      projectId: process.env['NG_APP_FIREBASE_PROJECT_ID'] || 'your-project-id',
      storageBucket: process.env['NG_APP_FIREBASE_STORAGE_BUCKET'] || 'your-project.appspot.com',
      messagingSenderId: process.env['NG_APP_FIREBASE_MESSAGING_SENDER_ID'] || '',
      appId: process.env['NG_APP_FIREBASE_APP_ID'] || '',
    },
  };
