import { useEffect, useRef, useState } from "react";
import { dashboardStreamUrl } from "../services/api";
import type { DashboardEvent } from "../types";

type StreamHandlers = {
  onEvent: (event: DashboardEvent) => void;
  onReconnect?: () => void;
};

const EVENTOS = ["atendimento-criado", "atendimento-atribuido", "atendimento-finalizado"] as const;

export function useDashboardStream({ onEvent, onReconnect }: StreamHandlers) {
  const [conectado, setConectado] = useState(false);
  const onEventRef = useRef(onEvent);
  const onReconnectRef = useRef(onReconnect);

  onEventRef.current = onEvent;
  onReconnectRef.current = onReconnect;

  useEffect(() => {
    const source = new EventSource(dashboardStreamUrl());

    source.onopen = () => {
      setConectado(true);
      onReconnectRef.current?.();
    };

    source.onerror = () => {
      setConectado(false);
    };

    const listeners = EVENTOS.map((eventName) => {
      const listener = (message: MessageEvent<string>) => {
        onEventRef.current(JSON.parse(message.data) as DashboardEvent);
      };

      source.addEventListener(eventName, listener);
      return { eventName, listener };
    });

    return () => {
      for (const { eventName, listener } of listeners) {
        source.removeEventListener(eventName, listener);
      }

      source.close();
    };
  }, []);

  return { conectado };
}
