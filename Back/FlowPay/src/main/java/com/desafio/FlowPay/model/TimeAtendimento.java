package com.desafio.FlowPay.model;

import java.text.Normalizer;
import java.util.Locale;

public enum TimeAtendimento {
	CARTOES,
	EMPRESTIMOS,
	OUTROS;

	public static TimeAtendimento fromAssunto(String assunto) {
		if (assunto == null) {
			return OUTROS;
		}

		String normalizado = Normalizer.normalize(assunto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");

		if (normalizado.contains("cart")) {
			return CARTOES;
		}

		if (normalizado.contains("emprestimo")) {
			return EMPRESTIMOS;
		}

		return OUTROS;
	}
}
