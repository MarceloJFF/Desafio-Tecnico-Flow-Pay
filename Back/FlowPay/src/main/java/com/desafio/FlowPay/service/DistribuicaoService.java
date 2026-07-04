package com.desafio.FlowPay.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
import com.desafio.FlowPay.messaging.DistribuicaoEventPublisher;
import com.desafio.FlowPay.model.Assunto;
import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendenteRepository;
import com.desafio.FlowPay.repository.AtendimentoRepository;
import com.desafio.FlowPay.repository.AssuntoRepository;

@Service
public class DistribuicaoService {

	private final AtendimentoRepository atendimentoRepository;
	private final AtendenteRepository atendenteRepository;
	private final AssuntoRepository assuntoRepository;
	private final DistribuicaoEventPublisher distribuicaoEventPublisher;
	private final DashboardEventPublisher dashboardEventPublisher;

	public DistribuicaoService(AtendimentoRepository atendimentoRepository, AtendenteRepository atendenteRepository,
			AssuntoRepository assuntoRepository, DistribuicaoEventPublisher distribuicaoEventPublisher,
			DashboardEventPublisher dashboardEventPublisher) {
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteRepository = atendenteRepository;
		this.assuntoRepository = assuntoRepository;
		this.distribuicaoEventPublisher = distribuicaoEventPublisher;
		this.dashboardEventPublisher = dashboardEventPublisher;
	}

	@Transactional
	public Atendimento criar(CriarAtendimentoRequest request) {
		if (request == null || request.assuntoId() == null) {
			throw new RequisicaoInvalidaException("Campo assuntoId e obrigatorio.");
		}

		Assunto assunto = assuntoRepository.findById(request.assuntoId())
				.orElseThrow(() -> new RequisicaoInvalidaException("Assunto nao encontrado: " + request.assuntoId()));

		TimeAtendimento time = assunto.getTime();
		Atendimento atendimento = new Atendimento();
		atendimento.setAssunto(assunto);
		atendimento.setObservacao(request.observacao());
		atendimento.setTime(time);
		atendimento.setStatus(StatusAtendimento.AGUARDANDO);

		Atendimento salvo = atendimentoRepository.saveAndFlush(atendimento);
		distribuicaoEventPublisher.atendimentoCriado(salvo.getId(), time);
		dashboardEventPublisher.atendimentoCriado(salvo.getId(), time);
		return atendimentoRepository.findById(salvo.getId()).orElseThrow();
	}

	@Transactional
	public Atendimento finalizar(UUID atendimentoId) {
		Atendimento atendimento = atendimentoRepository.buscarPorIdComLock(atendimentoId)
				.orElseThrow(() -> new AtendimentoNaoEncontradoException("Atendimento nao encontrado."));

		if (atendimento.getStatus() == StatusAtendimento.FINALIZADO) {
			throw new AtendimentoConflitoException("Atendimento ja finalizado.");
		}

		if (atendimento.getStatus() != StatusAtendimento.EM_ATENDIMENTO || atendimento.getAtendente() == null) {
			throw new AtendimentoConflitoException("Somente atendimentos em andamento podem ser finalizados.");
		}

		Atendente atendente = atendenteRepository.buscarPorIdComLock(atendimento.getAtendente().getId())
				.orElseThrow(() -> new AtendimentoConflitoException("Atendente do atendimento nao encontrado."));

		atendimento.setStatus(StatusAtendimento.FINALIZADO);
		atendimento.setFinalizadoEm(LocalDateTime.now());
		atendente.setAtendimentosAtivos(Math.max(0, atendente.getAtendimentosAtivos() - 1));

		distribuicaoEventPublisher.vagaLiberada(atendimento.getTime());
		dashboardEventPublisher.atendimentoFinalizado(atendimento.getId(), atendimento.getTime(), atendente.getId());
		return atendimento;
	}
}
