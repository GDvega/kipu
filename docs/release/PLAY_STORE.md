# Play Store — Kipu MVP

Checklist de publicación. Complementa `docs/ai/E2E_QA_CHECKLIST.md`.

**Última revisión documental:** 12 septiembre 2026 · Publicación pendiente. Ver [Internal testing](INTERNAL_TESTING.md).

Estado del código: H01–H05, M01–M02 y L01 corregidos con regresiones; L02 en observación. [Remediación](../qa/REMEDIATION_2026-09-09.md) registra 551 unitarias, 48 app y 55 datos PASS, además de compilación/lint debug. La [auditoría](../qa/KIPU_AUDIT_2026-09-09.md) conserva la línea base histórica. Faltan las comprobaciones humanas y de release; los pasos de Console y firma son una guía para el responsable, no acciones realizadas ni autorización para ejecutarlas.

---

## 1. Verificación de build (obligatorio)

```bash
./gradlew --no-daemon --max-workers=1 --continue :core:domain:test testDebugUnitTest lintDebug assembleDebug
```

En dispositivo físico arm64:

```bash
adb devices
ANDROID_SERIAL='SERIAL_DEL_DISPOSITIVO' ./gradlew --no-daemon --max-workers=1 --continue :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Usar un dispositivo de pruebas sin datos reales: las instrumentadas instalan debug. La [verificación del 9 de septiembre](../qa/VERIFICATION_2026-09-09.md) registró 523 pruebas unitarias, 87 instrumentadas y lint sin errores (64 advertencias), pero no cubre todos los hallazgos abiertos ni equivale a QA de release. Para publicar se requieren regresiones de los HIGH y checklist manual sobre la build firmada.

---

## 2. Artefacto de subida

| Campo | Valor |
|-------|-------|
| Formato | AAB del proyecto (`bundleRelease`); validar requisitos actuales de Console |
| `applicationId` | `pe.kipu.app` |
| `versionCode` | Incrementar en cada subida (`app/build.gradle.kts`) |
| `versionName` | Semver visible al usuario (ej. `1.0.0`) |
| ABI release | `arm64-v8a`; medir el artefacto nuevo, no asumir tamaño histórico |

```bash
./gradlew bundleRelease   # preferido Play Console
# o
./gradlew assembleRelease
```

Firmar con keystore de producción — ver `INTERNAL_TESTING.md` y `keystore.properties.example`. La tarea release falla intencionalmente si falta la configuración de firma; nunca usar debug signing como reemplazo.

---

## 3. Store listing (español Perú)

### Título (máx. 30 caracteres)

`Kipu — finanzas personales`

### Descripción corta (máx. 80 caracteres)

`Controla gastos, sobres y metas. Yape, Plin y efectivo sin claves bancarias.`

### Descripción completa (borrador)

Kipu te ayuda a registrar ingresos y gastos en Perú, organizar un plan diario, semanal o mensual y consultar tus movimientos. Este texto es un borrador: validar los flujos financieros pendientes antes de usarlo como descripción de una versión publicada.

**Qué puedes hacer**

- Registrar movimientos de Yape, Plin, efectivo y otros
- Compartir comprobantes desde Yape/Plin para registrar pagos más rápido
- Opcional: detectar ingresos desde notificaciones (tú confirmas antes de guardar)
- Sobres por ciclo, servicios mensuales de referencia y registro de pagos reales
- Reserva para imprevistos y propuestas de ajuste con confirmación; no garantizan llegar a fin de mes
- Consulta del gasto y alertas de gasto hormiga
- Metas, deudas sociales y pagos pendientes
- Cuentas compartidas: reparto de gastos y liquidación por participante
- Exportar o borrar todos tus datos cuando quieras

**Privacidad**

- Sin claves bancarias ni acceso a tu banca
- Registro financiero e interpretación de comprobantes locales
- Voz opcional: el servicio del sistema puede usar servidores del proveedor
- ML Kit puede enviar métricas técnicas; consultar la política

Ideal si usas Yape y Plin a diario y quieres orden sin complicarte.

### Categoría

Finanzas

### Correo de contacto del desarrollador

Configurar en Play Console (mismo dominio que política de privacidad recomendado).

### Política de privacidad (URL pública)

Publicar `docs/privacy/index.html` vía GitHub Pages (Settings → Pages → `/docs`):

- URL ejemplo: `https://<usuario>.github.io/kipu/privacy/`

Alternativas: raw markdown en GitHub o sitio propio. Ver `INTERNAL_TESTING.md` §3.

La app incluye una pantalla en **Perfil → Política de privacidad**. El contacto proporcionado por el responsable, vegacisneros.gd@gmail.com, se actualizó en pantalla y documentos y pasó su prueba de UI (AUD-L01). Falta la revisión final de la política/build y la comprobación humana de entrega del correo antes de publicar.

---

## 4. Data safety (Google Play)

Insumos para revisión humana, **no copiar como declaración certificada**. La [guía oficial de ML Kit](https://developers.google.com/ml-kit/android-data-disclosure) describe sus versiones más recientes y exige al desarrollador evaluar el uso concreto. Contrastar SDK resueltos, manifiesto final y proveedor de voz de la build a publicar.

| Pregunta | Respuesta |
|----------|-----------|
| ¿Recopila o comparte datos? | Sí: ML Kit recopila métricas técnicas para diagnóstico y analítica; Google declara que no las comparte con terceros |
| ¿Cifrado en tránsito? | Sí para las métricas recopiladas por ML Kit (HTTPS) |
| ¿El usuario puede pedir eliminación? | Sí — Perfil → Eliminar todos mis datos |
| Datos financieros | Base financiera y OCR locales; no hay backend financiero propio. No afirmar localía total si se usa voz remota |
| Datos recopilados por ML Kit | Información del dispositivo/app, identificadores por instalación, rendimiento, configuración, tamaños, eventos y errores |
| Finalidad | Diagnóstico y analítica de uso del SDK |
| ¿Datos vendidos? | No |
| ¿Solo procesamiento en dispositivo? | Local para base, parser de transcripción y OCR. La voz puede transmitirse al proveedor; ML Kit envía métricas técnicas |

Permisos declarados:

- **Notification listener** — opcional; explicar en descripción y política
- **RECORD_AUDIO** — opcional; solo se solicita al tocar el micrófono
- **POST_NOTIFICATIONS** — opcional en Android 13+; se solicita al guardar un plan con pagos fijos para mostrar recordatorios
- **INTERNET** — presente de forma transitiva por ML Kit/DataTransport para métricas técnicas; el flujo financiero principal sigue funcionando localmente

---

## 5. Contenido y clasificación

- Sin contenido restringido por edad en UI
- Cuestionario IARC: finanzas personales, sin apuestas ni cripto
- Preparar capturas de pantalla con datos ficticios (Inicio, Movimientos, Sobres, Plan y Perfil); comprobar cantidades y formatos vigentes en Console. No usar cámara física ni datos de usuarios.

---

## 6. Pre-lanzamiento interno

1. Internal testing track con AAB firmado
2. Instalar en Moto G24 (o equivalente arm64)
3. Recorrer `docs/ai/E2E_QA_CHECKLIST.md` manual
4. Verificar política de privacidad accesible desde Perfil y URL pública, contacto real y Data safety.
5. Registrar resultados y decisión humana frente a hallazgos abiertos; no equiparar fin de documentación con autorización de lanzamiento.

---

## 7. Post-publicación

- Monitorear Android Vitals (crashes ANR)
- No activar Firebase/analytics sin actualizar política de privacidad
- Incrementar `versionCode` en cada release
