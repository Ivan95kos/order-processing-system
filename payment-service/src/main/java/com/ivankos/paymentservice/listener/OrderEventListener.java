package com.ivankos.paymentservice.listener;

import com.ivankos.paymentservice.event.consumer.OrderCreatedEvent;
import com.ivankos.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${app.kafka.topic.order-events}",
            containerFactory = "orderCreatedEventKafkaListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received: {}", event);
        paymentService.processOrderCreated(event);
    }
}
