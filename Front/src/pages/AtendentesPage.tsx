import type { AtendenteStatus, TimeAtendimento } from "../types";
import { TIMES } from "../types";
import { timeLabel } from "../utils/formatters";
import { AtendenteBadgeOcupacao } from "../components/dashboard/AtendenteBadgeOcupacao";

type AtendentesPageProps = {
  atendentes: AtendenteStatus[];
};

export function AtendentesPage({ atendentes }: AtendentesPageProps) {
  function porSquad(time: TimeAtendimento) {
    return atendentes.filter((atendente) => atendente.time === time);
  }

  return (
    <div className="page-stack">
      <section className="panel page-heading">
        <p className="eyebrow">Atendentes</p>
        <h2>Squads e ocupacao</h2>
        <p>Lista agrupada por squad com a carga atual de cada atendente.</p>
      </section>

      <section className="squad-grid">
        {TIMES.map((time) => {
          const membros = porSquad(time);
          const ativos = membros.reduce((total, atendente) => total + atendente.atendimentosAtivos, 0);
          const capacidade = membros.length * 3;

          return (
            <article className={`panel squad-card squad-card--${time.toLowerCase()}`} key={time}>
              <div className="panel-header">
                <div>
                  <p className="eyebrow">Squad</p>
                  <h2>{timeLabel(time)}</h2>
                </div>
                <strong className="capacity-pill">{ativos}/{capacidade}</strong>
              </div>

              {membros.length === 0 ? (
                <p className="empty-state">Nenhum atendente cadastrado neste squad.</p>
              ) : (
                <div className="agents-grid">
                  {membros.map((atendente) => (
                    <AtendenteBadgeOcupacao
                      key={atendente.id}
                      nome={atendente.nome}
                      time={atendente.time}
                      atendimentosAtivos={atendente.atendimentosAtivos}
                    />
                  ))}
                </div>
              )}
            </article>
          );
        })}
      </section>
    </div>
  );
}
