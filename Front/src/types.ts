export type TimeAtendimento = "CARTOES" | "EMPRESTIMOS" | "OUTROS";

export type StatusAtendimento = "AGUARDANDO" | "EM_ATENDIMENTO" | "FINALIZADO";

export type AppPage = "dashboard" | "reports" | "atendimentos" | "atendentes";

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
  observacao?: string | null;
  status: StatusAtendimento;
  atendenteId?: string | null;
  atendenteNome?: string | null;
  criadoEm: string;
  atribuidoEm?: string | null;
  finalizadoEm?: string | null;
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
  tempoMedioEsperaSegundos: number | null;
  atendentes: AtendenteStatus[];
}

export interface DashboardEvent {
  tipo: "atendimento-criado" | "atendimento-atribuido" | "atendimento-finalizado";
  atendimentoId: string;
  time: TimeAtendimento;
  atendenteId?: string | null;
  timestamp: string;
}

export interface ErroResponse {
  erro: string;
}

export const TIMES: TimeAtendimento[] = ["CARTOES", "EMPRESTIMOS", "OUTROS"];

export const TIME_LABEL: Record<TimeAtendimento, string> = {
  CARTOES: "Cartoes",
  EMPRESTIMOS: "Emprestimos",
  OUTROS: "Outros",
};

export const STATUS_LABEL: Record<StatusAtendimento, string> = {
  AGUARDANDO: "Aguardando",
  EM_ATENDIMENTO: "Em atendimento",
  FINALIZADO: "Finalizado",
};
