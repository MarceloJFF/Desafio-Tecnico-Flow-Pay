# FlowPay Frontend

Frontend React da Fase 5, criado com Vite e TypeScript.

## Rodar localmente

```bash
npm install
npm run dev
```

A aplicacao abre em `http://localhost:5173` e usa proxy Vite para o backend em `http://localhost:8080`.

## Scripts

- `npm run dev`: inicia o servidor Vite.
- `npm run build`: valida TypeScript e gera `dist/`.
- `npm run preview`: serve o build localmente.

## Funcionalidades

- Snapshot inicial em `GET /api/dashboard/resumo`.
- Stream SSE em `GET /api/dashboard/stream`.
- Cards por time com fila e atendimentos ativos.
- Ocupacao dos atendentes.
- Fila FIFO com tempo de espera atualizado no client.
- Criacao de atendimento via `POST /api/atendimentos`.
- Listagem e finalizacao de atendimentos via `PATCH /api/atendimentos/{id}/finalizar`.
