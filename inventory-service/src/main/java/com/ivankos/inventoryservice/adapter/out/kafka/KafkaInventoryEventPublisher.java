package com.ivankos.inventoryservice.adapter.out.kafka;

import com.ivankos.inventoryservice.application.port.out.InventoryEventPublisher;
import com.ivankos.inventoryservice.application.port.out.event.InventoryEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaInventoryEventPublisher implements InventoryEventPublisher {

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @Value("${app.kafka.topic.inventory-events}")
    private String topic;

    @Override
    public void publish(InventoryEvent event) {
        kafkaTemplate.send(topic, event.orderId().toString(), event);
    }
}
