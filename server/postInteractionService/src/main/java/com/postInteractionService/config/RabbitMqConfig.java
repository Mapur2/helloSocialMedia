package com.postInteractionService.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // ---------- COMMENT QUEUE CONFIG ----------
    @Value("${rabbitmq.queues.comment}")
    private String commentQueue;

    @Value("${rabbitmq.exchanges.comment}")
    private String commentExchange;

    @Value("${rabbitmq.routing.comment}")
    private String commentRoutingKey;

    // ---------- LIKE QUEUE CONFIG ----------
    @Value("${rabbitmq.queues.like}")
    private String likeQueue;

    @Value("${rabbitmq.exchanges.like}")
    private String likeExchange;

    @Value("${rabbitmq.routing.like}")
    private String likeRoutingKey;


    // --- COMMENT QUEUE SETUP ---
    @Bean
    public Queue commentQueue() {
        return new Queue(commentQueue, true);
    }

    @Bean
    public DirectExchange commentExchange() {
        return new DirectExchange(commentExchange);
    }

    @Bean
    public Binding commentBinding(Queue commentQueue, DirectExchange commentExchange) {
        return BindingBuilder.bind(commentQueue).to(commentExchange).with(commentRoutingKey);
    }

    // --- LIKE QUEUE SETUP ---
    @Bean
    public Queue likeQueue() {
        return new Queue(likeQueue, true);
    }

    @Bean
    public DirectExchange likeExchange() {
        return new DirectExchange(likeExchange);
    }

    @Bean
    public Binding likeBinding(Queue likeQueue, DirectExchange likeExchange) {
        return BindingBuilder.bind(likeQueue).to(likeExchange).with(likeRoutingKey);
    }

    // --- JSON Message Converter ---
    @Bean
    public MessageConverter jsonConvertor() {
        return new Jackson2JsonMessageConverter();
    }
}
