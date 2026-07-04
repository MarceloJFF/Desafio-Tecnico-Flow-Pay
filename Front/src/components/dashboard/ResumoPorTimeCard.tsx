import type { TimeAtendimento } from "../../types";
import { timeLabel } from "../../utils/formatters";

type ResumoPorTimeCardProps = {
  time: TimeAtendimento;
  emFila: number;
  emAtendimento: number;
};

export function ResumoPorTimeCard({ time, emFila, emAtendimento }: ResumoPorTimeCardProps) {
  const total = emFila + emAtendimento;
  const filaPercentual = total === 0 ? 0 : Math.round((emFila / total) * 100);

  return (
    <article className={`team-card team-card--${time.toLowerCase()}`}>
      <div className="team-card__header">
        <span>{timeLabel(time)}</span>
        {emFila > 0 ? <strong className="queue-alert">Fila ativa</strong> : <strong>Sem fila</strong>}
      </div>
      <div className="team-card__numbers">
        <div>
          <small>Em fila</small>
          <strong>{emFila}</strong>
        </div>
        <div>
          <small>Em atendimento</small>
          <strong>{emAtendimento}</strong>
        </div>
      </div>
      <div className="team-card__bar" aria-label={`${filaPercentual}% em fila`}>
        <span style={{ width: `${filaPercentual}%` }} />
      </div>
    </article>
  );
}
