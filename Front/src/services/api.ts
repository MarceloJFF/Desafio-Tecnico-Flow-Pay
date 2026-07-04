import type {
  Assunto,
  Atendimento,
  CriarAtendimentoRequest,
  DashboardResumo,
  ErroResponse,
  StatusAtendimento,
  TimeAtendimento,
} from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

type AtendimentoFiltros = {
  status?: StatusAtendimento;
  atendenteId?: string;
  time?: TimeAtendimento;
};

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    ...options,
  });

  if (!response.ok) {
    let message = `Erro HTTP ${response.status}`;

    try {
      const body = (await response.json()) as Partial<ErroResponse>;
      message = body.erro ?? message;
    } catch {
      message = response.statusText || message;
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

function withQuery(path: string, params: Record<string, string | undefined>) {
  const query = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value) {
      query.set(key, value);
    }
  }

  const suffix = query.toString();
  return suffix ? `${path}?${suffix}` : path;
}

export function getDashboardResumo() {
  return request<DashboardResumo>("/api/dashboard/resumo");
}

export function getAssuntos() {
  return request<Assunto[]>("/api/assuntos");
}

export function getAtendimentos(filtros: AtendimentoFiltros = {}) {
  return request<Atendimento[]>(
    withQuery("/api/atendimentos", {
      status: filtros.status,
      atendenteId: filtros.atendenteId,
      time: filtros.time,
    }),
  );
}

export function criarAtendimento(payload: CriarAtendimentoRequest) {
  return request<Atendimento>("/api/atendimentos", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function finalizarAtendimento(id: string) {
  return request<Atendimento>(`/api/atendimentos/${id}/finalizar`, {
    method: "PATCH",
  });
}

export function dashboardStreamUrl() {
  return `${API_BASE_URL}/api/dashboard/stream`;
}
