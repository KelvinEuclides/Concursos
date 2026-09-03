# Concursos

> App Android para acompanhar concursos públicos de Moçambique publicados pela
> UFSA (Unidade Funcional de Supervisão das Aquisições), com filtros, favoritos,
> notificações de novos concursos e recomendações opcionais por IA (Google Gemini).

> Android app for tracking Mozambican public procurement tenders published by
> UFSA, with filtering, bookmarks, new-tender notifications and optional
> AI recommendations (Google Gemini).

[![Android CI](https://github.com/OWNER/Concursos/actions/workflows/android.yml/badge.svg)](../../actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

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
- **Recomendações por IA** (opcional): descreve o perfil da tua empresa e o
  Google Gemini sugere os concursos mais relevantes. Requer uma chave de API
  fornecida por ti.

### Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Arquitectura | MVVM (ViewModel + StateFlow) |
| Persistência | Room |
| Rede / scraping | OkHttp + Jsoup |
| Background | WorkManager |
| IA | Google Gemini (`generativelanguage.googleapis.com`) via REST |
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

### Chave de API do Gemini

A funcionalidade de IA é **opcional** e desligada por omissão. A chave é
introduzida pelo utilizador em **Definições → Chave Gemini** e guardada apenas
localmente, em `SharedPreferences` do dispositivo. **Nenhuma chave é distribuída
com o código.** Obtém a tua em <https://aistudio.google.com/app/apikey>.

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

## Contribuir / Contributing

Vê / see [CONTRIBUTING.md](CONTRIBUTING.md) e o
[Código de Conduta](CODE_OF_CONDUCT.md).

## Licença / License

[MIT](LICENSE) © 2026 Concursos UFSA &amp; CEF
