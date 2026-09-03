# Política de Segurança / Security Policy

## Reportar uma vulnerabilidade / Reporting a vulnerability

Não abras uma *issue* pública para vulnerabilidades. Envia os detalhes para
**kelvineuclides1@gmail.com** ou usa o
[Private Vulnerability Reporting](https://docs.github.com/code-security/security-advisories/guidance-on-reporting-and-writing-information-about-vulnerabilities/privately-reporting-a-security-vulnerability)
do GitHub. Tentamos responder no prazo de 7 dias.

Do not open a public issue for vulnerabilities. Email **kelvineuclides1@gmail.com**
or use GitHub Private Vulnerability Reporting. We aim to respond within 7 days.

## Notas / Notes

- A chave de API do Google Gemini é fornecida pelo utilizador e guardada apenas
  em `SharedPreferences` no dispositivo. Nunca é enviada para servidores deste
  projecto (não existem). É usada apenas em pedidos directos a
  `generativelanguage.googleapis.com`.
- A app não recolhe telemetria nem envia dados pessoais para terceiros.
- `local.properties`, ficheiros de *keystore* e chaves não devem ser
  versionados (ver `.gitignore`).
