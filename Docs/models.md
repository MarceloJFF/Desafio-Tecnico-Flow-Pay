# Models.md — Modelos de Dados (FlowPay)

Modelos usados no backend (Java/JPA) e correspondentes no frontend (TypeScript).

---

## 1. Diagrama Conceitual

```
Time (1) ──< (N) Atendente (1) ──< (N) Atendimento >── (N) 1 Time
```

- Um `Time` tem vários `Atendentes`.
- Um `Atendente` pertence a um `Time` e atende vários `Atendimentos`.
- Um `Atendimento` pertence a um `Time` e, opcionalmente, a um `Atendente` (nulo enquanto está em fila).

---

## 2. Backend — Entidades JPA (Spring Boot)

### `TimeAtendimento` (enum ou entidade — recomendado enum fixo, já que os 3 times são conhecidos e fixos no domínio)

```java
public enum TimeAtendimento {
    CARTOES,
    EMPRESTIMOS,
    OUTROS;

    public static TimeAtendimento fromAssunto(String assunto) {
        if (assunto == null) return OUTROS;
        String normalizado = assunto.trim().toLowerCase();
        if (normalizado.contains("cart")) return CARTOES;
        if (normalizado.contains("empréstimo") || normalizado.contains("emprestimo")) return EMPRESTIMOS;
        return OUTROS;
    }
}
```

> Nota: usar enum evita uma tabela `times` supérflua para um domínio fixo de 3 valores. Se o requisito evoluir para times dinâmicos/configuráveis, migrar para entidade `Time` com tabela própria.

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
    @Column(nullable = false)
    private TimeAtendimento time;

    @Column(nullable = false)
    private Integer atendimentosAtivos = 0;

    @Version
    private Long version; // controle de concorrência otimista adicional (defesa em profundidade)

    // getters/setters
}
```

### `Atendimento`

```java
@Entity
@Table(name = "atendimentos")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String assunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeAtendimento time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atendente_id")
    private Atendente atendente; // null enquanto AGUARDANDO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusAtendimento status;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime atribuidoEm;

    private LocalDateTime finalizadoEm;

    // getters/setters
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

### Repositório — método-chave para atribuição segura

```java
public interface AtendenteRepository extends JpaRepository<Atendente, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = """
        SELECT a.* FROM atendentes a
        WHERE a.time = :time AND a.atendimentos_ativos < 3
        ORDER BY a.atendimentos_ativos ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Atendente> buscarDisponivel(@Param("time") String time);
}
```

```java
public interface AtendimentoRepository extends JpaRepository<Atendimento, UUID> {

    @Query(value = """
        SELECT a.* FROM atendimentos a
        WHERE a.time = :time AND a.status = 'AGUARDANDO'
        ORDER BY a.criado_em ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Atendimento> buscarProximoDaFila(@Param("time") String time);

    List<Atendimento> findByStatus(StatusAtendimento status);
}
```

---

## 3. DTOs (contratos da API REST)

```java
public record CriarAtendimentoRequest(String assunto) {}

public record AtendimentoResponse(
    UUID id,
    String assunto,
    TimeAtendimento time,
    StatusAtendimento status,
    UUID atendenteId,
    LocalDateTime criadoEm,
    LocalDateTime atribuidoEm,
    LocalDateTime finalizadoEm
) {}

public record DashboardResumoResponse(
    Map<TimeAtendimento, Integer> emFilaPorTime,
    Map<TimeAtendimento, Integer> emAtendimentoPorTime,
    Integer finalizadosHoje,
    Double tempoMedioEsperaSegundos,
    List<AtendenteStatusResponse> atendentes
) {}

public record AtendenteStatusResponse(
    UUID id,
    String nome,
    TimeAtendimento time,
    Integer atendimentosAtivos
) {}
```

---

## 4. Eventos SSE (payload)

```java
public record DashboardEvent(
    String tipo,          // "atendimento-criado" | "atendimento-atribuido" | "atendimento-finalizado"
    UUID atendimentoId,
    TimeAtendimento time,
    UUID atendenteId,     // nullable
    LocalDateTime timestamp
) {}
```

---

## 5. Frontend — Tipos TypeScript

```typescript
export type TimeAtendimento = "CARTOES" | "EMPRESTIMOS" | "OUTROS";

export type StatusAtendimento = "AGUARDANDO" | "EM_ATENDIMENTO" | "FINALIZADO";

export interface Atendimento {
  id: string;
  assunto: string;
  time: TimeAtendimento;
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

export interface DashboardEvent {
  tipo: "atendimento-criado" | "atendimento-atribuido" | "atendimento-finalizado";
  atendimentoId: string;
  time: TimeAtendimento;
  atendenteId?: string;
  timestamp: string;
}
```

---

## 6. Schema SQL (referência, gerado via migrations — Flyway/Liquibase)

```sql
CREATE TABLE atendentes (
    id UUID PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    atendimentos_ativos INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_limite_atendimentos CHECK (atendimentos_ativos BETWEEN 0 AND 3)
);

CREATE TABLE atendimentos (
    id UUID PRIMARY KEY,
    assunto VARCHAR(255) NOT NULL,
    time VARCHAR(20) NOT NULL,
    atendente_id UUID REFERENCES atendentes(id),
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atribuido_em TIMESTAMP,
    finalizado_em TIMESTAMP
);

CREATE INDEX idx_atendimentos_status_time ON atendimentos (status, time, criado_em);
CREATE INDEX idx_atendentes_time_carga ON atendentes (time, atendimentos_ativos);
```

> O `CHECK` constraint no banco é uma segunda linha de defesa (além da lógica transacional) contra qualquer bug que tente estourar o limite de 3.
