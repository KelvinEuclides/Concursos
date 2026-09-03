# Contribuir para o Concursos / Contributing

Obrigado pelo interesse! / Thanks for your interest!

## 🇵🇹 Português

### Antes de começar

- Abre uma *issue* a descrever o bug ou a ideia antes de fazeres um PR grande.
- Para mudanças pequenas (typos, correcções óbvias) podes ir directo ao PR.

### Ambiente

1. Android Studio Ladybug ou mais recente, ou o Gradle wrapper incluído.
2. Cria o `local.properties` com `sdk.dir` a apontar para o teu Android SDK.
3. Confirma que compila e que os testes passam:

   ```bash
   ./gradlew assembleDebug test
   ```

### Estilo de código

- Kotlin oficial (`kotlin.code.style=official`, já definido em `gradle.properties`).
- Segue as convenções do código existente: nomes de domínio em português,
  ViewModels expõem `StateFlow`, sem lógica de rede na UI.
- Mantém as funções pequenas e testáveis; o parsing do scraper deve ter
  cobertura de testes (ver `app/src/test/.../UfsaScraperTest.kt`).

### Pull Requests

- Um PR por assunto. Descreve **o quê** e **porquê**.
- Não incluas `local.properties`, ficheiros de *keystore*, chaves de API nem
  artefactos de build.
- Referencia a issue relacionada (`Closes #123`).

### Commits

Mensagens no imperativo e concisas, ex.: `Adiciona filtro por data de abertura`.

## 🇬🇧 English

### Before you start

- Open an issue describing the bug or idea before a large PR.
- Small fixes (typos, obvious bugs) can go straight to a PR.

### Setup

1. Android Studio Ladybug+ or the bundled Gradle wrapper.
2. Create `local.properties` with `sdk.dir` pointing at your Android SDK.
3. Make sure it builds and tests pass:

   ```bash
   ./gradlew assembleDebug test
   ```

### Code style

- Official Kotlin style (already set in `gradle.properties`).
- Match the existing code: Portuguese domain names, ViewModels expose
  `StateFlow`, no network logic in the UI layer.
- Keep functions small and testable; scraper parsing changes should come with
  tests (`app/src/test/.../UfsaScraperTest.kt`).

### Pull Requests

- One topic per PR. Explain **what** and **why**.
- Never commit `local.properties`, keystores, API keys or build outputs.
- Link the related issue (`Closes #123`).

### Commits

Short, imperative messages, e.g. `Add filter by opening date`.
