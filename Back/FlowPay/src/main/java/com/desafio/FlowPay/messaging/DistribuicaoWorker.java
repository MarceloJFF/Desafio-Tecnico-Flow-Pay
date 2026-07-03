package com.desafio.FlowPay.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.desafio.FlowPay.config.RabbitMqConfig;
import com.desafio.FlowPay.service.DistribuicaoProcessor;

@Component
@ConditionalOnProperty(name = "flowpay.rabbit.enabled", havingValue = "true", matchIfMissing = true)
public class DistribuicaoWorker {

	private final DistribuicaoProcessor distribuicaoProcessor;

	public DistribuicaoWorker(DistribuicaoProcessor distribuicaoProcessor) {
		this.distribuicaoProcessor = distribuicaoProcessor;
	}

	@RabbitListener(queues = RabbitMqConfig.DISTRIBUICAO_QUEUE)
	public void processar(DistribuicaoEvento evento) {
		distribuicaoProcessor.tentarAtribuirProximo(evento.time());
	}
}
