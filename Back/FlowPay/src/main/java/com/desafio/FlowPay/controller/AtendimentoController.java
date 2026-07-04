package com.desafio.FlowPay.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desafio.FlowPay.dto.in.CriarAtendimentoRequest;
import com.desafio.FlowPay.dto.out.AtendimentoResponse;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.service.AtendimentoConsultaService;
import com.desafio.FlowPay.service.DistribuicaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Atendimentos", description = "Abertura, consulta e finalizacao de atendimentos")
@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

	private final DistribuicaoService distribuicaoService;
	private final AtendimentoConsultaService atendimentoConsultaService;

	public AtendimentoController(DistribuicaoService distribuicaoService,
			AtendimentoConsultaService atendimentoConsultaService) {
		this.distribuicaoService = distribuicaoService;
		this.atendimentoConsultaService = atendimentoConsultaService;
	}

	@Operation(summary = "Criar atendimento", description = "Cria atendimento em AGUARDANDO e publica evento RabbitMQ para distribuicao assincrona.")
	@PostMapping
	public ResponseEntity<AtendimentoResponse> criar(@RequestBody CriarAtendimentoRequest request) {
		Atendimento atendimento = distribuicaoService.criar(request);
		return ResponseEntity
				.created(URI.create("/api/atendimentos/" + atendimento.getId()))
				.body(AtendimentoMapper.toResponse(atendimento));
	}

	@Operation(summary = "Listar atendimentos", description = "Lista atendimentos, opcionalmente filtrando por status, atendenteId e/ou time. Inclui atendimentos finalizados.")
	@GetMapping
	public List<AtendimentoResponse> listar(
			@Parameter(description = "Status para filtro: AGUARDANDO, EM_ATENDIMENTO ou FINALIZADO")
			@RequestParam(required = false) StatusAtendimento status,
			@Parameter(description = "Identificador do atendente para listar todos os atendimentos dele, inclusive finalizados")
			@RequestParam(required = false) UUID atendenteId,
			@Parameter(description = "Time para filtro: CARTOES, EMPRESTIMOS ou OUTROS")
			@RequestParam(required = false) TimeAtendimento time) {
		return atendimentoConsultaService.listar(status, atendenteId, time).stream()
				.map(AtendimentoMapper::toResponse)
				.toList();
	}

	@Operation(summary = "Finalizar atendimento", description = "Finaliza um atendimento em andamento e publica evento de vaga liberada.")
	@PatchMapping("/{id}/finalizar")
	public AtendimentoResponse finalizar(@PathVariable UUID id) {
		return AtendimentoMapper.toResponse(distribuicaoService.finalizar(id));
	}
}
