/**
 * Environment Variable Injection Script for Vercel Deployment
 *
 * This script replaces placeholder values in the production environment file
 * with actual values from Vercel's environment variables during build time.
 *
 * Usage: node scripts/set-env.js
 *
 * Required Environment Variables in Vercel Dashboard:
 * - NG_APP_API_BASE_URL
 * - NG_APP_WS_BASE_URL
 * - NG_APP_CRYPTO_SECRET_KEY
 * - NG_APP_FIREBASE_API_KEY
 * - NG_APP_FIREBASE_AUTH_DOMAIN
 * - NG_APP_FIREBASE_PROJECT_ID
 * - NG_APP_FIREBASE_STORAGE_BUCKET
 * - NG_APP_FIREBASE_MESSAGING_SENDER_ID
 * - NG_APP_FIREBASE_APP_ID
 */

const fs = require('fs');
const path = require('path');

const envFilePath = path.join(__dirname, '../src/environments/environment.production.ts');

// Read the template file
let envFileContent = fs.readFileSync(envFilePath, 'utf8');

// Define placeholder-to-env-var mappings
const replacements = {
  '__NG_APP_API_BASE_URL__': process.env.NG_APP_API_BASE_URL || 'https://clearincorp-api.railway.app',
  '__NG_APP_WS_BASE_URL__': process.env.NG_APP_WS_BASE_URL || 'wss://clearincorp-api.railway.app/ws',
  '__NG_APP_CRYPTO_SECRET_KEY__': process.env.NG_APP_CRYPTO_SECRET_KEY || '__CRYPTO_KEY_NOT_SET__',
  '__NG_APP_FIREBASE_API_KEY__': process.env.NG_APP_FIREBASE_API_KEY || '',
  '__NG_APP_FIREBASE_AUTH_DOMAIN__': process.env.NG_APP_FIREBASE_AUTH_DOMAIN || 'clearincorp.firebaseapp.com',
  '__NG_APP_FIREBASE_PROJECT_ID__': process.env.NG_APP_FIREBASE_PROJECT_ID || 'clearincorp',
  '__NG_APP_FIREBASE_STORAGE_BUCKET__': process.env.NG_APP_FIREBASE_STORAGE_BUCKET || 'clearincorp.appspot.com',
  '__NG_APP_FIREBASE_MESSAGING_SENDER_ID__': process.env.NG_APP_FIREBASE_MESSAGING_SENDER_ID || '',
  '__NG_APP_FIREBASE_APP_ID__': process.env.NG_APP_FIREBASE_APP_ID || '',
};

// Replace all placeholders
for (const [placeholder, value] of Object.entries(replacements)) {
  envFileContent = envFileContent.replace(new RegExp(placeholder, 'g'), value);
}

// Write the updated file
fs.writeFileSync(envFilePath, envFileContent, 'utf8');

console.log('✅ Environment variables injected into environment.production.ts');
console.log('   API Base URL:', replacements['__NG_APP_API_BASE_URL__']);
console.log('   Firebase Project:', replacements['__NG_APP_FIREBASE_PROJECT_ID__']);
