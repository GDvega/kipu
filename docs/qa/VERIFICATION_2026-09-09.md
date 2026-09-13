# Verificación de Kipu — 8–9 septiembre 2026

Código auditado: `bc0547e` (`feat: integra reservas y recuperacion de gastos imprevistos`). Ruta de trabajo: `/media/toshiba/gerson/PROYECTOS/proyectos/kipu`. Zona del registro humano: `America/Lima`; los timestamps de XML instrumentado están en UTC. Este registro conserva fallos y repeticiones; no documenta una corrección de código.

## Secuencia y resultados

| Ejecución | Resultado | Interpretación |
|---|---|---|
| 8 sep, Gradle offline | FAIL en preparación: falta `foojay-resolver-convention:1.0.0` | No llegaron a comenzar las pruebas |
| 8 sep, descarga autorizada y suites conectadas | App: 44 pruebas, 43 PASS y 1 FAIL; `BUILD FAILED` en 16 min 59 s | Datos no se ejecutó porque la corrida se detuvo en app |
| 9 sep, verificación local offline con `--continue` | 400 pruebas de dominio PASS; comando global FAIL por otras dependencias ausentes | No equivale a fallo de los cálculos de dominio ni a lint aprobado |
| 9 sep, corrida final con descarga autorizada | Datos 43/43, app 44/44, unitarias 523/523 y lint PASS | `BUILD SUCCESSFUL` en 8 min 21 s; 689 tareas: 179 ejecutadas, 510 actualizadas |

Se descargaron las dependencias requeridas, sin cambiar las versiones declaradas ni usar `--refresh-dependencies`. No se modificaron aserciones, código, credenciales de publicación ni configuración de firma; no hubo acciones en Play Console.

### Comando de la corrida final

```bash
./gradlew --no-daemon --max-workers=1 --continue \
  :core:data:connectedDebugAndroidTest \
  :app:connectedDebugAndroidTest \
  :core:domain:test testDebugUnitTest lintDebug
```

`--continue` permite seguir con tareas independientes tras un fallo; no convierte un resultado fallido en PASS. El APK debug se compiló para la instrumentación. No se ejecutó ni validó release firmado en esta auditoría.

## Pruebas unitarias

| Módulo | Pruebas | Fallos / errores / omitidas |
|---|---:|---|
| core/domain | 400 | 0 / 0 / 0 |
| app | 10 | 0 / 0 / 0 |
| core/data | 70 | 0 / 0 / 0 |
| core/designsystem | 5 | 0 / 0 / 0 |
| feature/home | 14 | 0 / 0 / 0 |
| feature/juntas | 1 | 0 / 0 / 0 |
| feature/movements | 12 | 0 / 0 / 0 |
| feature/receipts | 11 | 0 / 0 / 0 |
| **Total** | **523** | **0 / 0 / 0** |

Los XML corresponden al 9 de septiembre. Se reutilizaron resultados de ese día cuando Gradle indicó `UP-TO-DATE`. Las tareas de módulos sin tests propios mostraron `NO-SOURCE`; no se cuentan como pruebas aprobadas. No se midió cobertura porcentual ni se certificó un objetivo de 80 %.

## Pruebas instrumentadas y fallo conservado

Moto G24, Android 14, 720×1612, fuente `1.0`. Corrida final: **87 pruebas, cero fallos, errores u omisiones**.

El primer fallo fue `HighControlsSemanticsTest.speedDialBackClosesMenuWithoutLeavingTheScreen`, línea 114. Extracto relevante del XML:

```text
java.lang.AssertionError: Failed: assertDoesNotExist.
Reason: Did not expect any node but found '1' node that satisfies:
(ContentDescription = 'Cerrar menú de registro' (ignoreCase: false))
```

La repetición completa pasó sin cambios. `KipuSpeedDialFab` solo tiene callers en dos clases de prueba (`HighControlsSemanticsTest`, `MediumReceiptHomeAccessibilityTest`), no en producción. AUD-L02 conserva la incertidumbre de foco/sincronización/componente; no es evidencia de un fallo de Atrás en la pantalla actual de Inicio.

### Procedencia de XML

Estas huellas se calcularon sobre las copias originales antes de actualizar documentación. No se incluyen logs, serial del teléfono ni capturas de datos personales en el repositorio. Los XML de `build/` pueden reemplazarse en futuras ejecuciones: comprobar fecha y huella, no solamente que el archivo exista.

| Artefacto | Timestamp XML (UTC) | SHA-256 |
|---|---|---|
| App, primera corrida | 2026-09-09T04:01:54 | `21fb09e97d36f2456b9d717e0f446e0c7e01ca2bd768248ee0a2a718518cedbf` |
| App, segunda corrida | 2026-09-09T15:28:48 | `697106945b7e6f49fd8820332217d4a0d38c60c061075230a00f7eb684a76c70` |
| Datos, segunda corrida | 2026-09-09T15:26:54 | `2409b9e500a7449d3c76b7c347995df6a443b1cd05097c4e30959c7406cb5074` |

El XML anterior de core:data, con 43 PASS y timestamp `2026-08-31T04:03:55`, era del 30 de agosto en Lima; no se utilizó como prueba de septiembre.

## Lint

Sin errores bloqueantes, **64 advertencias entre los informes de módulos**: app 34, designsystem 16, home 6, juntas 1, movements 2, plan 2, profile 1 y receipts 2. Datos y los demás módulos revisados no reportaron advertencias.

Clases: `OldTargetApi`, `UnusedAttribute`, `AndroidGradlePluginVersion`, `GradleDependency`, `NewerVersionAvailable`, `ModifierParameter`, `IconLocation`, `IconDuplicates`, `PrivateResource`, `Typos`, `ConfigurationScreenWidthHeight`. No todas tienen impacto funcional. Un aviso de versión disponible no autoriza actualizar dependencias ni cambiar el target sin pruebas.

## Recorrido manual y límites

APK debug nuevo: bienvenida → wizard → Atrás → Inicio → Registrar movimiento → «Registrar Gasto» → Atrás → Movimientos sin registros → Inicio. Se verificó árbol de UI y captura; no se guardaron importes ni un plan. Se dejó instalada la app debug con onboarding parcialmente recorrido.

No se comprobó en esta sesión el share real Yape/Plin, reconocimiento audible, TalkBack hablado, toda la matriz de pantallas/fuentes, proceso muerto, exportación concurrente ni release firmado. Los [hallazgos de auditoría](KIPU_AUDIT_2026-09-09.md) siguen abiertos a pesar de los PASS.

## Cierre de la actualización documental — 9 septiembre

Trabajo posterior a la auditoría: **17 documentos actualizados y 4 nuevos**. Se conservaron los informes históricos, los ejemplos técnicos generales y el cambio preexistente en el repositorio externo de skills. No se modificó producción, pruebas ni configuración de dependencias.

| Comprobación | Resultado | Alcance |
|---|---|---|
| Segunda revisión de contenido | PASS | Navegación contrastada con KipuNavGraph/Home/Sobres; 13 módulos; Room v22; separación de hechos, propuestas e históricos |
| Enlaces Markdown internos | PASS | 93 referencias en 26 archivos propios; se corrigió el ancla de dependencias. No es comprobación de disponibilidad de todos los sitios externos |
| HTML de privacidad | PASS | Etiquetas balanceadas y secciones 1–9; contacto marcado pendiente. Sin validación visual de navegador ni aprobación legal |
| `git diff --check` | PASS | Sin errores de whitespace en el diff |
| `./gradlew --offline --no-daemon --max-workers=1 assembleDebug` | PASS | BUILD SUCCESSFUL en 1 min 3 s; 403 tareas: 11 ejecutadas y 392 actualizadas |
| Instrumentadas / unitarias / lint durante el cambio documental | NO EJECUTADO nuevamente | Se conserva la evidencia de la auditoría anterior de este día; no se simula una nueva corrida |

La compilación documental no descargó dependencias ni instaló otra APK en el teléfono. Los diagramas Mermaid se revisaron como texto, sin renderizador independiente. No se crearon commits ni se publicó documentación en GitHub/Play.
