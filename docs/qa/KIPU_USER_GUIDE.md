# Cómo funciona Kipu

Guía vigente al **12 septiembre 2026**, contrastada con la base `bc0547e` y las correcciones `9d03f3d`. [Índice](../README.md) · [Estado y riesgos](../ai/PROJECT_STATE.md) · [Remediación](REMEDIATION_2026-09-09.md).

## Qué hace y qué no hace

Kipu registra tus ingresos y gastos en el teléfono y los compara con un plan. No entra a tu banco, no hace pagos ni mueve dinero real entre cuentas. Registrar una compra solo cambia tus registros. No usa un LLM para decidir qué puedes gastar ni garantiza llegar a fin de mes.

La interfaz llama **Cuentas compartidas** a los gastos en grupo. «Juntas» se conserva como nombre técnico de un módulo, no como nombre recomendado para usuarios.

## Primer uso

1. Abres la bienvenida «Arma tu plan» y eliges «Comenzar con mi plan».
2. El asistente recorre seis pasos: ingresos, gastos fijos, sobres, gastos hormiga, meta y resumen.
3. Configuras ingresos estimados y saldo inicial, gastos mensuales de referencia, límites de sobres, ciclo y aporte mensual previsto a la reserva. Puedes omitir la meta.
4. Revisas el resumen y guardas. Después puedes reabrir y modificar el plan desde Sobres; Inicio también tiene un acceso al paso de gastos.

Volver desde el asistente puede llevar a Inicio sin haber guardado un plan. No debe documentarse un botón «Configurar después» en la bienvenida: no es el flujo actual observado.

Ingresos **estimados** no son depósitos recibidos. El saldo inicial debe representar dinero ya disponible al empezar, sin duplicarlo luego como un nuevo ingreso. La reserva mensual configurada es una intención; el aporte real a la reserva se confirma en Inicio cuando hay saldo suficiente según el cálculo actual.

## Cómo interpretar los importes

| Concepto | Qué calcula hoy | Precaución |
|---|---|---|
| Efectivo real | Saldo inicial + ingresos confirmados − gastos confirmados | No consulta el saldo de bancos; depende de lo registrado |
| Reserva acumulada | Aportes/devoluciones − usos, considerando reversas | Es parte del efectivo, no dinero adicional |
| Efectivo sin reservar | Efectivo registrado − reserva positiva | No significa dinero libre de obligaciones; puede estar comprometido para recibos y necesidades |
| Restante del ciclo | Límites de sobres − gastos imputados al ciclo | No es el mismo concepto que efectivo real |
| Recomendación por día | Restante del ciclo / días restantes en semanal o mensual | Corregido el título a «POR DÍA ESTA SEMANA / ESTE MES»; no representa el total del período |
| Resumen mensual | Presupuesto de ingreso estimado y gasto confirmado del mes | Es una comparación con el plan, no una garantía de liquidez |

No sumar la reserva al efectivo real: eso duplicaría el mismo dinero. El saldo de efectivo y la reserva no se reinician al cambiar el mes; permanecen según el historial. Esto **no** significa que cada sobre tenga rollover individual: esa función no está implementada.

## Pagos mensuales: referencia frente a pago real

Si el plan estima Luz en S/ 45 y el recibo real es S/ 55, desde Pagos mensuales puedes registrar **55**. El gasto confirmado debe afectar el efectivo una sola vez y la referencia del plan puede continuar en **45**. También existe reconocimiento por voz de pagos de servicios, con revisión antes de confirmar.

**Corrección verificada en JVM, Room y tarjeta UI (AUD-H03):** al eliminar el pago, convertirlo en ingreso/pendiente o trasladarlo a otro mes de Lima, el recibo vuelve a pendiente. Cambiar solo el monto conserva el vínculo y muestra el pago real. Nunca se sustituye un pago inexistente por la referencia. Son verificaciones por capas; la marca sigue sin equivaler a una comprobación bancaria.

## Registrar ingresos y compras

- Manual: eliges ingreso/gasto, canal, importe y categoría; puedes asignar un sobre y marcar explícitamente una compra imprevista.
- Voz: el servicio Android convierte audio en texto; Kipu interpreta la transcripción por reglas locales y pide revisión. El reconocimiento del proveedor no se garantiza offline. No necesitas usar voz para registrar gastos.
- Comprobante: compartes o seleccionas una imagen existente; OCR local propone los campos. No hay captura con cámara propia.
- Notificaciones: acceso opcional; el listener filtra paquetes configurados y propone ingresos para confirmación. No se autoaprueban como movimientos definitivos.

Fuente para el límite del reconocimiento de voz: [API Android SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), consultada el 9 de septiembre. La [política documental](../release/PRIVACY_POLICY.md) explica OCR y métricas técnicas por separado.

## Qué pasa con una compra imprevista

1. Marcas la compra como imprevista; una compra cara no activa por sí sola un plan inteligente.
2. Kipu muestra cuánto propone cubrir con reserva respaldada y efectivo no comprometido; separa obligaciones pendientes, faltante previo y falta de efectivo.
3. Si falta cubrir un importe, propone recortes en los sobres predeterminados de Gastos hormiga, Ocio y Familia, en ese orden.
4. Seleccionas los ajustes y confirmas; también existe la opción de guardar sin reajustar.
5. La operación registra el gasto, el uso de reserva y los cambios aceptados. No transfiere dinero desde un banco ni obtiene financiación.

**Ejemplo verificado en pruebas, no una garantía:** efectivo S/ 1.000, reserva S/ 100, necesidades proyectadas pendientes S/ 800 y compra S/ 300. Kipu asigna S/ 100 de reserva y S/ 100 de efectivo no comprometido, y muestra S/ 100 por compensar. Una reserva nominal superior al efectivo no aumenta artificialmente la cobertura. Los recortes no resuelven por sí solos una falta de efectivo.

La proyección protege conservadoramente el ciclo actual completo y estima ciclos siguientes hasta fin de mes. Resta referencias de recibos ya pagados, no sus montos reales, porque estos ya descontaron efectivo. Los compromisos siguen las reglas de validación del plan; no se adivina si una deuda y un recibo representan la misma obligación, por lo que no debes duplicarlos.

El reajuste no recorta los límites de Comida o Transporte. Desmarca Familia si contiene gastos esenciales. Los límites aceptados seguirán vigentes en próximos ciclos hasta que los edites. Todo depende de que tus registros estén completos: no garantiza financiación de gastos desconocidos ni cuenta ingresos futuros como efectivo.

Al confirmar se releen los datos y la fecha: si cambiaron, no se guarda nada y debes volver a revisar. Un recorte no puede quedar por debajo del gasto vigente, incluida la propia compra. Si resulta incompatible, se revierte la compra junto con sus ajustes; puedes volver y elegir guardar sin reajustar, con el déficit mostrado. No se acepta silenciosamente una propuesta nueva (AUD-M01).

## Sobres y acumulación

El ciclo puede ser diario, semanal o mensual. Un movimiento afecta cero o un sobre; el vínculo explícito `envelopeId` prevalece. Sin vínculo, la categoría solo permite imputación cuando corresponde de forma no ambigua; no se distribuye automáticamente entre varios sobres de igual categoría.

El cálculo excluye movimientos ya vinculados a ciertos pagos mensuales o gastos compartidos para evitar doble imputación. El límite del sobre no es una cuenta bancaria separada ni un cargo real. Cambiarlo no crea efectivo. La reserva no utilizada y el saldo de efectivo se mantienen por historial; los límites de sobres se aplican al nuevo ciclo sin rollover individual.

## Metas y duplicados

El progreso de metas suma ahorro declarado e ingresos confirmados vinculados. **Corrección verificada en dominio, HomeVM y Room (AUD-H02):** aportar manualmente o por voz actualiza el ahorro declarado sin crear ingreso ni gasto ni mover dinero. No vuelvas a declarar un ingreso ya vinculado. Voz requiere una única meta activa en soles; si no existe o el nombre coincide con varias, conserva la confirmación con un error. La locución con el proveedor real no se ha repetido en esta ronda. Los registros históricos no se convierten automáticamente: revisa si cada gasto antiguo era real antes de modificarlo.

La detección de duplicados requiere una decisión humana. **Corrección verificada en dominio y Room (AUD-H04):** fusionar relee ambos movimientos y reconcilia los vínculos compatibles en una transacción. Si cambiaron o sus vínculos entran en conflicto, no los borra. Conserva un solo uso de reserva mediante reversas auditables; casos con devoluciones o múltiples gastos compartidos requieren revisión manual.

## Cuentas compartidas, exportación y privacidad

Cuentas compartidas registra participantes, quién pagó y el reparto; no cobra deudas automáticamente. Perfil permite exportar JSON v5 (incluye reserva, recibos y auditoría) o CSV de movimientos, y borrar datos con doble confirmación. Un archivo exportado puede contener información sensible y, si lo compartes, queda también fuera de Kipu.

La base y preferencias están excluidas del backup automático en la configuración actual. No se verificó restauración/importación como un flujo soportado: **exportar no significa poder restaurar desde la app**. Contacto de privacidad: vegacisneros.gd@gmail.com, proporcionado por el responsable y comprobado en la pantalla. No se ha probado entrega de mensajes desde esta revisión.

## Qué se verificó

El 12 de septiembre quedaron verificadas 551 pruebas unitarias, 48 de la app y 55 de datos, además de compilación debug y lint sin errores bloqueantes (50 entradas de advertencia). Las pruebas de imprevistos verifican que los importes completos caben junto a los avisos, tanto en voz como en escritura. Se conserva el fallo previo no reproducido de Atrás en un componente sin uso en producción. El [seguimiento de remediación](REMEDIATION_2026-09-09.md) distingue cobertura real, límites y tareas pendientes; no existe una certificación de «cero bugs». Los resultados del [9 de septiembre](VERIFICATION_2026-09-09.md) son históricos.
