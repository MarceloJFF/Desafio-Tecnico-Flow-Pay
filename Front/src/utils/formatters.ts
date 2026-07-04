import type { TimeAtendimento } from "../types";
import { TIME_LABEL } from "../types";

export function formatDateTime(value?: string | null) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

export function formatDuration(seconds?: number | null) {
  if (seconds === null || seconds === undefined || Number.isNaN(seconds)) {
    return "0s";
  }

  if (seconds < 60) {
    return `${Math.round(seconds)}s`;
  }

  const minutes = Math.floor(seconds / 60);
  const rest = Math.round(seconds % 60);
  return `${minutes}min ${rest}s`;
}

export function secondsSince(date: string) {
  return Math.max(0, Math.floor((Date.now() - new Date(date).getTime()) / 1000));
}

export function timeLabel(time: TimeAtendimento) {
  return TIME_LABEL[time];
}
