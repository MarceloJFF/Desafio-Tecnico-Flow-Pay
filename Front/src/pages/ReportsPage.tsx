import type { Atendimento, DashboardResumo, TimeAtendimento } from "../types";
import { TIMES } from "../types";
import { formatDateTime, formatDuration, timeLabel } from "../utils/formatters";

type ReportsPageProps = {
  resumo: DashboardResumo;
  atendimentos: Atendimento[];
};

export function ReportsPage({ resumo, atendimentos }: ReportsPageProps) {
  const totalFila = TIMES.reduce((total, time) => total + (resumo.emFilaPorTime[time] ?? 0), 0);
  const totalAtivos = TIMES.reduce((total, time) => total + (resumo.emAtendimentoPorTime[time] ?? 0), 0);
  const maiorVolume = Math.max(
    1,
    ...TIMES.map((time) => (resumo.emFilaPorTime[time] ?? 0) + (resumo.emAtendimentoPorTime[time] ?? 0)),
  );

  const finalizadosPorTime = TIMES.reduce(
    (acc, time) => ({
      ...acc,
      [time]: atendimentos.filter((atendimento) => atendimento.time === time && atendimento.status === "FINALIZADO").length,
    }),
    {} as Record<TimeAtendimento, number>,
  );
  const maiorFinalizados = Math.max(1, ...TIMES.map((time) => finalizadosPorTime[time] ?? 0));
  const finalizados = atendimentos
    .filter((atendimento) => atendimento.status === "FINALIZADO")
    .sort((a, b) => new Date(b.finalizadoEm ?? b.criadoEm).getTime() - new Date(a.finalizadoEm ?? a.criadoEm).getTime());

  return (
    <div className="page-stack">
      <section className="report-hero panel">
        <div>
          <p className="eyebrow">Reports</p>
          <h2>Leitura gerencial da operacao</h2>
          <p>Visao consolidada por squad para identificar gargalos, ocupacao e volume concluido.</p>
        </div>
        <div className="report-kpis">
          <article>
            <span>Fila total</span>
            <strong>{totalFila}</strong>
          </article>
          <article>
            <span>Ativos</span>
            <strong>{totalAtivos}</strong>
          </article>
          <article>
            <span>Espera media</span>
            <strong>{formatDuration(resumo.tempoMedioEsperaSegundos)}</strong>
          </article>
        </div>
      </section>

      <section className="reports-grid">
        <article className="panel chart-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Volume</p>
              <h2>Fila vs atendimento</h2>
            </div>
          </div>
          <div className="bar-chart" role="img" aria-label="Grafico de fila e atendimentos ativos por squad">
            {TIMES.map((time) => {
              const fila = resumo.emFilaPorTime[time] ?? 0;
              const ativos = resumo.emAtendimentoPorTime[time] ?? 0;
              return (
                <div className="bar-row" key={time}>
                  <span>{timeLabel(time)}</span>
                  <div className="stacked-bar">
                    <i className="bar-waiting" style={{ width: `${(fila / maiorVolume) * 100}%` }} />
                    <i className="bar-active" style={{ width: `${(ativos / maiorVolume) * 100}%` }} />
                  </div>
                  <strong>{fila + ativos}</strong>
                </div>
              );
            })}
          </div>
          <div className="chart-legend">
            <span><i className="legend-waiting" /> Fila</span>
            <span><i className="legend-active" /> Em atendimento</span>
          </div>
        </article>

        <article className="panel chart-panel">
          <div className="panel-header">
            <div>
              <p className="eyebrow">Conclusao</p>
              <h2>Finalizados por squad</h2>
            </div>
          </div>
          <div className="column-chart" role="img" aria-label="Grafico de atendimentos finalizados por squad">
            {TIMES.map((time) => (
              <div className="column-item" key={time}>
                <div className="column-track">
                  <span style={{ height: `${Math.max(8, ((finalizadosPorTime[time] ?? 0) / maiorFinalizados) * 100)}%` }} />
                </div>
                <strong>{finalizadosPorTime[time] ?? 0}</strong>
                <small>{timeLabel(time)}</small>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="panel panel--table">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Historico</p>
            <h2>Atendimentos finalizados</h2>
          </div>
          <strong className="capacity-pill">{finalizados.length}</strong>
        </div>

        {finalizados.length === 0 ? (
          <p className="empty-state">Nenhum atendimento finalizado ainda.</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Assunto</th>
                  <th>Squad</th>
                  <th>Atendente</th>
                  <th>Criado</th>
                  <th>Atribuido</th>
                  <th>Finalizado</th>
                </tr>
              </thead>
              <tbody>
                {finalizados.map((atendimento) => (
                  <tr key={atendimento.id}>
                    <td>{atendimento.assuntoNome}</td>
                    <td>{timeLabel(atendimento.time)}</td>
                    <td>{atendimento.atendenteNome ?? "-"}</td>
                    <td>{formatDateTime(atendimento.criadoEm)}</td>
                    <td>{formatDateTime(atendimento.atribuidoEm)}</td>
                    <td>{formatDateTime(atendimento.finalizadoEm)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
