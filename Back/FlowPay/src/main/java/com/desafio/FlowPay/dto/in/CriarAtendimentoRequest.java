package com.desafio.FlowPay.dto.in;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload para criar atendimento")
public record CriarAtendimentoRequest(
		@Schema(description = "Identificador do assunto selecionado", example = "a0000000-0000-0000-0000-000000000001")
		UUID assuntoId,
		@Schema(description = "Observacao livre sobre a solicitacao", example = "Cliente relata bloqueio do cartao")
		String observacao) {
}
