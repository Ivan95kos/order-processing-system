package com.ivankos.orderservice.mapper;

import com.ivankos.orderservice.dto.OrderItemRequest;
import com.ivankos.orderservice.dto.OrderResponse;
import com.ivankos.orderservice.event.producer.OrderCreatedEvent;
import com.ivankos.orderservice.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "orderId", expression = "java(order.getId())")
    @Mapping(target = "eventType", constant = "CREATED")
    OrderCreatedEvent toCreatedEvent(
            UUID eventId,
            Instant occurredAt,
            Order order,
            List<OrderItemRequest> items);
}
