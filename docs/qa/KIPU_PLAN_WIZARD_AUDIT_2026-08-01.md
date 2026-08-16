# Auditoría del wizard de plan — 1 agosto 2026

Alcance de la auditoría inicial: creación, resumen, guardado, reapertura, persistencia y presentación de los seis pasos del plan en un Moto G24 con Android 14. Esa etapa fue de solo análisis y no modificó producción ni pruebas; la sección del 9–10 de agosto documenta las remediaciones posteriores.

## Hallazgos

### HIGH

#### PLAN-H01 — cada paso hereda la posición de desplazamiento del anterior

- Evidencia: `feature/plan/src/main/java/pe/kipu/feature/plan/PlanWizardScreen.kt:131` conserva un único `rememberScrollState()` alrededor de todos los pasos.
- Resultado observado: después de completar Ingresos desde la parte baja, Gastos abrió directamente en el total; Sobres abrió en Ocio; Meta abrió en meses/deuda; Resumen omitió su inicio.
- Impacto: secciones obligatorias aparecen fuera de orden o quedan ocultas y el usuario puede continuar sin haberlas visto.
- Corrección mínima: reiniciar o recordar el scroll por `PlanWizardStep` y llevar foco/scroll al inicio tras cada transición.

#### PLAN-H02 — un monto del resumen queda ilegible en 360 dp

- Evidencia: `feature/plan/src/main/java/pe/kipu/feature/plan/ui/PlanWizardSteps.kt:929` coloca dos textos sin pesos ni adaptación en una fila.
- Resultado observado: `Sobres (equivalente mensual)` deja el valor `- S/ 140.00` en una columna de pocos píxeles, partido verticalmente.
- Impacto: el usuario no puede verificar un monto financiero antes de guardar.
- Corrección mínima: reservar espacio al valor y permitir que la etiqueta se adapte; validar también con fuente 1.3x.

#### PLAN-H03 — el ingreso mostrado no siempre es el ingreso mensual calculado

- Evidencia: `feature/plan/src/main/java/pe/kipu/feature/plan/ui/PlanWizardSteps.kt:964` muestra solo `fixedBaseText` o `normalWeekText`; `PlanWizardSaver.kt:235` calcula el mensual con frecuencia, segunda quincena y otros ingresos.
- Impacto: el banner y el resumen pueden mostrar un ingreso distinto del usado para validar y guardar el plan en perfiles semanal, quincenal o variable.
- Corrección mínima: exponer y reutilizar un único ingreso mensual calculado para banner, resumen, validación y guardado.

#### PLAN-H04 — editar ingresos semanales o variables no conserva el plan

- Evidencia: `PlanWizardStateLoader.kt:46` coloca el agregado mensual en `fixedBaseText`; `PlanWizardViewModel.kt:146` deja vacías las tres semanas variables; `EstimateMonthlyIncomeUseCase.kt:45` vuelve a multiplicar el valor semanal por cuatro.
- Impacto: un plan semanal puede cuadruplicar sus ingresos al volver a guardarse; uno variable reaparece sin los campos requeridos y no puede continuar sin reingresar datos.
- Corrección mínima: definir una rehidratación por perfil/frecuencia que preserve exactamente el agregado mensual y añadir pruebas de round-trip para todos los perfiles.

#### PLAN-H05 — editar una meta puede cambiar moneda y horizonte

- Evidencia: `PlanWizardStateLoader.kt:91` solo recupera nombre y montos; `PlanWizardUiState.kt:47` y `:51` vuelven a `EMERGENCY` y cinco meses; `PreparePlanSetupUseCase.kt:218` persiste esos valores.
- Impacto: guardar nuevamente una meta existente puede convertir USD a PEN y reemplazar su horizonte sin que el usuario lo haya solicitado.
- Corrección mínima: rehidratar y preservar `currencyCode` y `savingsHorizonMonths`; cubrir el round-trip con pruebas.

#### PLAN-H06 — el límite anti-hormiga parece seleccionado, pero el estado está vacío — ✅ Corregido

- Evidencia previa: `PlanWizardSteps.kt:520-521` mostraba el preset por defecto cuando el texto estaba vacío; `PlanWizardViewModel.kt:158-160` terminaba la inicialización nueva con `""`; `PlanWizardSaver.kt:228-235` rechazaba ese estado.
- Resultado observado antes de corregir: la UI mostró `S/ 35` y el chip seleccionado, pero Continuar no avanzó hasta volver a tocar el preset.
- Impacto: una selección visible no coincide con el valor que valida la aplicación.
- Corrección aplicada: el último fallback de `PlanWizardViewModel` usa el mismo `ANT_SPENDING_PRESETS[1]` que presenta la UI; conserva la precedencia plan guardado → sobre existente → preset `35`.
- Verificación: la regresión JVM de plan nuevo sin plan ni sobres fue RED con esperado `35`/actual vacío y luego GREEN. En Moto G24, `S/ 35, seleccionado` avanzó de Hormiga a `¿Qué quieres lograr?` al pulsar solo Continuar, sin tocar el preset.

### MEDIUM

#### PLAN-M01 — los errores pueden aparecer fuera de pantalla y sin anuncio accesible — ✅ Corregido

- Evidencia previa: `PlanWizardScreen.kt` renderizaba el error al final del contenido desplazable, mientras Continuar permanecía fijo.
- Resultado observado antes de corregir: con `font_scale=1.3`, al continuar sin ingreso desde la parte superior no apareció feedback visible hasta desplazarse tres veces al final.
- Corrección aplicada: el mensaje se renderiza en el área fija de acciones, inmediatamente antes de Continuar, con semántica `error` y `LiveRegionMode.Polite`.
- Verificación: la prueba instrumentada selecciona `No sé exacto`, vacía el aproximado, vuelve al inicio y pulsa Continuar. Antes fue RED porque el mensaje no estaba visible; después pasó verificando visibilidad, `Error` y `LiveRegion.Polite` en el Moto G24 con fuente 1.3x.

#### PLAN-M02 — la prueba llamada E2E no recorre el wizard — ✅ Corregido

- Evidencia previa: `app/src/androidTest/java/pe/kipu/app/PlanWizardE2ETest.kt` solo pulsaba `Comenzar con mi plan` y comprobaba un booleano.
- Impacto previo: el pipeline podía pasar aunque el orden, los seis pasos, el resumen o el guardado estuvieran rotos.
- Corrección aplicada: el caso ahora ejecuta `MainActivity`, entra por el CTA real del onboarding, recorre los seis pasos, guarda un ingreso aproximado de `5000`, vuelve a Home, reabre desde Sobres, comprueba `5000`, edita a `5200`, guarda y vuelve a comprobar `5200` tras otra reapertura. Continúa por Hormiga sin tocar el valor cargado; PLAN-H06 mantiene además su regresión JVM y su verificación física específica del preset `35`.
- Aislamiento: usa el `MainViewModel.resetOnboarding()` de producción para mostrar el CTA sin escribir DataStore por debajo del repositorio, no limpia Room y no depende del orden de clases. En una instalación sin plan la primera vuelta crea; si ya existe uno, guarda un estado conocido. La segunda vuelta siempre prueba edición y round-trip.
- Verificación: el caso enfocado pasó; la secuencia antes problemática `MainActivityLoadingInstrumentedTest` → `PlanWizardE2ETest` pasó 2/2; la suite completa pasó 18/18 app + 24/24 core:data.

#### PLAN-M03 — los montos del banner se superponen con fuente ampliada — ✅ Corregido

- Evidencia previa: la versión auditada de `PlanWizardScreen.kt:362-405` distribuía tres columnas sin adaptación y renderizaba los montos en una sola fila.
- Resultado observado durante PLAN-H05: en Moto G24 con `font_scale=1.3` y densidad 332 (~347 dp), `Ingreso mes`, `Asignado` y `Libre` se solaparon; la moneda y el horizonte de la meta sí permanecieron correctos.
- Impacto: los totales financieros de referencia dejan de ser legibles durante el wizard con una configuración de accesibilidad válida.
- Corrección aplicada: `WizardBalanceStickyBanner` usa el `FlowRow` ya disponible en Compose, con separación horizontal y vertical; cuando no caben tres valores, conserva el orden y envuelve el siguiente sin truncarlo.
- Verificación: en el mismo Moto G24, `Ingreso mes` y `Asignado` quedaron separados en la primera línea y `Libre` pasó completo a la segunda. Los bounds de UI confirmaron 25 px de separación horizontal y 17 px vertical, sin intersecciones.

### LOW

#### PLAN-L01 — la prueba de navegación depende de que no existan compromisos — ✅ Corregido

- Evidencia previa: `app/src/androidTest/java/pe/kipu/app/KipuNavigationE2ETest.kt:52` exigía el estado vacío `Sin compromisos`, aunque el objetivo del caso era verificar navegación.
- Resultado observado durante PLAN-H05: la primera corrida final reutilizó la meta USD sintética del recorrido manual y falló esa aserción; después de que el runner desinstaló la app, el mismo comando pasó 18/18 + 24/24 desde estado limpio, sin cambiar código ni pruebas.
- Impacto: una instalación debug con datos previos puede producir un falso negativo local aunque la navegación funcione.
- Corrección aplicada: el test comprueba `Nuevo compromiso`, control exclusivo de la pantalla disponible tanto con lista vacía como con compromisos. No borra Room ni modifica datos para obtener el PASS.
- Verificación: el caso enfocado pasó conservando una meta creada previamente y la suite completa volvió a pasar 18/18.

## Resultado funcional observado

- La reverificación de PLAN-H06 avanzó de Hormiga a Meta con el preset `S/ 35` inicial, sin volver a tocarlo.
- La auditoría inicial pudo completar los seis pasos después de seleccionar explícitamente el preset, conservando el RED histórico.
- El guardado llegó a Home y `Disponible hoy` reflejó el sobre de S/ 35.
- Sobres reflejó el límite y permitió volver a abrir Ingresos.
- La persistencia transaccional de plan, sobres, categorías y compromisos pasó sus pruebas instrumentadas.
- El error de ingreso queda visible junto a Continuar y expone semántica accesible de error/live region.
- El E2E real recorrió los seis pasos, guardó, reabrió, editó `5000` a `5200` y confirmó el valor tras una segunda reapertura.

## Reverificación de remediación — 9–10 agosto 2026

Estado actual: **10 de 10 hallazgos corregidos**: 6 HIGH, 3 MEDIUM y 1 LOW, todos con evidencia ejecutable o física.

| Hallazgo | Estado | Evidencia actual |
|----------|--------|------------------|
| PLAN-H01 | ✅ Corregido | `PlanWizardScreen.kt` conserva un `ScrollState` y ejecuta `scrollTo(0)` al cambiar `PlanWizardStep`. En Moto G24, Gastos, Sobres, Hormiga, Meta y Resumen abrieron desde su primera sección después de desplazar el paso anterior hasta el final. |
| PLAN-H02 | ✅ Corregido | `PlanWizardSteps.kt:937-951` asigna el espacio flexible a la etiqueta y reserva el monto en una sola línea alineada al final. En Moto G24, `- S/ 300.00` quedó legible a 360 dp exactos y fuente 1.3x; también pasó con la densidad original más estrecha del dispositivo. |
| PLAN-H03 | ✅ Corregido | `PlanWizardSaver.kt:36-49` centraliza el mapeo completo del estado hacia `EstimateMonthlyIncomeUseCase`; banner, resumen, validación y guardado reutilizan esa fórmula. La regresión cubre semanal + extras, quincenal + extras y variable. En Moto G24, S/ 1,000 semanales produjo `Ingreso mes S/ 4,000.00`, `Ingreso estimado S/ 4,000.00` y disponible S/ 3,700 tras asignar S/ 300 mensuales. |
| PLAN-H04 | ✅ Corregido | `PlanWizardStateLoader.incomeDefaults` reconstruye campos editables según perfil y frecuencia; `PlanWizardViewModel` carga segunda quincena, residual semanal y las tres semanas variables. Las pruebas round-trip cubren fijo mensual/semanal/quincenal, variable y aproximado. En Moto G24, S/ 1,000 semanales sobrevivió dos ciclos guardar→reabrir como `Sueldo semanal 1000`; un variable 300/600/900 reabrió normalizado como 600/600/600 y conservó `Ingreso mes S/ 2,400.00`. |
| PLAN-H05 | ✅ Corregido | `PlanWizardStateLoader.GoalDefaults` rehidrata `GoalType.DOLLARS` desde `currencyCode=USD` y recupera `savingsHorizonMonths`; `PlanWizardViewModel` coloca ambos valores en el estado antes de editar. La regresión loader→saver conserva USD y 11 meses, y una meta legada con horizonte nulo permanece vacía para revisión explícita. En Moto G24, una meta de US$ 800 a 11 meses sobrevivió dos ciclos guardar→reabrir con `Dólares` seleccionado, `11` visible y el texto `Para alcanzar US$ 800 en 11 meses`. |
| PLAN-H06 | ✅ Corregido | `PlanWizardViewModel.kt:158-160` inicializa un plan nuevo con el preset visible `35` cuando no hay plan ni sobre previo. `PlanWizardViewModelTest` cubre ese estado. En Moto G24, `S/ 35, seleccionado` avanzó directamente a Meta sin tocar el preset. |
| PLAN-M01 | ✅ Corregido | `PlanWizardScreen.kt` mantiene el error en el área fija de acciones y añade `Error` + `LiveRegion.Polite`. El E2E lo verifica visible desde el inicio con fuente 1.3x. |
| PLAN-M02 | ✅ Corregido | `PlanWizardE2ETest` recorre los seis pasos mediante `MainActivity`, guarda, vuelve a Home, reabre, edita `5000→5200` y comprueba la persistencia tras otra reapertura, sin borrar Room. |

La suite instrumentada recorre ahora los seis pasos y edita un plan guardado de extremo a extremo. PLAN-H03–H06 conservan regresiones JVM enfocadas; PLAN-L01 cubre navegación con independencia de los compromisos y PLAN-M01/M03 se verificaron en el Moto G24 con fuente ampliada.

Nota de modelo: `FinancialPlan` persiste el agregado mensual, el perfil y la frecuencia, pero no el desglose original de quincenas ni de semanas variables. La edición reconstruye un desglose canónico que conserva exactamente el agregado; no pretende recuperar valores históricos que nunca fueron almacenados.

Nota de meta: `Commitment` persiste moneda y horizonte, pero no el `GoalType` visual. La rehidratación distingue `DOLLARS` para USD y usa `EMERGENCY` para PEN; los nombres personalizados se conservan. Una meta existente con horizonte legado nulo deja el campo vacío para exigir revisión, mientras una meta nueva mantiene el valor inicial de cinco meses.

## Evidencia de verificación

| Comando o prueba | Resultado |
|------------------|-----------|
| `adb devices -l` | PASS — Moto G24 Android 14 autorizado |
| `./gradlew :feature:plan:testDebugUnitTest --rerun-tasks` | PASS |
| Tests domain de plan con `--rerun-tasks` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew :feature:plan:lintDebug :app:lintDebug` | PASS |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS — 18 + 24 pruebas |
| Auditoría inicial (precorrección): creación → resumen → guardar → Home → Sobres → editar | Completado; reprodujo PLAN-H01, H02 y H06 |
| Auditoría inicial (precorrección): ingreso semanal S/ 1,000 → guardar → reabrir | FAIL histórico — reprodujo PLAN-H03 y PLAN-H04; reapareció como S/ 4,000 semanales |
| `./gradlew :feature:plan:testDebugUnitTest assembleDebug :feature:plan:lintDebug` | PASS tras corregir PLAN-H01 |
| `./gradlew :app:connectedDebugAndroidTest` | PASS — 18 pruebas tras corregir PLAN-H01 |
| Recorrido manual fin de paso → continuar, pasos 1→6 | PASS — cada paso abrió desde su primera sección en Moto G24 |
| `./gradlew :feature:plan:testDebugUnitTest assembleDebug :feature:plan:lintDebug` | PASS tras corregir PLAN-H02 |
| `./gradlew :app:connectedDebugAndroidTest` | PASS — 18/18 pruebas tras corregir PLAN-H02 |
| Resumen mensual en Moto G24, 360 dp y fuente 1.3x | PASS — etiqueta adaptada y monto `- S/ 300.00` completo en una sola línea |
| `./gradlew :feature:plan:testDebugUnitTest --tests 'pe.kipu.feature.plan.ui.PlanWizardStepsTest'` antes del cambio | RED esperado — `expected 4500 but was 1000` |
| Mismo test después del cambio | PASS — semanal, quincenal y variable muestran el estimado mensual completo |
| `./gradlew :core:domain:test --tests 'pe.kipu.core.domain.usecase.EstimateMonthlyIncomeUseCaseTest' :feature:plan:testDebugUnitTest assembleDebug :feature:plan:lintDebug` | PASS tras corregir PLAN-H03 |
| `./gradlew :app:connectedDebugAndroidTest` | PASS — 18/18 pruebas tras corregir PLAN-H03 |
| Recorrido manual sueldo semanal S/ 1,000 → banner → resumen en Moto G24 | PASS — banner y resumen mostraron S/ 4,000 mensuales; disponible S/ 3,700 con S/ 300 asignados |
| `./gradlew :core:domain:test --tests 'pe.kipu.core.domain.plan.PlanWizardStateLoaderTest'` antes del cambio | RED esperado — faltaban los campos de rehidratación quincenal, semanal residual y variable |
| Mismo test después del cambio | PASS — round-trip exacto para fijo mensual/semanal/quincenal, variable y aproximado |
| `./gradlew :feature:plan:testDebugUnitTest` | PASS tras corregir PLAN-H04 |
| `./gradlew testDebugUnitTest` | PASS tras corregir PLAN-H04 |
| `./gradlew assembleDebug` | PASS tras corregir PLAN-H04 |
| `./gradlew lintDebug` | PASS tras corregir PLAN-H04 |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS — 18/18 app + 24/24 core:data en Moto G24 tras corregir PLAN-H04 |
| Recorrido manual semanal S/ 1,000 → guardar → reabrir → guardar → reabrir | PASS — `Semanal` seleccionado y `Sueldo semanal 1000` en ambas reaperturas; Sobres mantuvo `Ingresos/sem S/ 1,000.00` |
| Recorrido manual variable 300/600/900 → guardar → reabrir → continuar | PASS — campos rehidratados 600/600/600 y agregado mensual conservado en `S/ 2,400.00` |
| `./gradlew :core:domain:test --tests 'pe.kipu.core.domain.plan.PlanWizardStateLoaderTest'` antes del cambio H05 | RED esperado — `GoalDefaults` no exponía `goalType` ni `goalMonthsText` |
| Test `does not invent a horizon for an existing legacy goal` antes del ajuste final | RED esperado — recibió `5` en vez de vacío |
| `./gradlew :core:domain:test --tests 'pe.kipu.core.domain.plan.PlanWizardStateLoaderTest' :feature:plan:testDebugUnitTest --tests 'pe.kipu.feature.plan.presentation.PlanWizardSaverTest'` | PASS tras corregir PLAN-H05 |
| Round-trip JVM loader→saver de meta USD a 11 meses | PASS — la solicitud preparada conserva `currencyCode=USD` y `savingsHorizonMonths=11` |
| `./gradlew :core:domain:test :feature:plan:testDebugUnitTest` | PASS tras corregir PLAN-H05 |
| `./gradlew testDebugUnitTest` | PASS tras corregir PLAN-H05 |
| `./gradlew assembleDebug` | PASS tras corregir PLAN-H05 |
| `./gradlew lintDebug` | PASS tras corregir PLAN-H05 |
| Primera corrida final de `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | FAIL conservado — `KipuNavigationE2ETest:52` encontró la meta sintética del recorrido manual y no mostró `Sin compromisos`; no se modificó la prueba |
| Repetición del mismo comando desde el estado limpio dejado por el runner | PASS — 18/18 app + 24/24 core:data en Moto G24 tras corregir PLAN-H05 |
| Recorrido manual Dólares US$ 800, ahorrado US$ 200, 11 meses → guardar → reabrir → guardar → reabrir | PASS — `Dólares` siguió seleccionado y la UI mantuvo `11` y `Para alcanzar US$ 800 en 11 meses` en ambas reaperturas |
| `KipuNavigationE2ETest#bottomBarNavigatesToAllMainTabs` antes de PLAN-L01 | RED conservado — falló al exigir `Sin compromisos` cuando había una meta existente |
| Mismo caso tras PLAN-L01, conservando una meta de prueba | PASS 1/1 — verificó `Nuevo compromiso` sin limpiar Room |
| Banner anterior en Moto G24, ~347 dp y `font_scale=1.3` | RED visual — etiquetas y montos se solapaban |
| APK reconstruido con `FlowRow`, mismo dispositivo y configuración | PASS — valores completos; Ingreso/Asignado separados y Libre envuelto en segunda línea |
| `./gradlew testDebugUnitTest` | PASS tras corregir PLAN-L01 y PLAN-M03 |
| `./gradlew assembleDebug` | PASS tras corregir PLAN-L01 y PLAN-M03 |
| `./gradlew lintDebug` | PASS tras corregir PLAN-L01 y PLAN-M03 |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS — 18/18 app + 24/24 core:data tras corregir PLAN-L01 y PLAN-M03 |
| Recorrido físico previo a PLAN-H06, sin tocar el preset visible `S/ 35` | RED conservado — Continuar permaneció en Hormiga y mostró `Ingresa un límite válido para gastos hormiga` |
| `PlanWizardViewModelTest` antes de PLAN-H06 | RED esperado — esperaba `35` y recibió texto vacío para plan nuevo sin sobres |
| `./gradlew :feature:plan:testDebugUnitTest --tests '*PlanWizardViewModelTest*' --rerun-tasks` | PASS tras corregir PLAN-H06 |
| APK corregido, mismo recorrido físico sin tocar `S/ 35` | PASS — Continuar abrió `¿Qué quieres lograr?`, paso 5 de 6 |
| `./gradlew testDebugUnitTest` y `./gradlew assembleDebug` | PASS tras corregir PLAN-H06 |
| `./gradlew lintDebug` | PASS tras corregir PLAN-H06 |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS — 18/18 app + 24/24 core:data tras corregir PLAN-H06 |
| PLAN-M01 antes del cambio, Moto G24 con `font_scale=1.3` | RED físico conservado — el error no estaba visible junto a Continuar y solo apareció tras tres desplazamientos |
| Prueba instrumentada PLAN-M01 antes del cambio | RED esperado — `Ingresa un monto de ingreso válido` no estaba visible desde la parte superior |
| Misma prueba tras mover el error y añadir semántica | PASS — visible, `Error` correcto y `LiveRegion.Polite` |
| Primer fixture PLAN-M02 mediante escritura directa de DataStore, después de otra `MainActivity` | FAIL conservado — timeout al no abrir el wizard; se localizó una carrera entre disco y el repositorio singleton, sin cambiar producción ni debilitar aserciones |
| `MainActivityLoadingInstrumentedTest` → `PlanWizardE2ETest` con entrada por `MainViewModel` y CTA real | PASS — 2/2 en la misma instrumentación |
| Fixture compartido después de sustituir DataStore directo por `MainViewModel`, usando `ActivityScenario.recreate()` para salir del wizard | FAIL conservado — las preferencias quedaron correctas, pero Navigation restauró el back stack `Inicio → Wizard`; `KipuNavigationE2ETest` agotó 60 s esperando Inicio |
| `KipuNavigationE2ETest#bottomBarNavigatesToAllMainTabs` tras volver con el back nativo desde el wizard | PASS — 1/1; corrige el aislamiento en el helper compartido sin borrar datos ni ampliar timeouts |
| `./gradlew :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest` | PASS final — 18/18 app + 24/24 core:data en Moto G24 con `font_scale=1.3` |
| `./gradlew lintDebug` | PASS final con el fixture E2E definitivo |

## Riesgos residuales

- La semántica accesible está automatizada, pero el audio real de TalkBack requiere una comprobación humana breve.
- La prueba no borra Room. Por eso la rama estricta de creación nueva se da únicamente en una instalación sin plan; en una instalación reutilizada, la primera vuelta guarda un plan conocido y la segunda demuestra edición/round-trip de forma determinista. Al terminar deja el plan sintético de `5200` en el paquete debug, así que debe ejecutarse en una instalación de pruebas, no sobre datos reales.
- La auditoría del wizard no sustituye las tareas humanas de firma ni la carga en Play Console.

## Veredicto

**LISTO** para cerrar los hallazgos auditados del wizard. PLAN-H01–H06, PLAN-M01–M03 y PLAN-L01 quedaron corregidos y verificados; este veredicto no califica la carga humana en Play Console.
