# Política de privacidad — Kipu

**Última actualización:** 25 de agosto de 2026
**Aplicación:** Kipu (`pe.kipu.app`)
**Ámbito:** usuarios en Perú

> Publicar esta URL en Google Play Console (puede ser el archivo en GitHub o una copia en tu sitio web).

---

## 1. Resumen

Kipu es una app de finanzas personales. **Tus movimientos se guardan en tu dispositivo.** Kipu **no pide claves bancarias** ni **accede a tu banca**. Si usas comandos de voz, Kipu interpreta la transcripción localmente después del reconocimiento del sistema; nada se guarda hasta que lo confirmas.

## 2. Qué datos trata Kipu

| Dato | Origen | Dónde se guarda |
|------|--------|-----------------|
| Movimientos (montos, categorías, fechas, contrapartes) | Registro manual, comprobantes compartidos, notificaciones opcionales | Base de datos local (Room) en el dispositivo |
| Sobres, compromisos, plan financiero, cuentas compartidas | Lo ingresas tú en la app | Base de datos local |
| Preferencias (tema, flags de notificaciones, onboarding) | Configuración en Perfil | DataStore local |
| Imágenes de comprobantes | Comprobante compartido o imagen elegida | Se leen desde su app de origen para revisión y OCR local; Kipu no toma ni conserva fotos con la cámara |
| Exportaciones JSON/CSV | Acción explícita tuya | Archivo local que **tú** compartes con otras apps |

Kipu **no recopila** nombre legal, DNI, número de cuenta bancaria ni contraseñas de Yape, Plin o bancos.

## 3. Permisos opcionales

### Acceso a notificaciones (opcional)

Si lo activas en Perfil, Kipu puede leer **solo notificaciones de ingresos** de apps de pago configuradas (Yape, Plin) para sugerirte registrar un movimiento. **Nunca es obligatorio** para usar la app. Puedes desactivarlo cuando quieras.

### Compartir comprobantes

Cuando compartes una imagen desde Yape/Plin hacia Kipu, la imagen se procesa **en el dispositivo** con OCR local (ML Kit). No enviamos la imagen a servicios de IA generativa ni a nube propia por defecto.

Kipu no ofrece captura con cámara. Las imágenes compartidas o elegidas siguen perteneciendo a su app de origen y Kipu solo las lee durante la revisión.

### Comandos de voz (opcional)

Al tocar el micrófono, el servicio de reconocimiento de voz configurado en tu dispositivo convierte tu voz en texto según las condiciones de su proveedor. Después, Kipu interpreta esa transcripción en el dispositivo para proponer el tipo de movimiento, monto, categoría y medio de pago. No enviamos la transcripción a servidores de Kipu. El resultado aparece para revisión y **solo se guarda cuando lo confirmas**.

### Métricas técnicas de ML Kit

ML Kit procesa la imagen, el texto reconocido y el resultado del OCR completamente en el dispositivo; esos datos financieros no se envían a Google. El SDK sí recopila y envía a Google información técnica del dispositivo y la aplicación, identificadores por instalación, métricas de rendimiento, configuración de la API, tamaños de entrada/salida, versión de la función, eventos y códigos de error para diagnóstico y analítica de uso. Google declara que cifra estos datos en tránsito mediante HTTPS y no los transfiere a terceros.

Fuentes oficiales: [privacidad de ML Kit](https://developers.google.com/ml-kit/terms) y [divulgación de datos de ML Kit para Android](https://developers.google.com/ml-kit/android-data-disclosure).

## 4. Cómo usamos tus datos

- Mostrar tu resumen financiero, sobres, metas y cuentas compartidas **solo para ti** en el dispositivo.
- Detectar posibles duplicados y pedirte confirmación antes de guardar.
- Interpretar una transcripción de voz cuando activas voluntariamente esa función.
- Exportar o eliminar tus datos cuando lo pidas en Perfil.

**No vendemos ni compartimos** tus datos financieros con terceros con fines publicitarios.

## 5. Copias de seguridad del dispositivo

La base de datos financiera y preferencias sensibles están **excluidas** de la copia de seguridad automática de Google en la configuración actual de Kipu. Las exportaciones que generes tú son tu responsabilidad si las compartes.

## 6. Tus derechos

En Perfil puedes:

- **Exportar** todos tus datos en JSON; el CSV contiene solo movimientos para uso en hojas de cálculo.
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
