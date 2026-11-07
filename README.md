# WhatsApp Neo

Android (Kotlin) wrapper around WhatsApp Web with a classic neo-brutalism visual theme injected at runtime.

## Build (GitHub Actions)
1. Push this repo to GitHub.
2. Actions → "Build APK" → run workflow (or push to main).
3. Download artifact `WhatsAppNeo-debug` → `app-debug.apk`.

## Build (local Android Studio)
Open the project in Android Studio, sync Gradle, then Build → Build APK(s).

## Notes
- Login is untouched WhatsApp Web flow (QR / pair code).
- Chat loading is still network-bound (WA Web requires linked device online). Perceived speed comes from persistent session + caching + splash.
- Theme is CSS-only; selectors may need updates when WhatsApp Web changes.
- Package/name uses "whatsapp" — trademark risk; rename if publishing.
