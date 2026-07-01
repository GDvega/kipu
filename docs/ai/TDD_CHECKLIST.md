# TDD_CHECKLIST.md — Kipu

Checklist accionable para pruebas en Kipu, alineado con ECC y `AGENTS.md`.

---

## 1. Cuándo aplica TDD

### Sí aplica (obligatorio)

- UseCases con cálculo financiero
- Parsers de comprobantes (Yape, Plin)
- Validadores de input (OCR, intents, importación)
- Detección de duplicados, gastos hormiga, alertas
- Mappers con transformación no trivial
- Reglas de negocio en `core/domain`

### No aplica (excepción ECC justificada)

- Documentación Markdown
- Placeholders de UI sin lógica
- Temas, colores, tipografía
- Navegación sin estado ni reglas
- Configuración Gradle sin lógica

**Si no aplica:** declarar la excepción en el reporte ECC y usar otra verificación (`assembleDebug`, lint, instrumentado).

---

## 2. Ciclo RED → GREEN → REFACTOR

| Paso | Acción |
|------|--------|
| **RED** | Escribir test que exprese el comportamiento esperado; ejecutar y confirmar fallo por la causa correcta. |
| **GREEN** | Implementar el mínimo para que pase. |
| **REFACTOR** | Simplificar solo si reduce complejidad real; re-ejecutar tests. |

**Regla:** no modificar un test correcto para ocultar un fallo de producción.

---

## 3. Comandos de verificación

| Comando | Cuándo usar |
|---------|-------------|
| `./gradlew :<módulo>:testDebugUnitTest` | Test focalizado en un módulo (ej. `:core:domain:testDebugUnitTest`) |
| `./gradlew testDebugUnitTest` | Suite unitaria de todos los módulos con tests |
| `./gradlew assembleDebug` | **Siempre** — confirma compilación (no sustituye unit tests de lógica) |
| `./gradlew connectedDebugAndroidTest` | Flujos instrumentados; requiere emulador o dispositivo |

### Ejemplos por módulo futuro

```bash
./gradlew :core:domain:testDebugUnitTest
./gradlew :core:data:testDebugUnitTest
./gradlew connectedDebugAndroidTest   # smoke MainActivity, permisos
```

---

## 4. Módulos que requieren pruebas obligatorias

| Módulo de lógica | Módulo Gradle típico | Tipo de test |
|------------------|----------------------|--------------|
| Parser Yape | `core:domain` o `core:data` | Unit (JVM) |
| Parser Plin | `core:domain` o `core:data` | Unit (JVM) |
| Cálculo de sobres | `core:domain` | Unit (JVM) |
| Disponible diario | `core:domain` | Unit (JVM) |
| Gastos hormiga | `core:domain` | Unit (JVM) |
| Duplicados | `core:domain` | Unit (JVM) |
| Metas de ahorro | `core:domain` | Unit (JVM) |
| Plan financiero negativo | `core:domain` | Unit (JVM) |

Los tests deben vivir en `src/test/` del módulo que contiene la lógica (preferir `core:domain` para UseCases puros).

---

## 5. Checklists detallados por módulo

### Parser Yape

- [ ] Extrae monto (`S/ 10.50`, `S/0.10`, miles con coma)
- [ ] Extrae destinatario
- [ ] Extrae fecha
- [ ] Extrae hora
- [ ] Extrae mensaje (opcional)
- [ ] Extrae número de operación
- [ ] Detecta comprobante inválido o vacío
- [ ] Maneja montos pequeños (`S/ 0.10`)
- [ ] Rechaza texto que no es comprobante Yape
- [ ] No persiste sin validación exitosa

### Parser Plin

- [ ] Extrae monto
- [ ] Extrae destinatario
- [ ] Detecta ausencia de mensaje (campo opcional)
- [ ] Sugiere categoría por historial (cuando exista)
- [ ] Maneja comprobante incompleto
- [ ] Rechaza texto que no es comprobante Plin
- [ ] Normaliza variaciones de formato Plin

### Sobres

- [x] Resta gasto de la categoría correcta
- [x] Calcula porcentaje usado del sobre
- [x] Detecta sobre ajustado (cerca del límite)
- [x] Detecta sobre excedido
- [x] Maneja sobre sin gastos (0 %)
- [x] No permite sobres con monto negativo

### Disponible diario

- [x] Calcula disponible semanal restante
- [x] Divide por días restantes de la semana
- [x] Maneja saldo negativo (alerta, no crash)
- [x] Maneja cero días restantes (evitar división por cero)
- [x] Redondeo consistente con moneda PEN

### Gastos hormiga

- [x] Detecta varios gastos pequeños en ventana de 48 h
- [x] No marca todo gasto pequeño como hormiga
- [x] Usa frecuencia, monto y categoría en la heurística
- [x] Genera alerta ámbar (no roja) si aún hay presupuesto
- [x] No alerta con un solo gasto aislado bajo umbral

### Duplicados

- [x] Detecta mismo monto
- [x] Detecta misma fecha aproximada (tolerancia configurable)
- [x] Detecta mismo destinatario
- [x] Permite fusionar con confirmación
- [x] Permite guardar como nuevo con confirmación
- [x] No fusiona automáticamente sin acción del usuario

### Metas de ahorro

- [x] Calcula progreso (`ahorrado / meta`) con límites 0–100 %
- [x] Maneja meta cero o negativa (error de validación)
- [x] Maneja ahorro superior a la meta (cap o indicador "completada")
- [ ] Calcula fecha estimada de cumplimiento (si aplica)
- [ ] Actualiza progreso al registrar movimiento vinculado

### Validación plan financiero negativo

- [x] Detecta cuando ingresos < gastos fijos + sobres + compromisos
- [x] Retorna resultado estructurado (no excepción genérica)
- [x] Incluye déficit calculado para mostrar al usuario
- [x] Maneja lista vacía de ingresos
- [ ] No permite guardar plan inválido sin confirmación explícita *(CRUD UI — Fase 12+; seed demo y [FinancialPlanRepository.save] no aplican ValidateFinancialPlan)*

---

## 6. Cobertura

| Ámbito | Objetivo |
|--------|----------|
| `core:domain` (UseCases, parsers) | **≥ 80 %** cuando se mida con JaCoCo |
| `core:data` (mappers) | **≥ 80 %** en mappers con lógica |
| UI / Compose | Tests de UI opcionales; priorizar lógica de dominio |

Medir cobertura a partir de Fase 5 (cuando exista `core:domain` con lógica).

---

## 7. Tabla fase → obligación TDD

| Fase | Nombre | TDD obligatorio | Comando mínimo |
|------|--------|-----------------|----------------|
| 0 | Shell multi-módulo | No | `assembleDebug` |
| 1 | Documentación de control | No | `assembleDebug` |
| 2 | Design system ampliado | No | `assembleDebug` |
| 3 | Domain base | Parcial (modelos puros) | `assembleDebug` |
| 4 | DI + presentation base | No | `assembleDebug` |
| 5 | DataStore + preferencias | Parcial | `:core:data:testDebugUnitTest` |
| 6 | Room + movimientos | Sí (mappers) | `testDebugUnitTest` |
| 7 | Parsers + OCR | **Sí** | `:core:domain:testDebugUnitTest` |
| 8 | Sobres | **Sí** | `:core:domain:testDebugUnitTest` |
| 9 | Disponible diario + hormiga | **Sí** | `:core:domain:testDebugUnitTest` |
| 10 | Duplicados | **Sí** | `:core:domain:testDebugUnitTest` |
| 11 | Compromisos / metas | **Sí** | `:core:domain:testDebugUnitTest` |
| 12 | Notificaciones | Parcial | `:core:domain:test` + `:core:data:testDebugUnitTest` |
| 13 | Exportar / eliminar datos | Parcial | `testDebugUnitTest` + seguridad |
| 14 | Onboarding | No | `assembleDebug` |
| 15 | Comprobantes UI | Parcial | `testDebugUnitTest` |
| 16 | Juntas + pulido | Según lógica nueva | Escalera completa |

> **Numeración canónica:** ver `docs/ai/PROJECT_STATE.md`.

---

## 8. Notificaciones (Fase 12) — TDD parcial

### Parsers ingreso (`YapeIncomeNotificationParser`, `PlinIncomeNotificationParser`)

- [x] Extrae monto y tipo `INCOME` desde fixture estándar
- [x] Extrae contraparte cuando está en el texto
- [x] Rechaza notificación de gasto/pago saliente
- [x] Rechaza texto sin señales de ingreso
- [x] `source == NOTIFICATION`, canal correcto

### UseCases

- [x] `ParseNotificationTextUseCaseTest` — enruta Yape vs Plin por `packageName`
- [x] `RegisterNotificationIncomeUseCaseTest` — guarda `PENDING_CONFIRMATION`; no auto-confirma; dedup pendiente
- [x] `ConfirmPendingNotificationMovementUseCaseTest` — promueve a `CONFIRMED`; bloquea si duplicado `CONFIRMED` sin resolución; MERGE descarta pending

### Data (opcional JVM)

- [x] `NotificationListenerBridgeTest` — combina title+text sin exponer contenido en producción

---

## Referencias

- `AGENTS.md` — sección 7 (reglas TDD)
- `docs/ai/skills/tdd-workflow.md` — ciclo RED-GREEN-REFACTOR detallado
- `docs/ai/skills/receipt-parser-rules.md` — reglas específicas de parsers
