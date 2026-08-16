# KIPU_AI_WORKFLOW.md — Flujo de trabajo con IA

Guía para el gestor humano y para cualquier IA programadora en Kipu.  
Leer junto con `AGENTS.md`, `PROJECT_STATE.md` y los checklists en `docs/ai/`.

---

## 1. Cómo usar este workflow

1. **Antes de pedir código:** lee `docs/ai/PROJECT_STATE.md` y `docs/ai/MAPA_VISUAL_KIPU.artifact.md` para conocer la fase actual y la arquitectura.
2. **Copia la plantilla de prompt** (sección 3) y rellena las 10 secciones.
3. **Indica skill obligatoria:** ECC Engineering System.
4. **Optimiza tokens:** Asegúrate de que la IA conozca la existencia del archivo `.aiexclude` en la raíz para ignorar ruido técnico.
5. **Activa skills adicionales** según el mapa (sección 2).
6. **Al recibir el resultado:** usa la plantilla de revisión (sección 4) o los comandos del gestor (sección 5).
7. **Al cerrar una fase:** actualiza `PROJECT_STATE.md`.

**Reglas de oro:**

- Una capacidad dominante por encargo.
- Archivos permitidos y prohibidos explícitos.
- No pedir "haz toda la app".
- Exigir contrato de salida LISTO/NO LISTO.

---

## 2. Mapa de skills en `docs/ai/skills/`

| Skill | Archivo | Activar cuando… |
|-------|---------|-----------------|
| Clean Architecture | `android-clean-architecture.md` | Nuevos módulos, capas, UseCases, repositorios, DI |
| Kotlin patterns | `kotlin-patterns.md` | Modelos, sealed classes, errores, convenciones Kotlin |
| Coroutines & Flow | `coroutines-flow.md` | StateFlow, SharedFlow, scopes, cancelación, Room+Flow |
| Receipt parsers | `receipt-parser-rules.md` | Parsers Yape/Plin, OCR, validación de comprobantes |
| TDD workflow | `tdd-workflow.md` | Cualquier lógica de comportamiento (UseCases, parsers) |
| Security review | `security-review.md` | Datos financieros, permisos, intents, backup, export/wipe |

**Skill externa (siempre):** `super-android-kotlin-firebase` — orquestador maestro para Android/Kotlin/Compose/Firebase.

**Skill metodológica:** ECC Engineering System (`ecc-engineering-system`) — ciclo, riesgo, verificación, contrato de salida.

**Skill de simplicidad (código):** Ponytail (`ponytail`) en nivel `full` — YAGNI, reutilización y cambio mínimo después de investigar el flujo real. Si no está disponible, aplicar `AGENTS.md` sección 8.1 directamente.

**Resumen ECC en Kipu:** `docs/ai/ECC_INTEGRATION.md`.

---

## 3. Plantilla de prompt completa

Copiar y rellenar. Las 10 secciones son obligatorias.

---

```markdown
# Skill obligatoria

Aplica ECC Engineering System:
- Investiga el repo antes de editar.
- Una capacidad dominante por vez.
- Cambio mínimo, sin refactors oportunistas.
- Verifica con comandos reales; no afirmes "listo" sin evidencia.
- Reporta al final: Cambio / Evidencia / Riesgos / Comprobaciones no ejecutadas / Estado LISTO o NO LISTO.

Aplica Ponytail `full` después de investigar:
- Omite necesidades especulativas y reutiliza lo existente.
- Prefiere stdlib, plataforma y dependencias instaladas antes de código nuevo.
- No añadas abstracciones o dependencias sin una necesidad actual demostrada.
- No simplifiques seguridad, validación, accesibilidad, arquitectura ni TDD.

Para Android/Kotlin sigue: dominio libre de Android, lógica fuera de Composables, sin `!!`, sin `GlobalScope`.

# Contexto

Proyecto: Kipu — app Android de finanzas personales para Perú.
Fase actual: [N] — [nombre]. Ver `docs/ai/PROJECT_STATE.md`.
Package: `pe.kipu.app`. minSdk 26, compileSdk 37, targetSdk 36.

Evidencia disponible:
- [archivos, módulos o comportamiento ya existente que la IA debe leer]
- Referencia visual: `docs/ai/MAPA_VISUAL_KIPU.artifact.md` (Arquitectura, Navegación, DB)

Riesgo dominante: [BAJO / MEDIO / ALTO] — [justificación en una línea]

# Objetivo

Comportamiento esperado observable:
1. [criterio verificable 1]
2. [criterio verificable 2]
…

Fuera de alcance:
- [lo que NO debe hacerse en este encargo]

# Archivos que puedes crear o modificar

- [lista explícita de paths o módulos]

# Archivos que NO debes tocar

- [paths prohibidos]
- Invariantes: [ej. ningún feature depende de otro feature]

# Reglas de arquitectura

- [capas afectadas, dependencias permitidas]
- Ver `AGENTS.md` sección 4.

# Reglas de seguridad

- [superficies sensibles del encargo]
- Ver `docs/ai/SECURITY_CHECKLIST.md` si aplica.

# Resultado esperado

- [entregables concretos: archivos, funciones, pantallas]

# Pruebas requeridas

- TDD: [sí — módulos y casos] / [no — justificar excepción ECC]
- Verificación obligatoria: `./gradlew assembleDebug` → PASS
- Adicionales: [comandos]

# Criterios de aceptación

- [ ] [checklist verificable]
- [ ] `./gradlew assembleDebug` PASS
- [ ] Sin violaciones de arquitectura ni seguridad

# Qué debes reportar al finalizar (formato ECC + Kipu)

1. Archivos creados/modificados
2. Supuestos tomados
3. Evidencia (comandos y resultados)
4. Riesgos residuales
5. Comprobaciones no ejecutadas
6. Estado: LISTO / NO LISTO
```

---

## 4. Plantilla de revisión

Usar después de que una IA entregue código. Ordenar hallazgos por severidad.

```markdown
# Revisión de entrega IA — Kipu

Encargo: [resumen]
Fase: [N]
Revisor: [humano / IA revisora]

## Hallazgos

### CRITICAL
- [ ] [Descripción + archivo:línea + acción requerida]
  - Acción: bloquear merge; corregir antes de continuar.

### HIGH
- [ ] [Descripción + archivo:línea]
  - Acción: corregir en este PR/encargo.

### MEDIUM
- [ ] [Descripción + archivo:línea]
  - Acción: corregir o registrar deuda con issue.

### LOW
- [ ] [Descripción + archivo:línea]
  - Acción: opcional / siguiente iteración.

## Checklist de revisión

- [ ] Respeta dependencias de módulos (`feature` ↛ `feature`)
- [ ] Sin lógica financiera en ViewModels/Composables
- [ ] Sin entidades Room expuestas a UI
- [ ] Sin logs de datos financieros
- [ ] Sin permisos innecesarios
- [ ] Sin secretos hardcodeados
- [ ] Tests creados si había lógica de comportamiento
- [ ] `./gradlew assembleDebug` PASS (evidencia adjunta)
- [ ] Contrato LISTO/NO LISTO presente y honesto

## Veredicto

[ ] APROBADO  [ ] CORRECCIONES REQUERIDAS  [ ] RECHAZADO
```

---

## 5. Comandos del gestor humano

Frases cortas para dirigir la sesión sin reescribir la plantilla completa:

| Comando | Acción esperada de la IA |
|---------|--------------------------|
| **Actualiza resumen** | Actualizar `PROJECT_STATE.md` con lo hecho, hallazgos y próxima fase |
| **Dame siguiente prompt** | Generar plantilla de sección 3 lista para la siguiente fase/tarea |
| **Corrige error** | Diagnosticar con evidencia, cambio mínimo, re-verificar Gradle |
| **Revisa IA** | Ejecutar plantilla de revisión (sección 4) sobre el último diff |
| **Siguiente fase** | Proponer encargo de la fase N+1 según roadmap en `PROJECT_STATE.md` |

---

## 6. Ejemplo de prompt mínimo válido

> **Referencia únicamente — no ejecutar tal cual.**

```markdown
# Skill obligatoria
Aplica ECC Engineering System y Ponytail `full`. Reporta LISTO/NO LISTO.

# Contexto
Kipu Fase 2. `core/designsystem` existe con KipuTheme y KipuTopBar.
Riesgo dominante: BAJO.

# Objetivo
1. Crear componente `KipuPrimaryButton` reutilizable.
2. Corregir deprecación de KipuTopBar.

# Archivos permitidos
- `core/designsystem/**`

# Archivos prohibidos
- `feature/*`, `app/*`, Gradle

# Reglas de arquitectura
Solo `core/designsystem`. Sin dependencias nuevas.

# Reglas de seguridad
N/A (UI sin datos).

# Resultado esperado
- `KipuPrimaryButton.kt`
- `KipuTopBar.kt` actualizado

# Pruebas requeridas
TDD no aplica. `./gradlew assembleDebug` PASS.

# Criterios de aceptación
- [ ] Componente usado en preview
- [ ] Sin warning de deprecación en TopBar
- [ ] assembleDebug PASS
```

---

## Referencias rápidas

| Documento | Uso |
|-----------|-----|
| `AGENTS.md` | Reglas maestras del proyecto |
| `PROJECT_STATE.md` | Estado actual y roadmap |
| `TDD_CHECKLIST.md` | Cuándo y cómo testear lógica |
| `SECURITY_CHECKLIST.md` | Cuándo y cómo revisar seguridad |
| `ECC_INTEGRATION.md` | Resumen del ciclo ECC |
