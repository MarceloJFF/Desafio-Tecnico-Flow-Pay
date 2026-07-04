package com.desafio.FlowPay.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.desafio.FlowPay.dto.out.DashboardResumoResponse;
import com.desafio.FlowPay.model.Atendimento;
import com.desafio.FlowPay.model.StatusAtendimento;
import com.desafio.FlowPay.model.TimeAtendimento;
import com.desafio.FlowPay.repository.AtendimentoRepository;

@Service
public class DashboardService {

	private final AtendimentoRepository atendimentoRepository;
	private final AtendenteService atendenteService;

	public DashboardService(AtendimentoRepository atendimentoRepository, AtendenteService atendenteService) {
		this.atendimentoRepository = atendimentoRepository;
		this.atendenteService = atendenteService;
	}

	@Transactional(readOnly = true)
	public DashboardResumoResponse resumo() {
		LocalDateTime inicioDoDia = LocalDate.now().atStartOfDay();
		LocalDateTime fimDoDia = inicioDoDia.plusDays(1);

		return new DashboardResumoResponse(
				contarPorTime(StatusAtendimento.AGUARDANDO),
				contarPorTime(StatusAtendimento.EM_ATENDIMENTO),
				atendimentoRepository.countByStatusAndFinalizadoEmBetween(
						StatusAtendimento.FINALIZADO,
						inicioDoDia,
						fimDoDia),
				calcularTempoMedioEsperaSegundos(),
				atendenteService.listar());
	}

	private Map<TimeAtendimento, Long> contarPorTime(StatusAtendimento status) {
		return Arrays.stream(TimeAtendimento.values())
				.collect(Collectors.toMap(
						time -> time,
						time -> atendimentoRepository.countByStatusAndTime(status, time)));
	}

	private Double calcularTempoMedioEsperaSegundos() {
		return atendimentoRepository.findByAtribuidoEmIsNotNull().stream()
				.mapToLong(this::tempoEsperaSegundos)
				.average()
				.stream()
				.boxed()
				.findFirst()
				.orElse(0.0);
	}

	private long tempoEsperaSegundos(Atendimento atendimento) {
		return Duration.between(atendimento.getCriadoEm(), atendimento.getAtribuidoEm()).toSeconds();
	}
}
