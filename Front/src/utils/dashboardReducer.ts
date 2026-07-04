import type { DashboardEvent, DashboardResumo, TimeAtendimento } from "../types";

function alterarContagem(
  valores: Record<TimeAtendimento, number>,
  time: TimeAtendimento,
  delta: number,
) {
  return {
    ...valores,
    [time]: Math.max(0, (valores[time] ?? 0) + delta),
  };
}

export function aplicarEventoDashboard(
  resumo: DashboardResumo,
  evento: DashboardEvent,
): DashboardResumo {
  if (evento.tipo === "atendimento-criado") {
    return {
      ...resumo,
      emFilaPorTime: alterarContagem(resumo.emFilaPorTime, evento.time, 1),
    };
  }

  if (evento.tipo === "atendimento-atribuido") {
    return {
      ...resumo,
      emFilaPorTime: alterarContagem(resumo.emFilaPorTime, evento.time, -1),
      emAtendimentoPorTime: alterarContagem(resumo.emAtendimentoPorTime, evento.time, 1),
      atendentes: resumo.atendentes.map((atendente) =>
        atendente.id === evento.atendenteId
          ? {
              ...atendente,
              atendimentosAtivos: Math.min(3, atendente.atendimentosAtivos + 1),
            }
          : atendente,
      ),
    };
  }

  return {
    ...resumo,
    emAtendimentoPorTime: alterarContagem(resumo.emAtendimentoPorTime, evento.time, -1),
    finalizadosHoje: resumo.finalizadosHoje + 1,
    atendentes: resumo.atendentes.map((atendente) =>
      atendente.id === evento.atendenteId
        ? {
            ...atendente,
            atendimentosAtivos: Math.max(0, atendente.atendimentosAtivos - 1),
          }
        : atendente,
    ),
  };
}
