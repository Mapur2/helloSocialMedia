package com.recommendationservice.service;

import com.recommendationservice.config.RabbitMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RecommendationProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RabbitMqConfig rabbitMqConfig;



    public void sendRecommendationJob(String userId) {

        rabbitTemplate.convertAndSend(
                rabbitMqConfig.mediaExchange,
                rabbitMqConfig.mediaRoutingKey,
                userId
        );
    }
}