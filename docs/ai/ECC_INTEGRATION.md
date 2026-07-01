# ECC_INTEGRATION.md — Cómo ECC gobierna Kipu

Resumen corto de la integración entre **ECC Engineering System** y el flujo de trabajo de Kipu.  
Documento de referencia; el detalle operativo está en `AGENTS.md` y `KIPU_AI_WORKFLOW.md`.

---

## Principio rector

> Una capacidad dominante por vez. Cambio mínimo. Evidencia antes de "listo".

ECC no reemplaza las reglas de Kipu (producto, arquitectura, seguridad); las **envuelve** con un ciclo verificable.

---

## Ciclo ECC en Kipu

```
Brief → Evidencia → Cambio mínimo → TDD → Revisión → Cierre (LISTO/NO LISTO)
```

| Paso | En Kipu |
|------|---------|
| Brief | Usar plantilla de 10 secciones en `KIPU_AI_WORKFLOW.md` |
| Evidencia | Leer `PROJECT_STATE.md`, código real, dependencias Gradle |
| Cambio mínimo | No refactors oportunistas; un módulo/feature por encargo |
| TDD | Obligatorio en parsers y UseCases; excepción justificada en UI/docs |
| Revisión | `SECURITY_CHECKLIST.md` si hay datos, permisos, OCR o persistencia |
| Cierre | Contrato de salida con comandos Gradle ejecutados |

---

## Clasificación de riesgo

| Nivel | Gate de verificación |
|-------|---------------------|
| Bajo | `assembleDebug` |
| Medio | + unit tests + checklist seguridad parcial |
| Alto | + aprobación humana + checklist seguridad completo |

---

## Escalera de verificación

1. Test unitario focalizado (`:<módulo>:testDebugUnitTest`)
2. Suite unitaria (`testDebugUnitTest`)
3. Compilación (`assembleDebug`) — **mínimo universal**
4. Lint (`lintDebug`)
5. Instrumentado (`connectedDebugAndroidTest`) — flujos críticos

---

## Skills ECC en `docs/ai/skills/`

No reescribir; activar según tarea:

| Skill | Cuándo |
|-------|--------|
| `android-clean-architecture.md` | Módulos, capas, UseCases, repositorios |
| `kotlin-patterns.md` | Idioma Kotlin, sealed classes, inmutabilidad |
| `coroutines-flow.md` | Flow, StateFlow, scopes, cancelación |
| `receipt-parser-rules.md` | Parsers Yape/Plin, OCR, validación |
| `tdd-workflow.md` | Ciclo RED-GREEN-REFACTOR |
| `security-review.md` | Datos sensibles, permisos, logs, backup |

**Skill externa obligatoria:** `super-android-kotlin-firebase` para orquestar subskills de Android/Compose/Firebase.

**Skill metodológica obligatoria:** ECC Engineering System (`ecc-engineering-system`) para el ciclo de trabajo.

---

## Contrato LISTO / NO LISTO

**LISTO** = criterios de aceptación cumplidos + verificaciones obligatorias PASS + reporte ECC completo.

**NO LISTO** = cualquier criterio fallido, verificación no ejecutada cuando era obligatoria, o riesgo alto sin aprobación.
