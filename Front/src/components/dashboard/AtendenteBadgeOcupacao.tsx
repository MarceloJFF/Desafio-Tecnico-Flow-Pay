import type { TimeAtendimento } from "../../types";
import { timeLabel } from "../../utils/formatters";

type AtendenteBadgeOcupacaoProps = {
  nome: string;
  time: TimeAtendimento;
  atendimentosAtivos: number;
};

export function AtendenteBadgeOcupacao({ nome, time, atendimentosAtivos }: AtendenteBadgeOcupacaoProps) {
  const pontos = [0, 1, 2];

  return (
    <article className="agent-card">
      <div>
        <strong>{nome}</strong>
        <span>{timeLabel(time)}</span>
      </div>
      <div className="agent-load" aria-label={`${atendimentosAtivos} de 3 atendimentos ativos`}>
        {pontos.map((ponto) => (
          <span key={ponto} className={ponto < atendimentosAtivos ? "agent-load__dot filled" : "agent-load__dot"} />
        ))}
      </div>
    </article>
  );
}
