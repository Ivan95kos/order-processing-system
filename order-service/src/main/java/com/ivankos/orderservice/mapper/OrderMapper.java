package com.ivankos.orderservice.mapper;

import com.ivankos.orderservice.dto.OrderResponse;
import com.ivankos.orderservice.model.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);
}
