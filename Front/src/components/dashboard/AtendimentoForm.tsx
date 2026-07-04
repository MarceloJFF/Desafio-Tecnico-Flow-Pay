import { FormEvent, MouseEvent, useEffect, useState } from "react";
import type { Assunto } from "../../types";
import { criarAtendimento } from "../../services/api";
import { timeLabel } from "../../utils/formatters";

type AtendimentoFormProps = {
  assuntos: Assunto[];
  onCreated: () => void;
};

export function AtendimentoForm({ assuntos, onCreated }: AtendimentoFormProps) {
  const [aberto, setAberto] = useState(false);
  const [assuntoId, setAssuntoId] = useState("");
  const [observacao, setObservacao] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!aberto) {
      return;
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        fecharModal();
      }
    }

    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = overflowAnterior;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [aberto, enviando]);

  function abrirModal() {
    setErro(null);
    setAberto(true);
  }

  function fecharModal() {
    if (enviando) {
      return;
    }

    setAberto(false);
    setErro(null);
  }

  function handleOverlayClick(event: MouseEvent<HTMLDivElement>) {
    if (event.target === event.currentTarget) {
      fecharModal();
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!assuntoId) {
      setErro("Selecione um assunto.");
      return;
    }

    setEnviando(true);
    setErro(null);

    try {
      await criarAtendimento({ assuntoId, observacao: observacao.trim() || undefined });
      setAssuntoId("");
      setObservacao("");
      setAberto(false);
      onCreated();
    } catch (error) {
      setErro(error instanceof Error ? error.message : "Erro inesperado ao criar atendimento.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section className="panel form-panel">
      <div className="panel-header">
        <div>
          <p className="eyebrow">Entrada</p>
          <h2>Criar atendimento</h2>
        </div>
      </div>
      <p className="panel-copy">Abra uma nova solicitacao e deixe a distribuicao automatica cuidar da fila.</p>
      <button type="button" onClick={abrirModal} disabled={assuntos.length === 0}>
        Novo atendimento
      </button>

      {aberto ? (
        <div className="modal-overlay" onMouseDown={handleOverlayClick}>
          <div className="modal-card" role="dialog" aria-modal="true" aria-labelledby="novo-atendimento-title">
            <div className="modal-header">
              <div>
                <p className="eyebrow">Novo atendimento</p>
                <h2 id="novo-atendimento-title">Dados da solicitacao</h2>
              </div>
              <button className="icon-button" type="button" aria-label="Fechar modal" onClick={fecharModal}>
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6.7 5.3 12 10.6l5.3-5.3 1.4 1.4L13.4 12l5.3 5.3-1.4 1.4L12 13.4l-5.3 5.3-1.4-1.4 5.3-5.3-5.3-5.3 1.4-1.4Z" />
                </svg>
              </button>
            </div>

            <form onSubmit={handleSubmit}>
              <label htmlFor="assunto-atendimento">
                Assunto
                <select
                  id="assunto-atendimento"
                  value={assuntoId}
                  onChange={(event) => setAssuntoId(event.target.value)}
                  autoFocus
                >
                  <option value="">Selecione</option>
                  {assuntos.map((assunto) => (
                    <option key={assunto.id} value={assunto.id}>
                      {assunto.nome} - {timeLabel(assunto.time)}
                    </option>
                  ))}
                </select>
              </label>

              <label htmlFor="observacao-atendimento">
                Observacao
                <textarea
                  id="observacao-atendimento"
                  rows={4}
                  maxLength={500}
                  value={observacao}
                  onChange={(event) => setObservacao(event.target.value)}
                  placeholder="Detalhe livre opcional para o atendimento"
                />
              </label>

              {erro ? <p className="form-error">{erro}</p> : null}

              <div className="modal-actions">
                <button className="button-secondary" type="button" onClick={fecharModal} disabled={enviando}>
                  Cancelar
                </button>
                <button type="submit" disabled={enviando || assuntos.length === 0}>
                  {enviando ? "Criando..." : "Criar atendimento"}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}
    </section>
  );
}
