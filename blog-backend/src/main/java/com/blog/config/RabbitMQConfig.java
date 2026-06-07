package com.blog.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.rabbitmq-enabled", havingValue = "true")
public class RabbitMQConfig {

    public static final String EXCHANGE = "blog.exchange";
    public static final String QUEUE = "blog.article.queue";
    public static final String ROUTING_KEY = "blog.article.#";

    @Bean
    public TopicExchange blogExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue articleQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding articleBinding() {
        return BindingBuilder.bind(articleQueue()).to(blogExchange()).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
