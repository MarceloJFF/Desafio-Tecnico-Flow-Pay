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

## Deploy na Vercel

- Configure o projeto com `Root Directory` = `Front`.
- Configure `VITE_API_BASE_URL` com a URL do backend no Render.
- O arquivo `vercel.json` ja define o build Vite e o fallback SPA.

## Funcionalidades

- Snapshot inicial em `GET /api/dashboard/resumo`.
- Stream SSE em `GET /api/dashboard/stream`.
- Cards por time com fila e atendimentos ativos.
- Ocupacao dos atendentes.
- Fila FIFO com tempo de espera atualizado no client.
- Criacao de atendimento via `POST /api/atendimentos`.
- Listagem e finalizacao de atendimentos via `PATCH /api/atendimentos/{id}/finalizar`.
- Guia Reports com graficos CSS.
- Guia Atendentes agrupada por squad.
- Modal de detalhes do atendimento na guia Atendimentos.
