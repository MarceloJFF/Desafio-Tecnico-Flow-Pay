# Behavior.md — Comportamentos do Sistema (FlowPay)

Este documento descreve o comportamento esperado do sistema em cada cenário, incluindo casos de borda. Serve de base para escrever os testes e para os critérios de aceite das specs.

---

## 1. Seleção de Assunto → Time

O assunto é uma **entidade persistida** (`assuntos`), consultável via `GET /api/assuntos`. O frontend exibe os assuntos cadastrados para o usuário selecionar.

| Assunto (id fixo no seed) | Time destino |
|---|---|
| `a0000000-...-001` — "Problemas com cartão" | Time Cartões |
| `a0000000-...-002` — "Contratação de empréstimo" | Time Empréstimos |
| `a0000000-...-003` — "Outros" | Time Outros Assuntos |

**Comportamento esperado:** não há classificação por texto/regex. O `assuntoId` informado deve existir na tabela `assuntos`; caso contrário, retorna `400 Bad Request`. Assuntos novos podem ser inseridos via migration sem necessidade de alterar código.

---

## 2. Criação de Atendimento

**Entrada:** `POST /api/atendimentos { "assuntoId": "a0000000-0000-0000-0000-000000000001", "observacao": "Cliente relatou..." }`

**Fluxo:**
1. Sistema identifica o time a partir do assunto cadastrado.
2. Sistema persiste o atendimento com `status = AGUARDANDO`.
3. Após commit, publica uma mensagem `ATENDIMENTO_CRIADO` no RabbitMQ com `atendimentoId` e `time`.
4. O worker consome a mensagem e tenta atribuir o próximo atendimento `AGUARDANDO` daquele time.
5. Se houver atendente disponível: `status = EM_ATENDIMENTO`, `atendenteId` preenchido, `atribuidoEm = now()`, `atendimentosAtivos` incrementado.
6. Se não houver atendente disponível: atendimento permanece `AGUARDANDO` no banco, que é a fila real do sistema.

**Resposta:** `201 Created` com o atendimento recém-criado em `AGUARDANDO`. A atribuição é eventual, feita pelo worker ou pelo scheduler de segurança.

---

## 3. Regra do Limite de 3 Atendimentos Simultâneos

- Um atendente nunca pode ter `atendimentosAtivos > 3`.
- A verificação e o incremento acontecem na **mesma transação do worker** (`SELECT ... FOR UPDATE SKIP LOCKED` + `UPDATE`), evitando que dois workers concorrentes atribuam o mesmo atendente além do limite.
- **Caso de borda:** múltiplos workers processam mensagens do mesmo time com apenas 1 vaga livre no time inteiro → apenas um deve conseguir a vaga; os demais não atribuem nada e os atendimentos excedentes permanecem `AGUARDANDO`.

---

## 4. Fila de Espera

- A fila **não é uma estrutura separada no RabbitMQ** — é a consulta `atendimentos WHERE status = 'AGUARDANDO' AND time = ? ORDER BY criadoEm ASC`.
- RabbitMQ é usado como gatilho de processamento, não como fonte da verdade da fila.
- Ordem de atendimento: estritamente FIFO por time (quem chegou primeiro é atribuído primeiro quando surge vaga).
- **Caso de borda:** se um time nunca tiver nenhum atendente cadastrado, atendimentos desse time ficam indefinidamente em `AGUARDANDO`. Sistema não deve travar nem lançar erro — apenas o item nunca sai da fila (comportamento esperado e documentado como risco operacional, não como bug).

---

## 5. Finalização de Atendimento

**Entrada:** `PATCH /api/atendimentos/{id}/finalizar`

**Fluxo:**
1. Valida que o atendimento existe e está `EM_ATENDIMENTO` (se já `FINALIZADO`, retorna 409 Conflict — não é idempotente por reenvio acidental).
2. Marca `status = FINALIZADO`, `finalizadoEm = now()`.
3. Decrementa `atendimentosAtivos` do atendente.
4. Após commit, publica uma mensagem `VAGA_LIBERADA` no RabbitMQ com o `time`.
5. O worker consome a mensagem e tenta puxar o próximo `AGUARDANDO` do mesmo time.

**Caso de borda:** se não houver ninguém na fila do time no momento da tentativa, o atendente simplesmente fica disponível — nenhuma ação adicional é necessária.

---

## 6. Worker, RabbitMQ e Scheduler

- Mensagens RabbitMQ suportadas: `ATENDIMENTO_CRIADO` e `VAGA_LIBERADA`.
- Cada mensagem dispara uma única tentativa de atribuir o próximo `AGUARDANDO` do time informado.
- Se a tentativa não encontrar atendente disponível, a mensagem é considerada processada e o atendimento continua `AGUARDANDO` no banco.
- O scheduler roda periodicamente e tenta processar um item por time, cobrindo mensagem perdida, restart do worker ou indisponibilidade temporária do RabbitMQ.
- Se múltiplos workers processam ao mesmo tempo e há múltiplos itens na fila, cada tentativa puxa no máximo um item, sem duplicar atribuição do mesmo atendimento.

---

## 7. Dashboard — Snapshot REST e Stream SSE

- Endpoint implementado: `GET /api/dashboard/resumo`.
- Retorna quantidade em fila por time, quantidade em atendimento por time, total finalizado no dia, tempo médio de espera e status dos atendentes.
- O dashboard deve usar este endpoint como snapshot inicial e após reconexões.
- Stream SSE implementado: `GET /api/dashboard/stream` com `Content-Type: text/event-stream`.
- Ao conectar, o backend envia `dashboard-conectado` com payload `"ok"`.
- Eventos de negócio emitidos após commit: `atendimento-criado`, `atendimento-atribuido` e `atendimento-finalizado`.

---

## 8. API REST e Swagger

Endpoints implementados:

| Método | Endpoint | Comportamento |
|---|---|---|
| `GET` | `/api/assuntos` | Lista assuntos cadastrados. |
| `POST` | `/api/atendimentos` | Cria atendimento `AGUARDANDO` e publica evento RabbitMQ. |
| `GET` | `/api/atendimentos?status=&atendenteId=&time=` | Lista atendimentos, com filtros opcionais por status, atendente e time. |
| `PATCH` | `/api/atendimentos/{id}/finalizar` | Finaliza atendimento em andamento. |
| `GET` | `/api/atendentes` | Lista atendentes e ocupação atual. |
| `GET` | `/api/atendentes/{id}/atendimentos?status=` | Lista todos os atendimentos de um atendente, incluindo finalizados. |
| `GET` | `/api/dashboard/resumo` | Retorna snapshot do dashboard. |
| `GET` | `/api/dashboard/stream` | Abre stream SSE para atualizações incrementais do dashboard. |

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/api-docs
```

---

## 9. Comportamentos de Erro

| Cenário | Resposta esperada |
|---|---|
| Criar atendimento sem campo `assuntoId` | `400 Bad Request` |
| Criar atendimento com `assuntoId` inexistente | `400 Bad Request` |
| Payload JSON inválido | `400 Bad Request` |
| Query param inválido, ex. `status=INVALIDO` | `400 Bad Request` |
| Finalizar atendimento inexistente | `404 Not Found` |
| Finalizar atendimento já finalizado | `409 Conflict` |
| Finalizar atendimento que ainda está `AGUARDANDO` | `409 Conflict` |
| Time sem nenhum atendente cadastrado | Atendimento permanece em fila indefinidamente (não é erro) |
| RabbitMQ indisponível após criação | Scheduler reprocessa filas pelo banco quando a aplicação estiver ativa |

---

## 10. Garantias do Sistema (Invariantes)

Essas condições devem ser **sempre verdadeiras**, em qualquer momento, sob qualquer carga:

1. `atendente.atendimentosAtivos` nunca é negativo nem maior que 3.
2. Um `atendimento` nunca tem `atendenteId` preenchido com `status = AGUARDANDO`.
3. Um `atendimento` `FINALIZADO` nunca volta a `EM_ATENDIMENTO` ou `AGUARDANDO`.
4. A soma de `atendimentosAtivos` de todos os atendentes de um time é sempre igual à quantidade de atendimentos `EM_ATENDIMENTO` daquele time.
5. Um atendimento nunca é atribuído a um atendente de outro time.
