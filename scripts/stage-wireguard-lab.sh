#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_ROOT="${ROOT_DIR}/native/wireguard/build"
TARGET_ROOT="${ROOT_DIR}/app/src/wireguardLab/jniLibs"

ABIS=(
  "arm64-v8a"
  "x86_64"
)

rm -rf "${TARGET_ROOT}"

for abi in "${ABIS[@]}"; do
  source_dir="${SOURCE_ROOT}/${abi}"
  target_dir="${TARGET_ROOT}/${abi}"

  go_lib="${source_dir}/libsoberania-wireguard-go.so"
  jni_lib="${source_dir}/libsoberania-wg.so"

  for file in "${go_lib}" "${jni_lib}"; do
    if [[ ! -f "${file}" ]]; then
      echo "Native WireGuard artifact not found: ${file}" >&2
      echo "Run 'make build-all' inside native/wireguard first." >&2
      exit 1
    fi
  done

  mkdir -p "${target_dir}"
  cp "${go_lib}" "${target_dir}/"
  cp "${jni_lib}" "${target_dir}/"
done

echo "Staged WireGuard LAB JNI libraries:"
find "${TARGET_ROOT}" -type f -maxdepth 3 -print | sort
