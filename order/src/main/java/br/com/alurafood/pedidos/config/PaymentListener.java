package br.com.alurafood.pedidos.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {

    @RabbitListener(queues = "payment.done")
    public void onEvent(Message message) {
        System.out.printf("Message received: %s", message);
    }
}
