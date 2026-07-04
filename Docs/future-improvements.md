# Future Improvements — Melhorias Futuras (FlowPay)

Este documento concentra melhorias fora do escopo atual, mas relevantes para evoluir o projeto para um produto mais robusto.

---

## 1. Balanceamento Avançado Entre Atendentes

### Estado atual

A distribuição escolhe um atendente disponível do mesmo time com menor carga atual:

```sql
ORDER BY atendimentos_ativos ASC, id ASC
```

Isso é simples, determinístico e fácil de testar.

### Limitação

Quando há mais de um atendente do mesmo time com a mesma carga, por exemplo todos com `0` atendimentos ativos, o desempate por `id` tende a favorecer sempre o mesmo atendente primeiro.

### Evolução recomendada

Adicionar no modelo `Atendente` um campo:

```text
ultimo_atendimento_em TIMESTAMP
```

Então a ordenação passaria a ser:

```sql
ORDER BY atendimentos_ativos ASC, ultimo_atendimento_em ASC NULLS FIRST, id ASC
```

Comportamento esperado:

- Primeiro critério: menor carga atual.
- Segundo critério: quem está há mais tempo sem receber atendimento.
- Terceiro critério: `id` para desempate determinístico.

Ao atribuir atendimento, o worker atualizaria:

```java
atendente.setUltimoAtendimentoEm(LocalDateTime.now());
```

Essa abordagem é mais justa que `random()` e continua previsível para testes.

---

## 2. Autenticação e Autorização com JWT

### Estado atual

A API não possui autenticação/autorização.

### Evolução recomendada

Implementar Spring Security com JWT.

Papéis sugeridos:

```text
ADMIN
GESTOR
ATENDENTE
```

Regras iniciais:

- `ADMIN`: gerencia cadastros e acessa tudo.
- `GESTOR`: acessa dashboard, filas e relatórios.
- `ATENDENTE`: visualiza/finaliza apenas atendimentos atribuídos a ele.

Endpoints sugeridos:

```text
POST /api/auth/login
POST /api/auth/refresh
GET /api/auth/me
```

Campos futuros:

```text
usuarios
roles
atendente.usuario_id
```

---

## 3. Dead Letter Queue e Retry no RabbitMQ

### Estado atual

RabbitMQ é usado como gatilho assíncrono. Se o worker não encontra vaga, a mensagem é considerada processada e o atendimento permanece `AGUARDANDO` no banco.

### Evolução recomendada

Criar infraestrutura de retry para falhas técnicas reais, por exemplo erro de banco ou payload inválido.

Filas sugeridas:

```text
flowpay.distribuicao.queue
flowpay.distribuicao.retry.queue
flowpay.distribuicao.dlq
```

Regras:

- Erro de indisponibilidade de atendente: não vai para retry, pois o banco já representa a fila.
- Erro técnico transitório: retry com delay.
- Erro permanente: Dead Letter Queue.

---

## 4. Outbox Pattern

### Problema que resolve

Hoje o sistema publica no RabbitMQ após commit usando `TransactionSynchronization.afterCommit()`.

Isso evita publicar antes do commit, mas ainda existe um cenário possível:

```text
1. Banco commita.
2. Aplicação cai antes de publicar no RabbitMQ.
```

O scheduler reduz esse risco, mas não registra explicitamente o evento perdido.

### Evolução recomendada

Criar tabela de outbox:

```text
outbox_events
```

Campos sugeridos:

```text
id
tipo
payload
status
criado_em
publicado_em
tentativas
```

Fluxo:

```text
Transação de negócio grava atendimento + evento na outbox.
Worker de outbox publica no RabbitMQ.
Evento é marcado como publicado.
```

Esse padrão aumenta confiabilidade em ambientes distribuídos.

---

## 5. Observabilidade

Adicionar:

- Logs estruturados com correlation id.
- Métricas Micrometer/Prometheus.
- Health checks para Postgres e RabbitMQ.
- Métricas de fila por time.
- Tempo médio de espera por time.
- Alertas quando fila exceder um limite.

---

## 6. Cadastro Administrativo

Hoje `Assunto` e `Atendente` são seedados via migration.

Evoluções possíveis:

- CRUD de assuntos.
- CRUD de atendentes.
- Ativar/desativar atendente.
- Configurar capacidade máxima por atendente.
- Configurar times dinamicamente em tabela própria.

---

## 7. Auditoria e Histórico

Adicionar tabela de eventos de atendimento:

```text
atendimento_eventos
```

Eventos possíveis:

```text
CRIADO
ATRIBUIDO
FINALIZADO
REABERTO
TRANSFERIDO
ERRO_DISTRIBUICAO
```

Isso permite reconstruir a linha do tempo de cada atendimento.

---

## 8. Reatribuição Manual

Permitir que um gestor mova um atendimento entre atendentes do mesmo time ou entre times, respeitando capacidade e regras de auditoria.

Endpoints futuros:

```text
PATCH /api/atendimentos/{id}/reatribuir
PATCH /api/atendimentos/{id}/alterar-time
```

---

## 9. Testes Mais Abrangentes

Adicionar:

- Testes de controller com MockMvc.
- Testes de integração real com RabbitMQ via Testcontainers.
- Testes de migration com banco limpo.
- Testes de carga para criação concorrente.
- Testes de contrato OpenAPI.

---

## 10. Frontend Completo

Implementar o `Front/` com React:

- Tela de abertura de atendimento.
- Select de assuntos via `GET /api/assuntos`.
- Dashboard com snapshot via `GET /api/dashboard/resumo`.
- Futuro SSE via `GET /api/dashboard/stream`.
- Página de atendentes.
- Página de fila por time.
