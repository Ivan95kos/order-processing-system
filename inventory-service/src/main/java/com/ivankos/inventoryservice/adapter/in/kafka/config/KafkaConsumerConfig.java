package com.ivankos.inventoryservice.adapter.in.kafka.config;

import com.ivankos.inventoryservice.adapter.in.kafka.event.OrderCreatedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public JsonMapper kafkaObjectMapper() {
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Bean
    public JacksonJsonDeserializer<OrderCreatedEvent> orderCreatedEventDeserializer(JsonMapper kafkaObjectMapper) {
        JacksonJsonDeserializer<OrderCreatedEvent> deserializer =
                new JacksonJsonDeserializer<>(OrderCreatedEvent.class, kafkaObjectMapper);
        deserializer.setUseTypeHeaders(false);

        return deserializer;
    }

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory(
            KafkaProperties kafkaProperties,
            JacksonJsonDeserializer<OrderCreatedEvent> orderCreatedEventDeserializer) {

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                orderCreatedEventDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedEventConsumerFactory);

        return factory;
    }
}
