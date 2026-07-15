package com.ivankos.paymentservice.listener;

import com.ivankos.paymentservice.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @KafkaListener(
            topics = "${app.kafka.topic.order-events}",
            containerFactory = "orderCreatedEventKafkaListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received: {}", event);
    }
}
