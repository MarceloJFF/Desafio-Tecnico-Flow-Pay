package com.desafio.FlowPay.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um atendimento")
public record AtendimentoResponse(
		@Schema(description = "Identificador do atendimento")
		UUID id,
		@Schema(description = "Identificador do assunto selecionado")
		UUID assuntoId,
		@Schema(description = "Nome do assunto selecionado")
		String assuntoNome,
		@Schema(description = "Time responsavel pelo atendimento")
		TimeAtendimento time,
		@Schema(description = "Observacao livre informada na abertura")
		String observacao,
		@Schema(description = "Status atual do atendimento")
		StatusAtendimento status,
		@Schema(description = "Identificador do atendente atribuido, nulo enquanto aguardando")
		UUID atendenteId,
		@Schema(description = "Data/hora de criacao")
		LocalDateTime criadoEm,
		@Schema(description = "Data/hora de atribuicao ao atendente")
		LocalDateTime atribuidoEm,
		@Schema(description = "Data/hora de finalizacao")
		LocalDateTime finalizadoEm) {
}
