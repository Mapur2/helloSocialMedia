package com.postservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMqConfig {

    @Value("${rabbitmq.queues.comment}")
    private String commentQueue;
    @Value("${rabbitmq.exchanges.comment}")
    private String commentExchange;
    @Value("${rabbitmq.routing.comment}")
    private String commentRoutingKey;

    @Value("${rabbitmq.queues.like}")
    private String likeQueue;
    @Value("${rabbitmq.exchanges.like}")
    private String likeExchange;
    @Value("${rabbitmq.routing.like}")
    private String likeRoutingKey;

    // --- COMMENT QUEUE ---
    @Bean
    public Queue commentActivityQueue() {
        return new Queue(commentQueue, true);
    }

    @Bean
    public DirectExchange commentActivityExchange() {
        return new DirectExchange(commentExchange);
    }

    @Bean
    public Binding commentBinding(Queue commentActivityQueue, DirectExchange commentActivityExchange) {
        return BindingBuilder.bind(commentActivityQueue)
                .to(commentActivityExchange)
                .with(commentRoutingKey);
    }

    // --- LIKE QUEUE ---
    @Bean
    public Queue likeActivityQueue() {
        return new Queue(likeQueue, true);
    }

    @Bean
    public DirectExchange likeActivityExchange() {
        return new DirectExchange(likeExchange);
    }

    @Bean
    public Binding likeBinding(Queue likeActivityQueue, DirectExchange likeActivityExchange) {
        return BindingBuilder.bind(likeActivityQueue)
                .to(likeActivityExchange)
                .with(likeRoutingKey);
    }

    // --- JSON Converter ---
    @Bean
    public MessageConverter jsonConvertor() {
        return new Jackson2JsonMessageConverter();
    }
}
