# Auditoría de Kipu — 9 de septiembre de 2026

Estado histórico al 9 septiembre: hallazgos pendientes; este informe conserva la auditoría original, no documenta las correcciones posteriores. Para su estado vigente consultar [Remediación](REMEDIATION_2026-09-09.md). [Estado actual](../ai/PROJECT_STATE.md) · [Verificación y procedencia](VERIFICATION_2026-09-09.md).

Las rutas de evidencia son relativas a la raíz del repositorio. Las líneas corresponden a la revisión auditada `bc0547e`; la documentación se actualizó después sin cambiar producción.

Proyecto: `/media/toshiba/gerson/PROYECTOS/proyectos/kipu`. Revisión `bc0547e`. Auditoría sin correcciones de código. Los identificadores AUD son nuevos: no equivalen a los antiguos PLAN-H01–H06.

Los escenarios numéricos siguientes son ejemplos deducidos de las operaciones del código, no transacciones realizadas con dinero ni reproducciones visuales. Una prueba que pasa confirma sus aserciones, no la ausencia de todos los errores.

## HIGH

### AUD-H01 — La cobertura del imprevisto trata como libre dinero comprometido para el resto del mes

- Confianza: alta en el cálculo; no reproducido visualmente todavía.
- Evidencia: `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/CalculateAvailableBalanceUseCase.kt:15`, `CalculateUnexpectedExpenseCoverageUseCase.kt:15`, `PrepareUnexpectedExpenseUseCase.kt:15` (estos dos últimos archivos están en el mismo directorio).
- Flujo: Inicio → compra imprevista → cobertura y propuesta de reajuste.
- Comportamiento: el saldo disponible es efectivo neto menos reserva. No descuenta recibos pendientes ni necesidades restantes de alimentación o transporte. La propuesta de reajuste recibe únicamente el faltante después de consumir ese saldo.
- Ejemplo: efectivo S/ 1.000, reserva S/ 100 y obligaciones/necesidades pendientes S/ 800. Una compra de S/ 300 se considera cubierta por S/ 100 de reserva y S/ 200 de disponible. Quedan S/ 700 para necesidades de S/ 800, pero el faltante calculado de la compra es cero.
- Impacto: la cobertura puede transmitir tranquilidad aunque la compra comprometa los gastos esenciales pendientes. Además, no se limita la reserva utilizada al efectivo real: una reserva contable superior al efectivo puede aparentar cubrir una compra sin liquidez.
- Causa: se usa una medida de efectivo no reservado como si fuera capacidad de gasto sin afectar el plan.
- Corrección recomendada: distinguir efectivo real, reserva, dinero comprometido y margen realmente libre; evaluar el cierre del período después de la compra. No contar dos veces un recibo ya pagado ni presentar recortes de presupuesto como ingresos de efectivo.
- Prueba necesaria: el escenario anterior debe advertir un déficit proyectado de S/ 100; cubrir también reserva superior al efectivo, pagos parciales, cambio de mes y saldo arrastrado.

### AUD-H02 — El aporte por voz a una meta se guarda como gasto y no incrementa su progreso

- Confianza: alta en el trazado estático.
- Evidencia: `feature/home/src/main/java/pe/kipu/feature/home/presentation/HomeViewModel.kt:284`; `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/CommitmentLinkedIncomeCalculator.kt:14`; `ObserveCommitmentSummariesUseCase.kt:37` y `LinkMovementToCommitmentUseCase.kt:27`, del mismo directorio de casos de uso.
- Flujo: comando de voz interpretado como `GoalContribution` → confirmar → consultar la meta.
- Comportamiento: se crea un movimiento EXPENSE. El progreso de metas suma exclusivamente movimientos INCOME confirmados. La búsqueda del destino tampoco filtra por tipo de compromiso y, si no encuentra coincidencia, guarda el gasto sin vínculo.
- Impacto: disminuye el efectivo calculado, pero el aporte no aumenta el progreso calculado de la meta; puede registrarse una operación distinta de la intención del usuario.
- Causa: el alta por voz evita las reglas de vinculación usadas por el flujo convencional.
- Corrección recomendada: unificar la operación de aporte y exigir una meta válida y confirmada. No resolverlo simplemente cambiando EXPENSE por INCOME: mover dinero propio a ahorro tampoco debe inventar ingresos externos.
- Prueba necesaria: confirmar un aporte, verificar meta y efectivo conjuntamente, rechazar destino inexistente o ambiguo y probar edición/eliminación del aporte.

### AUD-H03 — Borrar el movimiento de un servicio deja el recibo como pagado

- Confianza: alta en el trazado estático.
- Evidencia: `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/DeleteMovementUseCase.kt:49`; `ObserveMonthlyServiceReceiptsUseCase.kt:44` y `UpdateMovementUseCase.kt:52`, del mismo directorio; `core/data/src/main/kotlin/pe/kipu/core/data/local/entity/MonthlyServiceReceiptEntity.kt:20`; `feature/home/src/main/java/pe/kipu/feature/home/ui/MonthlyReceiptsCard.kt:102`.
- Flujo: pagar Luz → eliminar o cambiar de tipo el movimiento desde Movimientos → volver a Inicio.
- Comportamiento: la eliminación no reconcilia el recibo. El observador conserva `isPaid` aunque el movimiento no exista; el total visual usa el monto referencial cuando falta el monto real. Tampoco valida el tipo del movimiento para mostrarlo como pago.
- Ejemplo: Luz referencial S/ 45, pago real S/ 55; al borrar el movimiento, el recibo puede seguir como pagado y el total usar S/ 45, aunque el gasto haya desaparecido.
- Impacto: contradicción entre pagos mensuales, historial y saldo.
- Causa: el estado de pago queda persistido independientemente del movimiento, sin reconciliación en todas las rutas de modificación.
- Corrección recomendada: aplicar una política explícita y transaccional para eliminación/cambio de tipo/fecha de movimientos vinculados; no mostrar una referencia como importe efectivamente pagado.
- Prueba necesaria: pagar → editar monto → eliminar → reabrir; convertir el pago en ingreso y moverlo de mes. Verificar tanto Room como Inicio.

### AUD-H04 — Fusionar duplicados puede dejar un consumo de reserva asociado a un movimiento eliminado

- Confianza: alta en el trazado estático.
- Evidencia: `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/ResolveDuplicateMovementUseCase.kt:17`; `CalculateReserveBalanceUseCase.kt:18`, del mismo directorio; `core/data/src/main/kotlin/pe/kipu/core/data/local/entity/ReserveEventEntity.kt:19`; `feature/movements/src/main/java/pe/kipu/feature/movements/presentation/MovementsViewModel.kt:306`.
- Flujo: detectar duplicados → fusionar, cuando el registro que se elimina tiene un uso de reserva.
- Comportamiento: la fusión elimina directamente el movimiento más reciente, sin reconciliar o transferir reserva y otros vínculos. Los eventos de reserva se conservan deliberadamente sin clave foránea y siguen participando del saldo.
- Impacto: historial y reserva dejan de representar la misma operación. La ruta normal de eliminación sí registra una reversa; la fusión no utiliza esa lógica.
- Causa: existen rutas de borrado con efectos contables distintos.
- Corrección recomendada: fusionar movimiento, reserva y vínculos de forma atómica. Decidir qué metadatos conserva el sobreviviente; no revertir ciegamente un gasto real ni dejar vínculos huérfanos.
- Prueba necesaria: fusionar duplicados con reserva, pago mensual y sobre, en ambos órdenes temporales; verificar una sola operación, reserva correcta y vínculos válidos.

## MEDIUM

### AUD-M01 — El reajuste valida el gasto de la propuesta, no el gasto vigente al confirmar

- Confianza: alta en la ausencia de revalidación; reproducción concurrente pendiente.
- Evidencia: `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/ApplyRecoveryPlanUseCase.kt:19`; `RegisterUnexpectedExpenseUseCase.kt:27` y `CreateManualMovementUseCase.kt:47`, del mismo directorio.
- Flujo: previsualizar una compra → registrar otro gasto o cambiar el estado financiero → confirmar la propuesta anterior.
- Comportamiento: comprueba que el límite vigente coincida con el límite original, pero compara el nuevo límite contra `spentAmount` incluido en la propuesta. No vuelve a consultar el gasto vigente. El movimiento se registra antes de aplicar el reajuste; el uso de reserva tampoco valida el saldo agregado actual.
- Impacto: una propuesta aparentemente vigente puede reducir un sobre por debajo de su gasto real o consumir una reserva que ya cambió.
- Corrección recomendada: recalcular dentro de la transacción y comparar una versión del estado financiero, no solamente el límite del sobre. Si cambió la propuesta, solicitar nuevamente confirmación.
- Prueba necesaria: propuesta con gasto S/ 20 y límite nuevo S/ 60; registrar otro gasto que lleve el acumulado a S/ 80 antes de confirmar. Debe rechazarse o regenerarse sin escrituras parciales.

### AUD-M02 — El encabezado semanal/mensual muestra un importe calculado por día

- Confianza: alta en el cálculo y su enlace a la UI; inspección visual pendiente.
- Evidencia: `core/domain/src/main/kotlin/pe/kipu/core/domain/usecase/CalculateCycleAvailableUseCase.kt:34`; `feature/home/src/main/java/pe/kipu/feature/home/HomeScreen.kt:705` y `:746`; `feature/home/src/main/java/pe/kipu/feature/home/presentation/HomeCycleText.kt:6`.
- Flujo: Inicio con ciclo semanal o mensual.
- Comportamiento: divide el restante del ciclo entre sus días restantes y muestra ese resultado bajo «DISPONIBLE ESTA SEMANA» o «DISPONIBLE ESTE MES».
- Ejemplo: S/ 700 restantes y siete días producen S/ 100 en una tarjeta titulada disponible semanal.
- Impacto: confusión entre el total del período y la recomendación diaria.
- Corrección recomendada: mostrar el restante del ciclo bajo ese título, o identificar inequívocamente el importe como recomendación por día. Conservar separado el saldo mensual real.
- Prueba necesaria: casos diario/semanal/mensual con valores distintos, validando número, unidad temporal y descripción de accesibilidad juntos.

## LOW

### AUD-L02 — Fallo no reproducido en la repetición de una prueba de un componente sin callers de producción

- Confianza: alta en el fallo de la primera ejecución; causa raíz todavía no confirmada.
- Evidencia: `app/src/androidTest/java/pe/kipu/app/HighControlsSemanticsTest.kt:111`; extracto y huella del resultado original en [Verificación](VERIFICATION_2026-09-09.md); `core/designsystem/src/main/java/pe/kipu/core/designsystem/component/KipuSpeedDialFab.kt:62`.
- Flujo: prueba aislada del componente `KipuSpeedDialFab` → pulsar Atrás. La búsqueda de `KipuSpeedDialFab` y `SpeedDialAction` en los archivos Kotlin del repositorio encontró únicamente la definición y las pruebas `HighControlsSemanticsTest` y `MediumReceiptHomeAccessibilityTest`, sin callers de producción. Inicio utiliza otros botones.
- Resultado del 8 de septiembre: tras `Espresso.pressBack()` y `waitForIdle()`, seguía existiendo el nodo «Cerrar menú de registro». La prueba falló en la línea 114; las otras 43 pruebas pasaron.
- Impacto: inestabilidad de la verificación de un componente aparentemente sin uso en producción. No demuestra que el botón Atrás de la app publicada falle. La suite repetida el 9 de septiembre pasó 44/44 sin modificar código ni pruebas.
- Causa raíz: pendiente. El componente sí declara `dismissOnBackPress = true` y un callback de cierre; por eso no sería correcto atribuirlo a la ausencia de manejador.
- Corrección recomendada: reproducir con Atrás físico/gesto y ejecutar de nuevo la prueba sin modificar sus aserciones. No agregar esperas arbitrarias para ocultar el fallo.
- Prueba necesaria: repetición aislada y en suite, con menú abierto y ventana enfocada, comprobando cierre y permanencia en la pantalla.

### AUD-L01 — La política de privacidad muestra una nota editorial de contacto

- Confianza: alta.
- Evidencia: `feature/profile/src/main/java/pe/kipu/feature/profile/PrivacyPolicyScreen.kt:118`.
- Flujo: Perfil → política de privacidad.
- Comportamiento: el contacto incluye literalmente «actualizar antes de publicar en Play Store».
- Impacto: contenido de preparación visible al usuario; el código no demuestra que el buzón mostrado haya sido validado.
- Corrección recomendada: confirmar un canal de contacto real y sustituir la nota editorial antes de publicación. Esta auditoría no comprobó la existencia ni la recepción del correo.
- Prueba necesaria: revisar el texto final y confirmar recepción mediante una comprobación humana autorizada.

## Cómo funciona la app

Kipu está dividida en la aplicación principal, dominio, datos, sistema visual y nueve módulos de funcionalidades. La interfaz es Jetpack Compose; los ViewModels coordinan casos de uso, y los repositorios guardan información en Room y preferencias en DataStore. Room está en versión 22.

El plan configura ingresos estimados, gastos de referencia, sobres, ciclo y reserva mensual. Los ingresos estimados no deben confundirse con ingresos efectivamente registrados. Inicio combina movimientos confirmados, plan, sobres, compromisos y eventos de reserva para calcular sus tarjetas.

Los gastos e ingresos entran manualmente, mediante interpretación de voz o revisión de comprobantes compartidos. Los pagos de servicios permiten usar el importe real. La compra imprevista requiere selección explícita y muestra cobertura y posibles recortes; estos últimos necesitan confirmación. La reserva usa un historial de aportes/usos/reversas y su saldo no se reinicia por consultar un nuevo mes.

El reajuste actual es una regla determinista: recorre sobres predeterminados de Gastos hormiga, Ocio y Familia (`BuildUnexpectedExpenseRecoveryPlanUseCase.kt:51`). No es un optimizador general de todos los sobres ni garantiza llegar a fin de mes. No propone recortar directamente los sobres predeterminados de Comida y Transporte, pero AUD-H01 limita la protección efectiva de esos gastos.

## Mejoras recomendadas, separadas de los fallos

1. Presentar cuatro conceptos con etiquetas distintas: efectivo real, reserva, gastos esenciales pendientes y margen libre; añadir recomendación diaria/semanal sin sustituir el resumen mensual.
2. Clasificar sobres según prioridad y mínimo protegido, incluyendo los personalizados. Revisar si «Familia» es realmente prescindible para cada usuario; el código actual usa identificadores fijos.
3. En el reajuste, mostrar antes/después, período afectado, déficit no resuelto y consecuencias. No prometer recuperación por reducir un límite si ya falta efectivo.
4. Unificar estados de guardado, bloqueo de doble toque y mensajes de error en manual, voz, recibos y menú; probar cierres durante guardado.
5. Añadir pruebas cruzadas: pagar/editar/eliminar/fusionar; voz/metas; reserva/cambio de mes; gasto nuevo/propuesta abierta. Las pruebas aisladas existentes no cubren todas estas combinaciones.
6. Evaluar una exportación consistente en un instante: `BuildUserDataSnapshotUseCase.kt:36` lee repositorios secuencialmente, por lo que debe probarse una escritura concurrente durante la exportación.
7. Revisar rendimiento con un historial grande antes de optimizar: `ObserveHomeInsightsUseCase.kt:49` combina colecciones completas y vuelve a calcular resúmenes. No se midió rendimiento en esta auditoría.
8. Planificar la migración de APIs obsoletas de iconos/pruebas Compose, sin mezclarla con correcciones financieras ni actualizar dependencias solamente para eliminar advertencias.

## Documentación que necesita matices

Estas observaciones se refieren a `bc0547e`, antes de la actualización documental. El 9 de septiembre se añadieron esos matices a PROJECT_STATE y se separaron resultados históricos y actuales. Esto corrige la descripción, **no los hallazgos del código**.

- `docs/ai/PROJECT_STATE.md:81` afirma protección de Comida/Transporte y confirmación vigente: el orden del reajuste evita esos sobres, pero la cobertura no separa el dinero comprometido y la vigencia solo comprueba límites, no nuevos gastos.
- `docs/ai/PROJECT_STATE.md:83` describe reconciliación al borrar: la ruta de fusión de duplicados no aplica esa reconciliación.
- Los 44/44 y 43/43 del 30 de agosto en `docs/ai/PROJECT_STATE.md:56` son resultados históricos, no evidencia de la ejecución del 8–9 de septiembre. Deben conservarse con fecha y añadirse los resultados nuevos sin borrar fallos anteriores.

## Verificación

- Primera ejecución conectada: 44 pruebas de app, 43 aprobadas y una fallida; terminó `BUILD FAILED`. Extracto y SHA-256 conservados en [Verificación](VERIFICATION_2026-09-09.md); no se incorporaron logs del dispositivo al repositorio.
- `core:data` no llegó a ejecutarse en esa primera corrida. Su XML de 43 pruebas, fechado el 30 de agosto, es histórico.
- Primera ejecución local del 9 de septiembre: dominio 400 pruebas, cero fallos/errores/omisiones; la verificación conjunta quedó incompleta por dependencias ausentes en modo offline.
- Segunda ejecución conectada, 9 de septiembre: **43/43 core:data y 44/44 app PASS**. Procedencia, fechas y huellas de los XML en [Verificación](VERIFICATION_2026-09-09.md). El fallo previo no se borró ni se dio por corregido.
- Verificación local completa: **523/523 PASS** (400 dominio, 10 app, 70 datos, 5 sistema visual, 14 Inicio, 1 cuentas compartidas, 12 Movimientos y 11 comprobantes). Los XML tienen fecha 9 de septiembre; algunos resultados del mismo día se reutilizaron por estar actualizados. Los módulos sin pruebas unitarias propias figuran `NO-SOURCE`, no como pruebas aprobadas.
- `lintDebug`: **PASS, sin errores bloqueantes**, con **64 advertencias en los informes de módulos**. Incluyen convenciones de Modifier, recursos, textos y avisos de versiones disponibles; no equivalen a 64 fallos funcionales. No se actualizaron versiones para silenciarlas. El informe de app contiene 34 advertencias.
- Comando final: `./gradlew --no-daemon --max-workers=1 --continue :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest :core:domain:test testDebugUnitTest lintDebug`. Resultado: **BUILD SUCCESSFUL en 8 min 21 s**.
- Recorrido manual con el APK recién compilado, Moto G24/Android 14, 720×1612, fuente 1.0: bienvenida → abrir wizard → volver → Inicio → Registrar movimiento → formulario «Registrar Gasto» → Atrás → pantalla Movimientos sin registros. No se guardaron importes ni un plan. El recorrido se contrastó con árboles de UI y una captura local de Movimientos; estos artefactos de sesión no son necesarios para leer este informe. La primera lectura de UI al lanzar la app devolvió raíz nula; una segunda lectura con la actividad ya visible permitió continuar.
- Se dejó instalado el APK debug y se volvió a Inicio después de la revisión. El onboarding se recorrió parcialmente; no se introdujeron datos financieros. La comprobación manual siguió la guía `android_ui_verification`: verificar coordenadas y árboles de UI antes de pulsar, y contrastar el resultado con una captura.

## Resumen ejecutivo

- Hallazgos de esta revisión: **0 CRITICAL demostrados, 4 HIGH, 2 MEDIUM y 2 LOW**. Los riesgos estáticos no se deben presentar como defectos reproducidos en el teléfono.
- Principales riesgos: cobertura de imprevistos sin proteger obligaciones pendientes; inconsistencia de pagos tras eliminar movimientos; falta de reconciliación en duplicados y aportes por voz.
- Las 87 pruebas instrumentadas y 523 unitarias de la ejecución final pasaron. El fallo previo de una prueba de un componente sin callers de producción queda documentado, sin afirmar que fue corregido.
- Veredicto: la app puede seguir en pruebas internas controladas, pero estos resultados no justifican declarar resuelta la consistencia financiera ni certificar producción. Priorizar los HIGH antes de confiar en recomendaciones de gasto con datos reales.

## Matriz de cobertura

| Área | Qué se comprobó | Límites |
|---|---|---|
| Dominio financiero | Lectura de cálculos y flujos; 400 pruebas JVM | Los escenarios cruzados HIGH necesitan pruebas específicas |
| Persistencia | 43 pruebas en dispositivo, incluyendo migraciones y transacciones | No prueba todas las rutas combinadas de edición/fusión |
| Interfaz y navegación | 44 pruebas en dispositivo; recorrido manual sin registrar dinero | Un teléfono y fuente 1.0; no inspección visual de todos los estados |
| Calidad estática | lint de módulos sin errores bloqueantes | 64 advertencias; no es auditoría de seguridad completa |
| Voz y OCR | Trazado de entradas y persistencia; pruebas existentes | No reconocimiento real con micrófono ni comprobantes bancarios reales |
| Privacidad y exportación | Lectura de manifiesto, política y snapshot | Sin evaluación externa de proveedores ni prueba de exportación concurrente |

## Código potencialmente eliminable

`core/designsystem/src/main/java/pe/kipu/core/designsystem/component/KipuSpeedDialFab.kt`: la búsqueda de los símbolos `KipuSpeedDialFab` y `SpeedDialAction` en fuentes Kotlin, excluyendo salidas de build y el repositorio externo de skills, encontró solamente esta definición y las pruebas `HighControlsSemanticsTest` y `MediumReceiptHomeAccessibilityTest`. Candidato a eliminar o a documentar como componente reservado; no se eliminó. No extrapolar el fallo de su prueba a la UI actual de Inicio.

## Límites y siguiente orden de trabajo

Esta revisión no certifica ausencia de errores ni preparación para producción. No se usaron cuentas bancarias, credenciales de publicación ni Play Console. No se hicieron compras reales. No están verificadas aquí la recepción real de notificaciones bancarias, la precisión de reconocimiento con distintas voces, todos los tamaños de pantalla, TalkBack audible, la recuperación tras muerte del proceso ni el rendimiento con grandes volúmenes.

Orden recomendado de lotes independientes: (1) reconciliación de pagos y duplicados, (2) contabilidad de aportes a metas, (3) cobertura real y confirmación transaccional de reajustes, (4) claridad de importes, (5) contenido, componentes sin uso y estabilidad de pruebas. Son propuestas: no se implementó ninguna.

Archivos de código modificados: ninguno. Commits creados: ninguno. Se descargaron dependencias de las versiones configuradas y se ejecutaron pruebas; el informe original se guardó fuera del repositorio y esta copia documental se incorpora el 9 de septiembre. Se conserva el cambio preexistente en `docs/ai/external-skills/repository`.
