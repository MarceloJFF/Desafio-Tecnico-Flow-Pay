# PRD — Software de Distribuição e Monitoramento de Atendimentos (FlowPay)

**Versão:** 2.0
**Stack:** Monorepo — React + Spring Boot + Postgres + RabbitMQ + SSE + Swagger/OpenAPI
**Autor:** Marcelo (desafio técnico Pleno Full Stack)

---

## 1. Contexto e Problema

A FlowPay precisa distribuir atendimentos de clientes entre 3 times (Cartões, Empréstimos, Outros Assuntos), com assunto selecionado de uma lista pré-cadastrada, respeitando:

1. Cada atendente atende no máximo **3 pessoas simultaneamente**.
2. Se todos os atendentes de um time estiverem ocupados, o atendimento **entra em fila** e é distribuído automaticamente assim que um atendente ficar livre.

Além da distribuição, é necessário um **dashboard em tempo real** para gestores acompanharem o estado dos atendimentos.

---

## 2. Objetivo

Entregar uma solução correta, testável e com boa experiência de monitoramento, dentro do prazo do desafio, priorizando:

- Corretude da regra de negócio (limite de 3, fila, redistribuição automática).
- API REST clara e documentada.
- Dashboard com snapshot REST e atualizações em **tempo real via SSE**.
- Organização em monorepo, com backend e frontend versionados juntos.

---

## 3. Escopo

### Dentro do escopo implementado no backend
- API REST (Spring Boot) para criar atendimentos, finalizar atendimentos, listar filas e atendentes.
- Motor de distribuição automática com fila modelada em banco relacional e RabbitMQ como gatilho assíncrono.
- Snapshot REST do dashboard em `GET /api/dashboard/resumo`.
- Stream SSE do dashboard em `GET /api/dashboard/stream`.
- Frontend React em `Front/` consumindo REST + SSE.
- Guia Reports, guia Atendimentos com modal de detalhes e guia Atendentes por squad.
- Docker Compose e scripts locais de dev/teste para facilitar avaliacao.
- Swagger/OpenAPI em `/swagger-ui.html` e `/api-docs`.
- Testes automatizados cobrindo a regra de negócio (limite de 3, fila, concorrência).

### Planejado nas próximas fases
- Autenticacao/autorizacao, outbox, DLQ/retry e escala horizontal.

### Fora do escopo
- Autenticação/autorização.
- Múltiplas instâncias da API em paralelo (escala horizontal).
- Reatribuição manual de atendimento pelo gestor.
- Histórico de longo prazo / exportação de relatórios.

---

## 4. Arquitetura — Monorepo

```
flowpay/
├── Back/FlowPay/              # Spring Boot
│   ├── src/main/java/com/desafio/FlowPay/
│   │   ├── model/             # entidades e enums
│   │   ├── service/           # regras de negócio e consultas
│   │   ├── controller/        # REST controllers
│   │   ├── repository/        # Spring Data JPA
│   │   ├── messaging/         # RabbitMQ publisher/worker/eventos
│   │   ├── scheduler/         # reprocessamento de filas
│   │   └── config/            # OpenAPI e RabbitMQ
│   └── src/test/java/...
├── Front/                     # React planejado
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/             # useSSE, useDashboard
│   │   └── services/          # api.ts
│   └── ...
└── Docs/                       # documentação do projeto
```

**Por que monorepo aqui:** desafio de escopo pequeno, um único time (você), sem necessidade de deploy independente. Facilita revisar backend, frontend planejado e documentação em um único repositório.

---

## 5. Decisão Arquitetural — Backend de Distribuição

Fonte única de verdade: **Postgres**. A fila é uma consulta lógica (`status = 'AGUARDANDO'`), não uma estrutura separada no broker. RabbitMQ funciona como gatilho assíncrono para o worker tentar atribuir o próximo atendimento do time. A atribuição é feita dentro de transação com `SELECT ... FOR UPDATE SKIP LOCKED`, evitando dois workers concorrentes estourarem o limite de 3.

O scheduler de segurança reprocessa periodicamente os times para cobrir mensagens perdidas, restart do worker ou indisponibilidade temporária do RabbitMQ. Ele não substitui o RabbitMQ; apenas garante consistência eventual pelo estado do banco.

---

## 6. Dashboard e Observabilidade

**Endpoints implementados:** `GET /api/dashboard/resumo` e `GET /api/dashboard/stream`

- `GET /api/dashboard/resumo` retorna filas por time, atendimentos em andamento por time, finalizados hoje, tempo médio de espera e ocupação dos atendentes.
- `GET /api/dashboard/stream` mantém conexão SSE aberta e envia eventos `atendimento-criado`, `atendimento-atribuido` e `atendimento-finalizado`.
- O snapshot deve ser usado na carga inicial e após reconexões SSE.

**Documentação da API:** `GET /swagger-ui.html` e `GET /api-docs`.

---

## 7. Fases de Desenvolvimento

Ver `phases.md` para o detalhamento com entregáveis por fase.

---

## 8. Documentos Relacionados

- `product.md` — funcionalidades principais resumidas.
- `behavior.md` — comportamentos e regras de negócio detalhados.
- `models.md` — modelos de dados (Java/JPA + TypeScript).
- `design.md` — componentes de tela do frontend.
- `phases.md` — fases de desenvolvimento e entregáveis.

---

## 9. Critérios de Aceite Gerais

- [ ] Nenhum atendente recebe mais de 3 atendimentos simultâneos, mesmo sob concorrência.
- [ ] Atendimento sem atendente disponível entra em fila e é atribuído automaticamente ao liberar vaga.
- [ ] Worker RabbitMQ processa criação e liberação de vaga sem atribuição duplicada.
- [ ] Scheduler reprocessa filas pendentes mesmo se uma mensagem for perdida.
- [x] API documentada (OpenAPI/Swagger).
- [x] Testes cobrindo a regra de negócio, incluindo cenário de concorrência.
- [x] Dashboard backend reflete mudanças de estado via SSE, sem polling.
- [x] Frontend React consome snapshot REST e stream SSE.
- [x] Ambiente completo com frontend e orquestração padronizada.
