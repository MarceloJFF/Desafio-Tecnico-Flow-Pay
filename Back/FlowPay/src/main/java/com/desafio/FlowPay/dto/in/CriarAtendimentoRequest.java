package com.desafio.FlowPay.dto.in;

import java.util.UUID;

public record CriarAtendimentoRequest(UUID assuntoId, String observacao) {
}
