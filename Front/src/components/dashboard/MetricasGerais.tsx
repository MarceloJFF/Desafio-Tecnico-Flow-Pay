import { formatDuration } from "../../utils/formatters";

type MetricasGeraisProps = {
  finalizadosHoje: number;
  tempoMedioEsperaSegundos: number | null;
};

export function MetricasGerais({ finalizadosHoje, tempoMedioEsperaSegundos }: MetricasGeraisProps) {
  return (
    <section className="metric-grid" aria-label="Metricas gerais">
      <article className="metric-card metric-card--dark">
        <span>Finalizados hoje</span>
        <strong>{finalizadosHoje}</strong>
      </article>
      <article className="metric-card">
        <span>Tempo medio de espera</span>
        <strong>{formatDuration(tempoMedioEsperaSegundos)}</strong>
      </article>
    </section>
  );
}
