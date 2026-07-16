package com.ivankos.orderservice.config;

import com.ivankos.orderservice.event.consumer.PaymentEvent;
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
    public JacksonJsonDeserializer<PaymentEvent> paymentEventDeserializer(JsonMapper kafkaObjectMapper) {
        JacksonJsonDeserializer<PaymentEvent> deserializer =
                new JacksonJsonDeserializer<>(PaymentEvent.class, kafkaObjectMapper);
        deserializer.setUseTypeHeaders(false);

        return deserializer;
    }

    @Bean
    public ConsumerFactory<String, PaymentEvent> paymentEventConsumerFactory(
            KafkaProperties kafkaProperties,
            JacksonJsonDeserializer<PaymentEvent> paymentEventDeserializer) {

        return new DefaultKafkaConsumerFactory<>(
                kafkaProperties.buildConsumerProperties(),
                new StringDeserializer(),
                paymentEventDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> paymentEventKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentEvent> paymentEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, PaymentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentEventConsumerFactory);

        return factory;
    }
}
