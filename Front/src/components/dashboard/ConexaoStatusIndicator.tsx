type ConexaoStatusIndicatorProps = {
  conectado: boolean;
};

export function ConexaoStatusIndicator({ conectado }: ConexaoStatusIndicatorProps) {
  return (
    <span className={conectado ? "status-pill status-pill--online" : "status-pill status-pill--offline"}>
      <span className="status-dot" />
      {conectado ? "Ao vivo" : "Reconectando"}
    </span>
  );
}
