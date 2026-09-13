# Documentación de Kipu

Revisión: **12 septiembre 2026**. Alcance: documentación propia del proyecto; se excluyen el repositorio externo de skills, salidas de build y fixtures de pruebas. Las correcciones actuales se siguen en Remediación; los resultados del 9 septiembre son la línea base histórica.

## Por dónde empezar

| Necesidad | Referencia vigente |
|---|---|
| Preparar y ejecutar el proyecto | [README principal](../README.md) |
| Estado, riesgos y próximo trabajo | [PROJECT_STATE](ai/PROJECT_STATE.md) |
| Correcciones actuales, RED/GREEN y pendientes | [Remediación de septiembre](qa/REMEDIATION_2026-09-09.md) |
| Entender la app sin programar | [Guía de uso](qa/KIPU_USER_GUIDE.md) |
| Evidencia de errores y mejoras | [Auditoría del 9 de septiembre](qa/KIPU_AUDIT_2026-09-09.md) |
| Comandos, conteos y fallo conservado | [Verificación del 8–9 de septiembre](qa/VERIFICATION_2026-09-09.md) |
| Arquitectura y navegación | [Mapa visual](ai/MAPA_VISUAL_KIPU.artifact.md) |
| Pruebas funcionales pendientes | [Checklist E2E](ai/E2E_QA_CHECKLIST.md) |
| Firma/publicación: trabajo humano pendiente | [Internal testing](release/INTERNAL_TESTING.md), [Play Store](release/PLAY_STORE.md) |
| Privacidad: borrador y validaciones pendientes | [Markdown](release/PRIVACY_POLICY.md), [HTML](privacy/index.html) |

## Documentos de trabajo

[AGENTS](../AGENTS.md), [workflow](ai/KIPU_AI_WORKFLOW.md), [ECC](ai/ECC_INTEGRATION.md), [TDD](ai/TDD_CHECKLIST.md) y [seguridad](ai/SECURITY_CHECKLIST.md) son guías de trabajo, no certificados de cumplimiento. Una casilla histórica no demuestra que un caso nuevo esté cubierto. Los conteos actuales se mantienen en Remediación y PROJECT_STATE; Verificación conserva la línea base del 9 septiembre.

Las seis referencias de [arquitectura](ai/skills/android-clean-architecture.md), [Kotlin](ai/skills/kotlin-patterns.md), [coroutines](ai/skills/coroutines-flow.md), [parsers](ai/skills/receipt-parser-rules.md), [TDD](ai/skills/tdd-workflow.md) y [seguridad](ai/skills/security-review.md) contienen patrones generales. Sus ejemplos no son un inventario de clases, dependencias instaladas o funciones implementadas de Kipu. Consultar el código y las referencias vigentes antes de reutilizarlos; esta actualización conserva sus ejemplos técnicos.

## Históricos: no usar como estado actual

- [Auditoría de junio](ai/AUDIT_REPORT_2026-06-21.md).
- [Revisión funcional de junio](qa/KIPU_APP_FUNCTIONAL_MAP_E2E_2026-06-30.md).
- [Explicación extensa de julio](qa/KIPU_LOGICA_APP_PARA_NO_TECNICOS_2026-07-11.md), sustituida como guía vigente por KIPU_USER_GUIDE.
- [Auditoría y correcciones PLAN de agosto](qa/KIPU_PLAN_WIZARD_AUDIT_2026-08-01.md).
- El historial de fases dentro de PROJECT_STATE conserva fechas y resultados previos. `PLAN-H01` y `AUD-H01` son hallazgos distintos; no reutilizar IDs ni mezclar sus cierres.

## Cómo mantener coherencia

1. Cambiar un hallazgo a corregido solo con cambio de código y prueba específica; registrar fallo anterior, repetición, fecha y revisión.
2. Actualizar PROJECT_STATE y la guía del flujo afectado, además de las pruebas pendientes.
3. Separar implementado, verificado, pendiente de corrección y pendiente de revisión humana. No usar un gráfico «100 %» ni «solo falta publicar» mientras existan riesgos abiertos.
4. Validar enlaces relativos del repositorio; no depender de rutas temporales, cachés o capturas de una sesión para entender el informe.
5. No editar artefactos generados ni el repositorio externo para aparentar que la documentación propia está actualizada.
6. En un cambio solo documental, comprobar contenido, enlaces y diff; no presentar resultados históricos de Gradle como una ejecución nueva.

## Actualización documental histórica del 9 septiembre

El 9 de septiembre se actualizaron 17 documentos y se añadieron este índice, la guía de uso, la auditoría y el registro de verificación. La revisión separó históricos de estado vigente, corrigió rutas/comandos y alineó la preparación de publicación con sus pendientes reales. Las seis referencias técnicas generales y todo el material de terceros en `docs/ai/external-skills/` se conservaron sin cambios.

Segunda comprobación: 93 enlaces internos válidos, HTML estructuralmente revisado, `git diff --check` sin errores y `assembleDebug` offline PASS. No se repitieron las suites del dispositivo por este cambio ni se corrigieron hallazgos de producto.
