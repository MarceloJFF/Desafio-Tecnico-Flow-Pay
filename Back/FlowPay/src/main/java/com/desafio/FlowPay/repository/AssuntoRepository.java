package com.desafio.FlowPay.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.desafio.FlowPay.model.Assunto;

public interface AssuntoRepository extends JpaRepository<Assunto, UUID> {
}
