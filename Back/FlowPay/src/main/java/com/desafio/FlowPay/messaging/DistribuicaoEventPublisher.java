package com.desafio.FlowPay.messaging;

import java.util.UUID;

import com.desafio.FlowPay.model.TimeAtendimento;

public interface DistribuicaoEventPublisher {

	void atendimentoCriado(UUID atendimentoId, TimeAtendimento time);

	void vagaLiberada(TimeAtendimento time);
}
