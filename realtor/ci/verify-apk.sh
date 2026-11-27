#!/usr/bin/env bash
set -euo pipefail

apk=$1
package=$2
version=$3
cert=${4:-}
tools_dir="${ANDROID_HOME:?ANDROID_HOME is required}/build-tools/35.0.0"
output=$(dirname -- "$apk")

"$tools_dir/apksigner" verify --verbose --print-certs "$apk" > "$output/signing.txt"
"$tools_dir/aapt" dump badging "$apk" > "$output/manifest.txt"

grep -F "package: name='$package' " "$output/manifest.txt" | grep -F " versionName='$version' " > /dev/null
grep -E "^native-code: 'arm64-v8a'[[:space:]]*$" "$output/manifest.txt" > /dev/null
if grep -Fxq 'application-debuggable' "$output/manifest.txt"; then
    echo "A debuggable APK cannot be distributed by this job." >&2
    exit 1
fi

if [[ "$package" == ru.vladis.realtorai ]]; then
    signer=$(sed -nE 's/^Signer #([0-9]+) certificate SHA-256 digest: ([0-9a-fA-F]+)$/\1:\2/p' "$output/signing.txt")
    if [[ ! "$cert" =~ ^[0-9A-Fa-f]{64}$ || "${signer,,}" != "1:${cert,,}" ]] ||
       grep -Eq '^Signer #[0-9]+ certificate DN: .*CN=Android Debug([,[:space:]]|$)' "$output/signing.txt"; then
        echo "The APK signer does not match the approved production certificate." >&2
        exit 1
    fi
fi
