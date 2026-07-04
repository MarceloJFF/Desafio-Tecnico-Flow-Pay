import type { AtendenteStatus } from "../../types";
import { AtendenteBadgeOcupacao } from "./AtendenteBadgeOcupacao";

type AtendentesGridProps = {
  atendentes: AtendenteStatus[];
};

export function AtendentesGrid({ atendentes }: AtendentesGridProps) {
  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Capacidade</p>
          <h2>Atendentes</h2>
        </div>
      </div>
      <div className="agents-grid">
        {atendentes.map((atendente) => (
          <AtendenteBadgeOcupacao
            key={atendente.id}
            nome={atendente.nome}
            time={atendente.time}
            atendimentosAtivos={atendente.atendimentosAtivos}
          />
        ))}
      </div>
    </section>
  );
}
