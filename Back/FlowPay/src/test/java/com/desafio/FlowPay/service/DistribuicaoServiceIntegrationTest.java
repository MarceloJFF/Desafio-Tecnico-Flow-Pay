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
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
import com.desafio.FlowPay.model.Atendente;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendenteRepository;
import com.desafio.FlowPay.repository.AtendimentoRepository;

@SpringBootTest
class DistribuicaoServiceIntegrationTest {

	private static final UUID ASSUNTO_CARTOES_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");
	private static final UUID ASSUNTO_EMPRESTIMOS_ID = UUID.fromString("a0000000-0000-0000-0000-000000000002");
	private static final UUID ASSUNTO_OUTROS_ID = UUID.fromString("a0000000-0000-0000-0000-000000000003");
	private static final int CAPACIDADE_POR_TIME = 3;

	private final DistribuicaoService distribuicaoService;
	private final DistribuicaoProcessor distribuicaoProcessor;
	private final AtendimentoRepository atendimentoRepository;
	private final AtendenteRepository atendenteRepository;

	@Autowired
	DistribuicaoServiceIntegrationTest(
			DistribuicaoService distribuicaoService,
			DistribuicaoProcessor distribuicaoProcessor,
			AtendimentoRepository atendimentoRepository,
			AtendenteRepository atendenteRepository) {
		this.distribuicaoService = distribuicaoService;
		this.distribuicaoProcessor = distribuicaoProcessor;
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteRepository = atendenteRepository;
	}

	@BeforeEach
	void resetDatabase() {
		atendimentoRepository.deleteAll();
		atendenteRepository.resetarAtendimentosAtivos();
	}

	@Test
	void criarAtendimentoMantemAguardandoAteWorkerProcessar() {
		Atendimento criado = distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));

		assertThat(criado.getTime()).isEqualTo(TimeAtendimento.CARTOES);
		assertThat(criado.getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO);
		assertThat(criado.getAtendente()).isNull();
		assertThat(criado.getObservacao()).isNull();
		assertThat(criado.getAssunto().getId()).isEqualTo(ASSUNTO_CARTOES_ID);

		assertThat(distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES)).isTrue();

		Atendimento atribuido = atendimentoRepository.findById(criado.getId()).orElseThrow();
		assertThat(atribuido.getStatus()).isEqualTo(StatusAtendimento.EM_ATENDIMENTO);
		assertThat(atribuido.getAtendente()).isNotNull();
		assertThat(atribuido.getAtribuidoEm()).isNotNull();
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(1);
	}

	@Test
	void workerMantemFilaQuandoTimeEstaLotado() {
		for (int i = 0; i < 10; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		}

		for (int i = 0; i < 10; i++) {
			distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES);
		}

		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(10 - CAPACIDADE_POR_TIME);
		assertThat(atendenteRepository.findAll())
				.filteredOn(atendente -> atendente.getTime() == TimeAtendimento.CARTOES)
				.allSatisfy(atendente -> assertThat(atendente.getAtendimentosAtivos()).isBetween(0, 3));
	}

	@Test
	void finalizarAtendimentoLiberaVagaEWorkerPuxaProximoDaFilaDoMesmoTime() {
		for (int i = 0; i < 10; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		}

		for (int i = 0; i < 10; i++) {
			distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES);
		}

		Atendimento emAtendimento = atendimentoRepository
				.findByStatusAndTimeOrderByCriadoEmAsc(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES)
				.getFirst();
		Atendimento aguardando = atendimentoRepository
				.findByStatusAndTimeOrderByCriadoEmAsc(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES)
				.getFirst();
		UUID aguardandoId = aguardando.getId();

		Atendimento finalizado = distribuicaoService.finalizar(emAtendimento.getId());

		assertThat(finalizado.getStatus()).isEqualTo(StatusAtendimento.FINALIZADO);
		assertThat(finalizado.getFinalizadoEm()).isNotNull();
		assertThat(atendimentoRepository.findById(aguardandoId).orElseThrow().getStatus())
				.isEqualTo(StatusAtendimento.AGUARDANDO);

		assertThat(distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES)).isTrue();

		Atendimento reatribuido = atendimentoRepository.findById(aguardandoId).orElseThrow();
		assertThat(reatribuido.getStatus()).isEqualTo(StatusAtendimento.EM_ATENDIMENTO);
		assertThat(reatribuido.getAtendente()).isNotNull();
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(10 - CAPACIDADE_POR_TIME - 1);
	}

	@Test
	void finalizarAtendimentoInexistenteRetornaErro() {
		assertThatThrownBy(() -> distribuicaoService.finalizar(UUID.randomUUID()))
				.isInstanceOf(AtendimentoNaoEncontradoException.class);
	}

	@Test
	void finalizarAtendimentoJaFinalizadoRetornaConflito() {
		Atendimento atendimento = distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES);
		distribuicaoService.finalizar(atendimento.getId());

		assertThatThrownBy(() -> distribuicaoService.finalizar(atendimento.getId()))
				.isInstanceOf(AtendimentoConflitoException.class);
	}

	@Test
	void workersConcorrentesNaoUltrapassamLimiteDeTresPorAtendente() throws Exception {
		int totalAtendimentos = 30;
		int totalThreads = 12;
		ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < totalAtendimentos; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
		}

		for (int i = 0; i < totalAtendimentos; i++) {
			futures.add(executor.submit(() -> {
				start.await(10, TimeUnit.SECONDS);
				distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES);
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
				.isEqualTo(CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(totalAtendimentos - CAPACIDADE_POR_TIME);
		assertThat(ativosDosAtendentes).isEqualTo(CAPACIDADE_POR_TIME);
	}

	@Test
	@Transactional
	void workersProcessamTimesAlternadosSemMisturarFilas() {
		for (int i = 0; i < 10; i++) {
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_CARTOES_ID, null));
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_EMPRESTIMOS_ID, null));
			distribuicaoService.criar(new CriarAtendimentoRequest(ASSUNTO_OUTROS_ID, null));
		}

		for (int i = 0; i < 10; i++) {
			distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.CARTOES);
			distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.EMPRESTIMOS);
			distribuicaoProcessor.tentarAtribuirProximo(TimeAtendimento.OUTROS);
		}

		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.CARTOES))
				.isEqualTo(CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.EMPRESTIMOS))
				.isEqualTo(CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.EM_ATENDIMENTO, TimeAtendimento.OUTROS))
				.isEqualTo(CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.CARTOES))
				.isEqualTo(10 - CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.EMPRESTIMOS))
				.isEqualTo(10 - CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.countByStatusAndTime(StatusAtendimento.AGUARDANDO, TimeAtendimento.OUTROS))
				.isEqualTo(10 - CAPACIDADE_POR_TIME);
		assertThat(atendimentoRepository.findByStatus(StatusAtendimento.EM_ATENDIMENTO))
				.allSatisfy(atendimento -> assertThat(atendimento.getAtendente().getTime()).isEqualTo(atendimento.getTime()));
	}
}
