package com.desafio.FlowPay.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.service.DistribuicaoProcessor;

@Component
@ConditionalOnProperty(name = "flowpay.distribuicao.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class DistribuicaoScheduler {

	private final DistribuicaoProcessor distribuicaoProcessor;

	public DistribuicaoScheduler(DistribuicaoProcessor distribuicaoProcessor) {
		this.distribuicaoProcessor = distribuicaoProcessor;
	}

	@Scheduled(fixedDelayString = "${flowpay.distribuicao.scheduler-delay-ms:2000}")
	public void reprocessarFilas() {
		for (TimeAtendimento time : TimeAtendimento.values()) {
			distribuicaoProcessor.tentarAtribuirProximo(time);
		}
	}
}
