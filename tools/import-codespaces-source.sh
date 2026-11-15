#!/usr/bin/env bash
# One-time, safe import from the existing Codespaces checkout.
# Does not modify the working launcher source or publish config/keys.
set -euo pipefail
ROOT=/workspaces/FreeMyTV-Xiaomi.
SRC="$ROOT/free-my-tv-main"
[ -d "$ROOT/.git" ] && [ -f "$SRC/build.sh" ] && [ -d "$SRC/app/src" ] || {
  echo "ERROR: expected Codespaces checkout was not found." >&2; exit 1;
}
command -v python3 >/dev/null
command -v git >/dev/null
git -C "$ROOT" fetch origin feature/full-tv-vpn
WORK="$(mktemp -d /tmp/freemytv-import.XXXXXX)"
cleanup() {
  git -C "$ROOT" worktree remove --force "$WORK" >/dev/null 2>&1 || true
  rmdir "$WORK" >/dev/null 2>&1 || true
}
trap cleanup EXIT
git -C "$ROOT" worktree add --detach "$WORK" origin/feature/full-tv-vpn

python3 - "$SRC" "$WORK" <<'PY'
from pathlib import Path
import shutil, sys, re
src, dst = (Path(x) for x in sys.argv[1:])
allowed = [
    "app/src", "app/assets", "app/res", "app/AndroidManifest.xml",
    "build.sh", "README.md", "LICENSE", "config.example.properties",
    "tests", ".github/workflows/build-apk.yml",
]
blocked_extensions = {".apk", ".idsig", ".zip", ".jks", ".keystore", ".pem",
                      ".key", ".env", ".class", ".dex", ".p12", ".pfx"}
blocked_names = {"config.properties", "freemytv.keystore", "local.properties"}
copied = []
for rel in allowed:
    root = src / rel
    candidates = [root] if root.is_file() else root.rglob("*") if root.is_dir() else []
    for p in candidates:
        if not p.is_file() or p.is_symlink():
            continue
        if p.name.lower() in blocked_names or p.suffix.lower() in blocked_extensions:
            continue
        if p.stat().st_size > 10_000_000:
            raise SystemExit("ERROR: unexpectedly large source file; import stopped")
        relative = p.relative_to(src)
        target = dst / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(p, target)
        copied.append(target)
for p in copied:
    # These two tests contain deliberate fake VLESS URLs and URL credentials.
    if p.name in {"VlessSubscriptionTest.java", "VlessSubscriptionLoaderTest.java"}:
        continue
    if p.suffix.lower() not in {".java", ".xml", ".json", ".sh", ".md", ".properties", ".txt", ".yml", ".yaml"}:
        continue
    data = p.read_text(encoding="utf-8", errors="replace")
    checks = [
        r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----",
        r"gh[pousr]_[A-Za-z0-9_]{20,}",
        r"vless://[0-9a-fA-F-]{32,}@",
        r"https?://[^/\s:@]+:[^@\s]+@",
    ]
    if any(re.search(pattern, data) for pattern in checks):
        raise SystemExit("ERROR: possible credential in " + str(p.relative_to(dst)) + "; nothing published")
print("Safe source files copied:", len(copied))
PY

# Build with a disposable test key. Existing personal keys are never read.
if [ -z "$(printenv ANDROID_HOME || true)" ] && [ -d /home/codespace/android-sdk/build-tools ]; then
  export ANDROID_HOME=/home/codespace/android-sdk
fi
( cd "$WORK" && bash build.sh --generic )
BT="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
"$BT/apksigner" verify "$WORK/FreeMyTV.apk"
cp "$WORK/FreeMyTV.apk" "$SRC/FreeMyTV-preview.apk"
echo "Test APK: $SRC/FreeMyTV-preview.apk"

git -C "$WORK" add -- app/src app/assets app/res app/AndroidManifest.xml \
  build.sh README.md LICENSE config.example.properties tests .github/workflows/build-apk.yml
git -C "$WORK" diff --cached --check
if ! git -C "$WORK" diff --cached --quiet; then
  git -C "$WORK" -c user.name="FreeMyTV Codespaces" \
    -c user.email="codespaces@users.noreply.github.com" \
    commit -m "Import complete safe Free My TV sources with INCY and traffic meter"
fi
git -C "$WORK" push origin HEAD:refs/heads/feature/full-tv-vpn
echo "SUCCESS: source published to feature/full-tv-vpn (no private keys)."
echo "GitHub Actions can now build the same source automatically."
