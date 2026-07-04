import { useEffect, useState } from "react";
import type { AtendenteStatus, Atendimento, StatusAtendimento, TimeAtendimento } from "../../types";
import { STATUS_LABEL, TIMES } from "../../types";
import { finalizarAtendimento } from "../../services/api";
import { formatDateTime, timeLabel } from "../../utils/formatters";

type AtendimentosPanelProps = {
  atendimentos: Atendimento[];
  atendentes: AtendenteStatus[];
  onChanged: () => void;
};

const STATUS_OPTIONS: Array<StatusAtendimento | "TODOS"> = [
  "TODOS",
  "AGUARDANDO",
  "EM_ATENDIMENTO",
  "FINALIZADO",
];

export function AtendimentosPanel({ atendimentos, atendentes, onChanged }: AtendimentosPanelProps) {
  const [status, setStatus] = useState<StatusAtendimento | "TODOS">("TODOS");
  const [time, setTime] = useState<TimeAtendimento | "TODOS">("TODOS");
  const [finalizandoId, setFinalizandoId] = useState<string | null>(null);
  const [selecionado, setSelecionado] = useState<Atendimento | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!selecionado) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setSelecionado(null);
      }
    }

    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = overflowAnterior;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [selecionado]);

  const filtrados = atendimentos.filter((atendimento) => {
    const statusOk = status === "TODOS" || atendimento.status === status;
    const timeOk = time === "TODOS" || atendimento.time === time;
    return statusOk && timeOk;
  });

  function nomeAtendente(atendenteId?: string | null) {
    if (!atendenteId) {
      return "-";
    }

    return atendentes.find((atendente) => atendente.id === atendenteId)?.nome ?? "Atendente nao encontrado";
  }

  async function handleFinalizar(id: string) {
    setFinalizandoId(id);
    setErro(null);

    try {
      await finalizarAtendimento(id);
      onChanged();
    } catch (error) {
      setErro(error instanceof Error ? error.message : "Erro inesperado ao finalizar atendimento.");
    } finally {
      setFinalizandoId(null);
    }
  }

  return (
    <section className="panel panel--table management-panel">
      <div className="panel-header panel-header--stacked">
        <div>
          <p className="eyebrow">Operacao</p>
          <h2>Atendimentos</h2>
        </div>
        <div className="filters">
          <label>
            Status
            <select value={status} onChange={(event) => setStatus(event.target.value as StatusAtendimento | "TODOS")}>
              {STATUS_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option === "TODOS" ? "Todos" : STATUS_LABEL[option]}
                </option>
              ))}
            </select>
          </label>
          <label>
            Time
            <select value={time} onChange={(event) => setTime(event.target.value as TimeAtendimento | "TODOS")}>
              <option value="TODOS">Todos</option>
              {TIMES.map((option) => (
                <option key={option} value={option}>
                  {timeLabel(option)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {erro ? <p className="form-error">{erro}</p> : null}

      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Assunto</th>
              <th>Time</th>
              <th>Status</th>
              <th>Atendente</th>
              <th>Criado</th>
              <th>Acao</th>
            </tr>
          </thead>
          <tbody>
            {filtrados.map((atendimento) => (
              <tr key={atendimento.id}>
                <td>{atendimento.assuntoNome}</td>
                <td>{timeLabel(atendimento.time)}</td>
                <td>
                  <span className={`status-tag status-tag--${atendimento.status.toLowerCase()}`}>
                    {STATUS_LABEL[atendimento.status]}
                  </span>
                </td>
                <td>{nomeAtendente(atendimento.atendenteId)}</td>
                <td>{formatDateTime(atendimento.criadoEm)}</td>
                <td className="table-actions">
                  <button className="button-secondary" type="button" onClick={() => setSelecionado(atendimento)}>
                    Visualizar
                  </button>
                  {atendimento.status === "EM_ATENDIMENTO" ? (
                    <button
                      className="button-secondary"
                      type="button"
                      disabled={finalizandoId === atendimento.id}
                      onClick={() => void handleFinalizar(atendimento.id)}
                    >
                      {finalizandoId === atendimento.id ? "Finalizando..." : "Finalizar"}
                    </button>
                  ) : (
                    null
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selecionado ? (
        <div className="modal-overlay" onMouseDown={(event) => event.target === event.currentTarget && setSelecionado(null)}>
          <div className="modal-card atendimento-details" role="dialog" aria-modal="true" aria-labelledby="detalhe-atendimento-title">
            <div className="modal-header">
              <div>
                <p className="eyebrow">Atendimento</p>
                <h2 id="detalhe-atendimento-title">Detalhes da solicitacao</h2>
              </div>
              <button className="icon-button" type="button" aria-label="Fechar modal" onClick={() => setSelecionado(null)}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6.7 5.3 12 10.6l5.3-5.3 1.4 1.4L13.4 12l5.3 5.3-1.4 1.4L12 13.4l-5.3 5.3-1.4-1.4 5.3-5.3-5.3-5.3 1.4-1.4Z" />
                </svg>
              </button>
            </div>

            <dl className="details-grid">
              <div>
                <dt>ID</dt>
                <dd>{selecionado.id}</dd>
              </div>
              <div>
                <dt>Assunto</dt>
                <dd>{selecionado.assuntoNome}</dd>
              </div>
              <div>
                <dt>Time</dt>
                <dd>{timeLabel(selecionado.time)}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{STATUS_LABEL[selecionado.status]}</dd>
              </div>
              <div>
                <dt>Atendente</dt>
                <dd>{nomeAtendente(selecionado.atendenteId)}</dd>
              </div>
              <div>
                <dt>Criado em</dt>
                <dd>{formatDateTime(selecionado.criadoEm)}</dd>
              </div>
              <div>
                <dt>Atribuido em</dt>
                <dd>{formatDateTime(selecionado.atribuidoEm)}</dd>
              </div>
              <div>
                <dt>Finalizado em</dt>
                <dd>{formatDateTime(selecionado.finalizadoEm)}</dd>
              </div>
              <div className="details-grid__full">
                <dt>Observacao</dt>
                <dd>{selecionado.observacao || "Sem observacao registrada."}</dd>
              </div>
            </dl>

            <div className="modal-actions">
              <button className="button-secondary" type="button" onClick={() => setSelecionado(null)}>
                Fechar
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
