package com.desafio.FlowPay.controller;

import java.util.UUID;

import com.desafio.FlowPay.dto.out.AtendimentoResponse;
import com.desafio.FlowPay.model.Assunto;
import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.Atendimento;

final class AtendimentoMapper {

	private AtendimentoMapper() {
	}

	static AtendimentoResponse toResponse(Atendimento atendimento) {
		Atendente atendente = atendimento.getAtendente();
		UUID atendenteId = atendente == null ? null : atendente.getId();
		Assunto assunto = atendimento.getAssunto();

		return new AtendimentoResponse(
				atendimento.getId(),
				assunto.getId(),
				assunto.getNome(),
				atendimento.getTime(),
				atendimento.getObservacao(),
				atendimento.getStatus(),
				atendenteId,
				atendimento.getCriadoEm(),
				atendimento.getAtribuidoEm(),
				atendimento.getFinalizadoEm());
	}
}
