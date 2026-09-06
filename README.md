# Concursos

> App Android para acompanhar concursos públicos de Moçambique publicados pela
> UFSA (Unidade Funcional de Supervisão das Aquisições), com filtros, favoritos,
> notificações de novos concursos e recomendações opcionais por IA (Google Gemini).

> Android app for tracking Mozambican public procurement tenders published by
> UFSA, with filtering, bookmarks, new-tender notifications and optional
> AI recommendations (Google Gemini).

[![Android CI](../../actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)
[![Release](../../actions/workflows/release.yml/badge.svg)](../../actions/workflows/release.yml)
[![Latest release](https://img.shields.io/github/v/release/KelvinEuclides/Concursos?sort=semver)](../../releases/latest)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**⬇️ [Descarregar o APK mais recente / Download the latest APK](../../releases/latest)**

---

## 🇵🇹 Português

### O que é

O **Concursos** recolhe (via scraping) os dados públicos do portal
[ufsa.gov.mz](https://www.ufsa.gov.mz/) e apresenta-os numa interface moderna em
Jetpack Compose. Permite:

- **Listar concursos** abertos, adjudicados e cancelados;
- **Filtrar** por província, por texto e por área de TI/informática;
- **Ver detalhes** de cada concurso (modalidade, objecto, UGEA, datas, ...);
- **Guardar** concursos como favoritos (persistidos localmente);
- **Consultar fornecedores** do Cadastro Único (CEF);
- **Receber notificações** periódicas quando surgem novos concursos, com um
  worker de sincronização em segundo plano (WorkManager);
- **Recomendações e Chat por IA** (opcional): descreve o perfil da tua empresa e a
  IA sugere os concursos mais relevantes e responde a dúvidas sobre os editais.
  Suporta **Google Gemini** (nuvem) e **Gemma 2B** (100% on-device / offline via MediaPipe GenAI).

### Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Persistência | Room |
| Rede / scraping | OkHttp + Jsoup |
| Background | WorkManager |
| IA Nuvem | Google Gemini (`generativelanguage.googleapis.com`) via REST |
| IA On-Device | Google Gemma 2B via MediaPipe Tasks GenAI (`tasks-genai`) |
| Build | Gradle (Kotlin DSL) + version catalog |

Requisitos: `minSdk 26`, `targetSdk 37`, JDK 11+.

### Como compilar

```bash
git clone https://github.com/OWNER/Concursos.git
cd Concursos

# Cria local.properties a apontar para o teu Android SDK
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties   # macOS
# echo "sdk.dir=$HOME/Android/Sdk" > local.properties          # Linux

./gradlew assembleDebug        # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # instala num dispositivo/emulador ligado
./gradlew test                 # testes unitários
```

Ou abre a pasta no Android Studio (Ladybug ou mais recente).

### Configuração de Inteligência Artificial

A app suporta dois motores de IA (configuráveis em **Definições → Inteligência Artificial (IA)**):

1. **Google Gemini (Nuvem):**
   Requer uma chave de API gratuita do Google AI Studio (<https://aistudio.google.com/app/apikey>). A chave é salva apenas em `SharedPreferences` locais e nunca é enviada para terceiros.
2. **Gemma 2B (On-Device / 100% Offline):**
   Executa o modelo do Google diretamente no dispositivo através do Google MediaPipe Tasks GenAI (`tasks-genai`). Não requer chave de API nem ligação à internet. Pode ser descarregado diretamente nas Definições (~1.3 GB) ou importado a partir de um ficheiro `.bin` existente no telemóvel.

### Estrutura do projecto

```
app/src/main/java/mz/co/kevin/concursos/
├── data/
│   ├── ai/            # GoogleAiService — chamadas REST ao Gemini
│   ├── local/         # Room: AppDatabase, DAOs, Converters
│   ├── model/         # Entidades e modelos de domínio
│   ├── remote/        # UfsaScraper — parsing do portal da UFSA
│   ├── repository/    # UfsaRepository, PerfilEmpresaRepository
│   └── settings/      # Preferências da app
├── ui/
│   ├── components/    # Cards reutilizáveis
│   ├── screens/       # Ecrãs + ViewModels (Concursos, Guardados, ...)
│   └── theme/         # Cores, tipografia, tema Compose
├── worker/            # DailySyncWorker (WorkManager)
├── MainActivity.kt
└── UfsaApplication.kt
```

### Aviso legal

Este projecto não tem qualquer afiliação com a UFSA nem com o Governo de
Moçambique. Depende da estrutura HTML do portal público, que pode mudar sem
aviso. Usa de forma responsável e respeita os termos de utilização do site de
origem.

---

## 🇬🇧 English

### What it is

**Concursos** scrapes public data from the Mozambican procurement portal
[ufsa.gov.mz](https://www.ufsa.gov.mz/) and presents it in a modern Jetpack
Compose UI. Features:

- List open, awarded and cancelled tenders;
- Filter by province, free text and IT/tech category;
- Tender detail view (modality, subject, procuring entity, dates, ...);
- Bookmark tenders (stored locally);
- Browse registered suppliers (CEF);
- Periodic notifications for new tenders via a background sync worker
  (WorkManager);
- Optional AI recommendations: describe your company profile and Google Gemini
  ranks the most relevant tenders. Requires your own API key.

### Tech stack

Kotlin · Jetpack Compose + Material 3 · MVVM (ViewModel + StateFlow) · Room ·
OkHttp + Jsoup · WorkManager · Google Gemini REST API · Gradle (Kotlin DSL).

Requirements: `minSdk 26`, `targetSdk 37`, JDK 11+.

### Build

```bash
git clone https://github.com/OWNER/Concursos.git
cd Concursos
echo "sdk.dir=$HOME/Android/Sdk" > local.properties   # point at your SDK
./gradlew assembleDebug
./gradlew test
```

Or open the folder in Android Studio.

### Gemini API key

The AI feature is **optional** and off by default. The key is entered by the
user in **Settings → Gemini key** and stored only locally in device
`SharedPreferences`. **No key ships with the source.** Get one at
<https://aistudio.google.com/app/apikey>.

### Disclaimer

Not affiliated with UFSA or the Government of Mozambique. It relies on the public
portal's HTML structure, which may change at any time. Use responsibly and
respect the source site's terms of use.

---

## Releases & signing

Versioning is automatic:

- **`versionName`** comes from the latest git tag (`v1.4.2` → `1.4.2`);
  **`versionCode`** is the commit count on `HEAD`.
- `.github/workflows/release.yml` builds a **signed** APK + AAB and publishes a
  GitHub Release marked *latest*. It runs when a `v*.*.*` tag is pushed, or
  manually (*Actions → Release → Run workflow*) which bumps the last tag first.
- The newest build is always at **[/releases/latest](../../releases/latest)**.

### Signing secrets (repository → Settings → Secrets → Actions)

| Secret | Value |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -i your-release.jks` (one line, no wrapping) |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

```sh
# from a machine that has the keystore + gh CLI:
gh secret set KEYSTORE_BASE64 < <(base64 -i path/to/release.jks)
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS
gh secret set KEY_PASSWORD
```

For a **local** signed release build, copy `keystore.properties.example` to
`keystore.properties` (git-ignored) and point it at your `.jks`. Without any
keystore, `assembleRelease` falls back to the debug signature so it never fails.

R8/shrinking is still off (see issue #8); `app/proguard-rules.pro` is ready for
when it's enabled.

---

## Contribuir / Contributing

Vê / see [CONTRIBUTING.md](CONTRIBUTING.md) e o
[Código de Conduta](CODE_OF_CONDUCT.md).

## Licença / License

[MIT](LICENSE) © 2026 Concursos UFSA &amp; CEF
