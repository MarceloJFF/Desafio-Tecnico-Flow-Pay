package com.desafio.FlowPay.messaging;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.desafio.FlowPay.model.TimeAtendimento;

@Component
@ConditionalOnProperty(name = "flowpay.rabbit.enabled", havingValue = "false")
public class NoopDistribuicaoEventPublisher implements DistribuicaoEventPublisher {

	@Override
	public void atendimentoCriado(UUID atendimentoId, TimeAtendimento time) {
	}

	@Override
	public void vagaLiberada(TimeAtendimento time) {
	}
}
