# Correcciones de la auditoría de septiembre

Inicio: 9 septiembre 2026, sobre `bc0547e` y la actualización documental no confirmada en Git. Petición: corregir los fallos de la app, sin debilitar pruebas ni mezclar cambios de terceros.

## Alcance y criterios

Corregir los ocho hallazgos AUD con pruebas del comportamiento, comprobar rutas relacionadas y conservar evidencia RED/GREEN. Se revisarán otros fallos reproducibles que aparezcan durante la verificación. No se promete ausencia absoluta de bugs ni se convierte una mejora hipotética en un defecto demostrado.

Fuera de alcance: Play Console, credenciales/firma reales, publicación, commits, actualización de dependencias, borrar datos personales y modificaciones al repositorio externo de skills. Las pruebas Room usan bases en memoria; las E2E usan la instalación debug de pruebas.

Metodología: ECC, implementación incremental, TDD y Ponytail full; guía Android/Room para persistencia. La separación de pruebas de reproducción mediante un agente sigue la guía TDD; Gradle y producción se coordinan desde la tarea principal.

## Lotes y seguimiento

| Lote | Hallazgo | Criterio de aceptación | Estado |
|---|---|---|---|
| 1 | AUD-H03 | Editar/borrar/invalidar pago conserva recibo, historial y total coherentes; nunca sustituye un pago inexistente por la referencia | JVM, 5 Room y tarjeta UI GREEN en las suites completas |
| 2 | AUD-H04 | Fusión mantiene una operación y reconcilia reserva/vínculos atómicamente | 5 regresiones Room GREEN en la suite completa |
| 3 | AUD-H02 | Aporte de meta con destino válido; progreso y efectivo coherentes, sin ingreso ficticio | Operación/HomeVM y aportes concurrentes Room GREEN; locución real no comprobada |
| 4 | AUD-H01 + AUD-M01 | Cobertura distingue obligaciones, reserva y liquidez; confirmación revalida estado vigente | Dominio/HomeVM/Room GREEN; 5 pruebas UI de imprevistos GREEN, incluida legibilidad y control negativo |
| 5 | AUD-M02 | Importe y etiqueta expresan la misma unidad temporal | Etiqueta corregida; prueba GREEN en suite completa |
| 6 | AUD-L01 | Contacto real confirmado, sin notas editoriales en pantalla | Correo aplicado en app/Markdown/HTML; prueba de pantalla GREEN. No se enviaron mensajes |
| 7 | AUD-L02 | Reproducir/diagnosticar Atrás sin debilitar aserciones ni sleeps arbitrarios | No reproducido: suite y 3/3 repeticiones PASS sin cambios; causa no resuelta, en observación |
| 8 | Verificación global | Unitarias, compilación, lint, Room y UI; segunda revisión de diff y documentación | PASS: 551 unitarias, 48 app, 55 datos, assembleDebug, lintDebug, diff y enlaces |
| 9 | AUD-H05 (descubierto durante corrección) | Dos registros manuales en el mismo milisegundo conservan sus movimientos y auditorías | RED reproducido; UUID añadido; regresiones JVM y Room GREEN en suites completas |

## Decisiones del lote 1

- El recibo pertenece a su mes en America/Lima. Un movimiento eliminado, no confirmado, convertido en ingreso o trasladado a otro mes deja de liquidarlo. No se crea automáticamente otro recibo ni se borra el gasto al cambiar su fecha.
- Cambiar solo el monto de un pago válido conserva el vínculo y la referencia del plan; Inicio muestra el monto real.
- La invalidación debe cubrir el punto de escritura compartido, no únicamente los botones de una pantalla. La lectura debe tolerar referencias inválidas preexistentes.

## Evidencia de ejecución

- RED AUD-H03 del 9 septiembre: `:core:domain:test --tests '*ObserveMonthlyServiceReceiptsIntegrityTest'`: 6 pruebas, 4 fallos esperados (desaparecido, ingreso, pendiente, otro mes). XML UTC 21:35:18.
- RED Room: clase `RoomMonthlyServiceReceiptRepositoryInstrumentedTest`: 5 pruebas, 3 fallos esperados (borrar, convertir ingreso, cambiar mes). XML UTC 21:36:10. Sin errores ni omisiones.
- Tras recuperar el disco: la suite focalizada `*ObserveMonthlyServiceReceipts*` pasó. El fixture antiguo que afirmaba tener un pago carecía del movimiento: se añadió ese movimiento manteniendo todas las aserciones; no se debilitó la prueba.
- La corrida conjunta falló antes de instrumentación en `:core:data:mergeLibDexDebugAndroidTest`: un artefacto apuntaba a `/home/gerson/proyectos/kipu/.../MovementDao.class`, fuera de la nueva raíz. Es un fallo de caché trasladada, no un resultado de tests. Se repite con `:core:data:clean`, limitado a salidas generadas del módulo, sin cambiar dependencias.
- Política H03 implementada en el punto compartido `MovementDao.upsert/deleteById`, con transacción; el observador valida referencias antiguas y los IDs excluidos de sobres requieren movimiento vigente. La tarjeta ya no reemplaza un pago inexistente por el monto referencial.

No cambiar un hallazgo a cerrado hasta completar sus comprobaciones y límites.

### Avance del 10 septiembre

- Tras limpiar salidas de core:data, se ejecutaron 8 pruebas: las 5 de recibos pasaron y las 3 nuevas de duplicados fallaron por las causas esperadas. El comando global fue FAIL (3 fallos); no se presenta como una suite completamente aprobada.
- AUD-H04 ahora delega a una operación atómica de persistencia que relee los dos movimientos. Conserva vínculo de sobre/meta/referencia bancaria cuando no hay conflicto, traslada el recibo y gasto compartido, y mantiene un solo uso activo de reserva mediante reversas auditables. No borra el historial del ledger.
- Vínculos incompatibles, pagos de otro mes, cambios desde la previsualización y devoluciones de reserva que exigen decisión manual rechazan la fusión sin borrar datos. Dos usos de reserva duplicados no se suman: una operación conserva el mayor uso individual válido.
- El repositorio de movimientos recibe la base Room para esta operación; se adaptaron los constructores de sus pruebas, sin cambiar sus aserciones.

- La repetición de Room pasó **8/8** (5 recibos + 3 duplicados). La misma ejecución encontró **3 fallos esperados de 13 pruebas HomeVM**: meta inexistente, nombre ambiguo y destino que era un pago pendiente. No se ocultó el FAIL combinado.
- AUD-H02: se reutiliza la semántica del aporte manual (ahorro declarado), con una operación compartida que relee la meta dentro de la transacción. Preserva sus metadatos, rechaza retiros superiores al ahorro declarado y no crea ingresos/gastos. Voz exige una única meta activa PEN. No se modifica automáticamente el historial de aportes antiguos: un gasto histórico puede ser real y necesita revisión del usuario.
- RED de la nueva API: compilación falló porque `AdjustSavingsGoalContributionUseCase` aún no existía. Después de implementarla, XML UTC `2026-09-10T05:09:33.583Z`: **4/4 PASS**. HomeVM, UTC `2026-09-10T05:10:01.497Z`: **14/14 PASS**, incluyendo el aporte válido sin cambio de efectivo y los tres rechazos.
- Segunda revisión H02: el aporte manual y el de voz usan la misma operación; la interfaz explica que declarar ahorro no registra una entrada/salida y no debe duplicar ingresos ya vinculados. Se añadió prueba Room de dos aportes concurrentes (pendiente de ejecución).
- RED AUD-M02: `HomeCycleTextTest` falla porque un importe por día todavía está titulado como total semanal. La suite focalizada de Home tuvo 15 pruebas, 1 fallo esperado.
- GREEN AUD-M02: `HomeCycleTextTest` **1/1 PASS**, XML UTC `2026-09-10T21:08:54.403Z`; semanal y mensual muestran «POR DÍA…», también en la descripción accesible que usa el mismo texto.
- AUD-L01: la prueba de privacidad falló al buscar el contacto facilitado por el responsable. Tras reemplazar el ejemplo y eliminar la nota editorial, **1/1 PASS** en Moto G24 (XML `2026-09-10T21:09:28`). Markdown y HTML usan el mismo contacto. No se comprobó entrega de correo ni se publicaron estos archivos.
- La ejecución combinada de este avance fue FAIL por el RED de una API de cálculo pendiente (`CalculateRemainingPlannedExpensesUseCase` no existía al compilar las pruebas). No invalida los resultados individuales anteriores ni demuestra todavía corregido AUD-H01.
- AUD-H01, RED: cobertura con reserva S/500 y efectivo S/100 asignaba S/300; también asignaba reserva sin efectivo. XML UTC `2026-09-10T21:11:01.696Z`: **4 pruebas, 2 fallos**.
- AUD-M01, RED: una confirmación abierta guardaba después de cambiar efectivo, reserva o fecha. HomeVM XML UTC `2026-09-10T21:11:34.875Z`: **17 pruebas, 3 fallos**. Se añade comparación de los datos financieros completos y fecha dentro de la transacción, antes de crear la compra; los recortes también deben respetar el gasto vigente después de registrar esa compra.
- Room, `RoomLocalTransactionRunnerInstrumentedTest`, XML `2026-09-10T21:12:08`: **4/4 PASS**, incluyendo aportes concurrentes H02 y rollback existente. No es todavía la verificación del nuevo cierre M01.
- La proyección pura de necesidades tiene **4/4 PASS**: monto de referencia frente a pago real; mes correcto; días futuros; semana que cruza fin de mes. Es conservadora: protege el ciclo actual completo y prorratea los futuros hasta fin de mes. Falta cerrar su integración con cobertura, liquidez y reajustes.

### Avance del 12 septiembre

- Recuperados los XML anteriores: HomeVM **17/17 PASS** después de validar la confirmación; Apply **4/4**, Register **1/1** y proyección **4/4 PASS**. La cobertura conservaba tres RED antes de su implementación final, incluidos S/1000 de efectivo, S/800 comprometidos, S/100 de reserva y compra S/300.
- Primera corrida completa de dominio: **419/420 PASS**. El único fallo reprodujo AUD-H05: `CreateManualMovementUseCase.kt` identificaba movimientos solo con el milisegundo; dos registros distintos se reemplazaban por upsert. El defecto era preexistente y se descubrió durante la remediación, no fue introducido por los cambios de recibos o reserva.
- Corrección H05: se añade UUID al ID manual usando la biblioteca estándar ya presente. La prueba que esperaba el ID temporal exacto ahora valida prefijo, UUID y ambas fechas; la nueva prueba exige dos IDs y dos valores conservados. La prueba Room que provocaba una colisión con un ID adivinado ahora provoca el conflicto real durante la escritura de reserva, verificando rollback de movimientos, auditoría y reserva. No se eliminaron aserciones de conservación de datos.
- En esa misma corrida, `:core:data:connectedDebugAndroidTest`: **54/54 PASS**, incluido rechazo de un recorte inferior al gasto de la propia compra y conservación atómica de los datos previos. Aún no incluía la nueva prueba Room de IDs; se ejecutará con el cierre global.
- H01: primero se protege la proyección de obligaciones; la reserva usable se limita por efectivo respaldado y el margen restante. Se distingue falta de efectivo de déficit presupuestario previo. Los recortes no se anuncian como generación de efectivo. El encabezado de saldo acumulado dice «Efectivo sin reservar», no «monto libre»; señala que puede estar comprometido y advierte si la reserva nominal supera la caja.
- M01: snapshot local de movimientos, reserva, plan, compromisos, recibos, sobres y fecha. Se relee bajo transacción antes de cualquier escritura. Un cambio exige volver a revisar; no se regenera/aplica un plan nuevo sin confirmación. Después de registrar la compra se leen los gastos de sobres dentro de la misma transacción y se validan todos los ajustes antes de guardar límites. Si no son aplicables, todo se revierte.
- Límites explícitos de la proyección: protege todo el ciclo actual aunque cruce de mes; los ciclos futuros se prorratean hasta fin de mes, redondeando hacia arriba. Los compromisos siguen la contabilidad de la validación del plan; no se adivina que una deuda y un recibo fijo representan la misma obligación. Evitar registrar el mismo compromiso dos veces. Los importes desconocidos/futuros no garantizan financiación. Los límites de sobres aceptados persisten en ciclos siguientes hasta editarlos; la UI lo informa y permite desmarcar Familia si incluye necesidades esenciales.
- Primera verificación global del 12 septiembre: **551 unitarias PASS**, `assembleDebug` PASS, `lintDebug` PASS (0 errores, 50 advertencias; no son 50 bugs). **Data 55/55 PASS**. **App 45/46 PASS**: falló la nueva prueba de maquetación del desglose porque las etiquetas largas ocultaban el importe. Era una regresión introducida por los nuevos avisos, no un fallo preexistente atribuido al usuario.
- Se corrige el reparto de ancho de las filas tanto en voz como en registro escrito, conservando espacio para el importe. Se añade prueba de voz equivalente. La prueba de recibos real55/referencia45 y transición a pendiente pasó. Atrás pasó en la suite completa sin cambiar componente ni aserciones; falta repetición focalizada, no se declara una causa solucionada.
- Segunda revisión de etiquetas: el panel de servicios ya no llama «Sueldo recibido» al ingreso estimado del plan, ni presenta el resultado de restar solo esos servicios como disponible total. La nueva ejecución global relevante está en curso.
- Segunda corrida: unitarias Home/Movimientos, `assembleDebug` y `lintDebug` PASS; app **45/47 PASS**. El reparto de ancho hace visibles los importes, pero ambas pruebas detectan `hasVisualOverflow`. Se conserva el fallo y se añaden dimensiones al mensaje de diagnóstico, sin cambiar las aserciones. La remediación no está cerrada.
- El primer intento de ejecución focalizada tras ese diagnóstico perdió su sesión antes de producir un resultado nuevo. Se comprobó que no quedaba ningún proceso Gradle activo y se reinició el mismo comando; no se contabiliza el intento interrumpido como PASS.
- Diagnóstico focalizado: **2/4 PASS**. El texto manual mide 101×35 px, mientras el `MultiParagraph` semántico mide 476×35; en voz son 117×42 frente a 650×42. No hay desbordamiento vertical. Se contrastó mediante `javap` el `ParagraphLayoutCache` de foundation 1.12.0 instalado: reconstruye `MultiParagraph` con las restricciones máximas del padre y conserva `layoutSize`. El [código oficial de AndroidX](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/modifiers/ParagraphLayoutCache.kt) contiene esa misma construcción en `slowCreateTextLayoutResultOrNull`. El indicador de ancho por sí solo no demuestra recorte de cifras.
- La comprobación de legibilidad ahora exige una línea, todos los caracteres presentes, ausencia de elipsis/recorte vertical y cada carácter dentro del ancho medido. Se añade un control negativo con ancho de 1 dp para demostrar que rechaza un importe realmente recortado. No se cambia producción para disimular el resultado semántico ni se ignora ninguna prueba. La regresión original (importe invisible por etiqueta sin peso) y su corrección de producción se conservan.
- Repetición focalizada de `UnexpectedExpenseUiTest`: **5/5 PASS**, `BUILD SUCCESSFUL in 1m 7s`. Pasa el control negativo: el verificador rechaza el texto deliberadamente recortado. Se inicia nuevamente la suite completa, offline, sin cambios de dependencias.
- Cierre global: **BUILD SUCCESSFUL in 3m 12s**, 700 tareas (19 ejecutadas, 681 up-to-date). Los resultados unitarios conservados suman **551/551 PASS**; la repetición final reutilizó esas salidas sin cambios de entradas. App ejecutó **48/48 PASS** y datos **55/55 PASS**, sin errores ni omisiones. `assembleDebug` y `lintDebug` PASS. El código de producción de la corrección de filas no cambió durante el diagnóstico del indicador semántico.

## Evidencia de verificación final

Comando global, ejecutado desde la raíz real del repositorio:

```bash
./gradlew --offline --no-daemon --max-workers=1 --continue \
  :core:domain:test testDebugUnitTest assembleDebug lintDebug \
  :app:connectedDebugAndroidTest :core:data:connectedDebugAndroidTest
```

| Comprobación | Resultado | Evidencia y límite |
|---|---|---|
| `:core:domain:test testDebugUnitTest` | PASS | XML: 551 pruebas, 0 fallos/errores; última pasada reutilizó resultados up-to-date |
| `assembleDebug` | PASS | Comando global termina con código 0; no equivale a release firmado |
| `lintDebug` | PASS | 0 errores bloqueantes, 50 entradas de advertencia entre informes de módulos |
| `:app:connectedDebugAndroidTest` | PASS | 48 pruebas, 0 fallos/errores/omitidas en Moto G24 Android 14 |
| `:core:data:connectedDebugAndroidTest` | PASS | 55 pruebas, 0 fallos/errores/omitidas; bases de prueba |
| Clase UI de imprevistos focalizada | PASS | 5/5, incluye importes en voz/escritura y control negativo de recorte |
| Atrás: 3 repeticiones aisladas sin cambios | PASS | Cada ejecución `OK (1 test)`; duraciones del runner 2.633, 2.377 y 2.285 s; no cuentan como tres pruebas distintas de la suite |
| `git diff --check` | PASS | Sin errores de whitespace; no valida comportamiento |
| Enlaces documentales relativos | PASS | 22 documentos cambiados/nuevos, 98 enlaces comprobados, 0 rotos |

Informes generados: `app/build/reports/androidTests/connected/debug/index.html`, `core/data/build/reports/androidTests/connected/debug/index.html` y los XML de `build/test-results/` de cada módulo. Los informes generados se sobrescriben al ejecutar nuevas pruebas; el historial RED/FAIL anterior se conserva arriba.

Para repetir Atrás se reinstalaron los APK debug ya construidos (ambas instalaciones `Success`) y se verificó el runner con `adb shell pm list instrumentation`. Se ejecutó tres veces, secuencialmente:

```bash
adb shell am instrument -w -r \
  -e class 'pe.kipu.app.HighControlsSemanticsTest#speedDialBackClosesMenuWithoutLeavingTheScreen' \
  pe.kipu.app.test/androidx.test.runner.AndroidJUnitRunner
```

En cada salida el caso terminó con `INSTRUMENTATION_STATUS_CODE: 0` y `OK (1 test)`. El `INSTRUMENTATION_CODE: -1` final es el resultado del runner, no un fallo del caso. Se confirmó de nuevo con búsqueda de referencias que `KipuSpeedDialFab` solo tiene callers en dos clases de prueba y ninguno en producción. No se eliminó el componente ni se modificaron esas pruebas para ocultar la incertidumbre.

## Segunda revisión del alcance y límites

- Cambios financieros en `core/domain`, transacciones/vínculos en `core/data` y estado/representación en las features; no se añadieron dependencias entre features ni acceso a DAO desde presentación.
- Hilt sigue proporcionando el ejecutor real de transacciones Room. Los ejecutores directos quedan para pruebas puras. Registro, reversas, auditoría y reajustes comparten transacción cuando corresponde.
- No se modificaron versiones Gradle, dependencias, manifests ni esquemas Room; no se requirió migración de base. Se conservó el cambio previo del submódulo externo de skills.
- Supuesto contable: la reserva es parte del efectivo registrado; ingresos futuros del plan no son depósitos. Los aportes de metas declaran ahorro y no son nuevos movimientos de efectivo. La app no consulta saldos bancarios.
- Las 50 entradas de advertencia lint proceden de los informes de varios módulos y pueden repetirse; no representan 50 defectos funcionales diferentes. No se suprimen ni se actualizan bibliotecas para silenciarlas.

## Comprobaciones no ejecutadas y riesgos residuales

- Voz audible con el proveedor Android, TalkBack, tamaños de fuente ampliados y comprobantes compartidos desde aplicaciones bancarias reales: requieren una ronda humana específica; la instrumentación actual usa estados/fixtures controlados en Moto G24 con Android 14 y fuente 1.0.
- AAB firmado, R8 de release instalado desde Play y publicación: fuera del encargo autorizado. No se utilizaron keystores reales ni se realizaron acciones en Play Console.
- El contacto de privacidad está confirmado por el responsable y visible en app/documentación, pero no se envió correo para probar entrega ni se certificó jurídicamente la política.
- Gastos históricos de aportes por voz: no se convierten automáticamente, porque no se puede distinguir con seguridad un gasto real de un registro equivocado. Revisión y decisión del usuario antes de editar.
- Un plan incompleto, obligaciones duplicadas o ingresos no registrados pueden distorsionar la proyección. La protección conservadora del ciclo y el prorrateo futuro no garantizan llegar a fin de mes ni crean liquidez.
- Las pruebas de tarjeta UI y de persistencia son comprobaciones por capas; no representan todos los recorridos reales de un usuario ni un porcentaje de cobertura de código medido.
- AUD-L02: el fallo previo de Atrás se conserva aunque no se haya repetido; un PASS no demuestra que su causa esté solucionada. No se han cambiado el componente ni sus pruebas.

## Próximos pasos acotados

1. Completar la matriz humana de voz, accesibilidad y share real antes de recomendar publicación.
2. Mantener L02 en observación; si reaparece, conservar foco/estado del diálogo y salida del caso para aislar la causa. La repetición acotada terminó 3/3 PASS y no justifica cambios especulativos.
3. Revisar individualmente los aportes históricos que el usuario identifique como incorrectos. Ninguno de estos pasos autoriza publicación ni modificación automática de datos históricos.

## Estado: NO LISTO

La verificación automática final está completa y aprobada para las correcciones implementadas. El encargo amplio de resolver **todos** los problemas no se declara completo: permanece AUD-L02 sin causa aislada y falta la matriz humana descrita. No se garantiza una app sin errores ni se certifica preparación de release.

Al cierre de la remediación, antes de la solicitud posterior de subir los cambios: código, pruebas y documentación locales sin commit; ningún commit creado ni acción sobre servicios externos/Play Console. No se descargaron ni actualizaron deliberadamente dependencias de Kipu. Sí se instalaron y ejecutaron APK debug de prueba en el dispositivo autorizado.

## Versionado posterior autorizado por el usuario

Tras cerrar la verificación, el usuario solicitó guardar y subir los cambios al repositorio local y GitHub. Código y pruebas quedaron en `9d03f3d`; la documentación se guarda en un commit separado. Destino: `GDvega/kipu`, rama `codex/gastos-imprevistos-reserva`, sin forzar historial ni fusionar en `main`. La autorización de Git no modifica el veredicto de producto ni autoriza publicación en Play Console.

Se excluye el submódulo `docs/ai/external-skills/repository`, cuyos cambios previos pertenecen a un repositorio de terceros. La configuración global de Cloudflare y sus credenciales tampoco forman parte del repositorio Kipu.
