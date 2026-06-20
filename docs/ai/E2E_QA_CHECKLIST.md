# E2E QA — Kipu MVP

> Checklist y suite automatizada post-Fase 16. Última revisión: jun 2026.

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
export ANDROID_SERIAL=<serial>   # ej. ZT322PDDPK (Moto G24)
./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

### Última ejecución automatizada (19 jun 2026 — Moto G24, Android 14)

| Módulo | Resultado |
|--------|-----------|
| `:app` | **11/11 PASS** |
| `:core:data` | **5/5 PASS** |

Incluye wizard de plan (4 pasos), navegación, crear junta, share intent, duplicados, migraciones y wipe instrumentado.

### Cobertura automatizada

| Módulo | Test | Flujo |
|--------|------|-------|
| `:app` | `MainActivitySmokeTest` | Launch MainActivity |
| `:app` | `KipuNavigationE2ETest` | Bottom bar: Inicio → Movimientos → Sobres → Compromisos → Perfil |
| `:app` | `KipuNavigationE2ETest` | Perfil → Ver juntas |
| `:app` | `KipuNavigationE2ETest` | Crear junta (nombre + participantes) |
| `:app` | `PlanWizardE2ETest` | Sobres → Ingresos → wizard 4 pasos → guardar |
| `:app` | `DuplicateResolutionDialogTest` | Diálogo duplicados en español |
| `:core:data` | `KipuDatabaseMigrationInstrumentedTest` | Migraciones v4→v6 y v6→v7 |
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

### Juntas (F16c)

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

### Navegación / regresión

| # | Caso | Pasos | Esperado |
|---|------|-------|----------|
| R1 | Tabs | Recorrer 5 tabs bottom bar | Sin crash; headers correctos |
| R2 | Sobres → movimientos | Ver movimientos de sobre Comida | Filtro categoría activo |
| R3 | Wizard plan | Onboarding → Comenzar (usuario nuevo) | Wizard ingresos/gastos/resumen |

---

## Criterios de salida QA

- Todos los tests instrumentados PASS en dispositivo físico arm64 (**Moto G24: 16/16 PASS**, 19 jun 2026)
- N1–N5 verificados en **dispositivo físico** con apps reales
- C1–C3 verificados al menos una vez
- J1–J4 verificados
- E1–E3 verificados
- `:app:lintDebug` + `assembleRelease` PASS

## Hallazgos conocidos

| ID | Nota |
|----|------|
| E2E-01 | Onboarding skip en tests requiere `performScrollTo()` — botón "Configurar plan después" está al final del scroll |
| E2E-02 | Orden de tests en `KipuNavigationE2ETest`: `@FixMethodOrder(NAME_ASCENDING)` |
| F12-05 | Automatizado parcial: packages corregidos + test JVM; ingreso real requiere hardware |
| F14c | Emulador x86 requiere `-Pkipu.x86Emulator=true`; release sigue arm64-only |
| F12-06 | MERGE en duplicados desde notificación no soportado en MVP |
