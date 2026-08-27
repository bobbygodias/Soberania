#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NATIVE_DIR="${ROOT_DIR}/native/wireguard/build/arm64-v8a"
JNI_DIR="${ROOT_DIR}/app/src/wireguardLab/jniLibs/arm64-v8a"

GO_LIB="${NATIVE_DIR}/libsoberania-wireguard-go.so"
JNI_LIB="${NATIVE_DIR}/libsoberania-wg.so"

for file in "${GO_LIB}" "${JNI_LIB}"; do
  if [[ ! -f "${file}" ]]; then
    echo "Native WireGuard artifact not found: ${file}" >&2
    echo "Run 'make build-arm64' inside native/wireguard first." >&2
    exit 1
  fi
done

rm -rf "${ROOT_DIR}/app/src/wireguardLab/jniLibs"
mkdir -p "${JNI_DIR}"

cp "${GO_LIB}" "${JNI_DIR}/"
cp "${JNI_LIB}" "${JNI_DIR}/"

echo "Staged WireGuard LAB JNI libraries:"
find "${ROOT_DIR}/app/src/wireguardLab/jniLibs" -type f -maxdepth 3 -print
