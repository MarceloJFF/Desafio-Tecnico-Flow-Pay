# Phases.md — Fases de Desenvolvimento (FlowPay)

Divisão pensada para caber no prazo do desafio (48h), com cada fase gerando algo demonstrável — útil também para você quebrar em specs/tickets individuais.

---

## Fase 0 — Setup do Monorepo (curto)

**Objetivo:** ambiente rodando de ponta a ponta, mesmo sem lógica de negócio ainda.

- Estrutura de pastas `backend/` (Spring Boot + Gradle/Maven) e `frontend/` (React + Vite).
- `docker-compose.yml` com Postgres.
- Backend sobe e conecta no Postgres (health check `GET /actuator/health`).
- Frontend sobe e faz uma chamada de "hello world" no backend.
- README inicial com instruções de `docker-compose up`.

**Entregável:** `docker-compose up` sobe tudo, front conversa com back.

---

## Fase 1 — Modelo de Dados e Migrations

**Objetivo:** schema do banco pronto e versionado.

- Entidades JPA (`Atendente`, `Atendimento`, enums `TimeAtendimento`, `StatusAtendimento`) — ver `models.md`.
- Migrations via Flyway/Liquibase.
- Seed inicial de atendentes (ex: 2-3 por time) para facilitar testes manuais.
- Testes de repositório (Testcontainers com Postgres real).

**Entregável:** banco criado automaticamente ao subir o backend, com dados de teste.

---

## Fase 2 — Motor de Distribuição (core do desafio)

**Objetivo:** a regra de negócio funcionando e testada — é a parte que mais pesa na avaliação.

- `DistribuicaoService`: lógica de criação de atendimento + atribuição automática.
- `SELECT ... FOR UPDATE SKIP LOCKED` para busca de atendente disponível.
- Finalização de atendimento + redistribuição automática da fila.
- **Testes de concorrência**: N threads criando atendimentos simultaneamente, validando que nenhum atendente ultrapassa 3.
- Testes unitários de classificação de assunto → time.

**Entregável:** endpoints REST de criação/finalização funcionando, com testes cobrindo os cenários de `behavior.md`.

---

## Fase 3 — API REST Completa + Documentação

**Objetivo:** contrato de API estável para o frontend consumir.

- `POST /api/atendimentos`
- `PATCH /api/atendimentos/{id}/finalizar`
- `GET /api/atendimentos?status=`
- `GET /api/atendentes`
- `GET /api/dashboard/resumo`
- Swagger/OpenAPI configurado (springdoc-openapi).
- Tratamento de erros padronizado (400/404/409 conforme `behavior.md`).

**Entregável:** API navegável via Swagger UI, pronta para o frontend.

---

## Fase 4 — SSE para o Dashboard

**Objetivo:** tempo real de fato.

- `DashboardController` com `GET /api/dashboard/stream` usando `SseEmitter`.
- `ApplicationEventPublisher` interno disparando eventos a cada criação/atribuição/finalização.
- Gerenciamento de lista de emitters ativos (adicionar ao conectar, remover ao desconectar/erro).
- Teste manual com múltiplas abas do browser conectadas simultaneamente.

**Entregável:** ao criar/finalizar atendimento via Swagger ou curl, qualquer client conectado no `/stream` recebe o evento imediatamente.

---

## Fase 5 — Frontend: Dashboard React

**Objetivo:** interface de monitoramento consumindo REST + SSE.

- Estrutura de componentes conforme `design.md`.
- `useDashboardSnapshot` (carga inicial) + `useDashboardStream` (atualizações via `EventSource`).
- Reducer de merge incremental de eventos.
- Indicador de status de conexão.
- Estilização básica, mas clara (não precisa ser um design system completo).

**Entregável:** dashboard funcional, atualizando sozinho ao criar/finalizar atendimentos em outra aba/terminal.

---

## Fase 6 — Polimento e Entrega

**Objetivo:** deixar o projeto fácil de avaliar.

- README completo: como rodar, decisões de arquitetura, trade-offs (o que você me perguntou nas etapas anteriores vira ótimo conteúdo aqui).
- Revisão dos testes (unitários + concorrência + repositório).
- Conferir que `docker-compose up` sobe tudo sem passos manuais extras.
- Opcional (se sobrar tempo): script/collection Postman ou exemplos de `curl` para popular atendimentos rapidamente e ver o dashboard reagir ao vivo.

**Entregável final:** repositório completo, rodável com um comando, documentado.

---

## Sugestão de Priorização se o Tempo Apertar

Se 48h ficar apertado, corte nesta ordem (do menos crítico para o mais crítico):

1. Polimento visual do frontend (Fase 6 estética).
2. Swagger/OpenAPI (Fase 3 — pode ficar como README de endpoints).
3. **Nunca corte:** Fase 2 (motor de distribuição + testes de concorrência) — é o coração do desafio e o que mais será avaliado.
4. **Nunca corte:** Fase 4/5 (SSE) — é requisito explícito do desafio ("tempo real"), mas se precisar simplificar, um fallback aceitável e documentado é polling curto no lugar do SSE, citando a limitação de tempo no README.
