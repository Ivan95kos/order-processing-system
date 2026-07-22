package com.ivankos.inventoryservice.adapter.in.kafka.mapper;

import com.ivankos.inventoryservice.adapter.in.kafka.event.OrderCreatedEvent;
import com.ivankos.inventoryservice.application.port.in.dto.ReserveOrderRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderCreatedMapper {

    ReserveOrderRequest toRequest(OrderCreatedEvent orderCreatedEvent);
}
