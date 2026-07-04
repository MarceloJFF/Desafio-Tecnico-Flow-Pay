package com.desafio.FlowPay.dto.out;

import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Assunto disponivel para abertura de atendimento")
public record AssuntoResponse(
		@Schema(description = "Identificador do assunto")
		UUID id,
		@Schema(description = "Nome exibido para selecao")
		String nome,
		@Schema(description = "Time responsavel por esse assunto")
		TimeAtendimento time) {
}
