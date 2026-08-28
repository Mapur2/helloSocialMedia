package com.recommendationservice.config;

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

    @Value("${rabbitmq.queues.media}")
    public String mediaQueue;
    @Value("${rabbitmq.exchanges.media}")
    public  String mediaExchange;
    @Value("${rabbitmq.routing.media}")
    public  String mediaRoutingKey;



    // --- Media QUEUE ---
    @Bean
    public Queue mediaActivityQueue() {
        return new Queue(mediaQueue, true);
    }

    @Bean
    public DirectExchange mediaActivityExchange() {
        return new DirectExchange(mediaExchange);
    }

    @Bean
    public Binding mediaBinding(Queue mediaActivityQueue, DirectExchange mediaActivityExchange) {
        return BindingBuilder.bind(mediaActivityQueue)
                .to(mediaActivityExchange)
                .with(mediaRoutingKey);
    }

    // --- JSON Converter ---
    @Bean
    public MessageConverter jsonConvertor() {
        return new Jackson2JsonMessageConverter();
    }
}
