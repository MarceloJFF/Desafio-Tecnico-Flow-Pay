# PRD — Software de Distribuição e Monitoramento de Atendimentos (FlowPay)

**Versão:** 2.0
**Stack:** Monorepo — React (front) + Spring Boot (back) + Postgres + RabbitMQ + SSE
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
- Dashboard com atualização em **tempo real via SSE** (sem polling).
- Organização em monorepo, com backend e frontend versionados juntos.

---

## 3. Escopo

### Dentro do escopo
- API REST (Spring Boot) para criar atendimentos, finalizar atendimentos, listar filas e atendentes.
- Motor de distribuição automática com fila modelada em banco relacional e RabbitMQ como gatilho assíncrono.
- Endpoint SSE (`/dashboard/stream`) que emite eventos em tempo real.
- Dashboard React consumindo o stream SSE, mostrando: fila por time, ocupação dos atendentes, atendimentos em andamento, métricas agregadas.
- Testes automatizados cobrindo a regra de negócio (limite de 3, fila, concorrência).

### Fora do escopo
- Autenticação/autorização.
- Múltiplas instâncias da API em paralelo (escala horizontal).
- Reatribuição manual de atendimento pelo gestor.
- Histórico de longo prazo / exportação de relatórios.

---

## 4. Arquitetura — Monorepo

```
flowpay/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/flowpay/
│   │   ├── domain/           # entidades (Atendimento, Atendente, Time)
│   │   ├── service/          # DistribuicaoService, DashboardService
│   │   ├── controller/       # REST controllers + SSE controller
│   │   ├── repository/       # Spring Data JPA
│   │   └── config/
│   └── src/test/java/...
├── frontend/                  # React
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/             # useSSE, useDashboard
│   │   └── services/          # api.ts
│   └── ...
├── docker-compose.yml          # postgres + rabbitmq + backend + frontend
└── docs/                       # este conjunto de arquivos .md
```

**Por que monorepo aqui:** desafio de escopo pequeno, um único time (você), sem necessidade de deploy independente. Facilita rodar tudo com um `docker-compose up` e revisar o código como um fluxo único.

---

## 5. Decisão Arquitetural — Backend de Distribuição

Fonte única de verdade: **Postgres**. A fila é uma consulta lógica (`status = 'AGUARDANDO'`), não uma estrutura separada no broker. RabbitMQ funciona como gatilho assíncrono para o worker tentar atribuir o próximo atendimento do time. A atribuição é feita dentro de transação com `SELECT ... FOR UPDATE SKIP LOCKED`, evitando dois workers concorrentes estourarem o limite de 3.

O scheduler de segurança reprocessa periodicamente os times para cobrir mensagens perdidas, restart do worker ou indisponibilidade temporária do RabbitMQ. Ele não substitui o RabbitMQ; apenas garante consistência eventual pelo estado do banco.

---

## 6. Dashboard em Tempo Real — SSE

**Endpoint:** `GET /api/dashboard/stream` (`Content-Type: text/event-stream`)

- Backend usa `SseEmitter` do Spring Web para manter conexão aberta por gestor conectado.
- Toda vez que uma transação de **criação, atribuição ou finalização** de atendimento é commitada, o `DistribuicaoService` publica um evento internamente (ex: via `ApplicationEventPublisher` do Spring) que o `DashboardController` escuta e repassa para todos os `SseEmitter` ativos.
- Frontend usa a API nativa `EventSource` (via hook `useSSE`) para consumir o stream e atualizar o estado do dashboard sem F5 e sem polling.
- Reconexão automática do `EventSource` já é nativa do browser — não precisa lib extra.

Isso resolve de fato o requisito de "tempo real": o gestor vê a mudança de estado assim que ela acontece no backend, sem intervalo artificial de polling.

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
- [ ] Dashboard reflete mudanças de estado via SSE, sem polling e sem F5 manual.
- [ ] API documentada (OpenAPI/Swagger).
- [ ] Testes cobrindo a regra de negócio, incluindo cenário de concorrência.
- [ ] `docker-compose up` sobe o ambiente completo (postgres + backend + frontend).
