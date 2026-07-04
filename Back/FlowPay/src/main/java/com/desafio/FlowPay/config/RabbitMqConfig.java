package com.desafio.FlowPay.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "flowpay.rabbit.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

	public static final String DISTRIBUICAO_EXCHANGE = "flowpay.distribuicao.exchange";
	public static final String DISTRIBUICAO_QUEUE = "flowpay.distribuicao.queue";
	public static final String DISTRIBUICAO_ROUTING_KEY = "flowpay.distribuicao";

	@Bean
	DirectExchange distribuicaoExchange() {
		return new DirectExchange(DISTRIBUICAO_EXCHANGE, true, false);
	}

	@Bean
	Queue distribuicaoQueue() {
		return new Queue(DISTRIBUICAO_QUEUE, true);
	}

	@Bean
	Binding distribuicaoBinding(Queue distribuicaoQueue, DirectExchange distribuicaoExchange) {
		return BindingBuilder.bind(distribuicaoQueue)
				.to(distribuicaoExchange)
				.with(DISTRIBUICAO_ROUTING_KEY);
	}

	@Bean
	MessageConverter jsonMessageConverter() {
		return new JacksonJsonMessageConverter("com.desafio.FlowPay.messaging");
	}
}
