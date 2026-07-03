# Design.md — Componentes de Tela (Frontend React)

Documento de design de componentes do Dashboard FlowPay. Foco em estrutura, responsabilidade e contrato de dados de cada componente — não em pixel-perfect visual.

---

## 1. Estrutura de Telas

Uma única página principal (`/dashboard`), sem necessidade de roteamento complexo nesta versão:

```
frontend/src/
├── pages/
│   └── DashboardPage.tsx
├── components/
│   ├── layout/
│   │   └── AppHeader.tsx
│   ├── dashboard/
│   │   ├── ResumoPorTimeCard.tsx
│   │   ├── FilaPorTimeTable.tsx
│   │   ├── AtendentesGrid.tsx
│   │   ├── AtendenteBadgeOcupacao.tsx
│   │   ├── MetricasGerais.tsx
│   │   └── ConexaoStatusIndicator.tsx
│   └── common/
│       ├── LoadingSpinner.tsx
│       └── ErrorBanner.tsx
├── hooks/
│   ├── useDashboardStream.ts     # conecta no SSE e mantém estado atualizado
│   └── useDashboardSnapshot.ts   # busca estado inicial via REST
└── services/
    └── api.ts                    # client REST (fetch/axios)
```

---

## 2. `DashboardPage.tsx` — Página Principal

**Responsabilidade:** orquestrar o carregamento inicial (snapshot REST) + assinatura do stream SSE, e distribuir dados para os componentes filhos.

**Fluxo:**
1. Ao montar, chama `useDashboardSnapshot()` → `GET /api/dashboard/resumo` para estado inicial.
2. Em paralelo, `useDashboardStream()` abre `EventSource` em `/api/dashboard/stream`.
3. Cada evento recebido atualiza o estado local (merge incremental, não re-fetch completo).
4. Se a conexão SSE cair e reconectar, dispara novo snapshot REST para resincronizar (ver `behavior.md`, seção 7).

O backend processa distribuição de forma assíncrona via RabbitMQ. Portanto, um atendimento recém-criado pode aparecer brevemente como `AGUARDANDO` antes do worker atribuir para um atendente.

**Estado local:**
```typescript
{
  resumo: DashboardResumo | null;
  conectado: boolean;
  carregando: boolean;
  erro: string | null;
}
```

**Layout (alto nível):**
```
┌─────────────────────────────────────────────┐
│ AppHeader                    [● Conectado]   │  ← ConexaoStatusIndicator
├─────────────────────────────────────────────┤
│ MetricasGerais (cards: finalizados hoje,     │
│ tempo médio de espera)                       │
├─────────────────────────────────────────────┤
│ ResumoPorTimeCard × 3 (Cartões / Empréstimos │
│ / Outros) — em fila vs em atendimento        │
├─────────────────────────────────────────────┤
│ AtendentesGrid — um badge por atendente      │
├─────────────────────────────────────────────┤
│ FilaPorTimeTable — lista detalhada da fila   │
└─────────────────────────────────────────────┘
```

---

## 3. `ResumoPorTimeCard.tsx`

**Props:**
```typescript
interface ResumoPorTimeCardProps {
  time: TimeAtendimento;
  emFila: number;
  emAtendimento: number;
}
```

**Responsabilidade:** exibir, por time, quantos atendimentos estão em fila e quantos em atendimento. Cor de destaque (ex: amarelo/vermelho) quando `emFila > 0`, sinalizando gargalo visualmente.

---

## 4. `AtendentesGrid.tsx` + `AtendenteBadgeOcupacao.tsx`

**Props do Grid:**
```typescript
interface AtendentesGridProps {
  atendentes: AtendenteStatus[];
}
```

**Props do Badge:**
```typescript
interface AtendenteBadgeOcupacaoProps {
  nome: string;
  time: TimeAtendimento;
  atendimentosAtivos: number; // 0-3
}
```

**Responsabilidade:** cada atendente é um card compacto mostrando nome, time e um indicador visual de ocupação (ex: `●●●○` para 3/4... na verdade `atendimentosAtivos/3`, tipo barra de progresso ou 3 pontos preenchidos). Agrupar visualmente por time.

---

## 5. `FilaPorTimeTable.tsx`

**Props:**
```typescript
interface FilaPorTimeTableProps {
  atendimentosEmFila: Atendimento[]; // status === AGUARDANDO
}
```

**Responsabilidade:** tabela com colunas: Assunto | Time | Criado em | Tempo em espera (calculado no client, atualizado a cada segundo via `setInterval` local — não depende de novo evento do backend para o relógio andar). Ordenada por `criadoEm` ascendente (FIFO visual).

---

## 6. `MetricasGerais.tsx`

**Props:**
```typescript
interface MetricasGeraisProps {
  finalizadosHoje: number;
  tempoMedioEsperaSegundos: number;
}
```

**Responsabilidade:** cards simples de KPI no topo da página.

---

## 7. `ConexaoStatusIndicator.tsx`

**Props:**
```typescript
interface ConexaoStatusIndicatorProps {
  conectado: boolean;
}
```

**Responsabilidade:** indicador visual (bolinha verde/vermelha + texto "Conectado" / "Reconectando...") do estado da conexão SSE — importante para o gestor saber se está vendo dados ao vivo ou potencialmente desatualizados.

---

## 8. Hook `useDashboardStream.ts`

**Contrato:**
```typescript
function useDashboardStream(onEvent: (event: DashboardEvent) => void): {
  conectado: boolean;
} {
  // abre new EventSource('/api/dashboard/stream')
  // onmessage → parse JSON → onEvent(evento)
  // onerror → seta conectado = false (o browser já tenta reconectar sozinho)
  // onopen → seta conectado = true
}
```

**Responsabilidade:** encapsular o ciclo de vida do `EventSource`, expor apenas o essencial (callback de evento + status de conexão) para a página consumir sem lidar com a API nativa diretamente.

---

## 9. Hook `useDashboardSnapshot.ts`

**Contrato:**
```typescript
function useDashboardSnapshot(): {
  resumo: DashboardResumo | null;
  carregando: boolean;
  erro: string | null;
  refetch: () => void;
}
```

**Responsabilidade:** buscar `GET /api/dashboard/resumo` — usado no carregamento inicial e sempre que o SSE reconectar (resincronização).

---

## 10. Estratégia de Atualização de Estado (merge incremental)

Ao invés de re-buscar o resumo inteiro a cada evento SSE, o estado é atualizado de forma incremental no reducer da página:

```typescript
function dashboardReducer(state: DashboardResumo, event: DashboardEvent): DashboardResumo {
  switch (event.tipo) {
    case "atendimento-criado":
      // incrementa emFilaPorTime[event.time] (será corrigido se logo em seguida vier "atribuido")
    case "atendimento-atribuido":
      // decrementa emFilaPorTime[event.time], incrementa emAtendimentoPorTime[event.time]
      // atualiza atendimentosAtivos do atendente correspondente
    case "atendimento-finalizado":
      // decrementa emAtendimentoPorTime[event.time], incrementa finalizadosHoje
      // atualiza atendimentosAtivos do atendente correspondente
  }
}
```

Isso evita round-trips desnecessários a cada evento, mantendo a UI responsiva mesmo com muitos eventos por segundo.

---

## 11. Considerações de UX

- Skeleton/loading state durante o carregamento inicial (`carregando = true`), antes do primeiro snapshot chegar.
- `ErrorBanner` visível se o snapshot inicial falhar (ex: backend fora do ar) — não travar a tela em branco.
- Indicador de conexão sempre visível — transparência sobre "isso está ao vivo ou não".
- Responsivo o suficiente para tela de monitoramento (geralmente TV/monitor grande na operação), mas sem necessidade de otimização mobile nesta fase.
