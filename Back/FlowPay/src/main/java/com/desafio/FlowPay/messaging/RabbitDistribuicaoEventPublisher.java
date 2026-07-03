package com.desafio.FlowPay.messaging;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.desafio.FlowPay.config.RabbitMqConfig;
import com.desafio.FlowPay.model.TimeAtendimento;

@Component
@ConditionalOnProperty(name = "flowpay.rabbit.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitDistribuicaoEventPublisher implements DistribuicaoEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public RabbitDistribuicaoEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void atendimentoCriado(UUID atendimentoId, TimeAtendimento time) {
		publicarAposCommit(new DistribuicaoEvento(atendimentoId, time, TipoDistribuicaoEvento.ATENDIMENTO_CRIADO));
	}

	@Override
	public void vagaLiberada(TimeAtendimento time) {
		publicarAposCommit(new DistribuicaoEvento(null, time, TipoDistribuicaoEvento.VAGA_LIBERADA));
	}

	private void publicarAposCommit(DistribuicaoEvento evento) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publicar(evento);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publicar(evento);
			}
		});
	}

	private void publicar(DistribuicaoEvento evento) {
		rabbitTemplate.convertAndSend(
				RabbitMqConfig.DISTRIBUICAO_EXCHANGE,
				RabbitMqConfig.DISTRIBUICAO_ROUTING_KEY,
				evento);
	}
}
