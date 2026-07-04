# Product.md — Funcionalidades Principais (FlowPay)

Resumo funcional do produto, pensado para leitura rápida (PM/gestor).

---

## Visão Geral

Sistema que recebe solicitações de atendimento, distribui automaticamente para o time e atendente corretos, e expõe métricas para gestores acompanharem a operação. Atualização em tempo real via SSE está planejada para a próxima fase.

---

## Funcionalidades

### 1. Abertura de Atendimento
- Cliente (ou sistema externo) cria um atendimento informando o **assunto** (selecionado de uma lista pré-cadastrada) e, opcionalmente, uma **observação**.
- O sistema identifica o time responsável a partir do assunto cadastrado:
  - "Problemas com cartão" → **Time Cartões**
  - "Contratação de empréstimo" → **Time Empréstimos**
  - "Outros" → **Time Outros Assuntos**
- Assuntos são cadastrados no banco (seed), sem necessidade de classificação por texto/regex.

### 2. Distribuição Automática
- O sistema cria o atendimento em fila e publica um evento RabbitMQ para processamento assíncrono.
- O worker tenta atribuir o atendimento a um atendente disponível do time correspondente.
- Um atendente é considerado disponível se tiver **menos de 3 atendimentos ativos**.
- Se nenhum atendente estiver disponível, o atendimento entra automaticamente em **fila de espera** do time.

### 3. Fila de Espera Inteligente
- Atendimentos em fila são atendidos em ordem de chegada (FIFO) por time.
- Assim que um atendente finaliza um atendimento e libera vaga, o próximo da fila do mesmo time é atribuído automaticamente — sem intervenção manual.
- Um scheduler de segurança reprocessa filas periodicamente para cobrir mensagens perdidas ou restart do worker.

### 4. Finalização de Atendimento
- Atendente (ou sistema) marca um atendimento como finalizado.
- Isso libera a vaga do atendente e dispara a tentativa de puxar o próximo da fila.
- Essa tentativa acontece via mensagem `VAGA_LIBERADA` consumida pelo worker.

### 5. Dashboard de Monitoramento
- Backend já expõe um snapshot REST para gestores acompanharem:
  - Quantidade de atendimentos em andamento por time.
  - Quantidade de atendimentos em fila por time.
  - Ocupação de cada atendente (0 a 3).
  - Tempo médio de espera na fila.
  - Total de atendimentos finalizados no dia.
- Atualização em tempo real via SSE está planejada para a próxima fase.

### 6. API REST
- Endpoints para criar, listar e finalizar atendimentos.
- Endpoint de métricas agregadas para o dashboard.
- Swagger/OpenAPI para testar a API pela web.

---

## Fora do Escopo (nesta versão)

- Login/autenticação de atendentes e gestores.
- Reatribuição manual de atendimento pelo gestor.
- Histórico de longo prazo e exportação de relatórios.
- Múltiplas instâncias do backend rodando em paralelo.

---

## Valor para o Negócio

- Reduz tempo ocioso de atendentes (fila sempre distribuída assim que há vaga).
- Dá visibilidade imediata a gestores sobre gargalos por time.
- Base extensível: a regra de distribuição e o modelo de dados já suportam evoluções futuras (priorização por SLA, reatribuição manual, múltiplos canais).
