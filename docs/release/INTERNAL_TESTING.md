# Internal testing — Play Console

Guía de trabajo humano para la publicación pendiente (Fase 27). Complementa [Play Store](PLAY_STORE.md) y [E2E](../ai/E2E_QA_CHECKLIST.md).

**Última revisión documental:** 12 septiembre 2026.

La [auditoría base](../qa/KIPU_AUDIT_2026-09-09.md) encontró **4 HIGH, 2 MEDIUM y 2 LOW**. Se verificaron las correcciones de H01–H04, M01–M02, L01 y el nuevo H05: 551 unitarias, 48 app y 55 datos PASS; compilación y lint debug PASS. [Remediación](../qa/REMEDIATION_2026-09-09.md) conserva L02 en observación y las comprobaciones humanas pendientes. No hay evidencia de AAB firmado, publicación ni validación del track en esta ronda. Esta guía no autoriza crear credenciales, publicar o invitar testers; esos pasos requieren intervención del responsable.

---

## 1. Generar keystore (una sola vez)

```bash
keytool -genkey -v \
  -keystore kipu-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias kipu
```

Ejecutar este paso únicamente por el responsable de firma y desde una ubicación privada **fuera del repo**, con copia de seguridad segura. No mostrar contraseñas ni adjuntar el keystore a reportes. `.gitignore` no es una protección suficiente para secretos.

Copiar plantilla y completar:

```bash
cp keystore.properties.example keystore.properties
# Editar storeFile, storePassword, keyPassword, keyAlias
```

---

## 2. Construir AAB firmado

```bash
./gradlew :core:domain:test :app:lintDebug bundleRelease
```

| Resultado | Ubicación |
|-----------|-----------|
| AAB firmado (con `keystore.properties`) | `app/build/outputs/bundle/release/app-release.aab` |

Sin `keystore.properties`, `preReleaseBuild` falla de forma intencional antes de generar un AAB nuevo. Kipu no permite producir silenciosamente un artefacto release sin firma. Si existe un AAB antiguo en `build/`, no debe usarse: genera uno nuevo después de configurar la firma.

Medir y registrar tamaño, fecha y checksum del AAB recién generado. No reutilizar estimaciones históricas de tamaño como resultado actual.

---

## 3. URL pública de privacidad

Opción recomendada — **GitHub Pages** desde el repo:

1. Settings → Pages → Source: **Deploy from branch**
2. Branch `main`, folder `/docs`
3. URL resultante: `https://<usuario>.github.io/kipu/privacy/`

Archivo estático incluido: [política HTML](../privacy/index.html), alineado con el [borrador Markdown](PRIVACY_POLICY.md). Contacto proporcionado por el responsable y comprobado en Perfil (AUD-L01). Antes de publicar: comprobar entrega de correo, aprobar el tratamiento de datos y verificar la URL sin autenticación. No se verificó que Pages esté publicado.

Pegar la URL en Play Console → **Política de privacidad**.

---

## 4. Play Console — internal testing

1. [Google Play Console](https://play.google.com/console) → Crear app **Kipu**
2. **Configuración** → completar cuenta de desarrollador si falta
3. **Prueba interna** → Crear nueva versión
4. Subir `app-release.aab`
5. **Notas de la versión** (ejemplo):

   ```
   MVP 1.0.0 — finanzas personales local.
   Sobres, movimientos, comprobantes, cuentas compartidas, export/wipe.
   ```

6. **Store listing** — copiar textos de `PLAY_STORE.md`
7. **Data safety** — revisar SDK y build final; `PLAY_STORE.md` §4 aporta insumos, no respuestas certificadas
8. **Contenido de la app** — cuestionario IARC (finanzas, sin apuestas)
9. Añadir testers (correos Gmail) en lista de prueba interna
10. **Revisar y publicar** en track interno

Compartir el enlace de participación con los testers según las opciones vigentes de Console. No se ha invitado ni notificado a nadie desde esta actualización.

---

## 5. Separar QA debug de validación del track

Las pruebas automatizadas instalan APK **debug** y de pruebas; no verifican el AAB descargado de Play. Usar un dispositivo de pruebas sin datos personales. No desinstalar la app real ni borrar sus datos para resolver conflictos de firma.

```bash
adb devices
# Sustituir el marcador por el dispositivo de pruebas elegido.
ANDROID_SERIAL='SERIAL_DEL_DISPOSITIVO' ./gradlew --no-daemon --max-workers=1 --continue :core:data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

Para validar el track, instalar su versión por el canal correspondiente y recorrer por separado el [checklist manual](../ai/E2E_QA_CHECKLIST.md), incluyendo los casos financieros abiertos. Registrar versión, origen de instalación, dispositivo, fecha y resultados. No ejecutar `connectedDebugAndroidTest` sobre esa instalación como si validara release.

---

## 6. Registro de ejecución Fase 27

| Paso | Estado | Fecha | Notas |
|------|--------|-------|-------|
| Keystore creado | ⏳ | | |
| `bundleRelease` firmado | ⏳ | | |
| URL privacidad publicada | ⏳ | | |
| AAB subido (internal) | ⏳ | | |
| Testers invitados | ⏳ | | |
| Instrumentadas debug | PASS | 9 septiembre 2026 | 43 core:data + 44 app; [evidencia y fallo anterior](../qa/VERIFICATION_2026-09-09.md). No valida release |
| Checklist manual N1–E4 | ⏳ | | |

---

## 7. Siguiente paso tras internal testing

- Corregir feedback de testers
- Incrementar `versionCode` en `app/build.gradle.kts`
- Revisar y corregir los HIGH, agregar sus regresiones y evaluar los demás hallazgos.
- Decidir avance de track solo con QA de la build firmada, privacidad/contacto revisados y requisitos vigentes de Console comprobados por el responsable. N1–E4 por sí solos no certifican preparación para producción.
