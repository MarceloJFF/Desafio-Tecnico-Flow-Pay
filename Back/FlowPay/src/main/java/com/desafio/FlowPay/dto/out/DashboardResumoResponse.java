package com.desafio.FlowPay.dto.out;

import java.util.List;
import java.util.Map;

import com.desafio.FlowPay.model.TimeAtendimento;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo consolidado para o dashboard")
public record DashboardResumoResponse(
		@Schema(description = "Quantidade de atendimentos aguardando por time")
		Map<TimeAtendimento, Long> emFilaPorTime,
		@Schema(description = "Quantidade de atendimentos em andamento por time")
		Map<TimeAtendimento, Long> emAtendimentoPorTime,
		@Schema(description = "Total de atendimentos finalizados no dia atual")
		long finalizadosHoje,
		@Schema(description = "Tempo medio de espera entre criacao e atribuicao, em segundos")
		Double tempoMedioEsperaSegundos,
		@Schema(description = "Status atual dos atendentes")
		List<AtendenteStatusResponse> atendentes) {
}
