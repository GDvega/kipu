# SECURITY_CHECKLIST.md — Kipu

Checklist de seguridad y privacidad alineado con ECC y `AGENTS.md`.

---

## 1. Cuándo activar revisión de seguridad

Activar **antes de merge** cuando el encargo toca:

| Trigger | Nivel mínimo |
|---------|--------------|
| Persistencia (Room, DataStore, archivos) | MEDIUM |
| Parsers, OCR, intents compartidos | HIGH |
| Permisos (notificaciones, micrófono, selector de imágenes) | HIGH |
| Exportar o eliminar datos | HIGH |
| Logs, crash reporting, analytics | MEDIUM |
| Backup Android (`allowBackup`, rules XML) | MEDIUM |
| Red, Firebase, sync | HIGH (aprobación explícita) |
| Solo UI placeholder / tema | LOW (checklist pre-commit básico) |

**Riesgo Alto en ECC:** detener y pedir aprobación humana antes de implementar.

Skill de referencia: `docs/ai/skills/security-review.md`.

---

## 2. Modelo de amenaza mínimo

### Activos a proteger

| Activo | Sensibilidad |
|--------|--------------|
| Montos y movimientos financieros | Alta |
| Nombres de contactos / destinatarios | Alta |
| Imágenes de comprobantes | Alta |
| Texto OCR completo | Alta |
| Preferencias y configuración | Media |
| Tokens o claves API (futuro Firebase) | Crítica |

### Entradas no confiables

- Texto OCR (ML Kit)
- Intents `ACTION_SEND` (comprobantes compartidos)
- Notificaciones del sistema (si se activa listener)
- Archivos importados (export/import CSV JSON)
- Input manual del usuario

### Fronteras de confianza

| Frontera | Regla |
|----------|-------|
| UI ↔ Domain | UI nunca persiste sin confirmación en movimientos sugeridos |
| Domain ↔ Data | Solo modelos de dominio; mappers obligatorios |
| App ↔ Red | Sin envío de datos financieros en MVP |
| App ↔ IA externa | Prohibido en MVP |
| App ↔ Backup Android | Revisar cuando exista Room (Fase 5+) |

---

## 3. Checklist pre-commit

Ejecutar antes de cada commit que toque código:

- [ ] No hay claves API, tokens ni secretos en el código
- [ ] No hay logs con montos reales
- [ ] No hay logs con nombres de destinatarios
- [ ] No hay logs con texto OCR completo
- [ ] No hay logs con contenido de comprobantes
- [ ] No se guardan imágenes de comprobantes en nube por defecto
- [ ] Todo permiso nuevo tiene explicación clara en UI
- [ ] El permiso de notificaciones es opcional (cuando exista)
- [ ] La app funciona sin permisos sensibles
- [ ] El usuario puede eliminar sus datos (cuando esté implementado)
- [ ] El usuario puede exportar sus datos (cuando esté implementado)
- [ ] Los datos financieros no se envían a IA en el MVP
- [ ] Los parsers validan entradas incompletas o maliciosas
- [ ] Si OCR falla, el usuario puede editar manualmente
- [ ] Si hay duplicado, la app pide confirmación
- [ ] Componentes exportados solo si es necesario
- [ ] `./gradlew assembleDebug` PASS

---

## 4. Checklist por superficie

### Comprobantes / OCR / intents compartidos

- [ ] Validar MIME type y tamaño de imagen antes de procesar
- [ ] No persistir imagen sin acción explícita del usuario
- [ ] OCR solo local (ML Kit); sin upload por defecto
- [ ] Sanitizar texto OCR antes de parsear
- [ ] Pantalla de confirmación antes de guardar movimiento sugerido
- [ ] Manejar intent malformado sin crash (fail gracefully)
- [ ] No loguear URI del comprobante ni texto OCR

### Room / DataStore

- [ ] Entidades no expuestas a Compose ni ViewModels
- [ ] Migraciones versionadas y probadas
- [ ] Queries sin interpolación de strings de usuario
- [ ] DataStore para preferencias no sensibles; evaluar cifrado si hay PII
- [ ] Eliminar datos borra todas las tablas y preferencias

### Permisos (notificaciones, micrófono, selector de imágenes)

- [ ] Permiso solicitado solo cuando el usuario activa la función
- [ ] Explicación en español simple antes del diálogo del sistema
- [ ] Flujo alternativo sin permiso documentado y funcional
- [ ] Listener de notificaciones filtra solo paquetes relevantes (Yape/Plin/bancos configurados)
- [ ] No leer notificaciones de otras apps sin necesidad

### Exportar / eliminar datos

- [ ] Export genera archivo local; no sube automáticamente
- [ ] Export incluye aviso de sensibilidad del archivo
- [ ] Eliminar pide confirmación doble (diálogo destructivo)
- [ ] Wipe borra DB, DataStore y archivos exportados temporales
- [ ] Operaciones irreversibles claramente etiquetadas

### Logs y crash reporting

- [ ] Prohibido loguear datos de la sección 6
- [ ] Crash reporting sin PII (cuando se integre Firebase Crashlytics)
- [ ] Niveles de log: DEBUG desactivado en release
- [ ] Revisar stack traces que no incluyan argumentos financieros

### Backup Android (`allowBackup`)

- [x] `android:allowBackup=false` en manifest (jun 2026 — remediación AUD-016); reglas XML mantienen exclusiones como defensa en profundidad
- [x] Room `kipu.db` (+ wal/shm) excluida de cloud backup y device transfer (rules XML)
- [x] DataStore `kipu_preferences.preferences_pb` excluido
- [x] Cache `exports/` excluida de backup/transfer
- [x] Decisión documentada en `PROJECT_STATE.md` (F0-02 cerrado Fase 19)

---

## 5. Severidad ECC y acción requerida

| Severidad | Ejemplo en Kipu | Acción |
|-----------|-----------------|--------|
| **CRITICAL** | Clave API en repo; log de montos en producción; envío de comprobantes a servidor externo sin consentimiento | Bloquear merge; corregir de inmediato |
| **HIGH** | Persistir sin confirmación; permiso obligatorio; backup expone DB financiera; parser sin validación | Corregir antes de merge |
| **MEDIUM** | `allowBackup` sin rules; export sin aviso; crash reporting con PII | Corregir en la fase o registrar hallazgo con due date |
| **LOW** | Deprecación API; warning lint; iconos extended en APK | Siguiente iteración |

---

## 6. Qué NUNCA loguear en Kipu

| Dato | Permitido en logs |
|------|-------------------|
| Montos (`S/`, decimales) | ❌ Nunca |
| Nombres de destinatarios | ❌ Nunca |
| Números de operación / teléfono | ❌ Nunca |
| Texto OCR completo | ❌ Nunca |
| Imágenes / URIs de comprobantes | ❌ Nunca |
| Contenido de notificaciones | ❌ Nunca |
| IDs internos opacos (UUID) | ✅ Solo en DEBUG, sin contexto financiero |
| Eventos genéricos ("parser_failed") | ✅ Sin adjuntar input |

---

## 7. Tabla fase → nivel de revisión de seguridad

| Fase | Nombre | Nivel | Checklist obligatorio |
|------|--------|-------|----------------------|
| 0 | Shell multi-módulo | Bajo | Pre-commit básico |
| 1 | Documentación de control | Bajo | Pre-commit |
| 2 | Design system ampliado | Bajo | Pre-commit |
| 3 | Domain base | Bajo | Pre-commit |
| 4 | DI + presentation base | Bajo | Pre-commit |
| 5 | DataStore + preferencias | Medio | Pre-commit + DataStore |
| 6 | Room + movimientos | Medio | Pre-commit + Room + Backup |
| 7 | Parsers + OCR | **Alto** | Completo (OCR + intents + logs) |
| 8 | Sobres | Medio | Pre-commit + dominio |
| 9 | Disponible diario + hormiga | Medio | Pre-commit + dominio |
| 10 | Duplicados | Medio | Pre-commit + dominio |
| 11 | Compromisos / metas | Medio | Pre-commit + dominio |
| 12 | Notificaciones | **Alto** | Completo (permisos + listener) |
| 13 | Exportar / eliminar datos | **Alto** | Completo (export + eliminar) |
| 14 | Onboarding | Bajo | Pre-commit |
| 15 | Comprobantes UI | **Alto** | Completo (superficie OCR) |
| 16 | Juntas + pulido | Medio | Pre-commit + lint + revisión final |

> **Numeración canónica:** ver `docs/ai/PROJECT_STATE.md`.

---

## Referencias

- `AGENTS.md` — secciones 3 y 6
- `PROJECT_STATE.md` — hallazgos abiertos F0-02 (`allowBackup`)
- `docs/ai/skills/security-review.md` — guía ECC detallada
