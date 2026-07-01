#!/usr/bin/env bash
# Instala app-debug.apk en un dispositivo físico vía adb.
# Uso: ./scripts/install-phone.sh [serial]
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
SERIAL="${1:-${ANDROID_SERIAL:-}}"

red() { printf '\033[31m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
yellow() { printf '\033[33m%s\033[0m\n' "$*"; }

if [[ ! -f "$APK" ]]; then
  yellow "APK no encontrado. Compilando..."
  (cd "$ROOT_DIR" && ./gradlew assembleDebug)
fi

ADB=(adb)
if [[ -n "$SERIAL" ]]; then
  ADB=(adb -s "$SERIAL")
fi

DEVICES="$("${ADB[@]}" devices | awk 'NR>1 && $2=="device" {print $1}')"
DEVICE_COUNT="$(echo "$DEVICES" | grep -c . || true)"

if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  red "No hay ningún celular conectado."
  echo "Conecta el USB, acepta «Depuración USB» en el teléfono y vuelve a ejecutar."
  exit 1
fi

if [[ -z "$SERIAL" ]]; then
  if [[ "$DEVICE_COUNT" -gt 1 ]]; then
    red "Hay varios dispositivos. Indica el serial:"
    echo "$DEVICES"
    echo "Ejemplo: ANDROID_SERIAL=340e501b $0"
    exit 1
  fi
  SERIAL="$(echo "$DEVICES" | head -1)"
  ADB=(adb -s "$SERIAL")
fi

MODEL="$("${ADB[@]}" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
ANDROID="$("${ADB[@]}" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
MIUI="$("${ADB[@]}" shell getprop ro.miui.ui.version.name 2>/dev/null | tr -d '\r' || true)"

green "Dispositivo: ${MODEL:-?} (Android ${ANDROID:-?}) serial=${SERIAL}"
if [[ -n "$MIUI" && "$MIUI" != "" ]]; then
  echo "MIUI detectado: $MIUI"
fi

echo "Instalando ${APK} ..."
set +e
INSTALL_OUT="$("${ADB[@]}" install -r -d -g "$APK" 2>&1)"
INSTALL_CODE=$?
set -e

echo "$INSTALL_OUT"

if [[ $INSTALL_CODE -eq 0 ]] && echo "$INSTALL_OUT" | grep -qi "Success"; then
  green "Instalación correcta."
  exit 0
fi

red "La instalación falló."
echo

if echo "$INSTALL_OUT" | grep -qi "USER_RESTRICTED"; then
  yellow "Problema: INSTALL_FAILED_USER_RESTRICTED (bloqueo de MIUI/HyperOS)"
  cat <<'EOF'

MIUI no deja instalar apps por USB hasta que actives un permiso extra.

En el celular:
  1. Ajustes → Ajustes adicionales → Opciones de desarrollador
  2. Activa «Depuración USB» (si no está)
  3. Activa «Instalar vía USB» o «Depuración USB (Ajustes de seguridad)»
     (el nombre exacto varía según la versión de MIUI/HyperOS)
  4. Si pide cuenta Mi / confirmación, acéptala en el teléfono
  5. Vuelve a ejecutar: ./scripts/install-phone.sh

Gradle/Android Studio usan el mismo adb; por eso connectedDebugAndroidTest también falla
hasta activar ese permiso.
EOF
elif echo "$INSTALL_OUT" | grep -qi "UPDATE_INCOMPATIBLE\|SIGNATURE"; then
  yellow "Problema: firma distinta a la app ya instalada"
  cat <<'EOF'

Ya tienes Kipu instalado con otra firma (por ejemplo, una build de release distinta).

Desinstala la app anterior y reinstala:
  adb uninstall pe.kipu.app
  ./scripts/install-phone.sh
EOF
elif echo "$INSTALL_OUT" | grep -qi "NO_MATCHING_ABIS"; then
  yellow "Problema: arquitectura del APK no compatible con el celular"
  cat <<'EOF'

El APK debug solo incluye arm64-v8a. Si tu celular es muy antiguo (solo 32 bits),
avisa para habilitar armeabi-v7a en el build.
EOF
elif echo "$INSTALL_OUT" | grep -qi "INSUFFICIENT_STORAGE"; then
  yellow "Problema: espacio insuficiente en el celular."
else
  yellow "Si instalas el APK copiándolo al teléfono (sin USB):"
  cat <<'EOF'

  1. Ajustes → Privacidad / Seguridad → Instalar apps desconocidas
  2. Permite la instalación para «Archivos» o la app con la que abres el APK
  3. Desactiva temporalmente «Verificar apps con Play Protect» si bloquea el APK de desarrollo
EOF
fi

exit 1
