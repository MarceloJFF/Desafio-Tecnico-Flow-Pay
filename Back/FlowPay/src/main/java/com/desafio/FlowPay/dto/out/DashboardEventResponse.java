package com.desafio.FlowPay.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento incremental enviado pelo stream SSE do dashboard")
public record DashboardEventResponse(
		@Schema(description = "Tipo do evento", example = "atendimento-criado")
		String tipo,
		@Schema(description = "Identificador do atendimento relacionado")
		UUID atendimentoId,
		@Schema(description = "Time afetado pelo evento")
		TimeAtendimento time,
		@Schema(description = "Identificador do atendente relacionado, quando houver")
		UUID atendenteId,
		@Schema(description = "Momento em que o evento foi publicado")
		LocalDateTime timestamp) {
}
