# E2E QA — Kipu MVP

> Checklist y suite automatizada post-Fase 16. Última revisión: 13 ago 2026.

## Suite automatizada (instrumented)

### Comando (emulador x86)

```bash
# Iniciar AVD (ej. Pixel_4)
$HOME/Android/Sdk/emulator/emulator -avd Pixel_4 -no-snapshot-load &

# Esperar dispositivo
adb wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done'

# Ejecutar E2E (incluye ABI x86_64 para emulador)
./gradlew -Pkipu.x86Emulator=true :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

### Comando (dispositivo físico arm64)

```bash
export ANDROID_SERIAL=<serial-del-dispositivo>
./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

### Última ejecución automatizada (13 ago 2026 — Moto G24, Android 14)

| Módulo | Resultado |
|--------|-----------|
| `:app` | **43/43 PASS** |
| `:core:data` | **24/24 PASS** |

**Comando ejecutado sin descargar ni actualizar dependencias:**
```bash
./gradlew --offline --no-daemon --max-workers=1 -Pksp.incremental=false :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

El dispositivo mantuvo `font_scale=1.3`. La regresión focalizada de `KipuNavigationE2ETest`, `MediumAccessibilitySemanticsTest` y `PlanWizardE2ETest` terminó **9/9 PASS**. La suite completa incluyó `ReceiptCaptureUriFactoryInstrumentedTest` y `ReceiptReviewViewModelErrorInstrumentedTest`; el parser de share verificó que solo acepta URI `content`.

Esto no valida C1–C3 con Yape/Plin ni un share externo en inicio frío/cálido, tampoco cámara física ni la locución audible con TalkBack: siguen en el checklist manual.

### Ejecución anterior (11 ago 2026 — Moto G24, Android 14)

| Módulo | Resultado |
|--------|-----------|
| `:app` | **38/38 PASS** |
| `:core:data` | **24/24 PASS** |

**Comando ejecutado sin descargar ni actualizar dependencias:**
```bash
./gradlew --offline --no-daemon --max-workers=1 -Pksp.incremental=false :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

El dispositivo mantuvo `font_scale=1.3`. La suite UI no tuvo fallos ni pruebas omitidas.
`PlanWizardE2ETest` deja un plan sintético con ingreso `5200` en el paquete debug; usar una instalación dedicada a pruebas.

### Ejecución anterior (20 jun 2026 — remediación auditoría, post-fix)

| Módulo | Resultado |
|--------|-----------|
| `:app` | **12/12 PASS** (incl. `PrivacyPolicyScreenTest`) |
| `:core:data` | **5/5 PASS** |

En esa versión incluía wizard de plan de 4 pasos, navegación, crear junta, share intent, duplicados, migraciones y wipe instrumentado.

### Cobertura automatizada

| Módulo | Test | Flujo |
|--------|------|-------|
| `:app` | `MainActivitySmokeTest` | Launch MainActivity |
| `:app` | `KipuNavigationE2ETest` | Bottom bar: Inicio → Movimientos → Sobres → Compromisos → Perfil |
| `:app` | `KipuNavigationE2ETest` | Perfil → Ver juntas |
| `:app` | `KipuNavigationE2ETest` | Crear junta (nombre + participantes) |
| `:app` | `PlanWizardE2ETest` | Onboarding → wizard 6 pasos → guardar → reabrir → editar `5000→5200` → reabrir |
| `:app` | `KipuNavigationE2ETest` | Perfil → Política de privacidad |
| `:app` | `DuplicateResolutionDialogTest` | Diálogo duplicados en español |
| `:app` | `PendingNotificationDuplicateDialogTest` | Duplicado notificación — Fusionar / No es duplicado |
| `:app` | `PrivacyPolicyScreenTest` | Política de privacidad en español |
| `:app` | `PendingPlanWizardInstrumentedTest` | DataStore `pendingPlanWizard` → navega a wizard ingresos |
| `:app` | `HighControlsSemanticsTest` | Contraste, selección, controles destructivos y speed dial accesible |
| `:app` | `HighFormsAccessibilityTest` | Error de monto único, visible y anunciado en formulario manual |
| `:app` | `OnboardingHighRemediationTest` | Reintento real y bloqueo de doble envío en onboarding |
| `:app` | `GatheringLinkMovementDialogTest` | Scroll y selección de movimientos posteriores al octavo |
| `:app` | `MediumDialogBehaviorTest` | Sin vibración de éxito prematura y sin cerrar diálogos/sheets durante guardado |
| `:app` | `MediumAccessibilitySemanticsTest` | Filas anti-hormiga y deuda social como switches etiquetados |
| `:app` | `MediumProfileAccessibilityTest` | Switch de Perfil y estados éxito/error anunciables |
| `:app` | `MediumReceiptHomeAccessibilityTest` | Resultado y error accesibles de comprobante, speed dial modal y categorías de navegación |
| `:app` | `ReceiptCaptureUriFactoryInstrumentedTest` | Solo elimina una captura de cámara creada por Kipu; no borra archivos ajenos ni URI con esquema incorrecto |
| `:app` | `ReceiptReviewViewModelErrorInstrumentedTest` | Error de carga recuperable, Reintentar sin doble procesamiento y limpieza de la captura propia al liberar la revisión |
| `:app` | `ReceiptShareIntentParserTest` | Acepta share de imagen únicamente mediante URI `content` |
| `:core:data` | `KipuDatabaseMigrationInstrumentedTest` | Migraciones hasta Room v16 y recorrido completo a la versión actual |
| `:core:data` | `RoomUserDataWipeInstrumentedTest` | Wipe Room + re-seed + prefs |
| `:core:data` | `MovementDaoInstrumentedTest` | DAO movimientos |
| `:core:domain` (JVM) | `MonitoredPaymentAppsTest` | Package names Yape/Plin verificados |

---

## Checklist manual (dispositivo real — obligatorio pre-release)

Marcar ✅ cuando se verifique en hardware con Yape/Plin instalados.

### Notificaciones (F12-05)

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| N1 | Permiso listener | Perfil → activar acceso a notificaciones → ir a Ajustes → conceder | Kipu aparece en apps con acceso |
| N2 | Ingreso Yape | Recibir yapeo real (o simulación) con listener activo | Tarjeta "Ingreso por confirmar" en Movimientos |
| N3 | Confirmar ingreso | Confirmar ingreso pendiente | Movimiento CONFIRMED en lista |
| N4 | Ingreso Plin | Recibir plineo vía Interbank APP | Mismo flujo que N2–N3 con canal Plin |
| N5 | Toggle off | Desactivar notificaciones en Perfil | No aparecen nuevos pendientes |

**Package names verificados (Google Play jun 2026):**

- Yape: `com.bcp.innovacxion.yapeapp`
- Plin (Interbank): `pe.com.interbank.mobilebanking`

### Comprobantes (share intent)

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| C1 | Share Yape | Yape → compartir comprobante → Kipu | Pantalla revisión con monto editable |
| C2 | Confirmar gasto | Editar si hace falta → Confirmar | Movimiento en lista Movimientos |
| C3 | Duplicado | Repetir mismo comprobante | Diálogo "Posible duplicado" |

**Estado:** C1–C3 permanecen sin marcar hasta probar el share externo real, tanto con Kipu cerrado como ya abierto. La cobertura automatizada solo valida el parser y las rutas locales; no sustituye esa comprobación.

### Juntas (Fase 18)

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| J1 | Crear junta | Perfil → Juntas → Nueva junta | Lista muestra junta |
| J2 | Gasto manual | Registrar gasto + pagador | Total y cuota por persona |
| J3 | Vincular movimiento | Vincular movimiento Yape/Plin | Movimiento desaparece de "sin vincular" |
| J4 | Liquidación | Ver tarjeta junta con gastos | "debe / le deben / al día" por participante |

### Export / wipe (F13)

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| E1 | Export JSON | Perfil → Exportar JSON | Chooser de compartir; JSON incluye juntas |
| E2 | Export CSV | Perfil → Exportar CSV | Solo movimientos (documentado en UI) |
| E3 | Wipe | Eliminar todos los datos (doble confirmación) | Onboarding reinicia; categorías seed presentes |
| E4 | Privacidad | Perfil → Política de privacidad | Pantalla scrollable; texto en español |

### Navegación / regresión

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| R1 | Tabs | Recorrer 5 tabs bottom bar | Sin crash; headers correctos |
| R2 | Sobres → movimientos | Ver movimientos de sobre Comida | Filtro categoría activo |
| R3 | Wizard plan | Onboarding → Comenzar (usuario nuevo) | Seis pasos en orden: ingresos, gastos fijos, sobres, hormiga, meta y resumen |

---

## Criterios de salida QA

- Todos los tests instrumentados PASS en dispositivo físico arm64
- N1–N5, C1–C3, J1–J4, E1–E4 verificados en hardware
- `:app:lintDebug` + `assembleRelease` PASS — ver `docs/release/PLAY_STORE.md`

## Hallazgos conocidos

| ID | Nota |
|----|------|
| E2E-01 | Onboarding skip en tests requiere `performScrollTo()` — botón "Configurar plan después" está al final del scroll |
| E2E-02 | Orden de tests en `KipuNavigationE2ETest`: `@FixMethodOrder(NAME_ASCENDING)` |
| F12-05 | Automatizado parcial: packages corregidos + test JVM; ingreso real requiere hardware |
| F14c | Emulador x86 requiere `-Pkipu.x86Emulator=true`; release sigue arm64-only |
| F12-06 | MERGE en duplicados desde notificación | ✅ Fase 25 — Fusionar / No es duplicado / Cancelar |
