# FlowPay

Sistema de distribuicao e monitoramento de atendimentos para o desafio tecnico FlowPay.

## Stack

- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, RabbitMQ, SSE, Swagger/OpenAPI.
- Frontend: React, TypeScript, Vite, SSE via `EventSource`.
- Infra local: Docker Compose com PostgreSQL e RabbitMQ Management.

## Funcionalidades

- Cadastro de atendimentos por assunto persistido.
- Distribuicao assincrona para o squad correto.
- Limite de 3 atendimentos ativos por atendente.
- Fila FIFO por squad quando nao ha vaga.
- Finalizacao com redistribuicao automatica da fila.
- Dashboard em tempo real via SSE.
- Reports com graficos de fila, ocupacao e finalizados.
- Tela de atendimentos com filtros, detalhes em modal e finalizacao.
- Tela de atendentes agrupados por squad.

## Deploy Recomendado

### Backend no Render

Arquivos preparados:

- `render.yaml`: blueprint do Render com Web Service + PostgreSQL.
- `Back/FlowPay/Dockerfile`: build Docker do backend Java 21.

Passos:

1. Suba o repositório para GitHub.
2. No Render, use `New > Blueprint` e selecione o repositório.
3. Confirme a criação do banco `flowpay-postgres` e do serviço `flowpay-backend`.
4. Depois do primeiro deploy, copie a URL pública do backend, por exemplo `https://flowpay-backend.onrender.com`.
5. Configure no Render a variável `FLOWPAY_CORS_ALLOWED_ORIGINS` com a URL final do frontend Vercel, por exemplo `https://flowpay.vercel.app`.
6. Opcional para previews da Vercel: configure `FLOWPAY_CORS_ALLOWED_ORIGIN_PATTERNS=https://*.vercel.app`.

No Render, RabbitMQ fica desligado por padrão:

```text
FLOWPAY_RABBIT_ENABLED=false
FLOWPAY_DISTRIBUICAO_SCHEDULER_ENABLED=true
```

Assim o scheduler continua distribuindo pela fila lógica no banco. Se quiser RabbitMQ em produção, crie um broker externo e configure `SPRING_RABBITMQ_*` + `FLOWPAY_RABBIT_ENABLED=true`.

### Frontend na Vercel

Arquivos preparados:

- `Front/vercel.json`: build Vite e fallback SPA.

Passos:

1. Na Vercel, importe o mesmo repositório.
2. Configure `Root Directory` como `Front`.
3. Configure a variável de ambiente:

```text
VITE_API_BASE_URL=https://flowpay-backend.onrender.com
```

4. Deploy.
5. Volte no Render e ajuste `FLOWPAY_CORS_ALLOWED_ORIGINS` para o domínio final da Vercel.

### Observacoes de Deploy

- O Render injeta dados do Postgres via `render.yaml`; o backend monta a URL JDBC com host, porta e database.
- Flyway roda automaticamente no boot e cria/valida o schema.
- SSE funciona na Vercel usando `EventSource` apontando para `VITE_API_BASE_URL`.
- O plano free do Render pode dormir; o primeiro request depois de inatividade pode demorar.

## Como Rodar Localmente

Use localmente apenas para teste/desenvolvimento. Rode backend e frontend em terminais separados.

1. Subir PostgreSQL/RabbitMQ local:

```bash
docker compose up -d
```

2. Em um terminal, rodar backend:

```powershell
cd Back\FlowPay
.\mvnw.cmd spring-boot:run
```

3. Em outro terminal, rodar frontend:

```bash
cd Front
npm install
npm run dev
```

## URLs

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- RabbitMQ Management: `http://localhost:15672` (`guest` / `guest`)

## Testes e Build

### Backend

```powershell
cd Back\FlowPay
.\mvnw.cmd test
```

Os testes usam o schema `flowpay_test` dentro do banco local `flowpay`, separado do schema `public` usado pela aplicacao em desenvolvimento. A suite limpa `atendimentos` antes de cada teste, mas agora isso acontece apenas no schema de teste.

### Frontend

```bash
cd Front
npm run build
```

## Configuracao

O backend usa defaults locais seguros em `application.properties` e aceita override por variaveis de ambiente. O arquivo `arquivo_de_variaveis` existe apenas para documentar os nomes esperados, sem segredos reais. Ele nao e usado automaticamente em producao.

Exemplo:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/flowpay
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=admin
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest
```

Use `arquivo_de_variaveis` como referencia. Credenciais reais de provedores externos nao devem ser versionadas.

## Endpoints Principais

- `GET /api/assuntos`
- `POST /api/atendimentos`
- `GET /api/atendimentos?status=&atendenteId=&time=`
- `PATCH /api/atendimentos/{id}/finalizar`
- `GET /api/atendentes`
- `GET /api/atendentes/{id}/atendimentos?status=`
- `GET /api/dashboard/resumo`
- `GET /api/dashboard/stream`

Exemplos prontos estao em `Docs/api-examples.http`.

## Decisoes de Arquitetura

- O banco e a fonte da verdade da fila: `status = AGUARDANDO`, ordenado por `criadoEm`.
- RabbitMQ e gatilho de processamento, nao a fila operacional final.
- Atribuicao e finalizacao sao transacionais.
- A concorrencia e protegida com lock por squad e `SELECT ... FOR UPDATE SKIP LOCKED`.
- Eventos SSE sao publicados apos commit para evitar dashboard refletir estado que pode sofrer rollback.
- Assunto e entidade persistida; nao ha classificacao por regex/texto livre.
- DTOs de entrada e saida ficam separados.

## Trade-offs

- Nao ha autenticacao/autorizacao nesta versao.
- Nao ha multi-instancia com outbox distribuido; isso esta documentado como melhoria futura.
- O frontend usa graficos CSS para manter a entrega leve, sem biblioteca de chart.
- Scheduler funciona como fallback para mensagens RabbitMQ perdidas ou indisponibilidade temporaria.

## Estrutura

```text
Back/FlowPay/        Backend Spring Boot
Front/               Frontend React
Docs/                Documentacao de produto, comportamento, modelos e exemplos
docker-compose.yml   Infra local PostgreSQL + RabbitMQ
```
