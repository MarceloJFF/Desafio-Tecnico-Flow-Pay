package com.desafio.FlowPay.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.desafio.FlowPay.dto.out.DashboardEventResponse;
import com.desafio.FlowPay.model.TimeAtendimento;

@Component
public class DashboardEventPublisher {

	private final ApplicationEventPublisher applicationEventPublisher;

	public DashboardEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void atendimentoCriado(UUID atendimentoId, TimeAtendimento time) {
		publicarAposCommit(new DashboardEventResponse(
				"atendimento-criado",
				atendimentoId,
				time,
				null,
				LocalDateTime.now()));
	}

	public void atendimentoAtribuido(UUID atendimentoId, TimeAtendimento time, UUID atendenteId) {
		publicarAposCommit(new DashboardEventResponse(
				"atendimento-atribuido",
				atendimentoId,
				time,
				atendenteId,
				LocalDateTime.now()));
	}

	public void atendimentoFinalizado(UUID atendimentoId, TimeAtendimento time, UUID atendenteId) {
		publicarAposCommit(new DashboardEventResponse(
				"atendimento-finalizado",
				atendimentoId,
				time,
				atendenteId,
				LocalDateTime.now()));
	}

	private void publicarAposCommit(DashboardEventResponse evento) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			applicationEventPublisher.publishEvent(evento);
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				applicationEventPublisher.publishEvent(evento);
			}
		});
	}
}
