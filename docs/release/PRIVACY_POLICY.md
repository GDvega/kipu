# Política de privacidad — Kipu

**Última actualización:** 20 de junio de 2026  
**Aplicación:** Kipu (`pe.kipu.app`)  
**Ámbito:** usuarios en Perú

> Publicar esta URL en Google Play Console (puede ser el archivo en GitHub o una copia en tu sitio web).

---

## 1. Resumen

Kipu es una app de finanzas personales. **Tus datos financieros se guardan en tu dispositivo.** Kipu **no pide claves bancarias**, **no accede a tu banca** y **no sube tus movimientos a servidores propios** en el MVP.

## 2. Qué datos trata Kipu

| Dato | Origen | Dónde se guarda |
|------|--------|-----------------|
| Movimientos (montos, categorías, fechas, contrapartes) | Registro manual, comprobantes compartidos, notificaciones opcionales | Base de datos local (Room) en el dispositivo |
| Sobres, compromisos, plan financiero, juntas | Lo ingresas tú en la app | Base de datos local |
| Preferencias (tema, flags de notificaciones, onboarding) | Configuración en Perfil | DataStore local |
| Imágenes de comprobantes | Solo mientras revisas un comprobante (OCR) | Memoria/cache temporal del dispositivo; no se suben por defecto |
| Exportaciones JSON/CSV | Acción explícita tuya | Archivo local que **tú** compartes con otras apps |

Kipu **no recopila** nombre legal, DNI, número de cuenta bancaria ni contraseñas de Yape, Plin o bancos.

## 3. Permisos opcionales

### Acceso a notificaciones (opcional)

Si lo activas en Perfil, Kipu puede leer **solo notificaciones de ingresos** de apps de pago configuradas (Yape, Plin) para sugerirte registrar un movimiento. **Nunca es obligatorio** para usar la app. Puedes desactivarlo cuando quieras.

### Compartir comprobantes

Cuando compartes una imagen desde Yape/Plin hacia Kipu, la imagen se procesa **en el dispositivo** con OCR local (ML Kit). No enviamos la imagen a servicios de IA generativa ni a nube propia por defecto.

## 4. Cómo usamos tus datos

- Mostrar tu resumen financiero, sobres, metas y juntas **solo para ti** en el dispositivo.
- Detectar posibles duplicados y pedirte confirmación antes de guardar.
- Exportar o eliminar tus datos cuando lo pidas en Perfil.

**No vendemos ni compartimos** tus datos financieros con terceros con fines publicitarios.

## 5. Copias de seguridad del dispositivo

La base de datos financiera y preferencias sensibles están **excluidas** de la copia de seguridad automática de Google en la configuración actual de Kipu. Las exportaciones que generes tú son tu responsabilidad si las compartes.

## 6. Tus derechos

En Perfil puedes:

- **Exportar** todos tus datos (JSON o CSV).
- **Eliminar** todos tus datos locales con confirmación doble.

Tras eliminar, la app vuelve al onboarding inicial.

## 7. Menores

Kipu no está dirigida a menores de 13 años.

## 8. Cambios

Publicaremos cambios relevantes en esta política y actualizaremos la fecha arriba. El uso continuado de la app implica aceptación de la versión vigente.

## 9. Contacto

Para consultas sobre privacidad: **privacidad@kipu.pe** (reemplazar con el correo real del responsable antes de publicar en Play Store).

---

*Kipu — finanzas personales en Perú, sin claves bancarias.*
