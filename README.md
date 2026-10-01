# OngSave · plataforma de logística de doação de alimentos

Aplicação Java Web (Servlet 6 / JSP / JDBC) com PostgreSQL **Neon**.
Empresas publicam excedentes, ONGs aceitam as propostas, motoristas fazem a coleta e a entrega,
e a administração modera contas, ocorrências e finanças.

## Arrancar em 4 passos

1. **Bibliotecas** – na raiz do projeto execute `baixar-libs.bat` (Windows) ou `./baixar-libs.sh` (Linux/macOS).
   Isto coloca em `src/main/webapp/WEB-INF/lib` o driver PostgreSQL e a JSTL 3.0.
2. **Credenciais do Neon** – no painel do Neon clique em **Connect**, copie a *connection string* e cole-a em
   `src/main/webapp/WEB-INF/ongsave.properties`:
   ```properties
   db.url=postgresql://neondb_owner:SENHA@ep-xxxx-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
   ```
   Serve tal como vem do Neon (com ou sem `-pooler`). Em alternativa, defina a variável de ambiente `DATABASE_URL`.
3. **Eclipse** – `File › Import › Existing Projects into Workspace` › pasta `OngSaveWebIdeia` › `F5` no projeto.
   Se o Tomcat tiver outro nome: `Properties › Targeted Runtimes` › marque o seu **Tomcat 10.1** (Java 17+).
4. `Run As › Run on Server` › abra **http://localhost:8080/ongsave/**

Na primeira execução as tabelas são criadas sozinhas (migrações em `WEB-INF/db`). Se algo faltar
(URL vazia, driver ausente, senha errada), o site mostra uma página a dizer exatamente o que corrigir,
e o detalhe técnico aparece na consola do Tomcat.

## Maven

O `pom.xml` gera o WAR sem depender do Eclipse (Java 17 e Maven 3.9+):

```bash
mvn clean package          # gera target/ongsave.war
```

Copie o `ongsave.war` para a pasta `webapps` de um Tomcat 10.1 (ou renomeie para `ROOT.war` para abrir na raiz).
No Maven, os JARs de `WEB-INF/lib` são ignorados: as bibliotecas vêm das dependências do `pom.xml`.
No Eclipse, se preferir, `File › Import › Maven › Existing Maven Projects` também funciona.

## Docker

```bash
docker build -t ongsave .
docker run -p 8080:8080 -e DATABASE_URL="postgresql://utilizador:senha@ep-xxxx.neon.tech/neondb?sslmode=require" ongsave
```

Abra **http://localhost:8080/** (no contêiner a aplicação fica na raiz, sem `/ongsave`).
A imagem já vem com `ONGSAVE_APP_SIMULACAO=true` (modo apresentação). Para produção: `-e ONGSAVE_APP_SIMULACAO=false`.

## Deploy (Render, Railway, Fly.io…)

1. Suba o projeto para o GitHub (o `ongsave.properties` fica de fora pelo `.gitignore`).
2. Crie um **Web Service** a partir do repositório, com ambiente **Docker** (o `Dockerfile` é detectado sozinho).
3. Em **Environment**, defina `DATABASE_URL` com a connection string do Neon
   (e, se quiser, `ONGSAVE_APP_SIMULACAO`, `ONGSAVE_APP_ADMINSENHA`).
4. A porta é lida de `$PORT`, que a plataforma define automaticamente.
5. Com HTTPS, ative `<secure>true</secure>` no cookie de sessão do `web.xml`.

> Planos gratuitos "dormem" após alguns minutos sem acesso e levam ~1 min a acordar (o simulador para junto).
> Abra o site uns minutos antes de apresentar.

## Contas de demonstração (senha `ongsave123`)

| Perfil | E-mail |
|---|---|
| Empresa | empresa@ongsave.com |
| Motorista | motorista@ongsave.com |
| ONG | ong@ongsave.com |
| Administrador | admin@ongsave.com |

Só são criadas com `app.dadosDemo=true` (padrão) **num banco vazio**. Com `false` existe apenas o administrador.
Novos cadastros ficam **pendentes** até o administrador aprovar os documentos (`Admin › Utilizadores`).

## Modo apresentação (simulação)

Com `app.simulacao=true` (já ligado em `ongsave.properties`) o servidor faz o papel do GPS e de uma pequena frota:
publica lotes, a ONG aceita, motoristas automáticos (Marina, Paulo, Lucas) vão até a empresa **pelas ruas reais**,
recolhem com o código de retirada, levam até a ONG, validam com token + foto e a ONG confere. Tudo passa pelos
serviços e pelo banco reais, por isso KPIs, notificações, carteira, ESG e auditoria mudam como num uso verdadeiro.
Os painéis atualizam a cada 3 s e os veículos deslizam no mapa. Um selo "Modo demonstração" aparece no canto.

**Sessões separadas:** cada perfil precisa da sua sessão. No mesmo navegador use endereços diferentes
(`http://localhost:8080/ongsave/`, `http://127.0.0.1:8080/ongsave/`) e janelas anónimas/privadas, ou navegadores diferentes.

**Roteiro sugerido (fluxo manual, conduzido por si):**

1. **Empresa** (`empresa@ongsave.com`) › Nova doação › publique um lote (qualquer imagem serve de foto).
2. **ONG** (`ong@ongsave.com`) › aceite a proposta.
3. **Motorista** (`motorista@ongsave.com`) › Entregas › aceite › Rota Ativa › **Simular trajeto**.
   A empresa, a ONG e o admin veem o veículo a aproximar-se.
4. **Empresa** › registre a retirada → aparece o código. **Motorista** › digite o código › Confirmar coleta › **Simular trajeto**.
5. **ONG** › quando aparecer "Na porta", gere o token. **Motorista** › token + **Usar foto de demonstração** › Validar entrega.
6. **ONG** › conferência do recebimento → o frete é liberado na carteira do motorista. **Admin** acompanha tudo no mapa ao vivo.

Enquanto isso, a frota automática mantém `sim.frota` entregas em andamento noutros pontos da cidade.
As rotas usam o OSRM/OpenStreetMap (precisa de internet); sem internet o veículo anda em linha reta.

## Produção (checklist)

- `app.simulacao=false` (desliga a frota automática e volta ao GPS real dos celulares).

- `app.dadosDemo=false` antes da primeira execução e `app.adminSenha=<senha forte>`.
- `geo.validarProximidade=true` (exige o motorista a ≤150 m da empresa/ONG para coletar e entregar).
- HTTPS e, no `web.xml`, `<secure>true</secure>` no cookie de sessão.
- Credenciais por variável de ambiente (`DATABASE_URL`) em vez do ficheiro; o `.gitignore` já exclui `ongsave.properties`
  (há um `ongsave.properties.exemplo` para versionar).

## Fluxo de uma doação

1. **Empresa** publica o lote (foto obrigatória). A ONG escolhida, ou a mais próxima compatível
   (categoria, câmara fria), recebe a proposta.
2. **ONG** aceita (respeitando a capacidade diária) ou recusa; ao recusar, a proposta passa para a próxima ONG compatível.
3. Só depois do aceite da ONG o lote aparece aos **motoristas** do raio. O aceite é atómico: só um motorista ganha (os outros recebem 409),
   e cada motorista tem no máximo uma entrega ativa (garantido por índice único no banco).
4. Na coleta a empresa regista peso/temperatura e recebe um **código de retirada** que o motorista digita.
5. Durante o trajeto o app envia a posição (só com consentimento LGPD; apagada após 90 dias).
6. Na ONG, a instituição gera um **token** (10 min, 5 tentativas) e o motorista valida com o token + **foto da descarga**.
7. O frete entra **retido**. A ONG confere peso e condição: tudo certo → frete liberado; divergência acima da tolerância
   ou alimento impróprio → **ocorrência** para o administrador decidir (liberar, ajustar proporcionalmente ou estornar).

## Arquitetura (MVC + DAO)

```
Navegador (JSP + JavaScript)
   │  páginas / JSON
   ▼
Controller ── controller/FrontController (páginas, login, cadastro) · web/api/*Api (ações de cada perfil)
   │
   ▼
Service ───── service/*Service (regras de negócio, validações, notificações, auditoria, transações)
   │
   ▼
DAO ───────── dao/*DAO (todo o SQL da aplicação; PreparedStatement, sem SQL injection)
   │
   ▼
Model ─────── model/* (entidades que espelham as tabelas)  →  PostgreSQL / Neon
```

```
src/main/java/br/com/ongsave/
├─ model/        Conta, Empresa, Motorista, Ong, Lote, Posicao, Documento, Movimento, Fatura,
│                Ocorrencia, Notificacao, RegistroAuditoria, NotaAdmin · Usuario (sessão), Perfil, ErroNegocio
├─ dao/          UsuarioDAO, EmpresaDAO, MotoristaDAO, OngDAO, LoteDAO, PosicaoDAO, DocumentoDAO,
│                MovimentoDAO, FaturaDAO, OcorrenciaDAO, NotificacaoDAO, AuditoriaDAO, NotaAdminDAO,
│                PlataformaDAO, RelatorioDAO (consultas agregadas do admin)
├─ service/      EmpresaService, MotoristaService, OngService, AdminService, AutenticacaoService,
│                PlataformaService, NotificacaoService, AuditoriaService, ArquivosService,
│                Estatisticas, Geocodificador, Simulador (modo apresentação)
├─ controller/   FrontController
├─ web/          Inicializador (arranque e manutenção), SegurancaFilter, Servicos, api/*Api
├─ db/           PoolConexoes, Banco (transações), Sql (execução JDBC), Migrador
├─ config/       Configuracao (ongsave.properties / variáveis de ambiente)
└─ util/         Json, Geo, Senhas, Codigos, Documentos, Imagens, Tempo
src/main/webapp/
├─ WEB-INF/db/                  esquema (V001), dados iniciais (V002), simulação (V003)
├─ WEB-INF/jsp/                 páginas (só acessíveis pelo controlador)
├─ WEB-INF/jspf/dados.jspf      JSON inicial do painel → lido por assets/js/plataforma.js
└─ assets/js/                   front-end; plataforma.js tem osApi() e a atualização periódica
```

Cada DAO recebe a `Connection` da transação aberta pelo service, por isso uma ação que mexe em várias tabelas
(ex.: validar a entrega grava o lote, o frete e as notificações) é gravada inteira ou não é gravada.
Os DAOs devolvem entidades do model para as regras de negócio e "vistas" (linhas planas já com os nomes
usados pelo JavaScript) para os painéis.

**Segurança:** senhas PBKDF2 (120 000 iterações), bloqueio de 15 min após 5 falhas, nova sessão no login,
CSRF em todo POST, controlo de perfil em todas as rotas, contas suspensas perdem a sessão em até 1 minuto,
fotos e documentos só visíveis a quem participa do lote (ou ao admin), auditoria de todas as decisões sensíveis.

### Principais rotas da API
| Perfil | Rotas |
|---|---|
| Todos | `GET /api/conta/plataforma`, `GET /api/conta/fotos/{lote\|entrega}/{id}`, `POST /api/conta/notificacoes/lidas`, `POST /api/conta/documentos/{id}`, `POST /api/conta/senha` |
| Empresa | `GET estado` · `POST lotes`, `lotes/{id}/cancelar`, `lotes/{id}/retirada`, `perfil`, `plano` |
| Motorista | `GET estado`, `ofertas?lat&lon`, `entrega` · `POST aceitar`, `coleta`, `posicao`, `finalizar`, `perfil`, `consentimento`, `sacar`, `contestar` |
| ONG | `GET estado` · `POST propostas/{id}/aceitar\|recusar`, `lotes/{id}/token\|conferencia\|reportar`, `perfil` |
| Admin | `GET estado`, `documentos/{id}/arquivo` · `POST usuarios/{id}/status\|notas\|solicitar`, `documentos/{id}`, `ocorrencias/{id}/analisar\|resolver`, `plataforma` |

Erros vêm sempre como `{"erro": "mensagem"}` com o status HTTP adequado (401, 403, 404, 409, 422, 503).

### Alterar o banco depois de estar em produção
Nunca edite `V001`/`V002` já aplicados: crie `WEB-INF/db/V003__descricao.sql`. É aplicado no próximo arranque.

## Problemas comuns
| Sintoma | Solução |
|---|---|
| Página "OngSave ainda não está ligado ao banco" | Leia a mensagem: falta `db.url`, o JAR do driver, ou a senha está errada. |
| `Cannot find the tag library descriptor for jakarta.tags.core` | Falta a JSTL em `WEB-INF/lib` (passo 1) e `F5` no Eclipse. |
| Primeiro pedido demora alguns segundos | O Neon suspende o banco inativo; o pool volta a ligar sozinho. |
| Motorista não vê ofertas | A ONG ainda não aceitou a proposta, o veículo não comporta a carga ou a validade passou. |
| Mapa sem localização correta | Complete o endereço no perfil (CEP e número); a posição é obtida pelo OpenStreetMap. |
