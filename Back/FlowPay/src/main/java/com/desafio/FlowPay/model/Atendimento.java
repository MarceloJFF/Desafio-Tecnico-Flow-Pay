package com.desafio.FlowPay.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "atendimentos")
public class Atendimento {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "assunto_id", nullable = false)
	private Assunto assunto;

	@Column(length = 500)
	private String observacao;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TimeAtendimento time;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id")
	private Atendente atendente;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StatusAtendimento status;

	@Column(name = "criado_em", nullable = false)
	private LocalDateTime criadoEm;

	@Column(name = "atribuido_em")
	private LocalDateTime atribuidoEm;

	@Column(name = "finalizado_em")
	private LocalDateTime finalizadoEm;

	@PrePersist
	void prePersist() {
		if (criadoEm == null) {
			criadoEm = LocalDateTime.now();
		}

		if (status == null) {
			status = StatusAtendimento.AGUARDANDO;
		}
	}
}
