package com.desafio.FlowPay.dto.out;

import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

public record AssuntoResponse(
		UUID id,
		String nome,
		TimeAtendimento time) {
}
