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
            KafkaProperties kafkaProperties,
            ConsumerFactory<String, OrderCreatedEvent> orderCreatedEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderCreatedEventConsumerFactory);

        // Hand-rolled factory: it does NOT inherit spring.kafka.listener.* the way the autoconfigured one
        // does. Only auto-startup is wired through (tests need the listener off without a broker);
        // concurrency, ack-mode, poll-timeout and the CommonErrorHandler are NOT applied and must be set
        // here by hand if ever needed.
        //
        // The systemic alternative is ConcurrentKafkaListenerContainerFactoryConfigurer, which applies the
        // whole spring.kafka.listener.* group - rejected for now because its configure() is declared on
        // <Object, Object> and needs an unchecked cast to fit a typed factory. Revisit at the retry/DLQ
        // step, where the listener config stops being a single property.
        factory.setAutoStartup(kafkaProperties.getListener().isAutoStartup());

        return factory;
    }
}
