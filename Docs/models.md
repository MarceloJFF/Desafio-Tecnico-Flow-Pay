# Models.md — Modelos de Dados (FlowPay)

Modelos atualmente implementados no backend Spring Boot e contratos correspondentes para consumo do frontend.

---

## 1. Visão Conceitual

```text
Assunto (N) -> (1) TimeAtendimento
Atendente (N) -> (1) TimeAtendimento
Atendimento (N) -> (1) Assunto
Atendimento (N) -> (0..1) Atendente
Atendimento (N) -> (1) TimeAtendimento
```

- `Assunto` é cadastro persistido no banco e define o `TimeAtendimento` responsável.
- `Atendimento` referencia um `Assunto` e pode ter uma `observacao` de texto livre.
- `Atendimento.time` é gravado no atendimento para facilitar consultas e distribuição.
- `Atendimento.atendente` é nulo enquanto o atendimento está `AGUARDANDO`.
- A fila real é o banco: `atendimentos` com `status = AGUARDANDO`, ordenados por `criado_em`.
- RabbitMQ é gatilho assíncrono de processamento, não a fonte da verdade da fila.

---

## 2. Entidades JPA

### `TimeAtendimento`

```java
public enum TimeAtendimento {
    CARTOES,
    EMPRESTIMOS,
    OUTROS
}
```

### `StatusAtendimento`

```java
public enum StatusAtendimento {
    AGUARDANDO,
    EM_ATENDIMENTO,
    FINALIZADO
}
```

### `Assunto`

```java
@Entity
@Table(name = "assuntos")
public class Assunto {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeAtendimento time;
}
```

Assuntos seeded atualmente:

| ID | Nome | Time |
|---|---|---|
| `a0000000-0000-0000-0000-000000000001` | Problemas com cartão | `CARTOES` |
| `a0000000-0000-0000-0000-000000000002` | Contratação de empréstimo | `EMPRESTIMOS` |
| `a0000000-0000-0000-0000-000000000003` | Outros | `OUTROS` |

### `Atendente`

```java
@Entity
@Table(name = "atendentes")
public class Atendente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeAtendimento time;

    @Column(name = "atendimentos_ativos", nullable = false)
    private Integer atendimentosAtivos = 0;

    @Version
    @Column(nullable = false)
    private Long version;
}
```

Seed final após migrations:

| ID | Nome | Time |
|---|---|---|
| `11111111-1111-1111-1111-111111111111` | Ana Cartoes | `CARTOES` |
| `22222222-2222-2222-2222-222222222221` | Diego Emprestimos | `EMPRESTIMOS` |
| `33333333-3333-3333-3333-333333333331` | Gabriela Outros | `OUTROS` |

> A migration `V1` criou 3 atendentes por time inicialmente. A migration `V4__keep_one_atendente_per_time.sql` reduz o seed final para 1 atendente por time e preserva a regra de no máximo 3 atendimentos ativos por atendente.

### `Atendimento`

```java
@Entity
@Table(name = "atendimentos")
public class Atendimento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assunto_id", nullable = false)
    private Assunto assunto;

    @Column(length = 500)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeAtendimento time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Atendente atendente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAtendimento status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atribuido_em")
    private LocalDateTime atribuidoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;
}
```

Não existe flag `processado`. O estado operacional é representado por `status`.

---

## 3. DTOs REST

### Entrada

```java
public record CriarAtendimentoRequest(
    UUID assuntoId,
    String observacao
) {}
```

### Saídas

```java
public record AtendimentoResponse(
    UUID id,
    UUID assuntoId,
    String assuntoNome,
    TimeAtendimento time,
    String observacao,
    StatusAtendimento status,
    UUID atendenteId,
    LocalDateTime criadoEm,
    LocalDateTime atribuidoEm,
    LocalDateTime finalizadoEm
) {}

public record AssuntoResponse(
    UUID id,
    String nome,
    TimeAtendimento time
) {}

public record AtendenteStatusResponse(
    UUID id,
    String nome,
    TimeAtendimento time,
    Integer atendimentosAtivos
) {}

public record DashboardResumoResponse(
    Map<TimeAtendimento, Long> emFilaPorTime,
    Map<TimeAtendimento, Long> emAtendimentoPorTime,
    long finalizadosHoje,
    Double tempoMedioEsperaSegundos,
    List<AtendenteStatusResponse> atendentes
) {}

public record ErroResponse(String erro) {}
```

---

## 4. Endpoints REST Implementados

| Método | Endpoint | Descrição |
|---|---|---|
| `GET` | `/api/assuntos` | Lista assuntos cadastrados para seleção no frontend. |
| `POST` | `/api/atendimentos` | Cria atendimento em `AGUARDANDO` e publica evento RabbitMQ. |
| `GET` | `/api/atendimentos` | Lista atendimentos. Aceita filtros opcionais `status`, `atendenteId` e `time`. |
| `PATCH` | `/api/atendimentos/{id}/finalizar` | Finaliza atendimento em andamento e publica `VAGA_LIBERADA`. |
| `GET` | `/api/atendentes` | Lista atendentes com time e ocupação atual. |
| `GET` | `/api/atendentes/{id}/atendimentos` | Lista todos os atendimentos de um atendente, incluindo finalizados. Aceita filtro opcional `status`. |
| `GET` | `/api/dashboard/resumo` | Retorna métricas agregadas do dashboard. |

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/api-docs
```

---

## 5. Repositórios e Concorrência

### Atendimento

```java
Optional<Atendimento> buscarPorIdComLock(UUID id);

Optional<Atendimento> buscarProximoDaFila(String time);

List<Atendimento> findByStatus(StatusAtendimento status);

List<Atendimento> findByAtendenteId(UUID atendenteId);

List<Atendimento> findByAtendenteIdAndStatus(UUID atendenteId, StatusAtendimento status);

List<Atendimento> findByAtribuidoEmIsNotNull();

List<Atendimento> findByStatusAndTimeOrderByCriadoEmAsc(
    StatusAtendimento status,
    TimeAtendimento time
);

long countByStatusAndTime(StatusAtendimento status, TimeAtendimento time);

long countByStatusAndFinalizadoEmBetween(
    StatusAtendimento status,
    LocalDateTime inicio,
    LocalDateTime fim
);
```

Consulta crítica da fila:

```sql
SELECT a.* FROM atendimentos a
WHERE a.time = :time AND a.status = 'AGUARDANDO'
ORDER BY a.criado_em ASC, a.id ASC
LIMIT 1
FOR UPDATE SKIP LOCKED
```

### Atendente

```java
void bloquearDistribuicaoDoTime(String time);

Optional<Atendente> buscarDisponivel(String time);

Optional<Atendente> buscarPorIdComLock(UUID id);

long countByTime(TimeAtendimento time);

void resetarAtendimentosAtivos();
```

Consulta crítica de atendente disponível:

```sql
SELECT a.* FROM atendentes a
WHERE a.time = :time AND a.atendimentos_ativos < 3
ORDER BY a.atendimentos_ativos ASC, a.id ASC
LIMIT 1
FOR UPDATE SKIP LOCKED
```

Também existe uma trava por time:

```sql
SELECT pg_advisory_xact_lock(hashtext(:time))
```

---

## 6. RabbitMQ

### Configuração

```java
DirectExchange flowpay.distribuicao.exchange
Queue flowpay.distribuicao.queue
Routing key flowpay.distribuicao
MessageConverter JacksonJsonMessageConverter
```

### Evento

```java
public enum TipoDistribuicaoEvento {
    ATENDIMENTO_CRIADO,
    VAGA_LIBERADA
}

public record DistribuicaoEvento(
    UUID atendimentoId,
    TimeAtendimento time,
    TipoDistribuicaoEvento tipo
) implements Serializable {}
```

Apesar do `record` implementar `Serializable`, a aplicação publica e consome as mensagens como JSON via `JacksonJsonMessageConverter`. Isso evita o bloqueio de segurança do Spring AMQP contra desserialização Java de classes não autorizadas.

### Fluxo

- `DistribuicaoService.criar()` salva atendimento `AGUARDANDO` e publica `ATENDIMENTO_CRIADO` após commit.
- `DistribuicaoService.finalizar()` finaliza atendimento e publica `VAGA_LIBERADA` após commit.
- `DistribuicaoWorker` consome a fila e chama `DistribuicaoProcessor.tentarAtribuirProximo(time)`.
- `DistribuicaoProcessor` tenta atribuir um atendimento por chamada.
- `DistribuicaoScheduler` roda periodicamente e tenta reprocessar um item por time.

Configurações relevantes:

```properties
flowpay.rabbit.enabled=true
flowpay.distribuicao.scheduler.enabled=true
flowpay.distribuicao.scheduler-delay-ms=5000
```

---

## 7. Schema SQL de Referência

```sql
CREATE TABLE atendentes (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    atendimentos_ativos INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_atendentes_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS')),
    CONSTRAINT chk_limite_atendimentos CHECK (atendimentos_ativos BETWEEN 0 AND 3)
);

CREATE TABLE assuntos (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    CONSTRAINT chk_assuntos_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS'))
);

CREATE TABLE atendimentos (
    id UUID PRIMARY KEY,
    assunto_id UUID NOT NULL REFERENCES assuntos(id),
    observacao VARCHAR(500),
    time VARCHAR(20) NOT NULL,
    atendente_id UUID REFERENCES atendentes(id),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atribuido_em TIMESTAMP,
    finalizado_em TIMESTAMP,
    CONSTRAINT chk_atendimentos_time CHECK (time IN ('CARTOES', 'EMPRESTIMOS', 'OUTROS')),
    CONSTRAINT chk_atendimentos_status CHECK (status IN ('AGUARDANDO', 'EM_ATENDIMENTO', 'FINALIZADO')),
    CONSTRAINT chk_atendimento_aguardando_sem_atendente CHECK (status <> 'AGUARDANDO' OR atendente_id IS NULL)
);

CREATE INDEX idx_atendimentos_status_time ON atendimentos (status, time, criado_em);
CREATE INDEX idx_atendentes_time_carga ON atendentes (time, atendimentos_ativos);
```

---

## 8. Tipos TypeScript

```typescript
export type TimeAtendimento = "CARTOES" | "EMPRESTIMOS" | "OUTROS";

export type StatusAtendimento = "AGUARDANDO" | "EM_ATENDIMENTO" | "FINALIZADO";

export interface CriarAtendimentoRequest {
  assuntoId: string;
  observacao?: string;
}

export interface Assunto {
  id: string;
  nome: string;
  time: TimeAtendimento;
}

export interface Atendimento {
  id: string;
  assuntoId: string;
  assuntoNome: string;
  time: TimeAtendimento;
  observacao?: string;
  status: StatusAtendimento;
  atendenteId?: string;
  criadoEm: string;
  atribuidoEm?: string;
  finalizadoEm?: string;
}

export interface AtendenteStatus {
  id: string;
  nome: string;
  time: TimeAtendimento;
  atendimentosAtivos: number;
}

export interface DashboardResumo {
  emFilaPorTime: Record<TimeAtendimento, number>;
  emAtendimentoPorTime: Record<TimeAtendimento, number>;
  finalizadosHoje: number;
  tempoMedioEsperaSegundos: number;
  atendentes: AtendenteStatus[];
}

export interface ErroResponse {
  erro: string;
}
```
