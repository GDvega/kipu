# PROJECT_STATE.md — Snapshot post-Fase 27 (internal testing Play Store)

Última actualización: **13 agosto 2026**.
Este documento es la **fuente de verdad** del estado del repositorio entre fases. Actualizar al cerrar cada fase o bloque de entrega significativo.

> **Para IAs:** leer primero [Trampas conocidas para futuras sesiones](#trampas-conocidas-para-futuras-sesiones-ia), [Navegación extendida](#navegación-extendida-post-fase-14) y [Dependencias entre módulos](#dependencias-actuales). Evitar reintroducir pasos de onboarding eliminados, rutas duplicadas o dependencias circulares feature↔feature.

---

## Mapa de fases (canónico)

Numeración **continua 0–27**. Tras el MVP (Fase 16), el trabajo post-MVP usa **17–27** sin saltos.

| Fase canónica | Nombre | Alias legacy |
|---------------|--------|--------------|
| 0–16 | MVP (shell → juntas + pulido) | — |
| **17** | Cierre riesgos F16 | 16b |
| **18** | Cierre riesgos restantes (juntas, wipe, acoplamiento) | 16c |
| **19** | Riesgos críticos (backup, wipe cache, plan inválido, wizard persistente) | 21 |
| **20** | Riesgos residuales (tests, ids seed, onboarding UseCase) | 21b |
| **21** | Armonización UX/UI | 21c |
| **22** | CRUD sobres + compromisos | 22 |
| **23** | Metas ↔ movimientos | 23 |
| **24** | Wizard edita plan (F14-02) | 24 |
| **25** | MERGE duplicados notificación (F12-06) | 25 |
| **26** | QA Play Store + cierre ECC F14 | 26 |
| **27** | Internal testing (release pipeline) | 27 |

**Próximo incremento:** subir AAB firmado a Play Console (pasos humanos en `docs/release/INTERNAL_TESTING.md`).

---

## Resumen ejecutivo (jun 2026)

| Área | Estado |
|------|--------|
| Fases 0–12 | ✅ Cerradas formalmente (ECC LISTO) |
| Fase 13 — Export / wipe | ✅ **MVP funcional** (Perfil → Tus datos) |
| Fase 14 — Onboarding + plan | ✅ **ECC LISTO** (cierre formal Fase 26) |
| Pulido UI Movimientos/Sobres | ✅ Paridad visual HTML; acciones cableadas |
| Optimización APK (14c) | ✅ `arm64-v8a` + R8/shrink release (~14 MB vs ~27 MB debug) |
| Fase 15 — Comprobantes UI | ✅ **MVP funcional** (share intent + revisión manual) |
| Fase 16 — Juntas + pulido | ✅ **MVP funcional** (CRUD juntas local + lint/release) |
| Fases 17–27 — Post-MVP | ✅ Repo listo — Play Console pendiente |
| Fase 17 — Cierre riesgos F16 | ✅ Migraciones Room, editar/reparto juntas, KSP, backup |
| Fase 18 — Cierre riesgos restantes | ✅ Wipe instrumentado, liquidación juntas, Room v7 |
| Fase 19 — Riesgos críticos | ✅ Backup, wipe cache comprobantes, plan inválido, wizard persistente |
| Fase 20 — Riesgos residuales | ✅ Tests domain/data, ids seed unificados, `CompleteOnboardingUseCase` |
| Fase 21 — Armonización UX/UI | ✅ Tokens layout, diálogos DS, tabs + secundarias alineadas |
| Fase 22 — CRUD producto | ✅ Crear/eliminar sobres; CRUD compromisos |
| Fase 23 — Metas ↔ movimientos | ✅ `commitmentId` en movimientos; progreso desde ingresos vinculados |
| Fase 24 — Wizard edita plan | ✅ Precarga plan; accesos directos sobres/meta (F14-02) |
| Fase 25 — MERGE duplicados notificación | ✅ Paridad con diálogo movimientos/comprobantes (F12-06) |
| Fase 26 — QA Play Store | ✅ Política privacidad in-app + docs release; ECC F14 cerrado |
| Fase 27 — Internal testing pipeline | ⚠️ Pipeline con guard de firma; AAB firmado y carga humana pendientes |
| Verificación reciente | 13 ago 2026: `:app` **43/43 PASS** y `:core:data` **24/24 PASS** en Moto G24 (Android 14, fuente 1.3) con Gradle offline; regresión focalizada **9/9 PASS** de navegación, semántica MEDIUM y wizard. El histórico 11 ago (38/38 + 24/24) se conserva abajo |

### Remediación HIGH de interfaz y controles (11 ago 2026)

| Superficie | Estado | Evidencia |
|------------|--------|-----------|
| Contraste claro/oscuro | ✅ Corregido | Roles Material 3 explícitos; regresión calcula mínimo 4.66:1 para texto y 3.17:1 para componentes/contornos |
| Acciones destructivas | ✅ Corregido | Borrado de datos, sobres, compromisos y juntas, además de desvincular meta, usan el rol `error`; doble confirmación de wipe preservada |
| Chips, tarjetas y radios | ✅ Corregido | Selección nativa `selected`/radio, grupos seleccionables y objetivos táctiles de 48 dp; prueba semántica instrumentada PASS |
| Speed dial de Inicio | ✅ Corregido | Una acción semántica y objetivo mínimo de 48 dp por opción, scrim sin foco redundante y Back cierra el menú; prueba y smoke físico PASS |
| Formularios y errores | ✅ Corregido | Contenido desplazable con IME; error asociado al campo con `Error` + `LiveRegion.Polite`; prueba instrumentada y smoke con teclado PASS |
| Inicio, Sobres y Juntas adaptativos | ✅ Corregido | `FlowRow` evita comprimir/truncar montos; Juntas permite seleccionar movimientos posteriores al octavo; prueba del movimiento 9 PASS |
| Onboarding | ✅ Corregido | Reintento ejecuta otra vez el caso de uso, bloquea doble envío y el CTA queda sobre la navegación de tres botones |
| Movimientos | ✅ Corregido | El FAB se oculta en Loading, Error y vacío para no duplicar acciones primarias |

Verificación del bloque: `testDebugUnitTest`, `assembleDebug`, `lintDebug`, 26/26 pruebas instrumentadas de app y 24/24 de core:data PASS. Smoke en Moto G24 con ancho aproximado de 347 dp, fuente 1.3x, teclado real y navegación de tres botones: Inicio, Sobres, onboarding, formulario manual, speed dial y confirmación destructiva sin solapes ni truncado observable. La locución audible con TalkBack permanece como revisión humana; las propiedades semánticas se verifican automáticamente.

### Remediación MEDIUM de interfaz y controles (11 ago 2026)

| Superficie | Estado | Evidencia |
|------------|--------|-----------|
| Movimiento manual | ✅ Corregido | El formulario abre sin error prematuro y mantiene Guardar deshabilitado hasta recibir un monto válido; regresión JVM PASS |
| Acciones asíncronas | ✅ Corregido | Las acciones cubiertas en sobres, compromisos, juntas, movimientos, comprobantes y Perfil rechazan doble envío, cierran solo en éxito y conservan el modal con error o snackbar al fallar |
| Estado ocupado | ✅ Corregido | Back, toque exterior, transición oculta de bottom sheets y controles editables quedan bloqueados durante la operación; regresiones instrumentadas PASS |
| Feedback de controles | ✅ Corregido | Se eliminaron vibraciones de éxito antes de validar o persistir; el feedback táctil genérico ya no comunica un resultado falso |
| Switches y acciones contextuales | ✅ Corregido | Las filas completas exponen etiqueta, rol y estado; duplicados anuncian rol botón y cada junta identifica su menú por nombre |
| Comprobantes y Perfil | ✅ Corregido | Guardado/fusión permanece visible hasta pulsar Listo; sus fallos y los estados éxito/error de Perfil se anuncian con semántica y color correctos |
| Speed dial y categorías de Inicio | ✅ Corregido | Menú expandido usa una ventana modal real y Back lo cierra; categorías se anuncian como navegación, no como filtros seleccionables |

Verificación del bloque: `testDebugUnitTest`, `assembleDebug`, `lintDebug`, 38/38 pruebas instrumentadas de app y 24/24 de core:data PASS en Moto G24. Las 12 regresiones MEDIUM focalizadas cubren vibración prematura, cierre durante guardado, switches, estados de Perfil, resultado y error de comprobantes, aislamiento modal del speed dial y semántica de categorías. No se añadieron dependencias ni se borraron datos. La locución audible con TalkBack permanece como comprobación humana; las propiedades semánticas se verifican automáticamente.

### Seguimiento de riesgos de comprobantes y consistencia (13 ago 2026)

| Superficie | Estado | Evidencia de implementación |
|------------|--------|-----------------------------|
| Captura temporal | ✅ Instrumentación app | Foto de cámara con UUID en `cache/receipts`; `ReceiptCaptureUriFactoryInstrumentedTest` y `ReceiptReviewViewModelErrorInstrumentedTest` comprueban que solo se borra una captura propia, al cancelar/cerrar o liberar la revisión |
| Share intent | ⚠️ Cobertura automática parcial | `ReceiptShareIntentParserTest` acepta `ACTION_SEND image/*` únicamente con URI `content`; `MainActivity` consume la URI pendiente en un único `LaunchedEffect`. El share externo en inicio frío/cálido sigue pendiente de prueba humana |
| Fallo OCR/carga | ✅ Instrumentación app | `ReceiptReviewViewModelErrorInstrumentedTest` verifica que una excepción inesperada expone Error recuperable y que Reintentar no duplica el procesamiento |
| Privacidad de errores Room | ✅ Verificado JVM | Los mappers ya no interpolan montos ni objetos financieros en `error()`; `SensitiveMapperErrorMessageTest` PASS |

Verificación en dispositivo del 13 ago: `./gradlew --offline --no-daemon --max-workers=1 -Pksp.incremental=false :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` terminó PASS (`:app` 43/43, `:core:data` 24/24) en Moto G24, Android 14 y fuente 1.3. La ejecución focalizada de `KipuNavigationE2ETest`, `MediumAccessibilitySemanticsTest` y `PlanWizardE2ETest` también terminó 9/9 PASS. No existe purga de capturas huérfanas al iniciar: una captura interrumpida puede permanecer en caché hasta la limpieza de Android o el borrado local, para no perder una captura aún pendiente de una cámara externa. C1–C3 de share real, cámara física y locución audible con TalkBack permanecen como comprobaciones humanas.

### Verificación de viewport compacto (13 ago 2026)

| Superficie | Estado | Evidencia automatizada en Moto G24, fuente 1.3 |
|------------|--------|-----------------------------------------------|
| Juntas | ✅ | `KipuNavigationE2ETest` crea una junta y completa sus dos campos en el formulario compacto; incluido en 43/43 y en la focalizada 9/9 |
| Wizard de plan | ✅ | `PlanWizardE2ETest` recorre los seis pasos, guarda y reabre el plan; incluido en 43/43 y en la focalizada 9/9 |
| Switches de hormiga y deuda social | ✅ | `MediumAccessibilitySemanticsTest` valida objetivo mínimo, rol y estado en contenido desplazable; incluido en 43/43 y en la focalizada 9/9 |

### Remediación HIGH de auditoría (1 ago 2026)

| Hallazgo | Estado | Evidencia |
|----------|--------|-----------|
| H-01 — exportación JSON incompleta | ✅ Corregido | Snapshot v3 incluye gastos de junta y todos los campos persistidos de plan, junta y preferencias; tests JSON/CSV PASS |
| H-02 — saldo inicial fuera de Efectivo real | ✅ Corregido | `CalculateCashFlowSummaryUseCase` suma saldo inicial + ingresos − gastos; test domain e integración Home PASS |
| H-03 — edición de junta invalida liquidación | ✅ Corregido | Conserva `isSettled` y rechaza quitar pagadores con gastos; test domain PASS |
| H-04 — eliminación de junta sin confirmación | ✅ Corregido | Diálogo destructivo explícito antes de ejecutar la cascada Room; compilación feature PASS |
| H-05 — telemetría ML Kit no divulgada | ✅ Corregido | Política in-app, Markdown, HTML y Data Safety distinguen OCR local de métricas técnicas del SDK |
| H-06 — AAB release sin firma | ✅ Guardrail implementado / ⏳ firma humana | `preReleaseBuild` falla si falta `keystore.properties`; no se crean ni usan credenciales reales desde IA |

Verificación del bloque:

| Comando | Resultado |
|---------|-----------|
| `./gradlew :core:domain:test` | PASS |
| `./gradlew testDebugUnitTest` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew lintDebug` | PASS |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS tras autorizar ADB — 18 pruebas app + 24 pruebas core:data en Moto G24 |
| `./gradlew bundleRelease` sin `keystore.properties` | FAIL esperado: el guard de firma bloqueó la generación |

### Auditoría del wizard de plan (1 ago 2026)

Auditoría y remediación incremental en Moto G24 documentadas en `docs/qa/KIPU_PLAN_WIZARD_AUDIT_2026-08-01.md`: **10/10 hallazgos corregidos**. PLAN-H01–H06 cubren scroll, filas adaptativas, conversión mensual única, rehidratación, moneda/horizonte y preset anti-hormiga. PLAN-M01 mantiene el error visible junto a Continuar con semántica `Error` + `LiveRegion.Polite`; PLAN-M02 reemplaza el booleano superficial por un E2E de `MainActivity` que recorre los seis pasos, guarda, reabre, edita `5000→5200` y confirma el round-trip sin borrar Room; PLAN-M03 usa `FlowRow` y PLAN-L01 verifica un control estable de Compromisos. Unitarias, `assembleDebug`, lint y 18/18 app + 24/24 core:data PASS con fuente 1.3x. Auditoría global del wizard: **LISTO**; TalkBack audible y Play Console siguen siendo comprobaciones humanas separadas.

### Remediación auditoría (21 jun 2026)

| Cambio | Detalle |
|--------|---------|
| Room v16 | `MIGRATION_15_16` lleva la configuración de gasto hormiga al plan Room para guardado transaccional; conserva migraciones anteriores |
| Plan wizard | `PlanWizardUiState.Error` + retry (AUD-002) |
| Dominio | Validación plan con ingresos vinculados; IDs borrador OCR con nonce (AUD-004, AUD-008) |
| Data | Wipe prefs antes de Room; OCR subsample; `allowBackup=false` (AUD-009, AUD-010, AUD-016) |
| Arquitectura | `ProcessReceipt*UseCase` en domain; `:feature:receipts` sin `:core:data` (AUD-005) |
| UX | Retry en 5 VMs; OCR retry; onboarding Error; parse límite sobres (AUD-006, AUD-013–015) |
| Tests | Migraciones v7→8, v11→12; `PendingPlanWizardInstrumentedTest` (AUD-011, AUD-012) |

**Room actual:** `KipuDatabase` version **16** — migraciones `MIGRATION_1_2` … `MIGRATION_15_16`.

---

## Fases completadas

| Fase | Nombre | Estado | Verificación |
|------|--------|--------|--------------|
| **0** | Shell multi-módulo | ✅ Completada | `./gradlew assembleDebug` PASS |
| **1** | Documentación de control | ✅ Completada | Revisión manual de docs; `assembleDebug` PASS |
| **2** | Design system ampliado | ✅ Completada | `assembleDebug` PASS; `:core:designsystem:lintDebug` PASS |
| **3** | Domain base | ✅ Completada | `:core:domain:test` PASS; `assembleDebug` PASS |
| **4** | DI + presentation base | ✅ Completada | Hilt + ViewModels; `assembleDebug` PASS; `:core:domain:test` PASS |
| **5** | DataStore + preferencias | ✅ Completada | `:core:data:testDebugUnitTest` PASS; `assembleDebug` PASS |
| **6** | Room + movimientos | ✅ Completada | Mappers TDD; backup rules; `assembleDebug` PASS |
| **7** | Parsers + OCR | ✅ Completada | 30+ tests domain; ML Kit; `assembleDebug` PASS |
| **8** | Sobres | ✅ Cerrada (ECC LISTO) | 14 tests nuevos; Room v2; revisión jun 2026; Gradle PASS |
| **9** | Disponible diario + hormiga | ✅ Cerrada (ECC LISTO + F9b) | 17 tests domain; Home insights; Gradle PASS |
| **10** | Duplicados | ✅ **Cerrada formalmente** (ECC LISTO + F10b + F10c) | 32+ tests domain; Room v3; lint PASS |
| **11** | Compromisos / metas | ✅ **Cerrada formalmente** (ECC LISTO + F11b) | 15 tests + Room v4; alerta plan sin compromisos; Gradle PASS |
| **12** | Notificaciones | ✅ **Cerrada formalmente** (ECC LISTO + F12b) | 19+ tests domain/data; dedup post-confirm; Gradle PASS |
| **13** | Exportar / eliminar datos | ✅ **MVP funcional** | JSON + CSV + wipe; Perfil; Gradle PASS |
| **14** | Onboarding + plan + pulido UI | ✅ **MVP funcional** | Wizard; movimientos/sobres HTML; APK arm64 |
| **15** | Comprobantes UI | ✅ **MVP funcional** | Share intent + OCR revisión; `:feature:receipts` |
| **16** | Juntas + pulido | ✅ **MVP funcional** | Room v5→v6; CRUD juntas; lint + release PASS |
| **17** | Cierre riesgos F16 | ✅ **Completada** | Migraciones Room v1→v6; editar/reparto juntas; KSP + backup |
| **18** | Cierre riesgos restantes | ✅ **Completada** | Wipe instrumentado; liquidación juntas; Room v7 |
| **19** | Riesgos críticos | ✅ **Completada** | Backup rules; wipe cache; plan inválido; `pendingPlanWizard` |
| **20** | Riesgos residuales | ✅ **Completada** | Tests domain/data; ids seed; onboarding UseCase |
| **21** | Armonización UX/UI | ✅ **Completada** | Design system tokens; pantallas tabs + secundarias alineadas |
| **22** | CRUD sobres + compromisos | ✅ **Completada** | UseCases domain + diálogos UI; `:core:domain:test` + `assembleDebug` PASS |
| **23** | Metas ↔ movimientos | ✅ **Completada** | `commitmentId` Room v10; vincular ingresos; progreso meta reactivo |
| **24** | Wizard edita plan (F14-02) | ✅ **Completada** | `PlanWizardStateLoader`; chips Sobres/Meta; precarga plan |
| **25** | MERGE duplicados notificación (F12-06) | ✅ **Completada** | `ConfirmPendingNotificationMovementUseCase` MERGE; diálogo paridad UI |
| **26** | QA Play Store + ECC F14 | ✅ **Completada** | `docs/release/*`; `PrivacyPolicyScreen`; lint + release PASS |
| **27** | Internal testing pipeline | ✅ **Completada** (repo) | Signing template; `bundleRelease`; E2E nav privacidad; `docs/privacy/` |

### Entregables Fase 1

- `AGENTS.md` — reglas maestras + ECC
- `docs/ai/KIPU_AI_WORKFLOW.md` — plantilla de prompt (10 secciones) y revisión
- `docs/ai/TDD_CHECKLIST.md` — obligaciones TDD por módulo y fase
- `docs/ai/SECURITY_CHECKLIST.md` — triggers, severidad y superficies
- `docs/ai/ECC_INTEGRATION.md` — resumen ciclo ECC en Kipu
- `docs/ai/PROJECT_STATE.md` — este snapshot

### Entregables Fase 2

- `KipuTopBar` — fix deprecación (`topAppBarColors`)
- `KipuPrimaryButton`, `KipuSecondaryButton`
- `KipuCard` (content + title/subtitle)
- `KipuAmountText` + `AmountType` + `formatPenAmountForDisplay`
- `KipuEmptyState`, `KipuErrorState`
- `KipuIncome`, `KipuExpense` en `KipuColors`
- `DesignSystemPreview` — showcase de componentes

### Entregables Fase 3

- Módulo `:core:domain` — Kotlin JVM puro (sin Android)
- Modelos: `Money`, `Movement`, `SuggestedMovement`, `Category`, `Envelope`, `Commitment`, `Gathering`, `FinancialPlan`, enums de dominio
- `SuggestionConfidence`, `operationNumber`, `isSettled`, `categorySuggestionReason` (ampliación post-revisión ECC)
- `DomainError`, `DomainResult` — errores y validación sin excepciones genéricas
- Interfaces: `MovementRepository`, `CategoryRepository`, `EnvelopeRepository`, `CommitmentRepository`, `GatheringRepository`, `FinancialPlanRepository`
- Paquete `usecase/` reservado (sin lógica financiera)
- Tests: `MoneyTest`, `MovementTest`, `SuggestedMovementTest`, `CommitmentTest`

### Entregables Fase 4

- **Hilt 2.59.2** + KSP en `app` y `feature/*`
- `KipuApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`)
- Módulo `:core:data` con `Fake*Repository` (6 repos en memoria)
- `RepositoryModule` — binds interfaces domain → fakes
- `CategorySuggestionTranslator` — claves dominio → español
- ViewModels + `UiState` (Loading/Content/Error) en los 5 features
- Pantallas conectadas con `hiltViewModel()` + `KipuEmptyState` / `KipuErrorState`
- `gradle.properties`: `android.disallowKotlinSourceSets=false`, `android.enableJetifier=false` (AGP 9 + Hilt)

### Entregables Fase 5

- `UserPreferences`, `ThemeMode`, `UserPreferencesRepository` en `core/domain`
- Preferences DataStore (`kipu_preferences`) en `core/data`
- `UserPreferencesKeys`, `UserPreferencesMapper`, `DataStoreUserPreferencesRepository`
- `DataStoreModule` — provee `DataStore<Preferences>` vía Hilt
- `RepositoryModule` — bind `UserPreferencesRepository` → DataStore impl
- `ProfileViewModel` observa/actualiza preferencias; `ProfileScreen` con toggles en español
- `clear()` en repositorio preparado para wipe (Fase 13)
- Tests: `UserPreferencesMapperTest` (mapper + validación de claves)

### Entregables Fase 6

- **Room 2.7.1** + KSP en `core/data`
- `KipuDatabase` (`kipu.db`, version 1) — `MovementEntity`, `CategoryEntity`
- `MovementDao`, `CategoryDao`; orden `recordedAtMillis DESC`
- `Money` persistido como `amountCents` (Long, centavos PEN)
- `MovementMapper`, `CategoryMapper` + tests JVM
- `RoomMovementRepository`, `RoomCategoryRepository` (validan con `validate()`)
- Seed categorías: `category-food`, `category-transport`, `category-services`, `category-other`
- `DatabaseModule` + binds Hilt (Movement/Category → Room; resto → fakes)
- `MovementsScreen` — lista con `KipuCard` + `KipuAmountText`
- `backup_rules.xml` / `data_extraction_rules.xml` — excluyen `kipu.db` del backup en nube
- `MovementDaoInstrumentedTest` — inserción mínima vía DAO
- **Nota wipe F13:** `UserPreferencesRepository.clear()` no borra Room aún

### Entregables Fase 7

- Parsers JVM: `YapeReceiptParser`, `PlinReceiptParser`, `ReceiptParserRouter`
- `ReceiptParseResult`, `OcrImage`, `CategoryIds`, `YapeMessageCategoryRules`
- `ReceiptOcrEngine` (domain) + `MlKitReceiptOcrEngine` (data, ML Kit 16.0.1)
- UseCases: `ParseReceiptTextUseCase`, `SuggestCategoryFromYapeMessageUseCase`, `SuggestCategoryFromPlinHistoryUseCase`, `ConfirmSuggestedMovementUseCase`
- `ProcessReceiptImageUseCase` — OCR → parse (sin persistir imagen)
- `OcrModule` — Hilt bind OCR
- Sanitización OCR: trim, colapsar espacios, máx 20k chars; `OcrImage` máx 10 MB
- Supuesto: comprobantes de pago → `MovementType.EXPENSE`
- Razones categoría: `receipt_keyword_match`, `plin_history_match`
- Tests: 8 fixtures OCR + 30 tests JVM (parsers + use cases)
- **Sin UI comprobantes** (Fase 15); **sin permisos** nuevos en Manifest

### Entregables Fase 7b (hotfix ECC)

- `SuggestedMovement.suggestedRecordedAt: Instant?` — fecha/hora comprobante (America/Lima)
- `ReceiptDateTimeParser` — español `16 jun. 2026 - 3:45 p. m.` y `dd/MM/yyyy HH:mm`
- `ReceiptConfidenceResolver` — HIGH con operación **o** fecha+hora (no solo fecha)
- `ConfirmSuggestedMovementUseCase` — `recordedAt` opcional; default desde `suggestedRecordedAt`
- Tests: `ReceiptDateTimeParserTest`, ampliación Yape/Plin/Confirm/Parse; `ProcessReceiptImageUseCaseTest` (fake OCR)
- Hallazgos cerrados: **F7-01 … F7-05**

### Entregables Fase 8

- `EnvelopeBudgetStatus`, `EnvelopeBudgetState`, `EnvelopeBudgetThresholds` (ADJUSTED ≥ 80 %)
- `WeekRange`, `WeekRangeCalculator` — semana actual America/Lima, inicio lunes 00:00
- UseCases: `CalculateCategoryWeeklySpentUseCase`, `CalculateEnvelopeBudgetStateUseCase`, `ObserveEnvelopeBudgetsUseCase`
- **Room v2** (`kipu.db`): `EnvelopeEntity`, `EnvelopeDao`, `EnvelopeMapper` + test
- `RoomEnvelopeRepository`; seed demo: Comida S/150, Transporte S/80, Servicios S/60
- `spentAmount` **no** persistido; calculado desde movimientos EXPENSE CONFIRMED de la semana
- `EnvelopesScreen` — lista con progreso, % usado y estado (OK / Cerca del límite / Excedido)
- `EnvelopesViewModel` → `ObserveEnvelopeBudgetsUseCase` (sin lógica financiera en VM)
- `RepositoryModule` — bind `EnvelopeRepository` → Room
- Migración MVP: `fallbackToDestructiveMigration` v1→v2 documentado
- Tests JVM: `WeekRangeCalculatorTest`, cálculo sobres, `ObserveEnvelopeBudgetsUseCaseTest` (+14 tests Fase 8)

### Cierre formal Fase 8

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | LISTO |
| **Revisión** | Gestor maestro — post-entrega IA programadora |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug` PASS |
| **Tests nuevos** | 14 (12 domain + 2 data) |
| **Próxima fase** | Fase 10 — Duplicados |

### Entregables Fase 9

- `TimeProvider` + `SystemTimeProvider` (JVM); `TimeModule` Hilt bind
- `WeekRangeCalculator` inyecta `TimeProvider` (cierra parcialmente **F8-04**)
- Modelos: `DailyAvailableBudget`, `AlertSeverity`, `AntSpendingAlert`, `HomeInsights`, `WeeklyEnvelopeTotals`
- `AntSpendingThresholds` — S/ 20.00, 3 gastos, ventana 48 h
- UseCases: `CalculateWeeklyEnvelopeTotalsUseCase`, `CalculateDailyAvailableUseCase`, `DetectAntSpendingUseCase`, `ObserveHomeInsightsUseCase`
- `HomeScreen` — tarjeta “Disponible hoy” + alertas gastos hormiga (ámbar/rojo)
- `HomeViewModel` → `ObserveHomeInsightsUseCase`; `HomeAlertTranslator` (ES)
- Alertas **no persistidas**; recalculadas desde movimientos + sobres
- Tests JVM: `CalculateDailyAvailableUseCaseTest`, `DetectAntSpendingUseCaseTest`, `ObserveHomeInsightsUseCaseTest` (+13 tests Fase 9)

### Entregables Fase 9b (hotfix ECC)

- `CalculateWeeklyEnvelopeTotalsUseCase` — `totalRemaining = totalLimit - totalSpent` global (no suma de `remainingAmount` por sobre)
- `CalculateWeeklyEnvelopeTotalsUseCaseTest` — caso sobre excedido + global dentro de presupuesto
- `CalculateDailyAvailableUseCaseTest` — días restantes cero + integración global remaining
- `AntSpendingAlertKeys` — claves dominio; `HomeAlertTranslator` sin depender de UseCase
- Hallazgos cerrados: **F9-01**, **F9-02**, **F9-03**

### Cierre formal Fase 9

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | LISTO |
| **Revisión** | Gestor maestro — entrega IA + hotfix F9b |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug` PASS |
| **Tests Fase 9** | 13 (domain) |
| **Tests Fase 9b** | +4 (domain) |
| **Próxima fase** | Fase 10 — Duplicados |

### Entregables Fase 10

- `DuplicateDetectionConfig`, `MovementDuplicateMatcher`, `DuplicateMatchReasonKeys`
- Modelos: `MovementDuplicatePair`, `DuplicateDetectionResult`, `ConfirmMovementResult`
- UseCases: `DetectDuplicateMovementUseCase`, `FindMovementDuplicatePairsUseCase`, `ResolveDuplicateMovementUseCase`, `ObserveMovementDuplicatePairsUseCase`, `ConfirmSuggestedMovementWithDuplicateCheckUseCase`, `DismissDuplicatePairUseCase`
- Refactor: `buildConfirmedMovementFromSuggestion` compartido; facade `ConfirmSuggestedMovementUseCase` (F10b)
- `MovementsScreen` — sección "Posibles duplicados" + `DuplicateResolutionDialog` (Fusionar / No es duplicado / Cancelar)
- `MovementDuplicateTranslator` — claves dominio → español
- Pares descartados persistidos en Room (`dismissed_duplicate_pairs`, v3)
- Tests JVM: matcher + UseCases; Compose test diálogo (F10c); `MainActivitySmokeTest` (F10c)

### Entregables Fase 10b (hotfix ECC)

- `ConfirmSuggestedMovementUseCase` delega a `ConfirmSuggestedMovementWithDuplicateCheckUseCase` (sin bypass de duplicados)
- `DuplicateConfirmationRequiredException` — fallo tipado cuando hay duplicado sin resolución
- Parámetro opcional `resolution: DuplicateResolution?` en el facade
- `ConfirmSuggestedMovementUseCaseTest` — +2 tests (bloqueo duplicado + SAVE_AS_NEW)
- Hallazgo cerrado: **F10-01**

### Entregables Fase 10c (hotfix hallazgos abiertos)

- **F10-02:** `DuplicateDismissalRepository` + Room v3 + `DismissDuplicatePairUseCase`
- **F10-03:** match fuerte por `operationNumber` exige mismo monto
- **F10-04:** `MERGE` con `createdAt` igual elimina movimiento con id lexicográficamente mayor
- **F9-05:** nombre de categoría en alertas hormiga (`HomeViewModel` + `HomeAlertTranslator`)
- **F9-04 / F8-04:** `TimeProvider.refreshTicks()` en insights Home y sobres
- **F7-06:** seed Room usa `CategoryIds` (eliminado `DefaultCategoryIds`)
- **F7-07:** `MovementRepository.findByCounterpartyName` + query Room indexada
- **F8-02:** KDoc en `Envelope.spentAmount` (calculado en `EnvelopeBudgetState`)
- **F0-04 / F10-05:** `MainActivitySmokeTest` + `DuplicateResolutionDialogTest` (androidTest)
- Hallazgos cerrados: **F10-02…F10-05**, **F9-04**, **F9-05**, **F8-04**, **F7-06**, **F7-07**, **F8-02**, **F0-04**

### Cierre formal Fase 10

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** |
| **Revisión** | Gestor maestro — entrega IA + hotfixes F10b/F10c + cierre |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:home:testDebugUnitTest :app:lintDebug assembleDebug` PASS |
| **Tests Fase 10** | 24+ (domain) |
| **Tests Fase 10b** | +2 (domain) |
| **Tests Fase 10c** | +6 (domain) + 2 (feature/home) + 2 (app androidTest) + 1 (data androidTest) |
| **Room** | v3 (`dismissed_duplicate_pairs`) |
| **Hallazgos cerrados en fase** | F10-01…F10-05, F9-04, F9-05, F8-04, F7-06, F7-07, F8-02, F0-03, F0-04 |
| **Próxima fase** | Fase 11 — Compromisos / metas |

### Entregables Fase 11

- Modelos: `SavingsGoalProgress`, `FinancialPlanValidationResult`, `CommitmentSummary`, `CommitmentsInsights`, `CommitmentStatusKeys`, `FinancialPlanValidationKeys`
- UseCases: `CalculateSavingsGoalProgressUseCase`, `ValidateFinancialPlanUseCase`, `ObserveCommitmentSummariesUseCase`, `ObserveCommitmentsInsightsUseCase`
- **Room v4** (`kipu.db`): `CommitmentEntity`, `FinancialPlanEntity`, DAOs, mappers + tests
- `RoomCommitmentRepository`, `RoomFinancialPlanRepository`; seed demo (meta S/500, deuda S/80, plan S/3000)
- `CommitmentsScreen` — lista con `KipuCard`, barra de progreso en metas, alerta plan negativo
- `CommitmentSummaryTranslator` — claves dominio → español
- `RepositoryModule` — bind Commitment/FinancialPlan → Room
- Tests JVM: progreso metas, validación plan, observer, mappers
- Pendiente documentado: vincular movimientos a metas (`commitmentId` en `Movement`)

### Entregables Fase 11b (hotfix ECC)

- **F11-01:** `CommitmentsScreen` muestra alerta de plan inválido aunque no haya compromisos en lista
- **F11-02:** `TDD_CHECKLIST` — ítem save plan inválido desmarcado y diferido a CRUD (Fase 12+); KDoc en `FinancialPlanRepository`
- Hallazgos cerrados: **F11-01**, **F11-02**

### Cierre formal Fase 11

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** |
| **Revisión** | Gestor maestro — entrega IA + hotfix F11b + cierre formal |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug` PASS |
| **Tests Fase 11** | 11 (domain UseCases) + 4 (data mappers) |
| **Tests Fase 11b** | 0 (fix UI + docs) |
| **Room** | v4 (`commitments`, `financial_plans`) |
| **Hallazgos cerrados en fase** | F11-01, F11-02 |
| **Hallazgos abiertos Fase 11** | F11-03, F11-04, F11-05 (ver tabla) |
| **Próxima fase** | **Fase 12 — Notificaciones** |

### Entregables Fase 12

- **Domain:** `MonitoredPaymentApps` (allowlist `com.bcp.yape`, `pe.interbank.plin`)
- Parsers ingreso JVM: `YapeIncomeNotificationParser`, `PlinIncomeNotificationParser`, `NotificationParserRouter`, `NotificationIncomeFieldExtractor`
- `NotificationParseResult`, `RegisterNotificationIncomeResult`
- UseCases: `ParseNotificationTextUseCase`, `RegisterNotificationIncomeUseCase`, `ObservePendingNotificationMovementsUseCase`, `ConfirmPendingNotificationMovementUseCase`, `DismissPendingNotificationMovementUseCase`
- Interfaces: `NotificationAccessChecker`, `NotificationAccessSettingsNavigator`
- **Data:** `KipuNotificationListenerService`, `NotificationListenerCoordinator`, `NotificationListenerBridge`, `AndroidNotificationAccessChecker`
- Hilt: `NotificationModule`, `CoroutineModule` (`@ApplicationScope`)
- **Manifest:** service con `BIND_NOTIFICATION_LISTENER_SERVICE`
- **Profile:** toggle + explicación español + diálogo previo a ajustes + estado acceso concedido/pendiente
- **Movements:** sección “Ingresos por confirmar” con Confirmar/Descartar + diálogo duplicados al confirmar
- `NotificationMovementTranslator` (feature/movements)
- Fixtures: `core/domain/src/test/resources/notifications/` (5 archivos)
- Tests JVM: 18 domain (parsers + use cases) + 3 data (`NotificationListenerBridgeTest`)
- **Sin Room v5** — reutiliza `movements` con `PENDING_CONFIRMATION` + `source = NOTIFICATION`
- **Sin persistir** texto crudo de notificación; **sin logs** de título/cuerpo/montos/nombres

### Entregables Fase 12b (hotfix ECC)

- **F12-02:** `RegisterNotificationIncomeUseCase` — dedup solo si existe `PENDING_CONFIRMATION` con mismo draft id; nuevo id con sufijo temporal tras confirmación previa
- **F12-01 / F12-03:** sincronización docs (`PROJECT_STATE`, `AGENTS.md`, conteo tests 18+3+1 F12b)
- **F12-04:** `:app:lintDebug` ejecutado (ver cierre formal)
- Hallazgos cerrados en hotfix: **F12-01**, **F12-02**, **F12-03**, **F12-04**

### Cierre formal Fase 12

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** |
| **Revisión** | Gestor maestro — entrega IA + hotfix F12b + cierre formal |
| **Riesgo** | **Alto** (permiso listener + contenido notificaciones) |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:lintDebug assembleDebug` PASS |
| **Tests Fase 12** | 18 (domain) + 3 (data bridge) |
| **Tests Fase 12b** | +1 (`RegisterNotificationIncomeUseCaseTest` re-ingreso tras confirmar) |
| **Room** | v4 sin cambios de schema |
| **Hallazgos cerrados en fase** | F12-01, F12-02, F12-03, F12-04 |
| **Hallazgos abiertos Fase 12** | F12-05, F12-06, F12-07 (ver tabla) |
| **Comprobación manual pendiente** | F12-05: `packageName` reales Yape/Plin + flujo E2E listener en dispositivo |
| **Próxima fase** | Fase 15 — Comprobantes UI |

---

## Entregables Fase 13 — Exportar / eliminar datos

Ver sección completa más abajo (post-Fase 14c) para detalle de archivos, wipe y tests.

---

## Entregables Fase 14 — Onboarding + plan financiero

### Alcance MVP (qué SÍ incluye)

- **Un solo paso de onboarding:** `PlanIntroStep` (welcome, permisos, tutorial Yape **eliminados** a petición de producto).
- **Dos acciones en intro:**
  - **"Comenzar con mi plan"** → marca flag en `MainViewModel` + completa onboarding → abre wizard plan.
  - **"Configurar plan después"** → solo completa onboarding (sin wizard).
- **Módulo `:feature:plan`** — wizard Compose de 6 pasos:
  1. `income` — ingreso mensual estimado (texto → `MoneyInputParser`).
  2. `expenses` — gastos fijos mensuales.
  3. `envelopes` — asignación por sobres.
  4. `ant` — límite y categorías de gastos hormiga.
  5. `goal` — meta opcional y deuda social.
  6. `summary` — validación plan + disponible diario + resumen antes de guardar.
- **Persistencia:** `SaveFinancialPlanUseCase` actualiza fila Room `financial_plans` (id canónico `financial-plan-primary`).
- **Sobres en wizard:** edición de límites en paso Sobres; re-edición vía chips en tab Sobres (Fase 24).

### Archivos clave

| Archivo | Rol |
|---------|-----|
| `feature/onboarding/ui/PlanIntroStep.kt` | UI intro; **sin** texto "datos demo" |
| `feature/onboarding/OnboardingScreen.kt` | Recibe `onStartPlan: () -> Unit` desde `MainActivity` |
| `feature/onboarding/presentation/OnboardingViewModel.kt` | `CompleteOnboardingUseCase`; persiste `onboardingCompleted` + `pendingPlanWizard` |
| `feature/plan/PlanWizardScreen.kt` | UI wizard (6 pasos) |
| `feature/plan/presentation/PlanWizardViewModel.kt` | Estado, navegación entre pasos, guardado, precarga (`PlanWizardStateLoader`) |
| `feature/plan/presentation/PlanWizardStep.kt` | Enum + mapeo rutas `income`/`expenses`/…/`goal`/`summary` |
| `app/.../MainActivity.kt` | Orquesta onboarding vs app; `LaunchedEffect` abre wizard si `pendingPlanWizard` |
| `app/.../MainViewModel.kt` | Observa `pendingPlanWizard` desde DataStore; `clearPendingPlanWizard()` |
| `app/.../KipuPlanRoutes.kt` | Rutas tipadas plan + movimientos por categoría |
| `core/domain/plan/FinancialPlanIds.kt` | `PRIMARY = "financial-plan-primary"` (dominio) |
| `core/domain/usecase/SaveFinancialPlanUseCase.kt` | Guarda plan + corre `ValidateFinancialPlanUseCase` |
| `core/domain/util/MoneyInputParser.kt` | Parse PEN desde campos de texto UI |

### IDs y seed demo (no confundir capas)

| Concepto | Id / valor | Dónde |
|----------|------------|-------|
| Plan primario | `financial-plan-primary` | `FinancialPlanIds.PRIMARY` (domain); seed usa mismo id (Fase 20 unificó `Default*Ids`) |
| Ingreso seed | S/ 3 000.00 | `DefaultFinancialPlanSeed` (`300_000` centavos) |
| Gastos fijos seed | S/ 1 800.00 | `DefaultFinancialPlanSeed` (`180_000` centavos) |
| Sobres seed | Comida S/150, Transporte S/80, Servicios S/60 **semanal** | `DefaultEnvelopeSeed` |

### Flujo onboarding → wizard (secuencia exacta)

```
MainActivity: onboardingCompleted == false
  → OnboardingScreen()
  → Usuario "Comenzar con mi plan"
      → OnboardingViewModel.onFinishOnboarding(pendingPlanWizard = true)
      → CompleteOnboardingUseCase → DataStore: onboardingCompleted=true, pendingPlanWizard=true
MainActivity: onboardingCompleted == true
  → rememberNavController()
  → LaunchedEffect(pendingPlanWizard) { navigate(plan/income); clearPendingPlanWizard() }
  → KipuNavGraph (bottom bar oculto en rutas secundarias)
```

**Persistencia wizard post-onboarding:** `UserPreferences.pendingPlanWizard` en DataStore (Fase 19). `MainActivity` consume el flag y navega a `plan/income`; se limpia tras abrir el wizard (`PendingPlanWizardInstrumentedTest`).

### Onboarding y `CompleteOnboardingUseCase`

- `CompleteOnboardingUseCase` cableado en `OnboardingViewModel` (Fase 20). El ViewModel delega persistencia de prefs vía el UseCase.

### Tests Fase 14 (domain)

- `SaveFinancialPlanUseCaseTest` — guardado válido + fallo id vacío.
- Total domain tests: **39** archivos JVM (incluye F14 + F14b).

---

## Entregables Fase 14b — Pulido UI Movimientos / Sobres

### Movimientos

| Capacidad | Implementación |
|-----------|----------------|
| Filtro canal (Todos/Yape/Plin/Bancos/Efectivo) | `MovementChannelFilter` + `KipuFilterChipRow` |
| Filtro por categoría / sobre | Ruta `movements/category/{categoryId}`; banner "Sobre: X" con limpiar |
| Tarjetas estilo HTML | `MovementHtmlCard` — badges fuente/estado, confianza heurística por `MovementSource` |
| Cambiar categoría | `CategoryChangeDialog` + `UpdateMovementCategoryUseCase` |
| Combine >5 flows | **`MovementsData`** internal + combine anidado en `MovementsViewModel` — **no** volver a 6-tuple plano |

**Archivos:** `MovementsScreen.kt`, `MovementsViewModel.kt`, `MovementsUiState.kt`, `MovementsData.kt`, `MovementHtmlCard.kt`, `CategoryChangeDialog.kt`, `MovementPresentation.kt` (`matchesCategoryFilter`, `formatMovementDateTime`).

**Regla de dominio:** campo de fecha en `Movement` es **`recordedAt`** (tipo `Instant`). **No existe `occurredAt`** — usar siempre `recordedAt` en UI y UseCases.

### Sobres

| Capacidad | Implementación |
|-----------|----------------|
| Tarjetas detalle HTML | Stats restante/usado/días + barra progreso |
| Últimos movimientos | `GetEnvelopeRecentMovementsUseCase` — hasta 3 EXPENSE CONFIRMED de la semana por `categoryId` |
| Ajustar límite semanal | `EnvelopeAdjustLimitDialog` + `UpdateEnvelopeWeeklyLimitUseCase` |
| Ingresos / Gastos / Meta | Nav a `plan/income`, `plan/expenses`, tab `commitments` |
| Ver movimientos | Nav a `movements/category/{categoryId}` |

**Archivos:** `EnvelopesScreen.kt`, `EnvelopesViewModel.kt`, `EnvelopesUiState.kt` (`EnvelopeBudgetUiModel`), `EnvelopeAdjustLimitDialog.kt`, `EnvelopePresentation.kt` (visualStyle, percentLabel, etc.).

### Design system añadido (F14b)

- `KipuFilterChip`, `KipuFilterChipRow` — filtros horizontales HTML.
- `KipuCompactBadge` — tags pequeños en tarjetas movimiento.
- `KipuBadgeTone.Purple` — tono extra para badges.
- `KipuScreenHeader(centered: Boolean)` — headers centrados en empty states.

### Tests Fase 14b (domain)

- `UpdateMovementCategoryUseCaseTest` — cambio ok + categoría inexistente.
- `GetEnvelopeRecentMovementsUseCaseTest` — filtro categoría/semana/estado + orden.

---

## Entregables Fase 14c — Optimización APK

| Cambio | Archivo | Efecto |
|--------|---------|--------|
| Solo ABI `arm64-v8a` | `app/build.gradle.kts` `ndk.abiFilters` | APK debug ~27 MB; **emuladores x86/x86_64 no soportados** — usar imagen arm64 o dispositivo físico |
| R8 + shrink resources | `app/build.gradle.kts` `release` | APK release ~14 MB |
| Reglas ProGuard | `app/proguard-rules.pro` | Hilt, Room, ML Kit, DataStore, entities Room |

**No revertir** `abiFilters` sin acordar impacto en QA emulador.

### Cierre Fase 14 (ECC LISTO — formalizado Fase 26)

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** (revisión retrospectiva jun 2026; hallazgo F14-06 cerrado) |
| **Evidencia original** | `./gradlew :core:domain:test assembleDebug` PASS; wizard 6 pasos; Room v4 sin cambios |
| **Evidencia Fase 26** | `./gradlew :app:lintDebug assembleRelease` PASS; política privacidad accesible desde Perfil |
| **Tests F14** | +1 `SaveFinancialPlanUseCaseTest`; F14b +2 UseCase tests |
| **Módulos nuevos** | `:feature:plan` |

---

## Entregables Fase 15 — Comprobantes UI

### Alcance MVP

- **Share intent** `ACTION_SEND` `image/*` → `MainActivity` (`singleTop`) → pantalla revisión.
- **Hub comprobantes** — `ReceiptsScreen`: elegir imagen desde galería (Photo Picker, sin permiso cámara).
- **Revisión OCR** — preview imagen en memoria, campos editables (monto, destinatario, operación, mensaje, categoría).
- **Confirmación humana** — `ConfirmReceiptMovementUseCase` + detección duplicados antes de persistir.
- **Entradas UI:** Inicio (tarjeta), Movimientos vacío, hub comprobantes.
- **Imágenes temporales y locales** — las compartidas o elegidas se leen desde su origen para preview/OCR; una foto tomada desde Kipu usa `cache/receipts` y se elimina al cancelar/cerrar o liberar su revisión. No existe purga al inicio: una captura interrumpida puede permanecer hasta la limpieza de Android o el borrado local, para no perder una captura pendiente. Sin logs de URI ni texto OCR y sin subida a nube.
- **Fallo recuperable** — un error al abrir o procesar el comprobante muestra Reintentar o Back; no persiste nada automáticamente.

### Archivos clave

| Capa | Archivos |
|------|----------|
| Domain | `ReceiptImageLoader.kt`, `ConfirmReceiptMovementUseCase.kt` |
| Data | `AndroidReceiptImageLoader.kt`, `ProcessReceiptFromUriUseCase.kt`, `OcrModule` bind loader |
| Feature | `:feature:receipts` — `ReceiptsScreen`, `ReceiptReviewScreen`, `ReceiptReviewViewModel`, `ReceiptDuplicateDialog` |
| App | `MainActivity` share intent, `MainViewModel.pendingReceiptUri`, `KipuNavGraph` rutas `receipts/*` |
| Manifest | `intent-filter` SEND + `launchMode=singleTop` |

### Cierre Fase 15 (funcional — ECC formal pendiente)

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** (revisión gestor pendiente) |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug` PASS |
| **Tests nuevos** | +1 `ConfirmReceiptMovementUseCaseTest`, +2 `ProcessReceiptFromUriUseCaseTest` |
| **Módulos nuevos** | `:feature:receipts` |
| **Próxima fase canónica** | **Fase 16 — Juntas + pulido** |
| **Hallazgos abiertos** | — (F15-01…F15-03 cerrados en F15b) |

### Entregables Fase 15b — Cierre riesgos F15

| ID | Resolución |
|----|------------|
| **F15-01** | Botón **Tomar foto** en `ReceiptsScreen` (`TakePicture` + `ReceiptCaptureUriFactory` + `FileProvider` cache `receipts/`) |
| **F15-02** | Tests instrumentados: `ReceiptShareIntentParserTest` y `ReceiptDuplicateDialogTest`; el smoke de share con espera fija se retiró el 13 ago por no comprobar el flujo de forma determinista |
| **F15-03** | `ReceiptDuplicateDialog` alineado con movimientos: Fusionar / No es duplicado / Cancelar; MERGE → `DuplicateMerged` sin persistir |

---

## Entregables Fase 16 — Juntas + pulido

### Alcance MVP

- **CRUD juntas local** — crear (nombre + participantes), listar, eliminar; sin reparto de gastos ni Firebase.
- **Room v5** — tabla `gatherings`; participantes pipe-separated en entity.
- **Export JSON v3** — `UserDataSnapshot` incluye juntas, gastos de junta y configuración completa; CSV sigue solo movimientos.
- **UI** — `:feature:juntas`: lista, empty state, diálogo crear; acceso desde **Perfil → Ver juntas** (sin 6.º tab).
- **Pulido** — fix lint `ReceiptShareIntentParser` (API 26+); `:app:lintDebug` + `assembleRelease` PASS.

### Archivos clave

| Capa | Archivos |
|------|----------|
| Domain | `GatheringParticipantParser.kt`, `ObserveGatheringsUseCase`, `SaveGatheringUseCase`, `DeleteGatheringUseCase`; export v2 en `UserDataSnapshot` / `UserDataJsonSerializer` |
| Data | `GatheringEntity`, `GatheringDao`, `GatheringMapper`, `RoomGatheringRepository`; `KipuDatabase` v5; wipe incluye `gatheringDao().deleteAll()` |
| Feature | `:feature:juntas` — `GatheringsScreen`, `GatheringsViewModel`, `CreateGatheringDialog`, `GatheringRoutes` |
| App | `KipuNavGraph` ruta `gatherings`; `ProfileScreen.onNavigateToGatherings`; `RepositoryModule` bind real repo |

### Cierre Fase 16

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** (revisión gestor pendiente) |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug :app:lintDebug assembleRelease` PASS |
| **Tests nuevos** | +6 domain (`GatheringTest`, `GatheringParticipantParserTest`, `SaveGatheringUseCaseTest`); +2 data (`GatheringMapperTest`); export tests actualizados v2 |
| **Módulos nuevos** | `:feature:juntas` |
| **Próxima fase canónica** | Internal testing Play Console |
| **Hallazgos abiertos** | — (F16-01…F16-03 cerrados en Fase 17) |

---

## Entregables Fase 17 — Cierre riesgos F16 (ex 16b)

| ID | Resolución |
|----|------------|
| **F16-01** | **Migraciones Room reales** v1→v6 (`KipuDatabaseMigrations`); eliminado `fallbackToDestructiveMigration` |
| **F16-02** | **Editar junta** — `UpdateGatheringUseCase` + diálogo editar en `GatheringsScreen` |
| **F16-03** | **Reparto igualitario** — `gathering_expenses` (Room v6), `RecordGatheringExpenseUseCase`, `CalculateGatheringEqualSplitUseCase`, totales en UI |
| **F16-04** | **KSP race** — `dependsOn(:core:domain:jar)` en módulos KSP (root + `:feature:juntas`) |
| **F8-01** | Cerrado vía F16-01 |
| **F13-02** | DataStore `kipu_preferences.preferences_pb` excluido de backup y device transfer |

### Archivos clave Fase 17

| Capa | Archivos |
|------|----------|
| Data | `KipuDatabaseMigrations.kt`, `GatheringExpenseEntity/Dao`, `RoomGatheringExpenseRepository`, DB v6 |
| Domain | `GatheringExpense`, `GatheringSummary`, `UpdateGatheringUseCase`, `RecordGatheringExpenseUseCase`, `CalculateGatheringEqualSplitUseCase`, `ObserveGatheringSummariesUseCase` |
| Feature | `GatheringsScreen` (editar + registrar gasto + split), `GatheringsViewModel` |
| Tests | `CalculateGatheringEqualSplitUseCaseTest`, `UpdateGatheringUseCaseTest`, `GatheringExpenseMapperTest`, `KipuDatabaseMigrationInstrumentedTest` |
| Infra | `build.gradle.kts` (KSP dep), `backup_rules.xml`, `data_extraction_rules.xml` |

---

## Entregables Fase 18 — Cierre riesgos restantes (ex 16c)

| ID | Resolución |
|----|------------|
| **F13-01** | **`RoomUserDataWipeInstrumentedTest`** — wipe transaccional Room + re-seed baseline + prefs |
| **F13-03** | CSV documentado en UI Perfil: "JSON incluye juntas; CSV solo movimientos" |
| **F14-04** | **`MovementDisplayLabels`** en domain; `:feature:envelopes` ya no depende de `:feature:movements` |
| **F16-05** | Vincular movimientos Yape/Plin a juntas — `LinkMovementToGatheringUseCase`, UI "Vincular movimiento" |
| **F16-06** | Liquidación por participante — `CalculateGatheringSettlementUseCase`, `ParticipantSettlement` en UI |
| **F16-07** | Room v7 — `paidByParticipant`, `movementId` (FK único) en `gathering_expenses`; `MIGRATION_6_7` |

### Archivos clave Fase 18

| Capa | Archivos |
|------|----------|
| Domain | `GatheringsDashboard`, `ParticipantSettlement`, `MovementDisplayLabels`, `CalculateGatheringSettlementUseCase`, `LinkMovementToGatheringUseCase`, `GatheringParticipantValidator`; `ObserveGatheringSummariesUseCase` incluye movimientos sin vincular |
| Data | `GatheringExpenseEntity` (+paidBy, movementId), `MIGRATION_6_7`, DB v7 |
| Feature | `GatheringsScreen` (liquidación, vincular movimiento, picker pagador), `GatheringsViewModel` |
| Tests | `CalculateGatheringSettlementUseCaseTest`, `GatheringExpenseMapperTest`, `RoomUserDataWipeInstrumentedTest`, `KipuDatabaseMigrationInstrumentedTest` v6→v7 |

### Cierre Fase 18

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest assembleDebug :app:lintDebug` PASS |
| **Hallazgos cerrados** | F13-01, F13-03, F14-04, F16-05, F16-06, F16-07 |

---

## Entregables Fase 19 — Riesgos críticos (ex Fase 21)

| ID | Resolución |
|----|------------|
| **F0-02** | `backup_rules.xml` + `data_extraction_rules.xml` excluyen DB Room, DataStore, cache exports y comprobantes |
| **F21-01** | Wipe borra imágenes de comprobantes — `UserDataExportFileRepository.clearLocalFileCaches()` en `WipeAllUserDataUseCase` |
| **F11-02** | `SaveFinancialPlanUseCase` rechaza plan `Invalid` (`InvalidFinancialPlanException`) |
| **F14-01** | Flag `pendingPlanWizard` persistido en DataStore; `PendingPlanWizardInstrumentedTest` |
| **F13-04** | `FileProvider` no exportado; rutas acotadas en `file_paths.xml` |
| **F12-05** | Export JSON snapshot v3 incluye juntas/gastos y configuración persistida completa; tests serializer actualizados |

### Cierre Fase 19

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Hallazgos cerrados** | F0-02, F21-01, F11-02, F14-01, F13-04, F12-05 (parcial — E2E hardware opcional) |

---

## Entregables Fase 20 — Riesgos residuales (ex Fase 21b)

| ID | Resolución |
|----|------------|
| **F11-03** | `ObserveCommitmentSummariesUseCaseTest` |
| **F11-04** | Seed/migraciones usan `CommitmentIds` / `FinancialPlanIds` (sin duplicar `Default*Ids`) |
| **F11-05** | `KipuDatabaseMigrationInstrumentedTest` v4→v9 |
| **F12-07** | `NotificationListenerCoordinatorTest` (pref ON/OFF) |
| **F14-03** | `CompleteOnboardingUseCase` cableado en `OnboardingViewModel` |
| **F14-05** | `PlanWizardE2ETest` (seis pasos + guardar/reabrir/editar round-trip) |

### Cierre Fase 20

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Hallazgos cerrados** | F11-03, F11-04, F11-05, F12-07, F14-03, F14-05 |

---

## Entregables Fase 21 — Armonización UX/UI (ex 21c)

### Design system (`core/designsystem`)

| Componente | Rol |
|------------|-----|
| `KipuLayout` | Tokens padding horizontal 24 dp, espaciado listas/secciones |
| `KipuSectionHeader` | Títulos de subsección (`horizontalPadding` configurable) |
| `KipuLoadingIndicator` | Spinner con color `KipuPrimary` |
| `KipuDialogConfirmButton` / `KipuDialogDismissButton` | Botones consistentes en diálogos |
| `KipuTextLink` | Acciones terciarias inline |

### Pantallas armonizadas

- Tabs: Home, Movimientos, Sobres, Compromisos, Perfil
- Secundarias: wizard plan (`PlanWizardScreen`, steps), Juntas, Comprobantes (hub + revisión + duplicados)
- Diálogos: movimientos, duplicados, perfil
- Onboarding: `PlanIntroStep` con `KipuScreenHeader`, `KipuLayout`, cards al design system

### Cierre Fase 21

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew assembleDebug` PASS |
| **Hallazgos cerrados** | — (pulido visual; sin hallazgos formales nuevos) |

---

## Entregables Fase 22 — CRUD sobres + compromisos

### Domain — UseCases nuevos

| UseCase | Archivo | Comportamiento |
|---------|---------|----------------|
| `CreateEnvelopeUseCase` | `core/domain/.../CreateEnvelopeUseCase.kt` | Crea sobre (`envelope-{epochMillis}`), valida categoría única, enlaza a `FinancialPlanIds.PRIMARY` |
| `DeleteEnvelopeUseCase` | `core/domain/.../DeleteEnvelopeUseCase.kt` | Elimina sobre y lo quita de `envelopeIds` del plan; **no** borra movimientos |
| `SaveCommitmentUseCase` | `core/domain/.../SaveCommitmentUseCase.kt` | Crear/editar meta, deuda social o pago pendiente |
| `DeleteCommitmentUseCase` | `core/domain/.../DeleteCommitmentUseCase.kt` | Elimina compromiso por id |

### Tests domain

- `CreateEnvelopeUseCaseTest` — creación ok, categoría duplicada, nombre vacío
- `SaveCommitmentUseCaseTest` — crear meta, editar título, validación título vacío

### Feature — UI

| Módulo | Archivos | Capacidad |
|--------|----------|-----------|
| `:feature:envelopes` | `EnvelopeCreateDialog.kt`, `EnvelopesViewModel.kt`, `EnvelopesScreen.kt` | Botón **Nuevo sobre**; crear con categoría/límite; **Eliminar sobre** con confirmación |
| `:feature:commitments` | `CommitmentFormDialog.kt`, `CommitmentsViewModel.kt`, `CommitmentsScreen.kt` | **Nuevo compromiso**; editar/eliminar en cards (meta, deuda, pago pendiente) |

### Supuestos de diseño

- Un sobre por categoría (no duplicar `categoryId`)
- IDs nuevos: `envelope-{timestamp}`, `commitment-{timestamp}`
- CRUD compromisos valida estructura vía `Commitment.validate()`; sin gate de plan negativo en save

### Cierre Fase 22

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew :core:domain:test :app:assembleDebug` PASS |
| **Tests nuevos** | +2 domain (`CreateEnvelopeUseCaseTest`, `SaveCommitmentUseCaseTest`) |
| **Hallazgos cerrados** | **F8-03**, **F11-06** |
| **Próxima fase sugerida** | Internal testing Play Console |

---

## Entregables Fase 23 — Metas ↔ movimientos (F11-07)

### Domain

| Pieza | Rol |
|-------|-----|
| `Movement.commitmentId` | Vínculo opcional ingreso → meta de ahorro |
| `CommitmentLinkedIncomeCalculator` | Suma ingresos CONFIRMED vinculados a una meta |
| `LinkMovementToCommitmentUseCase` | Vincular/desvincular ingreso confirmado a meta SAVINGS_GOAL |
| `ObserveSavingsGoalCommitmentsUseCase` | Lista metas para picker en Movimientos |
| `CalculateSavingsGoalProgressUseCase` | Progreso = `currentAmount` manual + ingresos vinculados |
| `ObserveCommitmentSummariesUseCase` | Combina compromisos + movimientos para progreso reactivo |

### Data

- **Room v10** — columna `movements.commitmentId` (FK → `commitments`, `ON DELETE SET NULL`)
- Migración `MIGRATION_9_10`
- Export JSON incluye `commitmentId`

### UI (`feature/movements`)

- `GoalLinkDialog` — elegir meta o quitar vínculo
- `MovementHtmlCard` — "Vincular a meta" / "Cambiar meta" solo en **ingresos**
- Progreso en Compromisos se actualiza al vincular ingresos y su tarjeta expone título, estado y avance textual para accesibilidad, además de la barra visual

### Reglas de negocio

- Solo movimientos **INCOME + CONFIRMED** pueden vincularse
- Solo metas **SAVINGS_GOAL** reciben vínculos
- Progreso = baseline manual (`currentAmount`) + suma ingresos vinculados

### Cierre Fase 23

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew :core:domain:test :core:data:testDebugUnitTest :app:assembleDebug` PASS |
| **Tests nuevos** | +3 domain (`LinkMovementToCommitmentUseCaseTest`, `CommitmentLinkedIncomeCalculatorTest`, ampliación summaries/progress); +1 data mapper; +1 androidTest migración v9→v10 |
| **Hallazgos cerrados** | **F11-07** |
| **Room** | v10 |

---

## Entregables Fase 24 — Cierre F14-02 (wizard edita sobres/metas)

### Contexto

El wizard ya incluía **6 pasos** (ingresos, gastos fijos, sobres, gastos hormiga, meta, resumen) con persistencia de límites y meta. F14-02 cerró la **re-edición** y **accesos directos**.

### Cambios

| Pieza | Rol |
|-------|-----|
| `PlanWizardStateLoader` | Precarga ingresos, gastos fijos y meta desde Room al reabrir wizard |
| `PlanWizardViewModel` | Usa loader + `SaveCommitmentUseCase`; flag `isEditingExistingPlan` |
| `EnvelopesScreen` | Chips **Ingresos / Gastos / Sobres / Meta** → rutas wizard |
| `KipuPlanRoutes` | Constantes `STEP_ANT`, `STEP_GOAL` |
| `PlanWizardScreen` | Botón **Guardar mi plan** vs **Crear mi plan** al re-editar |

### Cierre Fase 24

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew :core:domain:test :app:assembleDebug` PASS |
| **Tests nuevos** | +5 domain (`PlanWizardStateLoaderTest`) |
| **Hallazgos cerrados** | **F14-02** |
| **Próxima fase sugerida** | Internal testing Play Console |

---

## Entregables Fase 25 — MERGE duplicados notificación (F12-06)

### Contexto

Al confirmar un ingreso detectado por notificación, si ya existe un movimiento confirmado similar, la app mostraba solo **Guardar igual** / **Cancelar**. Movimientos y comprobantes ya tenían **Fusionar / No es duplicado / Cancelar**.

### Domain

| Pieza | Comportamiento |
|-------|----------------|
| `ConfirmPendingNotificationMovementUseCase` | `MERGE` → elimina el movimiento `PENDING_CONFIRMATION` (notificación duplicada); mantiene el confirmado existente |
| `SAVE_AS_NEW` | Promueve pending a `CONFIRMED` (sin cambio) |
| `CANCEL` | Deja pending sin cambios |

### UI (`feature/movements`)

- `PendingNotificationDuplicateDialog` — paridad con `DuplicateResolutionDialog`: **Fusionar**, **No es duplicado**, **Cancelar**
- Resumen compara ingreso por notificación vs ingreso confirmado

### Tests

- `ConfirmPendingNotificationMovementUseCaseTest` — +1 (`discards pending notification when user chooses merge`)
- `PendingNotificationDuplicateDialogTest` — acciones en español (instrumentado)

### Cierre Fase 25

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew :core:domain:test :app:assembleDebug` PASS |
| **Hallazgos cerrados** | **F12-06** |
| **Próxima fase sugerida** | Internal testing Play Console |

---

## Entregables Fase 26 — QA Play Store + cierre ECC F14

### Documentación release

| Archivo | Contenido |
|---------|-----------|
| `docs/release/PRIVACY_POLICY.md` | Política de privacidad (español Perú) — publicar URL en Play Console |
| `docs/release/PLAY_STORE.md` | Store listing, data safety, `bundleRelease`, checklist pre-lanzamiento |
| `docs/ai/E2E_QA_CHECKLIST.md` | Criterios release + test privacidad |

### UI

| Pieza | Rol |
|-------|-----|
| `PrivacyPolicyScreen` | Política in-app (paridad con markdown) |
| `ProfileRoutes.PRIVACY` | Ruta secundaria desde Perfil |
| `ProfileScreen` | Enlace **Política de privacidad** |

### Hallazgos cerrados

| ID | Resolución |
|----|------------|
| **F14-06** | ECC Fase 14 → **LISTO** retrospectivo jun 2026 |
| **F14-07** | Heurística UI en `MovementPresentation`; `SuggestionConfidence` en parsers — aceptado MVP |

### Tests

- `PrivacyPolicyScreenTest` — instrumentado

### Cierre Fase 26

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** (pre-publicación) |
| **Evidencia** | `./gradlew :core:domain:test :app:lintDebug assembleRelease` PASS |
| **Pendiente humano** | AAB firmado; URL pública privacidad; E2E manual hardware |
| **Próxima fase sugerida** | Subir AAB a Play Console — `INTERNAL_TESTING.md` |

---

## Entregables Fase 27 — Internal testing (release pipeline)

### Infra release

| Pieza | Rol |
|-------|-----|
| `keystore.properties.example` | Plantilla firma release (gitignored: `keystore.properties`, `*.jks`) |
| `app/build.gradle.kts` | `signingConfigs.release` si existe keystore; release falla temprano si falta; `versionName` **1.0.0** |
| `bundleRelease` | AAB para Play Console (`app/build/outputs/bundle/release/`) |

### Documentación

| Archivo | Contenido |
|---------|-----------|
| `docs/release/INTERNAL_TESTING.md` | Keystore → AAB → Play Console internal track → QA |
| `docs/privacy/index.html` | Política pública para GitHub Pages (`/docs` → `/privacy/`) |

### Tests

- `KipuNavigationE2ETest.profileNavigatesToPrivacyPolicy` — Perfil → Política de privacidad

### Cierre Fase 27

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **LISTO** (repo); **pendiente humano** Play Console |
| **Evidencia actual** | Unit tests, lint y debug build PASS; `bundleRelease` requiere firma real y falla temprano si falta `keystore.properties` |
| **Pendiente humano** | Crear keystore; activar GitHub Pages; subir AAB; invitar testers; E2E manual N1–E4 |
| **Próxima fase sugerida** | Closed testing / producción tras feedback internal |

---

## Entregables Fase 13 — Exportar / eliminar datos (detalle)

### Alcance MVP

- **Exportar JSON completo** — snapshot de movimientos, categorías, sobres, compromisos, planes, claves duplicados descartados y preferencias (sin subir a nube).
- **Exportar CSV** — solo movimientos (columnas tabulares para Excel/Sheets).
- **Compartir archivo** — intent `ACTION_SEND` vía `FileProvider` (cache local `exports/`).
- **Eliminar todos los datos** — confirmación **doble** en Perfil; wipe transaccional Room + `UserPreferencesRepository.clear()` + re-seed demo baseline.
- **Post-wipe:** preferencias vacías → `onboardingCompleted=false` → `MainActivity` muestra onboarding de nuevo.

### Archivos clave

| Capa | Archivos |
|------|----------|
| Domain | `UserDataSnapshot.kt`, `ExportFormat.kt`, `UserDataJsonSerializer.kt`, `UserDataCsvSerializer.kt`, `BuildUserDataSnapshotUseCase.kt`, `ExportUserDataUseCase.kt`, `WipeAllUserDataUseCase.kt`, `UserDataWipeRepository.kt`, `UserDataExportFileRepository.kt` |
| Data | `RoomUserDataWipeRepository.kt`, `AndroidUserDataExportFileRepository.kt`, `KipuDatabaseSeeder.kt`, `deleteAll()` en 6 DAOs |
| App | `RepositoryModule` binds, `file_paths.xml`, `FileProvider` en Manifest |
| UI | `ProfileScreen.kt`, `ProfileViewModel.kt`, `ProfileUiState.kt`, `ProfileEvent.kt` |

### Cierre Fase 13

| Campo | Valor |
|-------|-------|
| **Veredicto ECC** | **Funcional MVP** |
| **Evidencia** | `./gradlew clean :core:domain:test :core:data:testDebugUnitTest assembleDebug` PASS |
| **Tests nuevos** | +3 domain (`UserDataJsonSerializerTest`, `UserDataCsvSerializerTest`, `WipeAllUserDataUseCaseTest`) |
| **Próxima fase** | **Fase 15 — Comprobantes UI** |

---

## Navegación extendida (post-Fase 14)

### Tabs bottom bar (sin cambios de rutas base)

| Tab | Ruta exacta | Pantalla | Bottom bar |
|-----|-------------|----------|------------|
| Inicio | `home` | `HomeScreen` | ✅ Visible |
| Movimientos | `movements` | `MovementsScreen()` sin filtro | ✅ Visible |
| Sobres | `envelopes` | `EnvelopesScreen(...)` con callbacks nav | ✅ Visible |
| Compromisos | `commitments` | `CommitmentsScreen` | ✅ Visible |
| Perfil | `profile` | `ProfileScreen(onNavigateToGatherings=…)` | ✅ Visible |

### Rutas secundarias (fuera del bottom bar)

| Ruta plantilla | Argumentos | Pantalla | Cómo llegar | Back stack |
|----------------|------------|----------|-------------|------------|
| `plan/{startStep}` | `startStep`: `income` \| `expenses` \| `envelopes` \| `ant` \| `goal` \| `summary` | `PlanWizardScreen` | Onboarding; Sobres → chips Ingresos/Gastos/Sobres/Meta | `popBackStack()` al terminar |
| `privacy` | — | `PrivacyPolicyScreen` | Perfil → Política de privacidad | Back del sistema |
| `movements/category/{categoryId}` | `categoryId`: ej. `category-food` | `MovementsScreen(initialCategoryId=…)` | Sobres → Ver movimientos | Back del sistema |
| `gatherings` | — | `GatheringsScreen` | Perfil → Ver juntas | Back del sistema |
| `receipts` | — | `ReceiptsScreen` | Inicio / Movimientos vacío | Back del sistema |
| `receipts/review/{contentUri}` | `contentUri` (encoded) | `ReceiptReviewScreen` | Hub comprobantes; share intent | Back o Cancelar vuelve atrás; tras guardar/fusionar, el resultado permanece hasta pulsar `Listo` |

**Helpers:** `KipuPlanRoutes.wizard(startStep)`, `KipuPlanRoutes.movementsByCategory(categoryId)`, `GatheringRoutes.LIST`, `ReceiptRoutes.review(uri)`.

**Bottom bar:** `MainActivity` oculta barra cuando `currentRoute` **no** está en `KipuDestination.bottomBarDestinations`. Rutas con `{arg}` **no** matchean tab — barra oculta en wizard y movimientos filtrados.

**PlanWizardViewModel** lee `SavedStateHandle.get<String>("startStep")` — el nombre del argumento nav **debe** ser `startStep` (const `KipuPlanRoutes.START_STEP_ARG`).

### CategoryIds usados en navegación

```
category-food      → sobre Comida
category-transport → sobre Transporte
category-services  → sobre Servicios
category-other     → sin sobre seed dedicado
```

Definidos en `core/domain/.../CategoryIds.kt` — **única fuente canónica en domain**; seed Room importa estos ids.

---

## Trampas conocidas para futuras sesiones IA

### Build / Gradle

1. **KSP y domain jar stale:** tras añadir UseCases en `:core:domain`, si KSP dice "could not resolve XUseCase", ejecutar `./gradlew :core:domain:clean :core:domain:jar` antes de culpar imports.
2. **`combine` máximo 5 flows:** en `MovementsViewModel` y similares, agrupar en data class intermedia (`MovementsData`) — error de compilación silencioso hasta compileKotlin.
3. **Sandbox Gradle:** en algunos entornos CI/sandbox falla con "Could not determine a usable wildcard IP" — requiere permisos completos para `./gradlew`.
4. **Emulador x86:** con `arm64-v8a` only, build instala solo en arm64 — documentado en F14c.

### Arquitectura / módulos

5. **Dependencia cruzada feature↔feature:** `:feature:envelopes` **no** depende de `:feature:movements` desde Fase 18 (`MovementDisplayLabels` en domain). No reintroducir el acoplamiento.
6. **Domain puro:** ningún import Android/Room/Compose en `core/domain`. UseCases con `@Inject` están permitidos.
7. **ViewModels sin lógica financiera:** cálculos en UseCases; VMs combinan flows y delega.
8. **`CompleteOnboardingUseCase`:** cableado en `OnboardingViewModel` (Fase 20); persiste prefs vía UseCase, no acceso directo al repo en presentation.

### Modelos / campos

9. **`Movement.recordedAt`** — no usar `occurredAt` (no existe).
10. **IDs de plan:** usar `FinancialPlanIds.PRIMARY`; `DefaultFinancialPlanIds` fue eliminado en Fase 20.
11. **Confianza % en movimientos UI:** heurística por `MovementSource` (`MovementPresentation`); parsers/comprobantes usan `SuggestionConfidence` — sin campo en `Movement` (F14-07 aceptado MVP).
12. **Cambio categoría:** solo movimientos en lista principal (`MovementsViewModel` → confirmados del repo); no aplica a pending notification sin trabajo extra.

### UI Compose

13. **`MaterialTheme` en default parameter** de `@Composable` — prohibido (error compilación); pasar `Color?` y resolver dentro del composable (patrón `EnvelopeStatCell`).
14. **`material3` en feature modules:** si se añade `material.icons`, verificar que `libs.androidx.compose.material3` sigue en `build.gradle.kts` del feature.
15. **Onboarding eliminado:** no recrear Welcome, Cómo funciona, Permisos, Tutorial Yape salvo nueva petición explícita.

### Navegación

16. **No registrar rutas duplicadas** para `movements` — la ruta con categoría es **distinta** (`movements/category/...`).
17. **Meta en Sobres** navega a **Compromisos**, no a `plan/summary`.
18. **Flag wizard post-onboarding** se persiste en `UserPreferences.pendingPlanWizard`; los tests deben esperar su consumo antes de navegar o recrear la actividad.
19. **Share de comprobante:** enrutar `pendingReceiptUri` mediante un único `LaunchedEffect`; no volver a consumir ni navegar la misma URI desde otro efecto.

### Seguridad (AGENTS.md)

19. No loguear montos, nombres, comprobantes.
20. No hardcodear tokens.
21. Usuario confirma antes de guardar movimientos (flujos receipt/notification ya lo respetan).

---


## Módulos existentes

| Módulo Gradle | Package namespace | Responsabilidad |
|---------------|-------------------|-----------------|
| `:app` | `pe.kipu.app` | `KipuApplication`, `MainActivity`, `MainViewModel`, Hilt, `KipuNavGraph`, `KipuPlanRoutes`, bottom bar |
| `:core:designsystem` | `pe.kipu.core.designsystem` | Tema, componentes UI (`KipuFilterChip`, `KipuCompactBadge`, botones, cards, headers) |
| `:core:domain` | `pe.kipu.core.domain` | Modelos puros, UseCases, `CategoryIds`, `FinancialPlanIds`, repositorios (interfaces) |
| `:core:data` | `pe.kipu.core.data` | Room (`kipu.db` v16), DataStore, seeds, notification listener, OCR ML Kit |
| `:feature:home` | `pe.kipu.feature.home` | `HomeScreen` — disponible hoy + alertas hormiga |
| `:feature:movements` | `pe.kipu.feature.movements` | Lista movimientos HTML, filtros, duplicados, notificaciones pending, **cambio categoría** |
| `:feature:envelopes` | `pe.kipu.feature.envelopes` | Sobres HTML, crear/eliminar, ajuste límite, últimos movimientos, nav plan/compromisos |
| `:feature:commitments` | `pe.kipu.feature.commitments` | CRUD metas/deudas/pagos pendientes + alerta plan inválido |
| `:feature:profile` | `pe.kipu.feature.profile` | Preferencias + toggle notificaciones |
| `:feature:onboarding` | `pe.kipu.feature.onboarding` | **Solo** `PlanIntroStep` + completar onboarding |
| `:feature:plan` | `pe.kipu.feature.plan` | Wizard plan 6 pasos (`PlanWizardScreen`, `PlanWizardViewModel`) |
| `:feature:receipts` | `pe.kipu.feature.receipts` | Hub + revisión OCR comprobantes |
| `:feature:juntas` | `pe.kipu.feature.juntas` | Lista juntas + crear/eliminar |

### Dependencias actuales (grafo — **leer antes de añadir módulos**)

```
app
 ├── :core:designsystem, :core:domain, :core:data
 └── :feature:home, :movements, :envelopes, :commitments, :profile, :onboarding, :plan, :receipts, :juntas

core/data → core/domain (+ Room, DataStore, Hilt, ML Kit)

core/designsystem → (Compose BOM, sin domain)

core/domain → stdlib + coroutines (JVM puro, sin Android)

feature/home, movements, envelopes, commitments, profile, onboarding, plan, receipts, juntas
 └── core/designsystem + core/domain (+ Hilt, Compose)
```

**Regla:** `feature/*` **no** debe depender de `core/data` ni de otros `feature/*`; no hay excepciones activas.

Room persiste: movimientos, categorías, sobres, compromisos, plan financiero, pares duplicados descartados, **juntas**, **gastos de junta**. DataStore: preferencias no sensibles (`onboardingCompleted`, tema, flags notificaciones). **Migraciones incrementales v1→v16** (sin destructive fallback).

### `settings.gradle.kts` — módulos incluidos (orden canónico)

```
:app
:core:designsystem, :core:domain, :core:data
:feature:home, :movements, :envelopes, :commitments, :profile, :onboarding, :plan, :receipts, :juntas
```

Al añadir un feature nuevo: `include` aquí **y** `implementation(project(...))` en `app/build.gradle.kts`.

---

### UseCases domain añadidos en F14 / F14b

| UseCase | Archivo | Invocado desde |
|---------|---------|----------------|
| `SaveFinancialPlanUseCase` | `core/domain/.../SaveFinancialPlanUseCase.kt` | `PlanWizardViewModel` |
| `UpdateMovementCategoryUseCase` | `core/domain/.../UpdateMovementCategoryUseCase.kt` | `MovementsViewModel` |
| `UpdateEnvelopeWeeklyLimitUseCase` | `core/domain/.../UpdateEnvelopeWeeklyLimitUseCase.kt` | `EnvelopesViewModel` |
| `GetEnvelopeRecentMovementsUseCase` | `core/domain/.../GetEnvelopeRecentMovementsUseCase.kt` | `EnvelopesViewModel` |
| `CreateEnvelopeUseCase` | `core/domain/.../CreateEnvelopeUseCase.kt` | `EnvelopesViewModel` |
| `DeleteEnvelopeUseCase` | `core/domain/.../DeleteEnvelopeUseCase.kt` | `EnvelopesViewModel` |
| `SaveCommitmentUseCase` | `core/domain/.../SaveCommitmentUseCase.kt` | `CommitmentsViewModel` |
| `DeleteCommitmentUseCase` | `core/domain/.../DeleteCommitmentUseCase.kt` | `CommitmentsViewModel` |
| `MoneyInputParser` (util) | `core/domain/.../MoneyInputParser.kt` | Plan wizard, ajuste sobres, CRUD sobres/compromisos |
| `CompleteOnboardingUseCase` | `core/domain/.../CompleteOnboardingUseCase.kt` | `OnboardingViewModel` (Fase 20) |

---

## Configuración SDK

| Parámetro | Valor |
|-----------|-------|
| `applicationId` | `pe.kipu.app` |
| `minSdk` | 26 |
| `compileSdk` | 37 |
| `targetSdk` | 36 |

> `compileSdk 37` requerido por `androidx.core:core-ktx:1.19.0`.

---

## Tema visual Kipu

| Token | Valor |
|-------|-------|
| Primary | `#1B6B5A` |
| Secondary | `#C4A35A` |
| Background | `#F7F5F0` |
| Surface | `#FFFFFF` |
| OnSurface | `#1A1A1A` |
| Error | `#B3261E` |
| Income | `#2E7D32` |
| Expense | `#C62828` |

Sin dynamic color de Material (no morado por defecto).

---

## Navegación (referencia rápida)

> **Documentación completa:** [Navegación extendida (post-Fase 14)](#navegación-extendida-post-fase-14) — incluye rutas secundarias, argumentos nav y reglas bottom bar.

Bottom bar — 5 tabs (`KipuDestination`): `home`, `movements`, `envelopes`, `commitments`, `profile`.

Rutas secundarias (`KipuPlanRoutes`): `plan/{startStep}`, `movements/category/{categoryId}`.

Implementación: `app/.../KipuNavGraph.kt`, `KipuBottomBar.kt`, `MainActivity.kt`.

---

## Decisiones tomadas

| Decisión | Razón | Fase |
|----------|-------|------|
| Package `pe.kipu.app` | Identidad peruana; distinto de plantilla `com.example` | 0 |
| Multi-módulo desde Fase 0 | Evitar monolito; preparar Clean Architecture | 0 |
| `core/designsystem` separado | Tema compartido sin acoplar features entre sí | 0 |
| Navigation Compose en `app` | Navegación global no pertenece a un feature | 0 |
| Sin ViewModels en Fase 0 | Placeholders sin estado ni lógica | 0 |
| Sin Hilt/Koin en Fase 0 | DI se introduce cuando exista domain/data | 0 |
| `material-icons-extended` | Histórico: hoy solo `:feature:plan` usa los iconos extendidos concretos; R8 elimina los no alcanzables en release | 0 |
| ECC como metodología obligatoria | Ciclo verificable; contrato LISTO/NO LISTO | 1 |
| `PROJECT_STATE.md` como fuente de verdad | Sincronizar estado entre encargos de IA | 1 |
| Fase 1 = docs, no código | Separar control de calidad del shell funcional | 1 |
| Componentes DS en `core/designsystem` | Reutilización sin acoplar features; solo presentación | 2 |
| Iconos Material en designsystem | El designsystem usa `material-icons-core`; los extendidos no pertenecen al bottom bar ni a este módulo | 2 |
| `Money` con `BigDecimal` scale 2 | PEN con centavos; signo en `MovementType`, no en amount negativo | 3 |
| `core/domain` como JVM puro | Dominio libre de Android/Room/Compose | 3 |
| Hilt 2.59.2 para AGP 9 | DI multi-módulo compatible con compileSdk 37 | 4 |
| Fake repos en `core/data` | Presentation testeable sin Room hasta Fase 6 | 4 |
| DataStore para preferencias | Flags locales sin PII; `clear()` para wipe futuro | 5 |
| `amountCents` para Money en Room | Centavos PEN scale 2; dominio sigue usando `Money` | 6 |
| Excluir `kipu.db` del backup en nube | Datos financieros no salen por backup automático Google | 6 |
| `suggestedRecordedAt` solo con fecha+hora | Sin medianoche artificial; zona America/Lima | 7b |
| `spentAmount` no persistido en sobres | Calculado desde movimientos; `EnvelopeBudgetState` es snapshot | 8 |
| Semana presupuesto lunes–domingo Lima | `[start, end)` con `WeekRangeCalculator` | 8 |
| ADJUSTED ≥ 80 % del límite semanal | `EnvelopeBudgetThresholds.ADJUSTED_PERCENT` | 8 |
| `fallbackToDestructiveMigration` v1→v2 | Histórico; eliminado desde Fase 17. Room v16 usa migraciones incrementales | 8 |
| `totalRemaining` global en disponible diario | `totalLimit - totalSpent`; no sumar remainings por sobre (F9b) | 9b |
| Hormiga por categoría en ventana 48 h | Umbrales en `AntSpendingThresholds`; alertas no persistidas | 9 |
| `TimeProvider` inyectable | Tests deterministas; `WeekRangeCalculator` y Home insights | 9 |
| Duplicados solo con confirmación humana | `ResolveDuplicateMovementUseCase` único punto de delete; sin fusión silenciosa | 10 |
| Match fuerte por `operationNumber` + mismo monto | Duplicado aunque fecha exceda tolerancia de 15 min | 10c |
| Dismiss duplicados persistido en Room | Tabla `dismissed_duplicate_pairs`; clave canónica por par | 10c |
| Refresh periódico insights Home/sobres | `TimeProvider.refreshTicks()` cada 60 s | 10c |
| Reserva mensual sobres en plan | `sum(weeklyLimit) * 4` en `ValidateFinancialPlanUseCase` | 11 |
| Progreso meta cap 100 % | `CalculateSavingsGoalProgressUseCase`; completada si settled o ahorrado ≥ meta | 11 |
| Validación de plan | Histórico: el gate se difería. Hoy `SaveFinancialPlanUseCase` rechaza un plan inválido además de informar en UI | 11b |
| Alerta plan visible sin compromisos | `CommitmentsScreen` muestra tarjeta aunque lista vacía | 11b |
| Ingresos notificación solo PENDING | `RegisterNotificationIncomeUseCase`; confirmación en Movimientos | 12 |
| Dedup notificación solo pendiente | Mismo draft id: skip si PENDING; tras CONFIRMED, id con sufijo temporal | 12b |
| Listener allowlist Yape/Plin | `MonitoredPaymentApps`; respeta `notificationsEnabled` | 12 |
| Onboarding reducido a intro plan | Welcome/permisos/tutorial Yape eliminados por producto | 14 |
| Wizard plan sin editar sobres | Límites semanales solo vía seed + diálogo Ajustar en Sobres | 14 |
| Flag wizard en memoria (`MainViewModel`) | Histórico: hoy `UserPreferences.pendingPlanWizard` lo persiste y se consume al abrir el wizard | 14 |
| `feature:envelopes` → `feature:movements` | Histórico: resuelto al mover `MovementDisplayLabels` a domain; no existe dependencia feature→feature | 14b |
| `MovementsData` para combine flows | Kotlin `combine` máx. 5 parámetros | 14b |
| Cambio categoría vía UseCase + validate | `UpdateMovementCategoryUseCase`; VM no muta entidades | 14b |
| APK solo arm64 + R8 release | Tamaño y seguridad release; emulador x86 no soportado | 14c |
| Export local sin nube + aviso sensibilidad | AGENTS.md MVP; FileProvider cache | 13 |
| Wipe transaccional + re-seed baseline | App usable post-borrado; onboarding reinicia | 13 |
| `KipuDatabaseSeeder` compartido | onCreate Room y post-wipe usan mismo seed | 13 |

---

## Hallazgos abiertos

No hay hallazgos LOW confirmados abiertos en este snapshot. Las regresiones instrumentadas añadidas el 13 ago pasaron en Moto G24; siguen pendientes las comprobaciones humanas de cámara/share real y locución audible con TalkBack.

## Hallazgos cerrados

| ID | Severidad | Hallazgo | Resolución | Cerrado en |
|----|-----------|----------|------------|------------|
| F0-05 | LOW | `material-icons-extended` aumentaba tamaño APK | Release R8 conserva únicamente iconos alcanzables (29 clases en mapping; APK release 15.7 MB); el alcance actual extendido es `:feature:plan` | 13 ago 2026 |
| F0-01 | LOW | Deprecación `centerAlignedTopAppBarColors` en `KipuTopBar` | Migrado a `topAppBarColors` | Fase 2 |
| F1-01 | MEDIUM | Tablas de fase en `TDD_CHECKLIST` y `SECURITY_CHECKLIST` desactualizadas | Sincronizadas con roadmap 0–16; nota canónica añadida | Fase 1 |
| F7-01 | MEDIUM | Fecha/hora detectada pero no en `SuggestedMovement` | Campo `suggestedRecordedAt` + `ReceiptDateTimeParser` | Fase 7b |
| F7-02 | LOW | Sin test explícito fecha/hora Yape | `ReceiptDateTimeParserTest` + `YapeReceiptParserTest` | Fase 7b |
| F7-03 | LOW | `hasTimeSignal` no usada en confianza | `ReceiptConfidenceResolver` (fecha+hora → HIGH) | Fase 7b |
| F7-04 | LOW | Sin test JVM `ProcessReceiptImageUseCase` | `ProcessReceiptImageUseCaseTest` con fake OCR | Fase 7b |
| F7-05 | LOW | Sin test Plin + historial en `ParseReceiptTextUseCase` | Test integración `plin_no_message` + historial | Fase 7b |
| F8-ECC | — | Revisión ECC Fase 8 | LISTO; hallazgos F8-01…F8-05 documentados como abiertos | Fase 8 |
| F9-ECC | — | Revisión ECC Fase 9 | LISTO; disponible diario + hormiga en Home | Fase 9 |
| F9-01 | MEDIUM | `weeklyRemaining` sumaba remainings por sobre | Global `totalLimit - totalSpent` en F9b | Fase 9b |
| F9-02 | LOW | Sin test `daysRemaining == 0` | Test en `CalculateDailyAvailableUseCaseTest` | Fase 9b |
| F9-03 | LOW | `HomeAlertTranslator` acoplado a UseCase | `AntSpendingAlertKeys` en domain | Fase 9b |
| F0-03 | LOW | `lintDebug` no ejecutado en app | `:app:lintDebug` PASS | Fase 10c |
| F0-04 | LOW | Sin prueba instrumentada smoke MainActivity | `MainActivitySmokeTest` | Fase 10c |
| F7-06 | LOW | Duplicar `CategoryIds` y `DefaultCategoryIds` | Seed Room usa `CategoryIds` | Fase 10c |
| F7-07 | LOW | Historial Plin lee todos los movimientos | `findByCounterpartyName` en Room | Fase 10c |
| F8-02 | LOW | `Envelope.spentAmount` en domain siempre ZERO desde Room | KDoc; usar `EnvelopeBudgetState` | Fase 10c |
| F8-04 | LOW | Semana stale entre emisiones Flow | `refreshTicks()` 60 s | Fase 10c |
| F9-04 | LOW | Flow Home stale al cambiar semana | `refreshTicks()` en insights | Fase 10c |
| F9-05 | LOW | Alertas hormiga sin nombre categoría | `CategoryRepository` en Home | Fase 10c |
| F10-ECC | — | Revisión ECC Fase 10 | LISTO; cierre formal jun 2026 | Fase 10 |
| F10-01 | MEDIUM | `ConfirmSuggestedMovementUseCase` guardaba sin chequeo | Facade con wrapper | Fase 10b |
| F10-02 | LOW | Dismiss duplicados solo en sesión | Room v3 | Fase 10c |
| F10-03 | LOW | Match fuerte sin mismo monto | Exige amount en matcher | Fase 10c |
| F10-04 | LOW | MERGE con createdAt iguales ambiguo | Tie-break por id | Fase 10c |
| F10-05 | LOW | Sin test instrumentado diálogo Compose | `DuplicateResolutionDialogTest` | Fase 10c |
| F11-ECC | — | Revisión ECC Fase 11 | LISTO; hallazgos F11-01…F11-05 documentados | Fase 11 |
| F11-01 | LOW | Alerta plan oculta si lista compromisos vacía | `CommitmentsScreen` refactor F11b | Fase 11b |
| F11-02 | MEDIUM | TDD marcaba save plan inválido sin implementar gate | `SaveFinancialPlanUseCase` rechaza `Invalid`; test domain | Fase 19 |
| F14-01 | MEDIUM | Flag `pendingOpenPlanWizard` solo en memoria | `UserPreferences.pendingPlanWizard` + `PendingPlanWizardInstrumentedTest` | Fase 19 |
| F0-02 | MEDIUM | `allowBackup=true` sin política para DB financiera | DB + DataStore + cache exports/receipts excluidos | Fase 19 |
| F21-01 | HIGH | Wipe no borraba imágenes de comprobantes en cache | `clearLocalFileCaches()` en wipe (exports + receipts) | Fase 19 |
| F11-03 | LOW | Sin test directo de `ObserveCommitmentSummariesUseCase` | `ObserveCommitmentSummariesUseCaseTest` | Fase 20 |
| F11-04 | LOW | `DefaultCommitmentIds` / `DefaultFinancialPlanIds` duplican ids de dominio | Seed/migraciones usan `CommitmentIds` / `FinancialPlanIds` | Fase 20 |
| F11-05 | LOW | Sin test instrumentado Room tablas v4 | `KipuDatabaseMigrationInstrumentedTest` v4→v9 | Fase 20 |
| F12-05 | LOW | Package names Yape/Plin no verificados en dispositivo | Allowlist + `MonitoredPaymentAppsTest`; E2E hardware opcional | Pre-release |
| F12-07 | LOW | Sin tests presentation Profile/Movements notificaciones | `NotificationListenerCoordinatorTest` (pref ON/OFF) | Fase 20 |
| F14-03 | LOW | `CompleteOnboardingUseCase` orphan en domain | Cableado en `OnboardingViewModel` + test | Fase 20 |
| F14-05 | LOW | Sin test Compose del wizard | `PlanWizardE2ETest` (seis pasos + persistencia) | Fase 20 |
| F12-ECC | — | Revisión ECC Fase 12 | LISTO; hallazgos F12-01…F12-07 documentados | Fase 12 |
| F12-01 | LOW | Docs auto-cerrados sin revisión gestor | Sincronización PROJECT_STATE + AGENTS en F12b | Fase 12b |
| F12-02 | MEDIUM | Dedup bloqueaba re-ingreso tras confirmar | `RegisterNotificationIncomeUseCase` F12b | Fase 12b |
| F12-03 | LOW | Conteo tests incorrecto en docs (16 vs 18) | Corregido en F12b | Fase 12b |
| F8-01 | MEDIUM | `fallbackToDestructiveMigration` borraba DB al actualizar schema | `KipuDatabaseMigrations` v1→v6 | Fase 17 |
| F13-02 | LOW | DataStore no excluido de backup Google | exclude `kipu_preferences.preferences_pb` | Fase 17 |
| F16-01 | MEDIUM | Migraciones Room destructivas | Migraciones incrementales v1→v6 | Fase 17 |
| F16-02 | LOW | Sin edición de juntas | `UpdateGatheringUseCase` + UI | Fase 17 |
| F16-03 | LOW | Sin reparto de gastos en juntas | `gathering_expenses` + split igualitario | Fase 17 |
| F16-04 | LOW | KSP race con domain jar stale | `dependsOn(:core:domain:jar)` en módulos KSP | Fase 17 |
| F13-01 | LOW | Sin test instrumentado wipe Room real | `RoomUserDataWipeInstrumentedTest` | Fase 18 |
| F13-03 | LOW | CSV solo exporta movimientos | Documentado en UI Perfil | Fase 18 |
| F14-04 | LOW | Dependencia envelopes→movements acopla features | `MovementDisplayLabels` en domain | Fase 18 |
| F16-05 | LOW | Sin vincular movimientos a juntas | `LinkMovementToGatheringUseCase` + UI | Fase 18 |
| F16-06 | LOW | Sin liquidación por participante | `CalculateGatheringSettlementUseCase` + UI | Fase 18 |
| F16-07 | LOW | Room v6 sin paidBy/movementId | `MIGRATION_6_7`, DB v7 | Fase 18 |
| F8-03 | LOW | Sin UI **crear** sobres nuevos | `CreateEnvelopeUseCase` + `EnvelopeCreateDialog` + eliminar con confirmación | Fase 22 |
| F11-06 | LOW | Sin CRUD compromisos en UI | `SaveCommitmentUseCase` / `DeleteCommitmentUseCase` + `CommitmentFormDialog` | Fase 22 |
| F11-07 | LOW | Progreso metas no se actualiza desde movimientos | `commitmentId` + `LinkMovementToCommitmentUseCase` + progreso reactivo | Fase 23 |
| F14-02 | LOW | Wizard no edita sobres ni metas | Wizard 6 pasos; `PlanWizardStateLoader`; chips Sobres/Meta en tab Sobres | Fase 24 |
| F12-06 | LOW | Diálogo duplicado notificación sin MERGE | `ConfirmPendingNotificationMovementUseCase` MERGE + diálogo paridad UI | Fase 25 |
| F14-06 | LOW | ECC formal Fase 14 no cerrada por gestor | Revisión ECC retrospectiva; veredicto LISTO Fase 26 | Fase 26 |
| F14-07 | LOW | Confianza movimiento es heurística UI | Documentado MVP en `MovementPresentation`; sin campo en `Movement` | Fase 26 |

---

## Roadmap de fases

> **Numeración canónica** tras Fase 1. Las tablas en `TDD_CHECKLIST.md` y `SECURITY_CHECKLIST.md` deben reflejar estos números.

| Fase | Nombre | Entregable principal | Estado | TDD | Seguridad |
|------|--------|----------------------|--------|-----|-----------|
| 0 | Shell multi-módulo | Navegación + tema + placeholders | ✅ | No | Baja |
| 1 | Documentación de control | AGENTS, workflow, checklists, ECC | ✅ | No | Baja |
| 2 | Design system ampliado | Componentes reutilizables, fix TopBar | ✅ | No | Baja |
| 3 | Domain base | `core/domain`, modelos, interfaces repositorio | ✅ | Parcial | Baja |
| 4 | DI + presentation base | Hilt, ViewModels, fake repos | ✅ | No | Baja |
| 5 | DataStore + preferencias | UserPreferences, Profile toggles | ✅ | Parcial | Media |
| 6 | Room + movimientos | CRUD movimientos, mappers, backup rules | ✅ | Sí | Media |
| 7 | Parsers + OCR | ML Kit, parsers Yape/Plin, confirmación | ✅ | Sí | Alta |
| 8 | Sobres | Cálculo presupuesto semanal | ✅ | Sí | Media |
| 9 | Disponible diario + hormiga | UseCases de alertas + Home | ✅ Cerrada | Sí | Media |
| 10 | Duplicados | Detección y fusión con confirmación | ✅ **Cerrada formalmente** | Sí | Media |
| 11 | Compromisos / metas | Metas de ahorro, plan financiero | ✅ **Cerrada formalmente** | Sí | Media |
| 12 | Notificaciones | Listener opcional de ingresos | ✅ **Cerrada formalmente** | Parcial | Alta |
| 13 | Exportar / eliminar datos | CSV/JSON local, wipe completo | ✅ **MVP funcional** | Sí (3 tests) | Alta |
| 14 | Onboarding + plan + pulido UI | Intro plan, wizard, movimientos/sobres HTML, APK | ✅ **ECC LISTO** (Fase 26) | Sí (3 UseCases) | Baja |
| 15 | Comprobantes UI | Share intent, preview, edición manual | ✅ **MVP funcional** | Parcial | Alta |
| 16 | Juntas + pulido | Feature social, QA, lint, release prep | ✅ **MVP funcional** | Parcial | Media |
| 17 | Cierre riesgos F16 | Migraciones Room, juntas edit/reparto, KSP, backup | ✅ **Completada** | Parcial | **Alta** |
| 18 | Cierre riesgos restantes | Wipe instrumentado, liquidación juntas, Room v7 | ✅ **Completada** | Sí | Media |
| 19 | Riesgos críticos | Backup, wipe cache, plan inválido, wizard persistente | ✅ **Completada** | Parcial | **Alta** |
| 20 | Riesgos residuales | Tests domain/data, ids seed, onboarding UseCase | ✅ **Completada** | Sí | Baja |
| 21 | Armonización UX/UI | Tokens DS + pantallas alineadas | ✅ **Completada** | No | Baja |
| 22 | CRUD sobres + compromisos | Crear/eliminar sobres; CRUD compromisos | ✅ **Completada** | Sí (+2 tests) | Baja |
| 23 | Metas ↔ movimientos | `commitmentId`; vincular ingresos a metas | ✅ **Completada** | Sí (+3 tests) | Media |
| 24 | Wizard edita plan (F14-02) | Precarga + accesos sobres/meta en wizard | ✅ **Completada** | Sí (+5 tests) | Baja |
| 25 | MERGE duplicados notificación | Paridad diálogo duplicados en ingresos por notificación | ✅ **Completada** | Sí (+1 test) | Baja |
| 26 | QA Play Store + ECC F14 | Docs release, privacidad in-app, lint + release | ✅ **Completada** | Parcial (+1 test UI) | Media |
| 27 | Internal testing pipeline | Keystore, bundleRelease, E2E privacidad, Pages HTML | ✅ **Completada** (repo) | Sí (+1 E2E) | Alta |
| 28 | *(humano)* | Subir AAB Play Console + QA manual N1–E4 | ⏳ Pendiente | Manual | Alta |

### Mapeo TDD / Seguridad por fase (referencia cruzada)

| Fase | TDD (`TDD_CHECKLIST.md`) | Seguridad (`SECURITY_CHECKLIST.md`) |
|------|--------------------------|-------------------------------------|
| 0–2 | No / `assembleDebug` | Bajo / pre-commit |
| 3 | Parcial (modelos) | Bajo |
| 4 | No | Bajo |
| 5 | Parcial | Medio + DataStore |
| 6 | Sí (mappers) | Medio + Room + Backup |
| 7 | **Sí** (parsers) | **Alto** (OCR + intents) |
| 8–11 | **Sí** (UseCases) | Medio + dominio |
| 12 | Parcial | **Alto** (permisos + notificaciones) |
| 13 | Parcial + seguridad | **Alto** (export + wipe) |
| 14 | **Sí** (Save/Update/Get UseCases) | Bajo + plan financiero |
| 15 | Parcial | **Alto** (OCR UI) |
| 16 | Según lógica nueva | Medio + lint + revisión final |
| 17–19 | Sí (migraciones, wipe, plan) | **Alto** (backup, wipe, persistencia) |
| 20 | Sí (tests residuales) | Baja |
| 21 | No | Baja |
| 22–26 | Sí / docs release | Baja–Media |

---

## Post-MVP (Fases 17–24 completadas)

MVP funcional **completo** (Fases 0–16) + post-MVP **17–27 cerradas en repo**. Pendiente humano:

- Crear keystore y `bundleRelease` firmado — `docs/release/INTERNAL_TESTING.md`
- Activar GitHub Pages → URL privacidad
- Subir AAB a **internal testing** e invitar testers
- Checklist manual E2E hardware (N1–E4)

---

## Qué NO existe aún

### Producto / features

- CRUD completo de plan financiero fuera del wizard (solo wizard edita ingresos/gastos fijos/sobres/meta)
- Edición de movimientos manuales existentes en UI (el alta manual sí está implementada)
- Pasos onboarding eliminados: Welcome, Cómo funciona, Permisos, Tutorial Yape — **no reintroducir** sin petición
- Firebase / sync en nube
- Detección de **gastos** desde notificaciones (F12 solo ingresos)
- Verificación E2E dispositivo real Yape/Plin listener (F12-05 — allowlist cubierta por tests JVM)
- Export CSV de compromisos/sobres (F13-03 — CSV solo movimientos, documentado en Perfil)

### Técnico / infra

- Emulador x86 en build actual (F14c — solo arm64; usar `-Pkipu.x86Emulator=true` para tests)

### Comandos de verificación recomendados (jun 2026)

```bash
./gradlew :core:domain:test assembleDebug
./gradlew :core:data:testDebugUnitTest          # opcional, más lento
./gradlew :app:lintDebug                         # antes de release
./gradlew assembleRelease                        # verificar R8/ProGuard
```

Tras cambios en `:core:domain` UseCases, si KSP falla en features (mitigado en Fase 17 con `dependsOn(:core:domain:jar)`):

```bash
./gradlew :core:domain:clean :core:domain:jar :app:assembleDebug
```
