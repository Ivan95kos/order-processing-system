package com.ivankos.inventoryservice.adapter.out.kafka.config;

import com.ivankos.inventoryservice.application.port.out.event.InventoryEvent;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, InventoryEvent> inventoryEventProducerFactory(
            KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, InventoryEvent> inventoryEventKafkaTemplate(
            ProducerFactory<String, InventoryEvent> inventoryEventProducerFactory) {
        return new KafkaTemplate<>(inventoryEventProducerFactory);
    }
}
