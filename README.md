# FlowPay

Sistema de distribuicao e monitoramento de atendimentos.

## Descricao Do Desafio

A FlowPay e uma fintech que esta estruturando sua central de relacionamento. Essa central atende diversos tipos de solicitacoes dos clientes, sendo os principais: problemas com cartao e contratacao de emprestimo.

A FlowPay organizou seus atendentes em 3 times de atendimento:

- Time Cartoes: para solicitacoes com assunto "Problemas com cartao".
- Time Emprestimos: para solicitacoes de "Contratacao de emprestimo".
- Time Outros Assuntos: para os demais assuntos.

O software deve implementar a seguinte politica de distribuicao:

1. Cada atendente deve atender no maximo 3 pessoas de forma simultanea.
2. Caso todos os atendentes de um time estejam ocupados, os atendimentos devem ser enfileirados e distribuidos assim que um atendente ficar livre.

Requisitos adicionais do desafio pleno full stack:

1. Software de Distribuicao (Back-end): a API do software de distribuicao deve ser disponibilizada no estilo REST.
2. Dashboard de Acompanhamento (Front-end): deve existir uma tela de dashboard para acompanhar os dados de atendimentos em tempo real.

## Stack

- Backend: Java 21, Spring Boot, Spring MVC, Spring Data JPA, Flyway, PostgreSQL, RabbitMQ (CloudAMQP), SSE, Swagger/OpenAPI.
- Frontend: React, TypeScript, Vite, SSE.
- Infra local: Docker Compose com PostgreSQL (RabbitMQ opcional via CloudAMQP).

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

## Como Rodar Localmente

### 1. Banco de dados

```bash
docker compose up -d postgres
```

RabbitMQ nao e necessario localmente se usar as env vars do CloudAMQP (veja Configuracao). Caso queira RabbitMQ local:

```bash
docker compose up -d
```

### 2. Backend

Antes de rodar, defina as variaveis de ambiente do RabbitMQ no terminal. Para RabbitMQ local, use os defaults do `application.properties`. Para CloudAMQP, informe os dados do seu broker:

```powershell
$env:SPRING_RABBITMQ_HOST="seu-host-rabbitmq"
$env:SPRING_RABBITMQ_PORT=5671
$env:SPRING_RABBITMQ_USERNAME="seu_usuario"
$env:SPRING_RABBITMQ_PASSWORD="sua_senha"
$env:SPRING_RABBITMQ_VIRTUAL_HOST="seu_vhost"
$env:SPRING_RABBITMQ_SSL_ENABLED="true"

cd Back\FlowPay
.\mvnw.cmd spring-boot:run
```

### 3. Frontend

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

## Deploy no Render

### Preparacao

Arquivos versionados:

- `render.yaml`: blueprint com Web Service + PostgreSQL gerenciado.
- `Back/FlowPay/Dockerfile`: build Docker multi-stage.

### Passos

1. Crie uma branch `main` separada para producao.
2. Empurre o repositorio para GitHub.
3. No Render, use **New > Blueprint** e selecione o repositorio.
4. Render cria o banco `flowpay-postgres` e o servico `flowpay-backend`.
5. No dashboard do Render, adicione as seguintes env vars no servico `flowpay-backend` com os dados do CloudAMQP:

   ```
   SPRING_RABBITMQ_HOST=seu-host-rabbitmq
   SPRING_RABBITMQ_PORT=5671
   SPRING_RABBITMQ_USERNAME=seu_usuario
   SPRING_RABBITMQ_PASSWORD=sua_senha
   SPRING_RABBITMQ_VIRTUAL_HOST=seu_vhost
   SPRING_RABBITMQ_SSL_ENABLED=true
   FLOWPAY_CORS_ALLOWED_ORIGINS=https://flowpay.vercel.app
   ```

6. Apos o deploy, copie a URL do backend (ex.: `https://flowpay-backend.onrender.com`).

### Frontend na Vercel

1. Na Vercel, importe o mesmo repositorio.
2. Configure **Root Directory** como `Front`.
3. Adicione a env var:

   ```
   VITE_API_BASE_URL=https://flowpay-backend.onrender.com
   ```

4. Deploy.
5. Atualize `FLOWPAY_CORS_ALLOWED_ORIGINS` no Render com o dominio final da Vercel.

### Observacoes

- O Render injeta automaticamente as credenciais do PostgreSQL nas env vars do servico.
- Flyway roda no boot e gerencia o schema.
- Nao e necessario container RabbitMQ no Render — o CloudAMQP e externo.
- O plano free do Render pode hibernar; o primeiro request apos inatividade pode demorar alguns segundos.

## Configuracao

O `application.properties` usa placeholders `${VAR:default}`. O valor default funciona localmente com Postgres e RabbitMQ locais. Para usar PostgreSQL ou RabbitMQ externos, defina as variaveis de ambiente correspondentes no terminal, na IDE ou no painel do provedor de deploy.

Variaveis disponiveis:

| Variavel | Default | Descricao |
|---|---|---|
| `SERVER_ADDRESS` | `0.0.0.0` | Endereco do servidor |
| `SERVER_PORT` | `8080` | Porta do servidor |
| `SPRING_DATASOURCE_HOST` | `localhost` | Host do PostgreSQL |
| `SPRING_DATASOURCE_PORT` | `5432` | Porta do PostgreSQL |
| `SPRING_DATASOURCE_DATABASE` | `flowpay` | Nome do banco |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario do banco |
| `SPRING_DATASOURCE_PASSWORD` | `admin` | Senha do banco |
| `SPRING_RABBITMQ_HOST` | `localhost` | Host do RabbitMQ |
| `SPRING_RABBITMQ_PORT` | `5672` | Porta do RabbitMQ |
| `SPRING_RABBITMQ_USERNAME` | `guest` | Usuario RabbitMQ |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | Senha RabbitMQ |
| `SPRING_RABBITMQ_VIRTUAL_HOST` | `/` | Vhost RabbitMQ |
| `SPRING_RABBITMQ_SSL_ENABLED` | `false` | SSL habilitado |
| `FLOWPAY_RABBIT_ENABLED` | `true` | Habilitar worker RabbitMQ |
| `FLOWPAY_DISTRIBUICAO_SCHEDULER_ENABLED` | `true` | Habilitar scheduler |
| `FLOWPAY_DISTRIBUICAO_SCHEDULER_DELAY_MS` | `5000` | Intervalo do scheduler |
| `FLOWPAY_CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Origens CORS permitidas |
| `FLOWPAY_CORS_ALLOWED_ORIGIN_PATTERNS` | (vazio) | Padroes de origem CORS |

## Testes

```powershell
cd Back\FlowPay
.\mvnw.cmd test
```

Os testes usam schema `flowpay_test` isolado do `public`. Nao requerem RabbitMQ (desligado nas configs de teste).

## Endpoints Principais

- `GET /api/assuntos`
- `POST /api/atendimentos`
- `GET /api/atendimentos?status=&atendenteId=&time=`
- `PATCH /api/atendimentos/{id}/finalizar`
- `GET /api/atendentes`
- `GET /api/atendentes/{id}/atendimentos?status=`
- `GET /api/dashboard/resumo`
- `GET /api/dashboard/stream`

Exemplos em `Docs/api-examples.http`.

## Decisoes de Arquitetura

- O banco e a fonte da verdade da fila: `status = AGUARDANDO`, ordenado por `criadoEm`.
- RabbitMQ e gatilho de processamento, nao a fila operacional.
- Atribuicao e finalizacao sao transacionais com `SELECT FOR UPDATE SKIP LOCKED`.
- Eventos SSE publicados apos commit para evitar rollback parcial no dashboard.
- Assunto e entidade persistida; sem classificacao por regex.
- DTOs de entrada e saida separados.

## Trade-offs

- Sem autenticacao/autorizacao nesta versao.
- Sem multi-instancia com outbox (melhoria futura).
- Graficos CSS (sem biblioteca de chart).
- Scheduler como fallback do RabbitMQ.

## Estrutura

```text
Back/FlowPay/        Backend Spring Boot
Front/               Frontend React
Docs/                Documentacao e exemplos
docker-compose.yml   Infra local (PostgreSQL + RabbitMQ opcional)
render.yaml          Blueprint Render
```
