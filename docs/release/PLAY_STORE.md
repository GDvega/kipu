# Play Store — Kipu MVP

Checklist de publicación. Complementa `docs/ai/E2E_QA_CHECKLIST.md`.

**Última revisión:** 20 junio 2026 · **Fase 27** — ver también `INTERNAL_TESTING.md`

---

## 1. Verificación de build (obligatorio)

```bash
./gradlew :core:domain:test :app:lintDebug assembleRelease
```

En dispositivo físico arm64:

```bash
export ANDROID_SERIAL=<serial>
./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

Criterio: todos PASS + checklist manual E2E (N1–N5, C1–C3, J1–J4, E1–E3) en hardware real.

---

## 2. Artefacto de subida

| Campo | Valor |
|-------|-------|
| Formato | AAB recomendado (`bundleRelease`) o APK firmado |
| `applicationId` | `pe.kipu.app` |
| `versionCode` | Incrementar en cada subida (`app/build.gradle.kts`) |
| `versionName` | Semver visible al usuario (ej. `1.0.0`) |
| ABI release | `arm64-v8a` only (~14 MB con R8) |

```bash
./gradlew bundleRelease   # preferido Play Console
# o
./gradlew assembleRelease
```

Firmar con keystore de producción — ver `INTERNAL_TESTING.md` y `keystore.properties.example`.

---

## 3. Store listing (español Perú)

### Título (máx. 30 caracteres)

`Kipu — finanzas personales`

### Descripción corta (máx. 80 caracteres)

`Controla gastos, sobres y metas. Yape, Plin y efectivo sin claves bancarias.`

### Descripción completa (borrador)

Kipu te ayuda a llevar el control de tu plata en Perú: gastos del día, sobres semanales, metas de ahorro y juntas con amigos.

**Qué puedes hacer**

- Registrar movimientos de Yape, Plin, efectivo y otros
- Compartir comprobantes desde Yape/Plin para registrar pagos más rápido
- Opcional: detectar ingresos desde notificaciones (tú confirmas antes de guardar)
- Sobres semanales, disponible diario y alertas de gasto hormiga
- Metas, deudas sociales y pagos pendientes
- Juntas: reparto de gastos y liquidación por participante
- Exportar o borrar todos tus datos cuando quieras

**Privacidad**

- Sin claves bancarias ni acceso a tu banca
- Tus datos se procesan en el dispositivo
- Sin subir comprobantes a la nube por defecto

Ideal si usas Yape y Plin a diario y quieres orden sin complicarte.

### Categoría

Finanzas

### Correo de contacto del desarrollador

Configurar en Play Console (mismo dominio que política de privacidad recomendado).

### Política de privacidad (URL pública)

Publicar `docs/privacy/index.html` vía GitHub Pages (Settings → Pages → `/docs`):

- URL ejemplo: `https://<usuario>.github.io/kipu/privacy/`

Alternativas: raw markdown en GitHub o sitio propio. Ver `INTERNAL_TESTING.md` §3.

La app incluye la misma política en **Perfil → Política de privacidad**.

---

## 4. Data safety (Google Play)

Respuestas sugeridas para el formulario (revisar contra build actual):

| Pregunta | Respuesta |
|----------|-----------|
| ¿Recopila o comparte datos? | Sí, recopila (no comparte con terceros publicitarios) |
| ¿Cifrado en tránsito? | No aplica al flujo principal (sin backend propio en MVP) |
| ¿El usuario puede pedir eliminación? | Sí — Perfil → Eliminar todos mis datos |
| Datos financieros | Montos, categorías, descripciones — **opcional**, ingresados por el usuario o sugeridos localmente |
| Datos de app | Preferencias, configuración |
| ¿Datos vendidos? | No |
| ¿Solo procesamiento en dispositivo? | Sí para el núcleo de la app |

Permisos declarados:

- **Notification listener** — opcional; explicar en descripción y política
- **Sin INTERNET obligatorio** para flujo principal (verificar manifest si hay dependencias con red)

---

## 5. Contenido y clasificación

- Sin contenido restringido por edad en UI
- Cuestionario IARC: finanzas personales, sin apuestas ni cripto
- Capturas: Inicio, Movimientos, Sobres, Wizard plan, Perfil (mínimo 4)

---

## 6. Pre-lanzamiento interno

1. Internal testing track con AAB firmado
2. Instalar en Moto G24 (o equivalente arm64)
3. Recorrer `docs/ai/E2E_QA_CHECKLIST.md` manual
4. Verificar política de privacidad accesible desde Perfil y URL pública

---

## 7. Post-publicación

- Monitorear Android Vitals (crashes ANR)
- No activar Firebase/analytics sin actualizar política de privacidad
- Incrementar `versionCode` en cada release
