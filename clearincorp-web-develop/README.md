# ClearInCorp Frontend

Frontend application for the **ClearInCorp** platform — an LLC formation and business management system providing role-based dashboards, business workflows, document handling, task management, payments UI, marketplace, and a real-time forum experience.

This application consumes the **ClearInCorp Spring Boot backend APIs and WebSocket endpoints** and is designed for deployment on **AWS (S3 + CloudFront)**.

---

## 1) Project Overview

ClearInCorp Frontend provides the user-facing interface for all platform features, including:

- OTP-based login and authenticated sessions  
- Role-based dashboards (**Admin / Vendor / Superfiler / Consumer**)  
- LLC formation wizard with progress tracking  
- Document upload, preview (PDF), and download  
- Filer filing wizard with progress tracking  
- Task and to-do management with attachments  
- Payments & invoice viewing  
- Marketplace browsing for business services  
- Real-time forum with topics, posts, and replies  

This frontend is built with **modern Angular (standalone architecture)** and follows **enterprise-grade folder organization** for long-term scalability.

---

## 2) Tech Stack

- **Framework:** Angular (Standalone Components)  
- **Language:** TypeScript  
- **UI Library:** Angular Material  
- **Styling:** SCSS (global + component-level)  
- **State & Streams:** RxJS  
- **Authentication:** JWT (via HTTP interceptors)  
- **Build Tool:** Angular CLI  
- **Deployment:** AWS S3 + CloudFront (production)  

---

## 3) Folder Structure

```text
clearincorp-web/
│
├── src/
│   ├── app/
│   │   ├── auth/                     # Auth guards
│   │   ├── LANDING_PAGE/             # Public landing pages
│   │   ├── layouts/                  # App layouts (full, auth, minimal)
│   │   ├── models/                   # Shared TypeScript models & interfaces
│   │   ├── modules/                  # Feature modules (authentication)
│   │   ├── pages/                    # App-level pages
│   │   │   ├── apps/
│   │   │   │   ├── account-settings/     # User profile
│   │   │   │   ├── admin/                # Admin authorized UI components
│   │   │   │   ├── conversational_forum/ # Forum
│   │   │   │   ├── dashboard/            # Dashboard & formation details
│   │   │   │   ├── document/             # Consumer document components
│   │   │   │   ├── filer/                # SuperFiler authorized UI components
│   │   │   │   ├── invoice/              # Invoice views
│   │   │   │   ├── marketplace/          # BusinessHub modules
│   │   │   │   ├── todo/                 # Todo module
│   │   │   │   └── vendor/               # Vendor authorized UI components
│   │   │   ├── formation-wizard/         # LLC formation multi-step wizard
│   │   │   └── apps.routes.ts            # App-level routing
│   │   ├── pipe/                         # Custom Angular pipes
│   │   ├── services/
│   │   │   ├── apps/                     # App-specific services
│   │   │   ├── interceptor/              # HTTP interceptors
│   │   │   ├── loading/                  # Global loaders & spinners
│   │   │   ├── login/                    # Login & OTP services
│   │   │   ├── storage/                  # Local/session storage abstraction
│   │   │   ├── todo/                     # Todo & task services
│   │   │   ├── token/                    # Token & auth helpers
│   │   │   ├── business-hub.api.ts       # API definitions
│   │   │   ├── business-hub.service.ts
│   │   │   ├── chatbot.service.ts
│   │   │   ├── core.service.ts
│   │   │   ├── file-processing.service.ts
│   │   │   ├── forum.service.ts
│   │   │   ├── nav.service.ts
│   │   │   └── ws.service.ts             # WebSocket service
│   │   ├── shared/                       # Reusable UI components
│   │   │   ├── business-name-check-dialog/
│   │   │   ├── floating-chatbot/          # Chatbot component
│   │   │   ├── profile-upload/
│   │   │   ├── dialogs/                  # Confirm, delete, edit dialogs
│   │   │   ├── pdf-viewer.component.ts
│   │   │   ├── snackbar.service.ts
│   │   │   └── session-expired-dialog.component.ts
│   │   ├── app.component.ts
│   │   ├── app.routes.ts
│   │   ├── app.config.ts
│   │   ├── material.module.ts
│   │   └── config.ts
│   │
│   ├── assets/                           # Images, icons, static assets
│   ├── environments/                     # Environment configs
│   ├── styles.scss                       # Global styles
│   ├── _variables.scss                   # Global SCSS variables
│   ├── index.html
│   ├── main.ts
│   └── polyfills.ts
│
├── angular.json
├── package.json
├── netlify.toml
├── tsconfig.json
└── README.md



 High-level Flow

Angular UI
   ↓
Services (REST / WebSocket)
   ↓
Spring Boot Backend


 4) Configuration

 Environment Files
Located in: `src/environments/`

export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  wsBaseUrl: 'ws://localhost:8080/ws'
};
 Run Locally : 

Prerequisites ,Node.js 18+,Angular CLI,Backend running locally or reachable API

Steps -> npm install -> ng serve -> Application runs at : http://localhost:4200

Production build : ng build --configuration production -> Output directory: dist/


 5) Troubleshooting

* API Not Reachable

-> Verify apiBaseUrl in environment files

-> Ensure backend is running

-> Check CORS configuration on backend

* 401 / 403 Errors

-> Access token expired

-> Missing Authorization header

-> Role mismatch (UI may hide actions, backend enforces rules)


 FINAL NOTES : 
This README covers frontend only, Folder structure is designed for enterprise-scale growth




