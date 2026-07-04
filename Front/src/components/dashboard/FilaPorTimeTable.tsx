import { useEffect, useState } from "react";
import type { Atendimento } from "../../types";
import { formatDateTime, formatDuration, secondsSince, timeLabel } from "../../utils/formatters";

type FilaPorTimeTableProps = {
  atendimentosEmFila: Atendimento[];
};

export function FilaPorTimeTable({ atendimentosEmFila }: FilaPorTimeTableProps) {
  const [, setTick] = useState(0);

  useEffect(() => {
    const interval = window.setInterval(() => setTick((value) => value + 1), 1000);
    return () => window.clearInterval(interval);
  }, []);

  const ordenados = [...atendimentosEmFila].sort(
    (a, b) => new Date(a.criadoEm).getTime() - new Date(b.criadoEm).getTime(),
  );

  return (
    <section className="panel panel--table">
      <div className="panel-header">
        <div>
          <p className="eyebrow">FIFO por time</p>
          <h2>Fila de espera</h2>
        </div>
        <strong>{ordenados.length} aguardando</strong>
      </div>

      {ordenados.length === 0 ? (
        <p className="empty-state">Nenhum atendimento aguardando no momento.</p>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Assunto</th>
                <th>Time</th>
                <th>Criado em</th>
                <th>Espera</th>
              </tr>
            </thead>
            <tbody>
              {ordenados.map((atendimento) => (
                <tr key={atendimento.id}>
                  <td>{atendimento.assuntoNome}</td>
                  <td>{timeLabel(atendimento.time)}</td>
                  <td>{formatDateTime(atendimento.criadoEm)}</td>
                  <td>{formatDuration(secondsSince(atendimento.criadoEm))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
