package com.desafio.FlowPay.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
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

	public DistribuicaoService(AtendimentoRepository atendimentoRepository, AtendenteRepository atendenteRepository,
			AssuntoRepository assuntoRepository) {
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteRepository = atendenteRepository;
		this.assuntoRepository = assuntoRepository;
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
		preencherVagasDoTime(time);
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

		atribuirProximoDaFilaAoAtendente(atendimento.getTime(), atendente);
		return atendimento;
	}

	private void preencherVagasDoTime(TimeAtendimento time) {
		atendenteRepository.bloquearDistribuicaoDoTime(time.name());

		while (true) {
			Atendente atendente = atendenteRepository.buscarDisponivel(time.name()).orElse(null);
			if (atendente == null) {
				return;
			}

			Atendimento proximo = atendimentoRepository.buscarProximoDaFila(time.name()).orElse(null);
			if (proximo == null) {
				return;
			}

			atribuir(proximo, atendente);
		}
	}

	private void atribuirProximoDaFilaAoAtendente(TimeAtendimento time, Atendente atendente) {
		atendenteRepository.bloquearDistribuicaoDoTime(time.name());

		Atendimento proximo = atendimentoRepository.buscarProximoDaFila(time.name()).orElse(null);
		if (proximo != null && atendente.getAtendimentosAtivos() < 3) {
			atribuir(proximo, atendente);
		}
	}

	private void atribuir(Atendimento atendimento, Atendente atendente) {
		atendimento.setAtendente(atendente);
		atendimento.setStatus(StatusAtendimento.EM_ATENDIMENTO);
		atendimento.setAtribuidoEm(LocalDateTime.now());
		atendente.setAtendimentosAtivos(atendente.getAtendimentosAtivos() + 1);
	}
}
