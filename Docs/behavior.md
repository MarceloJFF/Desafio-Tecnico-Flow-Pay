# Behavior.md — Comportamentos do Sistema (FlowPay)

Este documento descreve o comportamento esperado do sistema em cada cenário, incluindo casos de borda. Serve de base para escrever os testes e para os critérios de aceite das specs.

---

## 1. Classificação de Assunto → Time

| Assunto recebido | Time destino |
|---|---|
| "Problemas com cartão" (case-insensitive, match exato ou normalizado) | Time Cartões |
| "Contratação de empréstimo" | Time Empréstimos |
| Qualquer outro valor, incluindo vazio/desconhecido | Time Outros Assuntos |

**Comportamento esperado:** a classificação nunca falha — todo assunto não mapeado explicitamente cai em "Outros Assuntos". Não deve haver erro 400 por assunto desconhecido.

---

## 2. Criação de Atendimento

**Entrada:** `POST /api/atendimentos { "assunto": "Problemas com cartão" }`

**Fluxo:**
1. Sistema classifica o time.
2. Sistema persiste o atendimento com `status = AGUARDANDO`.
3. Dentro da mesma transação, tenta atribuir a um atendente do time com `atendimentosAtivos < 3`, priorizando o atendente com **menor carga atual** (balanceamento).
4. Se atribuído: `status = EM_ATENDIMENTO`, `atendenteId` preenchido, `atribuidoEm = now()`, `atendimentosAtivos` do atendente é incrementado.
5. Se não atribuído: atendimento permanece `AGUARDANDO` — está na fila.
6. Evento é publicado para o stream SSE em ambos os casos (mudança de estado do dashboard).

**Resposta:** `201 Created` com o atendimento (já indicando se foi atribuído ou está em fila).

---

## 3. Regra do Limite de 3 Atendimentos Simultâneos

- Um atendente nunca pode ter `atendimentosAtivos > 3`.
- A verificação e o incremento acontecem na **mesma transação** (`SELECT ... FOR UPDATE SKIP LOCKED` + `UPDATE`), evitando que duas requisições concorrentes atribuam o mesmo atendente além do limite.
- **Caso de borda:** duas requisições de criação de atendimento chegam no mesmo milissegundo para o mesmo time com apenas 1 vaga livre no time inteiro → apenas uma delas deve conseguir a vaga; a outra entra em fila. Isso deve ser coberto por teste de concorrência (N threads disparando criação simultânea).

---

## 4. Fila de Espera

- A fila **não é uma estrutura separada** — é a consulta `atendimentos WHERE status = 'AGUARDANDO' AND timeId = ? ORDER BY criadoEm ASC`.
- Ordem de atendimento: estritamente FIFO por time (quem chegou primeiro é atribuído primeiro quando surge vaga).
- **Caso de borda:** se um time nunca tiver nenhum atendente cadastrado, atendimentos desse time ficam indefinidamente em `AGUARDANDO`. Sistema não deve travar nem lançar erro — apenas o item nunca sai da fila (comportamento esperado e documentado como risco operacional, não como bug).

---

## 5. Finalização de Atendimento

**Entrada:** `PATCH /api/atendimentos/{id}/finalizar`

**Fluxo:**
1. Valida que o atendimento existe e está `EM_ATENDIMENTO` (se já `FINALIZADO`, retorna 409 Conflict — não é idempotente por reenvio acidental).
2. Marca `status = FINALIZADO`, `finalizadoEm = now()`.
3. Decrementa `atendimentosAtivos` do atendente.
4. **Na mesma transação**, tenta buscar o próximo `AGUARDANDO` do mesmo time (mais antigo primeiro) e atribui a esse atendente que acabou de liberar vaga.
5. Publica evento(s) SSE: finalização do atendimento anterior e, se houve, nova atribuição.

**Caso de borda:** se não houver ninguém na fila do time no momento da liberação, o atendente simplesmente fica com `atendimentosAtivos` reduzido e disponível — nenhuma ação adicional.

---

## 6. Redistribuição ao Liberar Vaga

- Disparada exclusivamente pelo evento de finalização (não há job/cron de varredura periódica nesta versão).
- **Importante:** a redistribuição busca apenas o próximo item do mesmo time do atendente que liberou — atendentes não atendem fora do seu time.
- Se múltiplos atendentes do mesmo time finalizam ao mesmo tempo e há múltiplos itens na fila, cada finalização, isoladamente, deve puxar exatamente um item — sem duplicar atribuição do mesmo atendimento a dois atendentes (garantido pelo `FOR UPDATE SKIP LOCKED` na busca do próximo item da fila).

---

## 7. Dashboard — Comportamento do Stream SSE

- Conexão: `GET /api/dashboard/stream`, mantida aberta enquanto o cliente (browser) estiver com a tela aberta.
- Eventos emitidos:
  - `atendimento-criado`
  - `atendimento-atribuido`
  - `atendimento-finalizado`
- Cada evento carrega o payload mínimo necessário para o frontend atualizar o estado local (não exige nova requisição REST a cada evento).
- **Reconexão:** se a conexão cair (rede instável, backend reiniciado), o `EventSource` do browser tenta reconectar automaticamente (comportamento nativo). Ao reconectar, o frontend deve buscar o estado atual via `GET /api/dashboard/resumo` (snapshot) antes de voltar a escutar o stream, para evitar exibir dados desatualizados durante o gap de reconexão.
- **Múltiplos gestores conectados simultaneamente:** todos recebem o mesmo evento (broadcast simples via lista de `SseEmitter` ativos no backend).

---

## 8. Comportamentos de Erro

| Cenário | Resposta esperada |
|---|---|
| Criar atendimento sem campo `assunto` | `400 Bad Request` |
| Finalizar atendimento inexistente | `404 Not Found` |
| Finalizar atendimento já finalizado | `409 Conflict` |
| Time sem nenhum atendente cadastrado | Atendimento permanece em fila indefinidamente (não é erro) |
| Falha de conexão SSE | Reconexão automática do browser + snapshot via REST ao reconectar |

---

## 9. Garantias do Sistema (Invariantes)

Essas condições devem ser **sempre verdadeiras**, em qualquer momento, sob qualquer carga:

1. `atendente.atendimentosAtivos` nunca é negativo nem maior que 3.
2. Um `atendimento` nunca tem `atendenteId` preenchido com `status = AGUARDANDO`.
3. Um `atendimento` `FINALIZADO` nunca volta a `EM_ATENDIMENTO` ou `AGUARDANDO`.
4. A soma de `atendimentosAtivos` de todos os atendentes de um time é sempre igual à quantidade de atendimentos `EM_ATENDIMENTO` daquele time.
