# Release signing setup

The `Release` workflow (`.github/workflows/release.yml`) builds a **signed** APK/AAB
and publishes it to GitHub Releases. It needs a keystore + 4 repository secrets.

`KEY_ALIAS` is already set to `concursos`. Run the command below **on your machine**
to create the keystore and set the other three secrets — the password never leaves
your computer.

## One block — run it all at once

Paste the whole thing in one go (running it in pieces loses `$KS` between shells,
which uploads an empty `KEYSTORE_BASE64`):

```sh
KS="$HOME/Projects/_keystores/concursos-release.jks"

# create the keystore only if it doesn't exist yet
[ -f "$KS" ] || { mkdir -p "$(dirname "$KS")"; \
  keytool -genkeypair -v -keystore "$KS" -storetype PKCS12 -alias concursos \
    -keyalg RSA -keysize 4096 -validity 10000 \
    -dname "CN=Concursos, O=Concursos, C=MZ"; }
#   → keytool asks "Enter keystore password:" twice (>= 6 chars).
#     PKCS12 => the key password IS the keystore password.

B64="$(base64 -i "$KS")"
echo "keystore: $(wc -c < "$KS") bytes | base64: ${#B64} chars"
[ "${#B64}" -gt 1000 ] || echo "!! base64 too short — keystore missing/broken"

printf %s "$B64" | gh secret set KEYSTORE_BASE64
read -rsp "Keystore password again: " PW; echo "  (length: ${#PW})"
printf %s "$PW" | gh secret set KEYSTORE_PASSWORD
printf %s "$PW" | gh secret set KEY_PASSWORD
gh secret set KEY_ALIAS --body concursos
unset PW B64
```

Expected output: something like `base64: 3600 chars` and `length: 8`.

Notes:

- The alias **must** be `concursos`.
- `printf %s ... | gh secret set` sends the value with no trailing newline.
- If `base64:` prints under ~1000 or the workflow's *Decode keystore* step says
  `KEYSTORE_BASE64 secret is missing or too short`, `$KS` was empty when you ran
  it — paste the block again as one unit.
- If `length:` prints less than 6, the terminal ate a character — pick a longer
  password.
- A store/key password mismatch causes
  `java.security.UnrecoverableKeyException: Given final block not properly padded`
  in `:app:packageRelease` — PKCS12 avoids it.

## Verify locally first (optional)

```sh
cd ~/Projects/studio-projects/Concursos
read -rsp "Keystore password: " PW; echo
printf 'KEYSTORE_FILE=%s\nKEYSTORE_PASSWORD=%s\nKEY_ALIAS=concursos\nKEY_PASSWORD=%s\n' \
  "$HOME/Projects/_keystores/concursos-release.jks" "$PW" "$PW" > keystore.properties  # git-ignored
unset PW
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
