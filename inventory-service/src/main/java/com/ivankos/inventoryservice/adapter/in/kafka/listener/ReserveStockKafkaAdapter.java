package com.ivankos.inventoryservice.adapter.in.kafka.listener;

import com.ivankos.inventoryservice.adapter.in.kafka.event.OrderCreatedEvent;
import com.ivankos.inventoryservice.adapter.in.kafka.mapper.OrderCreatedMapper;
import com.ivankos.inventoryservice.application.port.in.ReserveStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveStockKafkaAdapter {

    private final ReserveStockUseCase reserveStockUseCase;
    private final OrderCreatedMapper orderCreatedMapper;

    @KafkaListener(
            topics = "${app.kafka.topic.order-events}",
            containerFactory = "orderCreatedEventKafkaListenerContainerFactory"
    )
    public void onOrderCreated(OrderCreatedEvent orderCreatedEvent) {
        log.info("Received: {}", orderCreatedEvent);
        reserveStockUseCase.reserve(orderCreatedMapper.toRequest(orderCreatedEvent));
    }

}
