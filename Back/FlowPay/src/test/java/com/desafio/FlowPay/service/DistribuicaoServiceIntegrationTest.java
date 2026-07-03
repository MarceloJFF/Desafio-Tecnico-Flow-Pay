package com.desafio.FlowPay.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendenteRepository;
import com.desafio.FlowPay.repository.AtendimentoRepository;
import com.desafio.FlowPay.repository.AssuntoRepository;

@SpringBootTest
class DistribuicaoServiceIntegrationTest {

	private static final UUID ASSUNTO_CARTOES_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

	private final DistribuicaoService distribuicaoService;
	private final AtendimentoRepository atendimentoRepository;
	private final AtendenteRepository atendenteRepository;
	private final AssuntoRepository assuntoRepository;

	@Autowired
	DistribuicaoServiceIntegrationTest(
			DistribuicaoService distribuicaoService,
			AtendimentoRepository atendimentoRepository,
			AtendenteRepository atendenteRepository,
			AssuntoRepository assuntoRepository) {
		this.distribuicaoService = distribuicaoService;
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteRepository = atendenteRepository;
		this.assuntoRepository = assuntoRepository;
	}

	@BeforeEach
	void resetDatabase() {
		atendimentoRepository.deleteAll();
		atendenteRepository.resetarAtendimentosAtivos();
	}

	@Test
	void criarAtendimentoAtribuiParaAtendenteDisponivel() {
		Atendimento atendimento = distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));

		assertThat(atendimento.getTime()).isEqualTo(TimeAtendimento.CARTOES);
		assertThat(atendimento.getStatus()).isEqualTo(StatusAtendimento.EM_ATENDIMENTO);
		assertThat(atendimento.getAtendente()).isNotNull();
		assertThat(atendimento.getAtribuidoEm()).isNotNull();
		assertThat(atendimento.getObservacao()).isNull();
		assertThat(atendimento.getAssunto().getId()).isEqualTo(ASSUNTO_CARTOES_ID);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(1);
	}

	@Test
	void criarAtendimentoMantemFilaQuandoTimeEstaLotado() {
		for (int i = 0; i < 10; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		}

		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(9);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(1);
		assertThat(atendenteRepository.findAll())
				.filteredOn(atendente -> atendente.getTime() == TimeAtendimento.CARTOES)
				.allSatisfy(atendente -> assertThat(atendente.getAtendimentosAtivos()).isBetween(0, 3));
	}

	@Test
	void finalizarAtendimentoPuxaProximoDaFilaDoMesmoTime() {
		for (int i = 0; i < 10; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		}

		Atendimento emAtendimento = atendimentoRepository
				.findByStatusAndTimeOrderByCriadoEmAsc(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES)
				.getFirst();
		Atendimento aguardando = atendimentoRepository
				.findByStatusAndTimeOrderByCriadoEmAsc(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES)
				.getFirst();
		UUID atendenteLiberadoId = emAtendimento.getAtendente().getId();
		UUID aguardandoId = aguardando.getId();

		Atendimento finalizado = distribuicaoService.finalizar(emAtendimento.getId());

		Atendimento reatribuido = atendimentoRepository.findById(aguardandoId).orElseThrow();
		assertThat(finalizado.getStatus()).isEqualTo(StatusAtendimento.FINALIZADO);
		assertThat(finalizado.getFinalizadoEm()).isNotNull();
		assertThat(reatribuido.getStatus()).isEqualTo(StatusAtendimento.EM_ATENDIMENTO);
		assertThat(reatribuido.getAtendente().getId()).isEqualTo(atendenteLiberadoId);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isZero();
	}

	@Test
	void finalizarAtendimentoInexistenteRetornaErro() {
		assertThatThrownBy(() -> distribuicaoService.finalizar(UUID.randomUUID()))
				.isInstanceOf(AtendimentoNaoEncontradoException.class);
	}

	@Test
	void finalizarAtendimentoJaFinalizadoRetornaConflito() {
		Atendimento atendimento = distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		distribuicaoService.finalizar(atendimento.getId());

		assertThatThrownBy(() -> distribuicaoService.finalizar(atendimento.getId()))
				.isInstanceOf(AtendimentoConflitoException.class);
	}

	@Test
	void criacoesConcorrentesNaoUltrapassamLimiteDeTresPorAtendente() throws Exception {
		int totalAtendimentos = 30;
		int totalThreads = 12;
		ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < totalAtendimentos; i++) {
			futures.add(executor.submit(() -> {
				start.await(10, TimeUnit.SECONDS);
				distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
				return null;
			}));
		}

		start.countDown();
		for (Future<?> future : futures) {
			future.get(30, TimeUnit.SECONDS);
		}
		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		List<Atendente> atendentesCartoes = atendenteRepository.findAll().stream()
				.filter(atendente -> atendente.getTime() == TimeAtendimento.CARTOES)
				.toList();
		int ativosDosAtendentes = atendentesCartoes.stream()
				.mapToInt(Atendente::getAtendimentosAtivos)
				.sum();

		assertThat(atendentesCartoes).allSatisfy(atendente -> assertThat(atendente.getAtendimentosAtivos()).isBetween(0, 3));
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(9);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(totalAtendimentos - 9);
		assertThat(ativosDosAtendentes).isEqualTo(9);
	}
}
