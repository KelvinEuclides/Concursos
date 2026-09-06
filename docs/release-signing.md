# Release signing setup

The `Release` workflow (`.github/workflows/release.yml`) builds a **signed** APK/AAB
and publishes it to GitHub Releases. It needs a keystore + 4 repository secrets.

`KEY_ALIAS` is already set to `concursos`. Run the command below **on your machine**
to create the keystore and set the other three secrets — the password never leaves
your computer.

## Commands

**1. Create the keystore** — let `keytool` prompt for the password (it validates
the ≥ 6 char rule; PKCS12 means the key password is the keystore password, so a
mismatch is impossible):

```sh
KS=~/Projects/_keystores/concursos-release.jks
mkdir -p "$(dirname "$KS")"
keytool -genkeypair -v -keystore "$KS" -storetype PKCS12 -alias concursos \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Concursos, O=Concursos, C=MZ"
#   → "Enter keystore password:"  (type it twice)
```

**2. Set the three secrets** — re-enter the same password:

```sh
read -rsp "Same password: " PW; echo "  (length: ${#PW})"
gh secret set KEYSTORE_BASE64   --body "$(base64 -i "$KS")"
gh secret set KEYSTORE_PASSWORD --body "$PW"
gh secret set KEY_PASSWORD      --body "$PW"
unset PW
```

Notes:

- The alias **must** be `concursos` (that's what the `KEY_ALIAS` secret holds).
- If step 2 prints `length: 5` (or less), your terminal ate a character — retype
  or pick a longer password.
- `--body` passes the value with no trailing newline (unlike `echo | gh secret set`).
- A store/key password mismatch would cause
  `java.security.UnrecoverableKeyException: Given final block not properly padded`
  during `:app:packageRelease` — PKCS12 avoids this.

## Verify locally first (optional)

```sh
cd ~/Projects/studio-projects/Concursos
printf 'KEYSTORE_FILE=%s\nKEYSTORE_PASSWORD=%s\nKEY_ALIAS=concursos\nKEY_PASSWORD=%s\n' \
  "$KS" "$PW" "$PW" > keystore.properties   # git-ignored
./gradlew :app:assembleRelease -q
~/Library/Android/sdk/build-tools/36.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

If the certs don't say `CN=Android Debug`, signing works.

## Cut a release

- **Automatic:** *Actions → Release → Run workflow* (choose `patch` / `minor` /
  `major`). It bumps the last tag, tags, builds, signs, and publishes to
  [`/releases/latest`](../../releases/latest).
- **Manual:** push a tag — `git tag v1.0.0 && git push origin v1.0.0`.

## Secrets reference

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -i concursos-release.jks` (single line) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | `concursos` (already set) |
| `KEY_PASSWORD` | key password (same as keystore password) |

> ⚠️ Back up `concursos-release.jks` and its password. If the app goes on the Play
> Store, this key signs **every** future update — losing it is unrecoverable
> without Google Play key-reset support.
