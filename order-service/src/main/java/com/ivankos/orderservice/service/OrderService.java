package com.ivankos.orderservice.service;

import com.ivankos.orderservice.dto.CreateOrderRequest;
import com.ivankos.orderservice.dto.OrderResponse;
import com.ivankos.orderservice.event.OrderEvent;
import com.ivankos.orderservice.exception.OrderNotFoundException;
import com.ivankos.orderservice.mapper.OrderMapper;
import com.ivankos.orderservice.model.Order;
import com.ivankos.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    @Value("${app.kafka.topic.order-events}")
    private final String orderEventsTopic;

    private final KafkaTemplate<String, OrderEvent> orderEventTemplate;

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        var order = Order.create(
                createOrderRequest.customerId(),
                BigDecimal.ZERO); // TODO: real pricing from Inventory (step 3)

        var savedOrder = orderRepository.save(order);

        var orderCreatedEvent = orderMapper.toCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                savedOrder,
                createOrderRequest.items()
        );

        orderEventTemplate.send(orderEventsTopic, savedOrder.getId().toString(), orderCreatedEvent);

        return orderMapper.toResponse(savedOrder);
    }

    public OrderResponse getOrder(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found by id " + orderId));

        return orderMapper.toResponse(order);
    }
}
