# Internal testing — Play Console

Guía paso a paso para **Fase 27**. Complementa `PLAY_STORE.md` y `E2E_QA_CHECKLIST.md`.

**Última revisión:** 24 agosto 2026

---

## 1. Generar keystore (una sola vez)

```bash
keytool -genkey -v \
  -keystore kipu-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias kipu
```

Guardar `kipu-release.jks` **fuera del repo** o en la raíz del proyecto (está en `.gitignore` vía `*.jks`).

Copiar plantilla y completar:

```bash
cp keystore.properties.example keystore.properties
# Editar storeFile, storePassword, keyPassword, keyAlias
```

---

## 2. Construir AAB firmado

```bash
./gradlew :core:domain:test :app:lintDebug bundleRelease
```

| Resultado | Ubicación |
|-----------|-----------|
| AAB firmado (con `keystore.properties`) | `app/build/outputs/bundle/release/app-release.aab` |

Sin `keystore.properties`, `preReleaseBuild` falla de forma intencional antes de generar un AAB nuevo. Kipu no permite producir silenciosamente un artefacto release sin firma. Si existe un AAB antiguo en `build/`, no debe usarse: genera uno nuevo después de configurar la firma.

Verificar tamaño ~10–15 MB (arm64 + R8).

---

## 3. URL pública de privacidad

Opción recomendada — **GitHub Pages** desde el repo:

1. Settings → Pages → Source: **Deploy from branch**
2. Branch `main`, folder `/docs`
3. URL resultante: `https://<usuario>.github.io/kipu/privacy/`

Archivo estático incluido: `docs/privacy/index.html` (paridad con `PRIVACY_POLICY.md`).

Alternativa: enlace raw al markdown en GitHub (menos legible para usuarios).

Pegar la URL en Play Console → **Política de privacidad**.

---

## 4. Play Console — internal testing

1. [Google Play Console](https://play.google.com/console) → Crear app **Kipu**
2. **Configuración** → completar cuenta de desarrollador si falta
3. **Prueba interna** → Crear nueva versión
4. Subir `app-release.aab`
5. **Notas de la versión** (ejemplo):

   ```
   MVP 1.0.0 — finanzas personales local.
   Sobres, movimientos, comprobantes, cuentas compartidas, export/wipe.
   ```

6. **Store listing** — copiar textos de `PLAY_STORE.md`
7. **Data safety** — respuestas de `PLAY_STORE.md` §4
8. **Contenido de la app** — cuestionario IARC (finanzas, sin apuestas)
9. Añadir testers (correos Gmail) en lista de prueba interna
10. **Revisar y publicar** en track interno

Los testers reciben enlace de opt-in por correo.

---

## 5. QA en dispositivo (post-instalación)

Con la build de internal testing instalada en arm64 (ej. Moto G24):

```bash
export ANDROID_SERIAL=<serial>
./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

Checklist manual: `docs/ai/E2E_QA_CHECKLIST.md` (N1–E4).

Registrar resultados en la tabla al final de este doc o en `PROJECT_STATE.md`.

---

## 6. Registro de ejecución Fase 27

| Paso | Estado | Fecha | Notas |
|------|--------|-------|-------|
| Keystore creado | ⏳ | | |
| `bundleRelease` firmado | ⏳ | | |
| URL privacidad publicada | ⏳ | | |
| AAB subido (internal) | ⏳ | | |
| Testers invitados | ⏳ | | |
| E2E automatizado PASS | ⏳ | | |
| Checklist manual N1–E4 | ⏳ | | |

---

## 7. Siguiente paso tras internal testing

- Corregir feedback de testers
- Incrementar `versionCode` en `app/build.gradle.kts`
- **Closed testing** → **Producción** cuando N1–E4 estén verificados
