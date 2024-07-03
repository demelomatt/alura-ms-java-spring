package br.com.alurafood.pedidos.controller;

import br.com.alurafood.pedidos.dto.PagamentoDto;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentListener {

    @RabbitListener(queues = "payment.details-order")
    public void onEvent(PagamentoDto message) {
        String msg = """
                Dados do pagamento: %s
                Número do pedido: %s
                Valor R$: %s
                Status: %s
                """.formatted(message.getId(),
                message.getPedidoId(),
                message.getValor(),
                message.getStatus());
        System.out.printf("Message received: %s\n", msg);
    }
}
