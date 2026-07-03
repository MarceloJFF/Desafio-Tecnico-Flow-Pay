package com.desafio.FlowPay.messaging;

import java.io.Serializable;
import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

public record DistribuicaoEvento(
		UUID atendimentoId,
		TimeAtendimento time,
		TipoDistribuicaoEvento tipo) implements Serializable {
}
