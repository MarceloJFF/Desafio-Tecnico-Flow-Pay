import { useEffect, useState } from "react";
import { getDashboardResumo } from "../services/api";
import type { DashboardResumo } from "../types";

export function useDashboardSnapshot() {
  const [resumo, setResumo] = useState<DashboardResumo | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [versao, setVersao] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    setCarregando(true);
    getDashboardResumo()
      .then((data) => {
        if (!controller.signal.aborted) {
          setResumo(data);
          setErro(null);
        }
      })
      .catch((error: Error) => {
        if (!controller.signal.aborted) {
          setErro(error.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setCarregando(false);
        }
      });

    return () => controller.abort();
  }, [versao]);

  return {
    resumo,
    setResumo,
    carregando,
    erro,
    refetch: () => setVersao((atual) => atual + 1),
  };
}
