Revisión profunda de Kipu — errores e inconsistencias

  Revisé el estado actual en disco (incluyendo el refactor sin commitear). Marco severidad y doy escenario concreto para cada hallazgo. Verifiqué cada
  afirmación leyendo el código; los falsos positivos descartados van al final.

  🔴 CRÍTICO

  1. PenAmountParser corrompe montos ≥ S/ 1000 escritos sin coma de miles

  core/domain/.../parser/PenAmountParser.kt:11-12

  El regex S/\s*([0-9]{1,3}(?:,[0-9]{3})*|[0-9]+)(?:\.([0-9]{1,2}))? prueba primero la alternativa "1–3 dígitos + grupos con coma". Como la alternancia
  es leftmost-first y el decimal es opcional, el motor acepta solo los primeros 3 dígitos y nunca retrocede a [0-9]+. Lo verifiqué empíricamente
  (mismo comportamiento en Java/Kotlin):

  "S/ 1234.50"  → S/ 123.00     ❌ (pierde un dígito y los decimales)
  "S/ 1000.00"  → S/ 100.00     ❌
  "S/ 12345.00" → S/ 123.00     ❌
  "S/ 3000"     → S/ 300.00     ❌
  "S/ 1,234.50" → S/ 1234.50    ✅ (solo funciona CON coma)

  Este parser es la única extracción de monto de YapeReceiptParser, PlinReceiptParser, YapeIncomeNotificationParser y PlinIncomeNotificationParser.
  Cualquier comprobante/notificación cuyo monto sea ≥ S/ 1000 sin coma de miles (alquileres, sueldos, transferencias grandes; el OCR frecuentemente
  omite la coma) genera un movimiento con monto ~10× menor, de forma silenciosa. Es corrupción de datos financieros.
  Fix: exigir que los grupos con coma solo apliquen con +, p. ej. ([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+), o consumir todos los dígitos contiguos antes del
  decimal.

  🟠 ALTO

  2. La feature "ciclo de presupuesto" (DIARIO/SEMANAL/MENSUAL) está a medio cablear → Home y Sobres se contradicen

  Afecta domain + data + feature. Hay dos fuentes de verdad y ambas rutas ignoran el ciclo elegido:

  - El wizard guarda el ciclo solo en FinancialPlan.budgetCycle (Room), vía SaveFinancialPlanUseCase.kt:46.
  - EnvelopesViewModel.kt:108 lee plan?.budgetCycle → la pantalla de Sobres muestra la etiqueta "mensual/diario" y los "días restantes" del mes.
  - Pero ObserveHomeInsightsUseCase.kt:42 calcula el ciclo desde preferences.budgetCycle (DataStore), y ningún updatePreferences{} escribe jamás ese
  campo (el plumbing DataStore sin commitear en UserPreferencesMapper/Keys quedó sin productor). → El Home siempre calcula en SEMANAL aunque el usuario
  elija MENSUAL.
  - Además, aunque se sincronizara, ObserveEnvelopeBudgetsUseCase.kt:30 y GetEnvelopeRecentMovementsUseCase.kt:20 hardcodean BudgetCycle.WEEKLY al
  calcular el gasto del sobre, y CalculateEnvelopeBudgetStateUseCase compara ese gasto contra envelope.weeklyLimit. Como el wizard etiqueta el límite
  "por mes" cuando eliges MENSUAL (PlanWizardSteps.kt:424), un límite mensual se contrasta contra una ventana de gasto semanal.

  Escenario: eliges "Mensual". Sobres dice "mensual · quedan 20 días" pero la barra de %usado refleja solo el gasto de esta semana contra el límite
  mensual (se ve artificialmente bajo); el Home ignora todo y calcula en semanal. Números internamente incoherentes.
  Fix: una sola fuente de verdad (leer plan.budgetCycle en Home, o escribir la pref al guardar el plan) y propagar el cycle real a
  ObserveEnvelopeBudgetsUseCase/GetEnvelopeRecentMovementsUseCase/preview del wizard en vez de WEEKLY.

  🟡 MEDIO

  3. Código muerto tras el refactor de auto-aprobación en RegisterNotificationIncomeUseCase

  core/domain/.../usecase/RegisterNotificationIncomeUseCase.kt:66-72
  Tras quitar la auto-aprobación, finalMovement siempre se fuerza a PENDING_CONFIRMATION, pero el bloque de duplicados sigue ejecutándose: lee todos
  los movimientos (observeMovements().first()) y corre el matcher O(N) en cada notificación, y hasDuplicates nunca se usa. Además la dependencia
  userPreferencesRepository (línea 29) quedó huérfana (ya no se lee) y el KDoc (17-24) sigue describiendo la auto-aprobación ("it confirms the movement
  automatically"), contradiciendo el nuevo comportamiento. → Trabajo desperdiciado por notificación + documentación engañosa. (Decide: eliminar el
  bloque, o realmente propagar hasDuplicates a la UI como aviso.)

  4. UI colgante de "auto-registro" que ya nunca dispara

  feature/movements/.../presentation/MovementsViewModel.kt:74,151-160
  El filtro source==NOTIFICATION && status==CONFIRMED && !operationNumber.isNullOrBlank() y el snackbar "N movimientos auto-registrados" eran la
  contraparte de la auto-aprobación. Ahora los ingresos por notificación nunca se auto-confirman; y como los parsers crean el SuggestedMovement con
  operationReference = null, el filtro es siempre false. Código muerto que conviene borrar.

  5. isGoalAtRisk compara flujo de caja contra el objetivo total de las metas

  core/domain/.../usecase/CalculateCashFlowSummaryUseCase.kt:33-38
  totalGoalTarget suma el targetAmount completo de todas las metas no liquidadas (ignora lo ya ahorrado) y hace netCash < totalGoalTarget. Mezcla flujo
  (mes) contra stock (objetivo acumulado). Escenario: meta de S/ 10 000 con S/ 9 000 ya ahorrados y net cash S/ 500 → marca "en riesgo" aunque solo
  falten S/ 1 000. En la práctica marcará "en riesgo" casi siempre. Debería compararse contra el faltante o la cuota mensual.

  🟢 BAJO

  - CalculateEnvelopeBudgetStateUseCase.kt:67-74 — intValueExact() sin clamp: con weeklyLimit minúsculo (S/ 0.01) y gasto enorme, el % supera
  Int.MAX_VALUE y lanza ArithmeticException. Usa coerceAtMost (como sí hace CalculateSavingsGoalProgress).
  - Reparto de juntas (CalculateGatheringEqualSplitUseCase.kt:18-21 / CalculateGatheringSettlementUseCase) — total/N con HALF_UP no distribuye el
  residuo de centavos; una liquidación de S/ 10 entre 3 descuadra en ±0.01.
  - MoneyInputParser.kt:9 — replace(",", "") convierte la coma decimal cultural "1,50" en 150 (error 100×) y Money.of redondea en silencio
  "12.567"→12.57. Bajo impacto con la convención peruana (punto decimal), pero conviene validar la entrada.
  - DefaultFinancialPlanSeed.kt:26-28 — insertInto es no-op (código muerto) y su lista de sobres (5, sin SERVICES) es inconsistente con
  DefaultEnvelopeSeed (6). Induce a error aunque no afecta runtime.
  - exportSchema=true (cambio sin commitear) — solo se exporta 15.json y core/data/schemas/ está sin trackear; hay que commitearlo. Un futuro
  MigrationTestHelper fallaría sin los esquemas 1–14.
  - gathering_expenses.movementId ON DELETE CASCADE — borrar un movimiento borra el gasto de junta asociado; quizá deba ser SET NULL (decisión de
  producto).
  - NotificationIncomeFieldExtractor.kt:55-60 — rama LOW redundante (estética).

  ✅ Verificado sin defectos (para descartar ruido)

  - Migraciones Room v1→v15: coherentes; las 14 registradas en ALL y cableadas en databaseBuilder; ALTER TABLE ↔ entities ↔ FKs (RESTRICT/SET
  NULL/CASCADE) verificados columna por columna. La versión sigue en 15 (solo cambió exportSchema).
  - Money: aritmética scale-2, no-negatividad, redondeo HALF_UP correctos; sin división pública insegura. Conversión Money↔centavos sin pérdida.
  - Detección de duplicados: tolerancia 15 min simétrica, match fuerte por operationNumber + mismo monto, tie-break de MERGE determinista, punto único
  de borrado. Correcta.
  - CycleRangeCalculator: rangos [start,end), lunes 00:00, zona Lima (sin DST) — sin off-by-one. CalculateCycleAvailableUseCase protege bien la
  división por cero.
  - Mappers, seeds, allowlist de notificaciones, parseBudgetCycle/parseThemeMode: robustos ante null/valores inválidos; el refactor de auto-aprobación
  quedó consistente en Profile (UI/State/VM sin referencias colgantes).

  ---
  Notas sobre el estado del árbol

  El refactor no está commiteado ni compilado/verificado. Archivos sin trackear que deben incluirse: core/data/schemas/15.json,
  feature/plan/.../PlanWizardSaver.kt, feature/plan/src/test/. Recomiendo antes de cerrar:
  ./gradlew :core:domain:test :core:data:testDebugUnitTest :feature:plan:testDebugUnitTest assembleDebug

  Los dos accionables prioritarios son #1 (corrupción de montos) y #2 (ciclo de presupuesto a medio cablear). ¿Quieres que corrija alguno? Puedo
  empezar por PenAmountParser (arreglo acotado + un test que cubra montos de 4–5 dígitos) y luego proponerte una decisión de diseño para unificar el
  budgetCycle.
