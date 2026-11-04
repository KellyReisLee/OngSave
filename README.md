<div align="center">

<img src="docs/screenshots/00-banner.png" alt="Painéis do administrador, do motorista e da empresa no OngSave" width="100%">

# OngSave

**Logística B2B para redistribuição de excedentes de alimentos.**
Empresas publicam o que sobrou, ONGs aceitam, motoristas parceiros recolhem e entregam, e a plataforma comprova cada etapa com código, token e foto.

[![CI](https://github.com/Evolua-app/ongsave/actions/workflows/ci.yml/badge.svg)](https://github.com/Evolua-app/ongsave/actions/workflows/ci.yml)
[![CodeQL](https://github.com/Evolua-app/ongsave/actions/workflows/codeql.yml/badge.svg)](https://github.com/Evolua-app/ongsave/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Jakarta EE](https://img.shields.io/badge/Servlet%206%20%C2%B7%20JSP%203.1-Jakarta%20EE%2010-1f6feb)
![Tomcat](https://img.shields.io/badge/Tomcat-10.1-F8DC75?logo=apachetomcat&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-4169E1?logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-pronto-2496ED?logo=docker&logoColor=white)

[Demonstração](#-demonstração) · [Funcionalidades](#-funcionalidades) · [Arquitetura](#-arquitetura) · [Como executar](#-como-executar) · [Segurança](#-segurança) · [API](#-api)

</div>

---

## Sumário

- [O problema](#-o-problema)
- [Demonstração](#-demonstração)
- [Funcionalidades](#-funcionalidades)
- [Fluxo de uma doação](#-fluxo-de-uma-doação)
- [Arquitetura](#-arquitetura)
- [Modelo de dados](#-modelo-de-dados)
- [Tecnologias](#-tecnologias)
- [Como executar](#-como-executar)
- [Configuração](#-configuração)
- [Deploy](#-deploy)
- [Segurança](#-segurança)
- [API](#-api)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Problemas comuns](#-problemas-comuns)
- [Limitações conhecidas e próximos passos](#-limitações-conhecidas-e-próximos-passos)

## 🎯 O problema

Supermercados, padarias e atacadistas descartam todos os dias alimentos ainda próprios para consumo, enquanto ONGs a poucos quilómetros não têm como ir buscá-los. O OngSave liga as duas pontas:

- **Empresas doadoras** pagam uma assinatura, publicam os excedentes e recebem relatórios ESG (CO₂ evitado, refeições geradas, economia com descarte).
- **ONGs** recebem propostas compatíveis com a sua capacidade, categorias aceites e câmara fria.
- **Motoristas parceiros** num raio de 15 km aceitam a corrida e recebem o frete na carteira digital.
- **Administração** aprova cadastros, analisa ocorrências e acompanha as finanças da plataforma.

## 🎬 Demonstração

A página inicial tem o link **Ver demonstrativo** (`/demo`). Cada cartão entra sem senha no painel de um perfil, e os quatro podem ficar abertos ao mesmo tempo em abas do mesmo navegador. Com a simulação ligada, uma pequena frota publica, recolhe e entrega lotes sozinha: os KPIs, os mapas e a carteira mudam a cada poucos segundos.

<p align="center">
  <img src="docs/screenshots/02-demo.png" alt="Página Ver demonstrativo com os cartões Empresa, ONG, Motorista e Administrador" width="900">
</p>

<details open>
<summary><b>🏪 Empresa doadora</b></summary>

| Dashboard operacional | Registar excedente |
|---|---|
| <img src="docs/screenshots/05-empresa-dashboard.png" alt="Dashboard da empresa"> | <img src="docs/screenshots/06-empresa-nova-doacao.png" alt="Formulário de novo lote"> |

| Relatório ESG |
|---|
| <img src="docs/screenshots/07-empresa-esg.png" alt="Relatório ESG da empresa"> |
</details>

<details open>
<summary><b>🤝 ONG</b></summary>

<img src="docs/screenshots/08-ong-dashboard.png" alt="Dashboard da ONG">
</details>

<details open>
<summary><b>🚚 Motorista parceiro (mobile first)</b></summary>

<p align="center">
  <img src="docs/screenshots/12-motorista-dashboard.png" alt="Visão geral do motorista" width="260">
  <img src="docs/screenshots/13-motorista-entregas.png" alt="Entregas disponíveis no mapa" width="260">
  <img src="docs/screenshots/14-motorista-carteira.png" alt="Carteira digital do motorista" width="260">
</p>
</details>

<details open>
<summary><b>🛡️ Administrador</b></summary>

| Controlo global | Financeiro |
|---|---|
| <img src="docs/screenshots/09-admin-dashboard.png" alt="Dashboard do administrador"> | <img src="docs/screenshots/10-admin-financeiro.png" alt="Auditoria financeira"> |

| Gestão de utilizadores |
|---|
| <img src="docs/screenshots/11-admin-utilizadores.png" alt="Gestão de utilizadores"> |
</details>

<details>
<summary><b>🌐 Páginas públicas</b></summary>

| Página inicial | Login |
|---|---|
| <img src="docs/screenshots/01-home.png" alt="Página inicial"> | <img src="docs/screenshots/03-login.png" alt="Login"> |

| Recuperar o acesso (link por e-mail) |
|---|
| <img src="docs/screenshots/04-recuperar-senha.png" alt="Recuperação de senha" width="600"> |
</details>

> As capturas foram feitas com os dados de demonstração e a simulação ligada. Nelas os mapas aparecem sem o fundo de ruas, porque foram tiradas num ambiente sem acesso ao OpenStreetMap; na aplicação o mapa mostra as ruas normalmente.

**Contas de demonstração** (senha `ongsave123`, criadas só com `app.dadosDemo=true` num banco vazio):

| Perfil | E-mail |
|---|---|
| Empresa | `empresa@ongsave.com` |
| ONG | `ong@ongsave.com` |
| Motorista | `motorista@ongsave.com` |
| Administrador | `admin@ongsave.com` |

## ✨ Funcionalidades

| Perfil | O que faz |
|---|---|
| **Empresa** | Publica lotes com foto, categoria, peso, conservação e janela de recolha · escolhe a ONG ou deixa a plataforma encontrar a mais próxima compatível · regista a retirada (peso e temperatura) e gera o código para o motorista · acompanha o veículo no mapa · relatório ESG com CO₂ evitado, refeições e economia · gere plano, franquia e faturas |
| **ONG** | Aceita ou recusa propostas respeitando a capacidade diária · vê o motorista a chegar · gera o token de entrega (10 min, 5 tentativas) · confere peso e condição e abre ocorrência quando há divergência · envia documentos para aprovação |
| **Motorista** | Vê as ofertas no raio, compatíveis com o seu veículo · aceite atómico (só um motorista ganha) · rota ativa com GPS (só com consentimento LGPD) · confirma a coleta com o código da empresa e a entrega com token + foto · carteira com fretes retidos, liberados e saques |
| **Admin** | Fila de aprovação de cadastros e documentos · suspende, bloqueia ou reativa contas com justificativa auditada · resolve ocorrências (liberar, ajustar ou estornar o frete) · mapa ao vivo de todas as entregas · financeiro (receita, fretes, reserva de liquidez) · parâmetros da plataforma |

**Transversal:** notificações em tempo quase real, auditoria de todas as decisões sensíveis, retenção de dados (posições GPS apagadas após 90 dias), migrações de banco automáticas e modo simulação para apresentações.

## 🔄 Fluxo de uma doação

```mermaid
sequenceDiagram
    autonumber
    actor E as Empresa
    participant P as OngSave
    actor O as ONG
    actor M as Motorista

    E->>P: Publica lote (foto, peso, categoria, validade)
    P->>O: Proposta para a ONG escolhida ou a mais próxima compatível
    O->>P: Aceita (dentro da capacidade do dia)
    P->>M: Oferta para motoristas no raio com veículo compatível
    M->>P: Aceita a corrida (aceite atómico)
    E->>P: Regista a retirada → gera código
    M->>P: Digita o código na empresa → coleta confirmada
    loop Durante o trajeto
        M-->>P: Posição GPS (com consentimento)
        P-->>E: Veículo no mapa
        P-->>O: Veículo no mapa
    end
    O->>P: Gera token (10 min)
    M->>P: Token + foto da descarga → entrega validada
    O->>P: Confere peso e condição
    alt Tudo certo
        P->>M: Frete liberado na carteira
    else Divergência ou alimento impróprio
        P->>P: Ocorrência para o administrador decidir
    end
```

## 🏛 Arquitetura

MVC clássico em Jakarta EE, sem frameworks: um controlador frontal para as páginas, APIs JSON para as ações de cada perfil, serviços com as regras de negócio e DAOs com todo o SQL.

```mermaid
flowchart LR
    subgraph Navegador
        JSP[Páginas JSP<br/>Tailwind · Leaflet · Chart.js]
        JS[assets/js<br/>osApi · atualização periódica]
    end

    subgraph Tomcat["Tomcat 10.1"]
        direction TB
        F[Filtros<br/>SegurancaFilter · DemoFilter]
        FC[FrontController<br/>páginas, login, cadastro, /demo]
        API[web/api/*Api<br/>JSON por perfil]
        S[service/*Service<br/>regras, transações, auditoria]
        D[dao/*DAO<br/>PreparedStatement]
        SIM[Simulador<br/>frota e GPS simulados]
        MIG[Migrador<br/>V001…V004]
    end

    DB[(PostgreSQL<br/>Neon)]
    EXT[OpenStreetMap · OSRM<br/>Nominatim · Brevo]

    JSP --> F --> FC
    JS -->|fetch + CSRF| F --> API
    FC --> S
    API --> S --> D --> DB
    SIM --> S
    MIG --> DB
    S -.-> EXT
```

**Decisões principais**

- **Transações no service:** cada DAO recebe a `Connection` aberta pelo service. Validar uma entrega grava o lote, o frete e as notificações numa única transação.
- **Regras críticas no banco:** um motorista só pode ter uma entrega ativa (índice único parcial) e só existe um frete por lote. O aceite da corrida é um `UPDATE … WHERE estado = 'aguardando'`, por isso dois motoristas nunca ganham o mesmo lote.
- **Migrações versionadas** (`WEB-INF/db/V###__descricao.sql`) aplicadas no arranque, com advisory lock: funcionam com várias instâncias e com o pooler do Neon.
- **Estado inicial no HTML:** cada painel recebe o JSON do perfil já na primeira resposta (sem tela vazia) e depois só atualiza por polling.

## 🗃 Modelo de dados

```mermaid
erDiagram
    USUARIOS ||--o| EMPRESAS : "perfil empresa"
    USUARIOS ||--o| MOTORISTAS : "perfil motorista"
    USUARIOS ||--o| ONGS : "perfil ong"
    USUARIOS ||--o{ DOCUMENTOS : envia
    USUARIOS ||--o{ NOTIFICACOES : recebe
    USUARIOS ||--o{ SENHA_TOKENS : "recuperação"
    USUARIOS ||--o{ LOTES : "empresa publica"
    USUARIOS ||--o{ LOTES : "ong recebe"
    USUARIOS ||--o{ LOTES : "motorista leva"
    LOTES ||--o{ POSICOES : rastreio
    LOTES ||--o{ LOTE_RECUSAS : "recusado por"
    LOTES ||--o| MOVIMENTOS : frete
    LOTES ||--o{ OCORRENCIAS : gera
    USUARIOS ||--o{ MOVIMENTOS : carteira
    USUARIOS ||--o{ FATURAS : "empresa paga"

    USUARIOS {
        bigserial id PK
        varchar perfil "empresa | motorista | ong | admin"
        varchar email UK
        varchar senha_hash "PBKDF2-SHA256"
        varchar status "pendente | ativo | suspenso | bloqueado | rejeitado"
    }
    LOTES {
        bigserial id PK
        bigint empresa_id FK
        bigint ong_id FK
        bigint motorista_id FK
        numeric peso_kg
        varchar estado "aguardando | aceito | transito | entregue | cancelado"
        varchar retirada_codigo
        varchar token_codigo
        numeric frete
    }
    MOVIMENTOS {
        bigserial id PK
        varchar tipo "frete | saque | ajuste"
        numeric valor
        varchar status "disponivel | retido | estornado | solicitado | pago"
    }
    SENHA_TOKENS {
        bigserial id PK
        char token_hash "SHA-256, nunca o token"
        timestamptz expira_em
        timestamptz usado_em
    }
```

Tabelas também presentes: `plataforma_config` (parâmetros em JSONB editados pelo admin), `notas_admin`, `auditoria` e `schema_versao` (controlo das migrações).

## 🧰 Tecnologias

| Camada | Tecnologia |
|---|---|
| Back-end | Java 17 · Jakarta Servlet 6 · JSP 3.1 + JSTL 3 · JDBC puro com pool próprio |
| Banco | PostgreSQL 15+ (Neon serverless) · migrações SQL versionadas |
| Front-end | JSP + JavaScript sem build · Tailwind CSS · Font Awesome · Leaflet + OpenStreetMap · Chart.js |
| Serviços externos | OSRM (rotas) · Nominatim (geocodificação) · ViaCEP · Brevo (e-mail transacional) |
| Infra | Tomcat 10.1 · Docker multi-stage · GitHub Actions (build, CodeQL, Dependabot) · Render/Railway/Fly |

## 🚀 Como executar

### Pré-requisitos

- Java 17 ou superior
- Apache Tomcat 10.1 (Eclipse) ou Maven 3.9 ou Docker
- Uma base PostgreSQL. O plano gratuito do [Neon](https://neon.tech) serve.

### 1. Banco de dados

Há duas formas:

- **Automática:** deixe o banco vazio. No primeiro arranque a aplicação cria as tabelas e os dados de demonstração.
- **Manual:** no Neon, abra o **SQL Editor**, cole o conteúdo de [`database/ongsave_neon.sql`](database/ongsave_neon.sql) e clique em **Run**.

### 2. Configuração

Copie `src/main/webapp/WEB-INF/ongsave.properties.exemplo` para `ongsave.properties` (na mesma pasta) e cole a *connection string* do Neon:

```properties
db.url=postgresql://neondb_owner:SENHA@ep-xxxx-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require
```

O `ongsave.properties` está no `.gitignore` e nunca vai para o repositório.

### 3. Escolha como rodar

<details open>
<summary><b>Eclipse (Dynamic Web Project)</b></summary>

1. Na raiz do projeto, execute `./baixar-libs.sh` (macOS/Linux) ou `baixar-libs.bat` (Windows). Isto coloca o driver PostgreSQL e a JSTL em `WEB-INF/lib`.
2. `File › Import › Existing Projects into Workspace` e selecione a pasta do projeto. Depois `F5`.
3. Em `Properties › Targeted Runtimes`, marque o seu **Tomcat 10.1**.
4. `Run As › Run on Server` e abra <http://localhost:8080/ongsave/>.
</details>

<details>
<summary><b>Maven</b></summary>

```bash
mvn clean package            # gera target/ongsave.war
```

Copie o `ongsave.war` para `webapps/` de um Tomcat 10.1. Pelo Maven, as bibliotecas vêm do `pom.xml` e os JARs de `WEB-INF/lib` são ignorados.
</details>

<details>
<summary><b>Docker</b></summary>

```bash
docker build -t ongsave .
docker run -p 8080:8080 \
  -e DATABASE_URL="postgresql://utilizador:senha@ep-xxxx.neon.tech/neondb?sslmode=require" \
  ongsave
```

Abra <http://localhost:8080/>. No contêiner a aplicação fica na raiz e o Tomcat corre com um utilizador sem privilégios.
</details>

## ⚙️ Configuração

Cada chave pode vir do `ongsave.properties`, de uma propriedade da JVM (`-Ddb.url=…`) ou de uma variável de ambiente. A variável tem prioridade: `db.url` → `DATABASE_URL`, `app.demo` → `ONGSAVE_APP_DEMO`, `mail.brevoApiKey` → `ONGSAVE_MAIL_BREVOAPIKEY`, etc.

| Chave | Padrão | Descrição |
|---|---|---|
| `db.url` | — | **Obrigatória.** Connection string do Neon (`postgresql://…`) ou URL JDBC |
| `db.usuario` / `db.senha` | — | Só se a senha tiver caracteres que estraguem a URL |
| `db.pool.maximo` | `10` | Conexões simultâneas |
| `app.demo` | `true` | Página `/demo` com entrada sem senha. **Liga também a simulação.** |
| `app.dadosDemo` | `true` | Cria contas e lotes de demonstração num banco vazio |
| `app.adminSenha` | — | Substitui a senha inicial do `admin@ongsave.com` no arranque |
| `app.urlPublica` | — | Endereço público, usado nos links dos e-mails (ex.: `https://ongsave.onrender.com`) |
| `app.simulacao` | `false` | Frota e GPS simulados (sempre ligada com `app.demo=true`) |
| `mail.brevoApiKey` | — | Chave da API da [Brevo](https://www.brevo.com) para e-mails de recuperação de senha |
| `mail.remetente` | — | Remetente verificado na Brevo |
| `geo.validarProximidade` | `false` | Exige o motorista a menos de `geo.raioValidacaoMetros` para coletar e entregar |
| `geo.raioValidacaoMetros` | `150` | Raio da validação por GPS |
| `geo.geocodificar` | `true` | Converte endereços em coordenadas pelo Nominatim |
| `sim.segundosPorTrecho` · `sim.frota` · `sim.intervaloMs` · `sim.contasManuais` | `60` · `2` · `3000` · `motorista@ongsave.com` | Afinação do modo simulação |

> Sem `mail.brevoApiKey`, os links de recuperação de senha aparecem apenas no log do Tomcat. Isto é útil para testar no Eclipse.

## ☁️ Deploy

1. Suba o repositório para o GitHub.
2. No Render, Railway ou Fly.io, crie um **Web Service** a partir do repositório com ambiente **Docker**.
3. Defina as variáveis de ambiente: `DATABASE_URL` e, para produção, `ONGSAVE_APP_DEMO=false`, `ONGSAVE_APP_ADMINSENHA`, `ONGSAVE_APP_URLPUBLICA`, `ONGSAVE_MAIL_BREVOAPIKEY` e `ONGSAVE_MAIL_REMETENTE`.
4. A porta vem de `$PORT`. O HTTPS do proxy é reconhecido sozinho (cookie `Secure` e HSTS ligam automaticamente).

**Checklist de produção**

- [ ] `app.demo=false` (desliga a entrada sem senha e a simulação)
- [ ] `app.dadosDemo=false` **antes** do primeiro arranque e `app.adminSenha` com uma senha forte
- [ ] `geo.validarProximidade=true`
- [ ] `app.urlPublica` + Brevo configurados (recuperação de senha)
- [ ] Credenciais só em variáveis de ambiente

## 🔐 Segurança

O projeto segue as recomendações da **OWASP** (Top 10, ASVS e Cheat Sheets). O relatório completo de vulnerabilidades e como reportá-las está em [`SECURITY.md`](SECURITY.md).

| Área | Controlo |
|---|---|
| **Senhas** | PBKDF2-HMAC-SHA256 com **600 000 iterações** e sal de 16 bytes (recomendação OWASP). Hashes antigos são refeitos no login seguinte. Senhas da lista das mais comuns são recusadas. |
| **Força bruta** | Bloqueio de 15 min após 5 senhas erradas na mesma conta, mais **limite por IP** em login (10/5 min), cadastro (5/h), recuperação de senha (5/15 min) e ações da API (90/min por utilizador). Respostas `429` com `Retry-After`. |
| **Recuperação de senha** | Link de **uso único, válido 30 min**, enviado por e-mail. Só o hash SHA-256 do token fica no banco. A resposta é igual exista ou não a conta, e o link nunca usa o cabeçalho `Host` do pedido. |
| **Sessão** | Nova sessão no login (sem fixação), cookie `HttpOnly` + `SameSite=Lax` + `Secure` em HTTPS, expiração por inatividade. Contas suspensas perdem a sessão em até 1 minuto. |
| **CSRF** | Token por sessão em todo POST (formulário ou cabeçalho `X-CSRF-Token`), comparação em tempo constante. |
| **Autorização** | Lista branca de páginas por perfil. Cada ação confere se o lote ou documento pertence a quem pede (sem IDOR). Fotos só para quem participa do lote ou para o admin. |
| **Injeção** | 100% `PreparedStatement`. Escape de HTML no JSP (`c:out`, `fn:escapeXml`) e no JavaScript (`esc()` / `textContent`). JSON embutido com `< > & '` escapados. |
| **Uploads** | Tipo detetado pela assinatura do ficheiro (JPEG, PNG, WEBP, HEIC, PDF), não pela extensão. Limite de tamanho, e o conteúdo é servido com CSP `sandbox`. |
| **Cabeçalhos** | `Content-Security-Policy` com lista branca de origens, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `Cross-Origin-Opener-Policy` e `Strict-Transport-Security`. |
| **Redirecionamentos** | O parâmetro `next` do login só aceita caminhos internos (sem open redirect). |
| **Erros** | Mensagens neutras para o utilizador e detalhes só no log. Credenciais do banco mascaradas nos logs. |
| **Privacidade (LGPD)** | GPS só durante a entrega e com consentimento. Posições apagadas após 90 dias, notificações lidas após 180 dias e tokens de senha após 7 dias. |
| **Supply chain** | Chart.js, Leaflet e Font Awesome com versão fixada, Dependabot (Maven, Docker, Actions) e CodeQL `security-extended` em cada push. |
| **Infra** | Contêiner sem root, `RemoteIpValve` só confia em proxies de redes internas, segredos fora do repositório. |
| **Modo demonstração** | Aviso no log ao arrancar. As contas partilhadas não podem trocar senha nem e-mail, e contas suspensas por visitantes são reativadas ao entrar de novo. |

## 📡 API

Todas as rotas ficam sob `/api`, exigem sessão e o perfil certo e devolvem JSON. Os erros vêm como `{"erro": "mensagem"}` com o status adequado (`400`, `401`, `403`, `404`, `409`, `413`, `429`, `503`).

| Perfil | Rotas |
|---|---|
| Todos | `GET /api/conta/plataforma` · `GET /api/conta/fotos/{lote\|entrega}/{id}` · `POST /api/conta/notificacoes/lidas` · `POST /api/conta/documentos/{id}` · `POST /api/conta/senha` |
| Empresa | `GET estado` · `POST lotes`, `lotes/{id}/cancelar`, `lotes/{id}/retirada`, `perfil`, `plano` |
| Motorista | `GET estado`, `ofertas?lat&lon`, `entrega` · `POST aceitar`, `coleta`, `posicao`, `finalizar`, `perfil`, `consentimento`, `sacar`, `contestar` |
| ONG | `GET estado` · `POST propostas/{id}/aceitar\|recusar`, `lotes/{id}/token\|conferencia\|reportar`, `perfil` |
| Admin | `GET estado`, `documentos/{id}/arquivo` · `POST usuarios/{id}/status\|notas\|solicitar`, `documentos/{id}`, `ocorrencias/{id}/analisar\|resolver`, `plataforma` |

Páginas: `/` · `/login` · `/cadastro` · `/recuperar-senha` · `/demo` · `/demo/{perfil}` · `/{perfil}/{pagina}` · `/logout`.

## 📁 Estrutura do projeto

```
ongsave/
├─ .github/                 CI, CodeQL e Dependabot
├─ database/                ongsave_neon.sql (banco completo para colar no Neon)
├─ docs/screenshots/        imagens deste README
├─ src/main/java/br/com/ongsave/
│  ├─ config/               Configuracao (ficheiro / variáveis de ambiente)
│  ├─ controller/           FrontController (páginas, login, cadastro, recuperação, demo)
│  ├─ web/                  Inicializador, SegurancaFilter, DemoFilter, Servicos
│  │  └─ api/               ApiBase + EmpresaApi, OngApi, MotoristaApi, AdminApi, ContaApi
│  ├─ service/              regras de negócio, EmailService, Simulador, Geocodificador
│  ├─ dao/                  todo o SQL (UsuarioDAO, LoteDAO, SenhaTokenDAO, ...)
│  ├─ db/                   PoolConexoes, Banco (transações), Sql, Migrador
│  ├─ model/                entidades e Perfil, ErroNegocio
│  └─ util/                 Senhas, LimiteTaxa, Json, Geo, Imagens, Codigos, Documentos
├─ src/main/webapp/
│  ├─ META-INF/context.xml  cookie SameSite e RemoteIpValve
│  ├─ WEB-INF/db/           migrações V001 a V004
│  ├─ WEB-INF/jsp/          páginas (public, empresa, ong, motorista, admin, erros)
│  └─ assets/               css e js de cada perfil
├─ Dockerfile
├─ pom.xml
└─ SECURITY.md
```

**Evoluir o banco:** nunca edite uma migração já aplicada. Crie `WEB-INF/db/V005__descricao.sql`, que é aplicado no próximo arranque.

## 🩺 Problemas comuns

| Sintoma | Solução |
|---|---|
| Página "OngSave ainda não está ligado ao banco" | Leia a mensagem: falta `db.url`, o JAR do driver ou a senha está errada. |
| `Cannot find the tag library descriptor for jakarta.tags.core` | Falta a JSTL em `WEB-INF/lib` (execute `baixar-libs`) e `F5` no Eclipse. |
| Primeiro pedido demora alguns segundos | O Neon suspende o banco inativo. O pool volta a ligar sozinho. |
| "Muitas tentativas seguidas a partir desta rede" | Limite por IP atingido. Aguarde o tempo indicado. |
| Não chegou o e-mail de recuperação | Sem Brevo configurada, o link está no log do Tomcat. Com Brevo, confira `app.urlPublica` e o remetente verificado. |
| Motorista não vê ofertas | A ONG ainda não aceitou, o veículo não comporta a carga ou a validade passou. |
| Veículos parados no modo demonstração | Confirme `app.demo=true` (ou `app.simulacao=true`) e reinicie o Tomcat. |

## 🧭 Limitações conhecidas e próximos passos

- **Tailwind Play CDN:** é prático para protótipos, mas em produção o ideal é gerar o CSS no build e remover `'unsafe-inline'` da CSP com nonces.
- **Limites por IP em memória:** com várias instâncias, cada uma conta à parte. Em escala, o próximo passo é usar Redis.
- **Pagamentos:** a carteira e as faturas registam os valores, mas a liquidação deve ser feita por um provedor de pagamentos com conta de custódia.
- **Testes automatizados:** o próximo passo é cobrir os serviços com JUnit e Testcontainers (PostgreSQL) no pipeline de CI.
- **Autenticação de dois fatores** para contas de administrador.

---

<div align="center">

Feito com 🧡 para reduzir o desperdício de alimentos em São Paulo.

</div>
