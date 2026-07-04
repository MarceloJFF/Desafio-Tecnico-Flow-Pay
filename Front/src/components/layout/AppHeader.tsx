import { ConexaoStatusIndicator } from "../dashboard/ConexaoStatusIndicator";
import type { AppPage } from "../../types";

type AppHeaderProps = {
  conectado: boolean;
  activePage: AppPage;
  onNavigate: (page: AppPage) => void;
};

const NAV_ITEMS: Array<{ page: AppPage; label: string }> = [
  { page: "dashboard", label: "Dashboard" },
  { page: "reports", label: "Reports" },
  { page: "atendimentos", label: "Atendimentos" },
  { page: "atendentes", label: "Atendentes" },
];

export function AppHeader({ conectado, activePage, onNavigate }: AppHeaderProps) {
  return (
    <header className="app-header">
      <div className="brand-row">
        <img className="brand-logo" src="https://www.ubots.com.br/android-chrome-512x512.png" alt="Ubots" />
        <div>
          <p className="eyebrow">FlowPay Operations</p>
          <h1>Dashboard operacional</h1>
          <p className="header-copy">Acompanhe filas, ocupacao dos atendentes e distribuicao em tempo real.</p>
        </div>
      </div>
      <div className="header-actions">
        <ConexaoStatusIndicator conectado={conectado} />
        <nav className="app-nav" aria-label="Navegacao principal">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.page}
              className={activePage === item.page ? "nav-tab nav-tab--active" : "nav-tab"}
              type="button"
              aria-current={activePage === item.page ? "page" : undefined}
              onClick={() => onNavigate(item.page)}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </div>
    </header>
  );
}
