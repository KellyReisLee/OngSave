# Plano de testes · OngSave

## 1. Objetivo

Verificar que as funcionalidades principais do OngSave cumprem as regras de negócio e devolvem as respostas certas, incluindo os casos de erro: API REST (empresa, ONG, motorista e conta), segurança (autenticação, senha, CSRF, controlo de acesso) e sensores IoT.

## 2. Ambiente

| Item | Versão / configuração |
|---|---|
| Servidor | Apache Tomcat 10.1, Java 17 |
| Banco | PostgreSQL (Neon), criado com `database/ongsave_neon.sql` (migrações V001 a V005) |
| Configuração | `app.demo=true` (contas de demonstração e simulação ligadas) |
| Ferramentas | Postman (coleção `docs/OngSave.postman_collection.json`), Python 3 (`iot/simulador_sensores.py`), navegador |

## 3. Estratégia

- **Testes de API:** cada pedido da coleção do Postman tem testes automáticos (status HTTP e conteúdo da resposta). Rodam todos de uma vez no **Runner** do Postman.
- **Testes de IoT:** o script `iot/simulador_sensores.py` envia leituras em ciclo. A pasta **07 · IoT** da coleção testa cada caso isoladamente.
- **Testes de segurança:** pedidos que a API tem de recusar (sem token CSRF, sem chave do sensor, método não permitido) e testes do serviço de autenticação (senha errada, bloqueio).
- **Teste de interface:** o fluxo de edição de lote no painel da empresa, feito à mão no navegador.

## 4. Casos de teste

**Status:** ✅ Aprovado · ⏳ A executar

| ID | Funcionalidade | Cenário | Entrada | Resultado esperado | Resultado obtido | Status |
|---|---|---|---|---|---|---|
| CT01 | Autenticação | Entrar na conta de demonstração da empresa | `GET /demo/empresa` (pasta 00 do Postman) | 200, painel da empresa com token CSRF | | ⏳ |
| CT02 | Autenticação | Login com a senha certa | `ong@ongsave.com` / senha correta | Entra como ONG | Entrou como ONG | ✅ |
| CT03 | Segurança | Bloqueio por tentativas | 5 logins com senha errada, depois a senha certa | Bloqueia a conta por 15 min, mesmo com a senha certa | "Muitas tentativas sem sucesso. Tente de novo em 15 minuto(s)." | ✅ |
| CT04 | Segurança | Senha guardada com hash | Login de uma conta antiga (120 mil iterações) | Senha regravada como `pbkdf2_sha256$600000$…`, nunca em texto | `pbkdf2_sha256$600000$…` | ✅ |
| CT05 | Segurança | E-mail inexistente | `naoexiste@x.com` | Mesma mensagem da senha errada (não revela se a conta existe) | "E-mail ou senha incorretos." | ✅ |
| CT06 | Empresa · API | Editar lote que aguarda motorista | `PUT /api/empresa/lotes/{id}` com `{"vol":3,"j1":"10:00","j2":"17:00"}` | 200 e volumes = 3 | 200, `"volumes":3` | ✅ |
| CT07 | Empresa · API | Editar lote com janela invertida (erro) | `PUT /api/empresa/lotes/{id}` com `{"j1":"15:00","j2":"09:00"}` | 422 com mensagem de erro | 422, "O início da janela deve ser antes do fim." | ✅ |
| CT08 | Empresa · API | Cancelar lote | `DELETE /api/empresa/lotes/{id}` | 200 e estado `cancelado` | 200, `"estado":"cancelado"` | ✅ |
| CT09 | Empresa · API | Cancelar o mesmo lote outra vez (erro) | `DELETE /api/empresa/lotes/{id}` repetido | 409 | 409 | ✅ |
| CT10 | Empresa · API | Cancelar lote inexistente (erro) | `DELETE /api/empresa/lotes/999999` | 404 | 404 | ✅ |
| CT11 | ONG · API | Recusar proposta de doação | `DELETE /api/ong/propostas/{id}` com motivo | 200 e `"recusada": true` | 200, `"recusada":true` | ✅ |
| CT12 | Motorista · API | Procurar ofertas sem enviar a posição (erro) | `GET /api/motorista/ofertas` | 422 | 422, "Posição inválida." | ✅ |
| CT13 | Motorista · API | Revogar e voltar a dar o consentimento de localização (LGPD) | `DELETE` e depois `POST /api/motorista/consentimento` | 200 e `consentimento` nulo; depois 200 com data | Nulo; depois data preenchida | ✅ |
| CT14 | Segurança · API | Escrita com token CSRF errado (erro) | `PUT /api/empresa/perfil` com `X-CSRF-Token: token-falso` | 403 | 403 | ✅ |
| CT15 | API | Método não permitido (erro) | `PUT /api/conta/plataforma` | 405 | 405 | ✅ |
| CT16 | IoT | Temperatura normal da câmara fria | `POST /api/iot/leituras` `{"sensor":"camara-fria-casa-esperanca","valor":4.5}` | 200, sem alerta | 200, `"alerta":false` | ✅ |
| CT17 | IoT | Temperatura acima de 8 °C | Mesmo sensor, `"valor": 9.4` | 200, alerta e notificação para a ONG | 200, `"alerta":true`, `"notificado":true` | ✅ |
| CT18 | IoT | Temperatura continua alta | Mesmo sensor, `"valor": 9.8` | Alerta gravado, sem repetir a notificação | `"alerta":true`, `"notificado":false` | ✅ |
| CT19 | IoT | Pesagem com divergência acima de 5 % | Balança, lote de 60 kg pesado com 48 kg | 200, alerta para a empresa e para a ONG | 200, alerta "−20 %, tolerância 5 %"; 2 notificações | ✅ |
| CT20 | IoT · Segurança | Leitura sem a chave do sensor (erro) | `POST /api/iot/leituras` sem `X-Sensor-Key` | 401 | 401 | ✅ |
| CT21 | IoT | Sensor inexistente (erro) | `{"sensor":"nao-existe","valor":4}` | 404 | 404 | ✅ |
| CT22 | Interface | Editar lote pelo painel da empresa | Painel da empresa → lote a aguardar → **Editar lote** → mudar volumes → **Guardar** | Aviso "Lote #… atualizado." e volumes novos no detalhe | | ⏳ |

**Resumo:** 22 casos, dos quais 12 de erro ou de segurança. **20 executados, todos aprovados.** O CT01 roda na pasta 00 do Postman e o CT22 é manual, na interface.

## 5. Como reproduzir

1. **API:** importe a coleção no Postman, rode a pasta **00 · Autenticação** e depois as pastas **01 a 07** no **Runner**.
2. **IoT:** com o Tomcat ligado, rode `python3 iot/simulador_sensores.py`. Os alertas aparecem no terminal e no sino de notificações dos painéis da ONG e da empresa.
3. **Autenticação (CT02 a CT05):** na página `/login`, entre com `ong@ongsave.com` e a senha `ongsave123`; depois erre a senha 5 vezes. Para ver o hash, rode no Neon `SELECT email, left(senha_hash, 30) FROM usuarios;`.
   Depois do teste de bloqueio, a conta volta a entrar ao fim de 15 minutos.

## 6. Evidências

As evidências (prints do Runner do Postman, da saída do simulador IoT e do painel com o alerta) estão no PDF da entrega.
