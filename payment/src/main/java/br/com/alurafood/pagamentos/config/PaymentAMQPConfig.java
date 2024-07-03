package br.com.alurafood.pagamentos.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentAMQPConfig {

    @Bean
    public Queue createQueue() {
        return QueueBuilder.nonDurable("payment.done").build();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory conn) {
        return new RabbitAdmin(conn);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> init(RabbitAdmin admin) {
        return event -> admin.initialize();
    }
}
