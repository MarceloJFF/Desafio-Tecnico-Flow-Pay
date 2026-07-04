import { useEffect, useState } from "react";
import { AppHeader } from "../components/layout/AppHeader";
import { ErrorBanner } from "../components/common/ErrorBanner";
import { LoadingSpinner } from "../components/common/LoadingSpinner";
import { AtendimentoForm } from "../components/dashboard/AtendimentoForm";
import { AtendentesGrid } from "../components/dashboard/AtendentesGrid";
import { AtendimentosPanel } from "../components/dashboard/AtendimentosPanel";
import { FilaPorTimeTable } from "../components/dashboard/FilaPorTimeTable";
import { MetricasGerais } from "../components/dashboard/MetricasGerais";
import { ResumoPorTimeCard } from "../components/dashboard/ResumoPorTimeCard";
import { AtendentesPage } from "./AtendentesPage";
import { ReportsPage } from "./ReportsPage";
import { useDashboardSnapshot } from "../hooks/useDashboardSnapshot";
import { useDashboardStream } from "../hooks/useDashboardStream";
import { getAssuntos, getAtendimentos } from "../services/api";
import type { AppPage, Assunto, Atendimento, DashboardEvent } from "../types";
import { TIMES } from "../types";
import { aplicarEventoDashboard } from "../utils/dashboardReducer";

export function DashboardPage() {
  const { resumo, setResumo, carregando, erro, refetch } = useDashboardSnapshot();
  const [assuntos, setAssuntos] = useState<Assunto[]>([]);
  const [atendimentos, setAtendimentos] = useState<Atendimento[]>([]);
  const [erroDados, setErroDados] = useState<string | null>(null);
  const [reloadAtendimentos, setReloadAtendimentos] = useState(0);
  const [activePage, setActivePage] = useState<AppPage>("dashboard");

  function recarregarAtendimentos() {
    setReloadAtendimentos((atual) => atual + 1);
  }

  function recarregarTudo() {
    refetch();
    recarregarAtendimentos();
  }

  function handleDashboardEvent(evento: DashboardEvent) {
    setResumo((atual) => (atual ? aplicarEventoDashboard(atual, evento) : atual));
    recarregarAtendimentos();
  }

  const { conectado } = useDashboardStream({
    onEvent: handleDashboardEvent,
    onReconnect: recarregarTudo,
  });

  useEffect(() => {
    let ativo = true;

    getAssuntos()
      .then((data) => {
        if (ativo) {
          setAssuntos(data);
        }
      })
      .catch((error: Error) => {
        if (ativo) {
          setErroDados(error.message);
        }
      });

    return () => {
      ativo = false;
    };
  }, []);

  useEffect(() => {
    let ativo = true;

    getAtendimentos()
      .then((data) => {
        if (ativo) {
          setAtendimentos(data);
          setErroDados(null);
        }
      })
      .catch((error: Error) => {
        if (ativo) {
          setErroDados(error.message);
        }
      });

    return () => {
      ativo = false;
    };
  }, [reloadAtendimentos]);

  const atendimentosEmFila = atendimentos.filter((atendimento) => atendimento.status === "AGUARDANDO");

  return (
    <main className="app-shell">
      <AppHeader conectado={conectado} activePage={activePage} onNavigate={setActivePage} />

      {erro ? <ErrorBanner message={erro} /> : null}
      {erroDados ? <ErrorBanner message={erroDados} /> : null}

      {carregando && !resumo ? (
        <div className="loading-area">
          <LoadingSpinner />
          <span>Carregando dashboard...</span>
        </div>
      ) : null}

      {resumo ? (
        <>
          {activePage === "dashboard" ? (
            <>
              <MetricasGerais
                finalizadosHoje={resumo.finalizadosHoje}
                tempoMedioEsperaSegundos={resumo.tempoMedioEsperaSegundos}
              />

              <section className="team-grid" aria-label="Resumo por time">
                {TIMES.map((time) => (
                  <ResumoPorTimeCard
                    key={time}
                    time={time}
                    emFila={resumo.emFilaPorTime[time] ?? 0}
                    emAtendimento={resumo.emAtendimentoPorTime[time] ?? 0}
                  />
                ))}
              </section>

              <div className="content-grid">
                <div className="left-column">
                  <FilaPorTimeTable atendimentosEmFila={atendimentosEmFila} />
                </div>

                <div className="right-column">
                  <AtendimentoForm assuntos={assuntos} onCreated={recarregarTudo} />
                  <AtendentesGrid atendentes={resumo.atendentes} />
                </div>
              </div>
            </>
          ) : null}

          {activePage === "reports" ? <ReportsPage resumo={resumo} atendimentos={atendimentos} /> : null}

          {activePage === "atendimentos" ? (
            <div className="page-stack">
              <div className="page-action-row">
                <section className="panel page-heading">
                  <p className="eyebrow">Atendimentos</p>
                  <h2>Operacao e historico</h2>
                  <p>Liste, filtre, visualize detalhes e finalize atendimentos em andamento.</p>
                </section>
                <AtendimentoForm assuntos={assuntos} onCreated={recarregarTudo} />
              </div>
              <AtendimentosPanel atendimentos={atendimentos} atendentes={resumo.atendentes} onChanged={recarregarTudo} />
            </div>
          ) : null}

          {activePage === "atendentes" ? <AtendentesPage atendentes={resumo.atendentes} /> : null}
        </>
      ) : null}
    </main>
  );
}
