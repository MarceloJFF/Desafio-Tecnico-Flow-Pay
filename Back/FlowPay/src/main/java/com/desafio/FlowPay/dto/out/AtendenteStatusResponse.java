package com.desafio.FlowPay.dto.out;

import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status operacional de um atendente")
public record AtendenteStatusResponse(
		@Schema(description = "Identificador do atendente")
		UUID id,
		@Schema(description = "Nome do atendente")
		String nome,
		@Schema(description = "Time do atendente")
		TimeAtendimento time,
		@Schema(description = "Quantidade atual de atendimentos ativos")
		Integer atendimentosAtivos) {
}
