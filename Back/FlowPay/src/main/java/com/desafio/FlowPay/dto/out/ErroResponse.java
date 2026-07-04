package com.desafio.FlowPay.dto.out;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padronizada de erro")
public record ErroResponse(
		@Schema(description = "Mensagem de erro")
		String erro) {
}
