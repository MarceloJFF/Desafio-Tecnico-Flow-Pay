package com.desafio.FlowPay.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;

public record AtendimentoResponse(
		UUID id,
		UUID assuntoId,
		String assuntoNome,
		TimeAtendimento time,
		String observacao,
		StatusAtendimento status,
		UUID atendenteId,
		LocalDateTime criadoEm,
		LocalDateTime atribuidoEm,
		LocalDateTime finalizadoEm) {
}
