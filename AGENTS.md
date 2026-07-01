# AGENTS.md — Reglas de trabajo con IA para Kipu

Documento maestro para cualquier IA programadora que trabaje en este repositorio.  
Complementa `docs/ai/KIPU_AI_WORKFLOW.md`, `docs/ai/PROJECT_STATE.md` y los checklists en `docs/ai/`.

---

## 1. Proyecto y audiencia

**Kipu** es una app Android de finanzas personales para usuarios en **Perú**.

**Audiencia:** personas que usan Yape, Plin y efectivo diario; quieren controlar gastos hormiga, sobres semanales, metas, deudas sociales y juntas sin entregar claves bancarias.

**Idioma:** código en inglés; strings de UI en español peruano.

**Estado actual:** Fases 0–27 completadas en repo. Siguiente paso humano: Play Console internal testing. Ver `docs/release/INTERNAL_TESTING.md`.

---

## 2. Stack técnico

### Actual (hasta Fase 12)

| Tecnología | Estado |
|------------|--------|
| Kotlin | ✅ Activo |
| Jetpack Compose + Material 3 | ✅ Activo |
| Navigation Compose | ✅ Activo |
| Módulos Gradle multi-módulo | ✅ Activo |
| `minSdk` 26, `compileSdk` 37, `targetSdk` 36 | ✅ Activo |
| Package / `applicationId` `pe.kipu.app` | ✅ Activo |
| Hilt + KSP (DI) | ✅ Activo |
| `core/domain` (JVM puro + parsers/UseCases financieros) | ✅ Activo |
| `core/data` (Room v12 + DataStore + ML Kit OCR + NotificationListener) | ✅ Activo |
| ViewModels + UiState por feature | ✅ Activo |
| DataStore (preferencias usuario) | ✅ Activo |
| Room (`kipu.db` v12 — movimientos, categorías, sobres, compromisos, plan + `incomeProfile`/`payFrequency`) | ✅ Activo |
| UseCases presupuesto semanal (sobres) | ✅ Activo |
| Disponible diario + gastos hormiga (Home insights) | ✅ Activo |
| Duplicados con confirmación humana | ✅ Activo |
| Compromisos / metas + validación plan | ✅ Activo |
| Listener notificaciones ingresos Yape/Plin (opcional) | ✅ Activo |
| `TimeProvider` inyectable | ✅ Activo |
| ML Kit Text Recognition (OCR local) | ✅ Activo |
| Parsers Yape/Plin comprobantes + notificaciones ingreso (regex local, sin LLM) | ✅ Activo |

### Pendiente (fases futuras)

| Tecnología | Estado | Fase estimada |
|------------|--------|---------------|
| Firebase (opcional) | ⏳ Futuro, no MVP | — |
| IA generativa | ❌ Fuera del MVP | — |

---

## 3. Principios de producto

1. **Sin claves bancarias.** Kipu nunca pide contraseñas, PIN ni tokens de banca.
2. **Sin promesas criptográficas falsas.** No usar el término "Zero-Knowledge" si no existe arquitectura criptográfica real que lo respalde.
3. **Notificaciones opcionales.** Kipu puede detectar ingresos desde notificaciones cuando el permiso está activo; nunca es obligatorio.
4. **Comprobantes compartidos.** Kipu registra pagos Yape/Plin cuando el usuario comparte el comprobante (intent/share).
5. **Funciona sin permisos.** Registro manual y comprobantes compartidos deben ser suficientes para usar la app.
6. **Confirmación humana.** El usuario siempre revisa y confirma antes de guardar un movimiento sugerido (OCR, notificación o importación).
7. **Procesamiento local prioritario.** Datos financieros se procesan en el dispositivo; sin dependencia de red para el flujo principal.
8. **OCR local.** Reconocimiento de texto con ML Kit en el dispositivo; no enviar imágenes a servicios externos por defecto.
9. **Imágenes no a la nube por defecto.** Los comprobantes no se suben a servidores ni a IA en el MVP.
10. **Sin IA generativa en el MVP.** No integrar LLMs ni APIs generativas en producción durante el MVP.
11. **Exportar datos.** El usuario debe poder exportar todos sus datos (requisito MVP).
12. **Eliminar datos.** El usuario debe poder eliminar todos sus datos locales (requisito MVP).
13. **Permisos mínimos.** Solo pedir permisos estrictamente necesarios, con explicación en lenguaje simple.
14. **Validar entradas externas.** Todo input de comprobantes, intents y OCR debe validarse antes de persistir.
15. **Duplicados con confirmación.** Si se detecta un posible duplicado, la app pide confirmación; nunca fusiona en silencio.

---

## 4. Arquitectura obligatoria

### Estructura REAL actual (Fase 9)

```
kipu/
├── core/
│   ├── domain/
│   │   ├── parser/               → Yape/Plin parsers, ReceiptDateTimeParser
│   │   ├── usecase/              → comprobantes, sobres, disponible diario, hormiga
│   │   ├── time/                 → WeekRange, WeekRangeCalculator, TimeProvider
│   │   ├── ocr/                  → ReceiptOcrEngine (interface)
│   │   └── category/             → CategoryIds, YapeMessageCategoryRules
│   └── data/
│       ├── local/                → Room v2: movements, categories, envelopes
│       ├── repository/           → RoomMovement/Category/EnvelopeRepository
│       ├── ocr/                  → MlKitReceiptOcrEngine
│       └── di/                   → DatabaseModule, OcrModule, DataStoreModule, TimeModule
├── feature/
│   └── home/                     → HomeScreen insights (disponible hoy + hormiga)
```

### Estructura objetivo (fases futuras)

```
core/
├── domain/       → modelos puros, UseCases, interfaces de repositorio (sin Android)
├── data/         → Room, DAOs, entidades, mappers, implementaciones de repositorio
└── designsystem/ → tema, colores, componentes Compose reutilizables

feature/
├── home, movements, envelopes, commitments, profile  (existentes)
├── onboarding, comprobantes, juntas                  (futuros)
└── cada feature expone pantallas públicas; ViewModels en presentation
```

### Reglas de dependencia

| Regla | Descripción |
|-------|-------------|
| `app` → `feature/*` + `core/*` | El módulo app ensambla features y core; contiene navegación global. |
| `feature/*` → `core/designsystem` (+ `domain` vía presentation en fases futuras) | Ningún feature depende de otro feature. |
| `feature/*` ↛ `feature/*` | Prohibidas dependencias cruzadas entre features. |
| `domain` ↛ Android / Room / Compose / Firebase | Capa de dominio pura Kotlin. |
| `data` → `domain` | Implementa interfaces definidas en domain. |
| `presentation` no accede a DAOs | ViewModels usan UseCases o repositorios vía interfaces. |
| Entity ↔ Domain solo con mappers | Toda conversión pasa por mappers dedicados. |
| No exponer entidades Room a Compose | La UI consume modelos de dominio o UI state. |
| ViewModels sin lógica financiera compleja | Cálculos y parsers viven en UseCases. |
| Lógica fuera de Composables | Composables solo renderizan; sin reglas de negocio. |

### Contrato público de features

Cada feature expone al menos:

```kotlin
@Composable
fun XxxScreen(modifier: Modifier = Modifier)
```

---

## 5. Reglas Kotlin

- Preferir `val` sobre `var`.
- Usar `data class` inmutables.
- Usar `sealed interface` / `sealed class` para estados y errores.
- Evitar `!!`; usar safe calls, `requireNotNull` o early return.
- Evitar `GlobalScope`; usar `viewModelScope` o scopes inyectados.
- Usar `Result` o sealed classes para errores de dominio.
- Usar `Flow` / `StateFlow` para datos observables.
- Usar `SharedFlow` para eventos de una sola vez (snackbars, navegación).
- Código en inglés; strings de UI en español.

---

## 6. Reglas de seguridad y privacidad

### Producto

- Sin claves bancarias (ver principios de producto).
- Sin "Zero-Knowledge" sin arquitectura real.
- Confirmación humana antes de persistir movimientos sugeridos.
- Exportar y eliminar todos los datos (requisito MVP).
- OCR local; imágenes no a nube por defecto.
- Sin IA generativa en MVP.

### Técnico

- **No loguear:** montos, nombres, comprobantes, texto OCR completo ni datos financieros personales.
- **No hardcodear** secretos, tokens ni API keys.
- **Permisos mínimos** con explicación simple antes de solicitar.
- **Validar** todo input de comprobantes, intents compartidos y OCR.
- **`allowBackup`:** `false` en manifest (jun 2026); reglas XML como defensa en profundidad — ver `SECURITY_CHECKLIST.md`.
- **Componentes exportados** solo si es estrictamente necesario (`MainActivity` launcher).
- **Clasificación ECC:** CRITICAL / HIGH / MEDIUM / LOW (ver sección 9).

Checklist detallado: `docs/ai/SECURITY_CHECKLIST.md`.

---

## 7. Reglas TDD

Crear pruebas unitarias **antes o junto** con la implementación para:

| Módulo de lógica | Ubicación |
|------------------|-----------|
| Parser comprobantes Yape / Plin | `core/domain` ✅ |
| Cálculo de sobres | `core/domain` UseCase ✅ |
| Cálculo de disponible diario | `core/domain` UseCase ✅ |
| Detección de gasto hormiga | `core/domain` UseCase ✅ |
| Detección de duplicados | `core/domain` UseCase |
| Cálculo de metas de ahorro | `core/domain` UseCase |
| Validación de plan financiero negativo | `core/domain` UseCase |

**No aplica TDD** para: documentación, placeholders de UI, temas, navegación sin lógica.

Checklist detallado: `docs/ai/TDD_CHECKLIST.md`.

---

## 8. Metodología ECC

Aplicar **ECC Engineering System** en todo encargo de código:

| Paso | Acción |
|------|--------|
| 1. Brief | Definir objetivo observable, alcance, fuera de alcance, criterios de aceptación y riesgo dominante. |
| 2. Evidencia | Investigar el repo antes de editar; leer archivos reales; no asumir estructura. |
| 3. Cambio mínimo | Una capacidad dominante por vez; sin refactors oportunistas. |
| 4. TDD | RED → GREEN → REFACTOR cuando hay lógica de comportamiento (o justificar excepción). |
| 5. Revisión | Revisar diff, capas, nulos, concurrencia y seguridad según superficie. |
| 6. Cierre | Ejecutar verificación; reportar con contrato LISTO/NO LISTO. |

Resumen de integración: `docs/ai/ECC_INTEGRATION.md` (si existe).

---

## 9. Clasificación de riesgo

| Nivel | Criterio | Ejemplos en Kipu | Acción |
|-------|----------|------------------|--------|
| **Bajo** | Cambio local, reversible, sin datos sensibles | Docs, placeholders UI, tema, navegación vacía | Implementar + `assembleDebug` |
| **Medio** | Comportamiento compartido, persistencia, parsers, permisos | Room schema, parser Yape, export CSV, permiso notificaciones | TDD + checklist seguridad + revisión diff |
| **Alto** | Datos sensibles, producción, migraciones, sync, auth | Backup rules con datos financieros, Firebase, cifrado, sync nube | Aprobación explícita + revisión seguridad completa |

---

## 10. Escalera de verificación Gradle

Ejecutar en orden según el cambio:

| Orden | Comando | Cuándo |
|-------|---------|--------|
| 1 | `./gradlew :<módulo>:testDebugUnitTest` | Lógica de negocio en un módulo |
| 2 | `./gradlew testDebugUnitTest` | Cambios que afectan tests unitarios |
| 3 | `./gradlew assembleDebug` | **Siempre** — mínimo obligatorio |
| 4 | `./gradlew lintDebug` | UI, Manifest, recursos, permisos |
| 5 | `./gradlew connectedDebugAndroidTest` | Flujos instrumentados (emulador/dispositivo) |

No declarar **LISTO** si falla una comprobación obligatoria del encargo.

---

## 11. Contrato de salida obligatorio

Al terminar cualquier tarea, la IA debe reportar:

```
## Cambio
[Qué se hizo y por qué]

## Evidencia
- Comandos ejecutados y resultado (PASS/FAIL)
- Revisión manual si aplica

## Riesgos residuales
[Lo que queda pendiente o sin verificar]

## Comprobaciones no ejecutadas
[Lo que no se pudo correr y por qué]

## Estado: LISTO / NO LISTO
```

**LISTO** solo si todos los criterios de aceptación del encargo se cumplen y las verificaciones obligatorias pasan.

---

## 12. Flujo de trabajo con IA

1. Leer `AGENTS.md` y `docs/ai/PROJECT_STATE.md`.
2. Usar la plantilla de `docs/ai/KIPU_AI_WORKFLOW.md` (10 secciones).
3. Indicar skill obligatoria: **ECC Engineering System**.
4. Activar skills de `docs/ai/skills/` según el tipo de tarea (ver mapa en workflow).
5. Definir archivos permitidos y prohibidos explícitamente.
6. Pedir una capacidad dominante por encargo; no "haz toda la app".
7. Aplicar `TDD_CHECKLIST.md` o `SECURITY_CHECKLIST.md` cuando corresponda.
8. Cerrar con contrato de salida (sección 11).

---

## 13. Frase de control al finalizar tareas

Cuando una IA genere o modifique código para Kipu, debe responder siempre con:

1. **Archivos** creados o modificados (con breve descripción).
2. **Supuestos** tomados.
3. **Evidencia** de verificación (comandos y resultados).
4. **Pruebas** recomendadas o ejecutadas.
5. **Riesgos** residuales.
6. **Estado:** LISTO / NO LISTO.
