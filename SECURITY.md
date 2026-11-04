# Política de segurança

## Como reportar uma vulnerabilidade

Não abra uma *issue* pública. Use **Security › Report a vulnerability** neste repositório (GitHub Private Vulnerability Reporting) ou fale diretamente com as mantenedoras do projeto.

Inclua:

- o que encontrou e o impacto;
- os passos para reproduzir;
- a versão ou o commit afetado.

Respondemos em até 5 dias úteis e combinamos a divulgação depois da correção.

## Versões suportadas

Só a branch `main` recebe correções de segurança.

## Revisão de segurança (novembro de 2026)

A revisão cobriu o código Java, as JSPs, o JavaScript dos painéis, a configuração do Tomcat, o Dockerfile e as dependências, usando como referência o OWASP Top 10 (2021) e o OWASP ASVS 4.0.

### Problemas encontrados e corrigidos

| # | Gravidade | Problema | Correção |
|---|---|---|---|
| 1 | **Alta** | A recuperação de senha pedia só o e-mail e o CPF/CNPJ ou telefone. O CNPJ de uma empresa é público, por isso qualquer pessoa podia tomar a conta de uma empresa ou ONG (A07 Identification and Authentication Failures). | Link de uso único enviado por e-mail: 256 bits aleatórios, válido 30 min, só o hash SHA-256 guardado (migração `V004`), resposta igual exista ou não a conta, máximo de 3 pedidos por hora por conta e `Referrer-Policy: no-referrer` na página do link. O link usa `app.urlPublica`, nunca o cabeçalho `Host`. |
| 2 | Média | PBKDF2 com 120 000 iterações, abaixo das 600 000 recomendadas pela OWASP para PBKDF2-HMAC-SHA256 (A02 Cryptographic Failures). | 600 000 iterações. Os hashes antigos são refeitos de forma transparente no login seguinte. |
| 3 | Média | Sem limite por IP: o bloqueio por conta não impedia testar milhares de contas a partir da mesma origem, nem adivinhar códigos de retirada (A07). | `LimiteTaxa` com janela deslizante: login, cadastro, recuperação, `/demo` e ações da API. Respostas `429` com `Retry-After`. |
| 4 | Média | Sem `Content-Security-Policy` (A05 Security Misconfiguration). | CSP com lista branca de origens, `object-src 'none'`, `base-uri`, `form-action` e `frame-ancestors`, mais `Cross-Origin-Opener-Policy` e `Permissions-Policy` ampliada. |
| 5 | Média | Cookie de sessão sem `SameSite`. Atrás do proxy HTTPS (Render) o Tomcat não sabia que o pedido era seguro, por isso não marcava o cookie como `Secure` nem enviava HSTS. Os limites por IP também veriam só o IP do proxy. | `META-INF/context.xml` com `SameSite=Lax` e `RemoteIpValve` (só confia em proxies de redes internas). |
| 6 | Média | A página `/demo` entra sem senha na conta do administrador. Um visitante podia trocar a senha ou o e-mail das contas partilhadas e estragar a demonstração para os outros. | Na sessão de demonstração não é possível trocar a senha e o e-mail fica fixo. Há um aviso no log ao arrancar e o README traz o checklist com `app.demo=false` para produção. |
| 7 | Baixa | Chart.js carregado de uma URL sem versão (`cdn.jsdelivr.net/npm/chart.js`): uma versão nova, ou comprometida, entraria sozinha (A08 Software and Data Integrity Failures). | Versão fixa 4.4.1 em todas as páginas. |
| 8 | Baixa | O contêiner Docker corria o Tomcat como `root`. | Utilizador de sistema `ongsave` sem shell. |
| 9 | Baixa | Senhas triviais eram aceites (`12345678`, `password123`...). | Lista de senhas comuns recusada no cadastro, na troca e na recuperação. |
| 10 | Baixa | Ao guardar o perfil na demonstração, a conta era gravada direto na sessão, por isso o logout de uma aba não a terminava. | O `DemoFilter` atualiza só o mapa de contas da demonstração. |

### Controlos que já existiam e foram verificados

- `PreparedStatement` em todo o SQL (sem concatenação de dados do utilizador).
- Token CSRF por sessão comparado em tempo constante.
- Nova sessão no login (sem fixação de sessão) e proteção contra open redirect no `next`.
- Verificação de dono em todas as ações sobre lotes e documentos (sem IDOR).
- Escape de HTML no JSP e no JavaScript (`esc()` / `textContent`).
- Uploads validados pela assinatura do ficheiro e servidos com CSP `sandbox`.
- Aceite atómico da corrida e índices únicos para as regras críticas.
- Mensagens de erro neutras e credenciais mascaradas nos logs.

### Riscos aceites ou pendentes

- `'unsafe-inline'` na CSP, por causa dos scripts embutidos nas JSPs e do Tailwind Play CDN. A evolução é gerar o CSS no build e usar nonces.
- O cadastro informa quando o e-mail já existe (enumeração de contas), o que é uma troca comum para a usabilidade. O limite de 5 cadastros por hora por IP reduz o abuso.
- Os limites por IP ficam em memória (por instância).
- Não há 2FA para administradores.
