package com.ivankos.orderservice.listener;

import com.ivankos.orderservice.event.consumer.PaymentEvent;
import com.ivankos.orderservice.service.OrderPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderPaymentService orderPaymentService;

    @KafkaListener(
            topics = "${app.kafka.topic.payment-events}",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    public void onPaymentResult(PaymentEvent event) {
        log.info("Received: {}", event);

        switch (event.paymentStatus()) {
            case "PAYMENT_COMPLETED" -> orderPaymentService.markPaid(event.orderId());
            case "PAYMENT_FAILED" -> orderPaymentService.cancel(event.orderId());
            default -> log.warn(
                    "Unknown payment status '{}' for order {}, ignoring", event.paymentStatus(), event.orderId());
        }
    }
}
