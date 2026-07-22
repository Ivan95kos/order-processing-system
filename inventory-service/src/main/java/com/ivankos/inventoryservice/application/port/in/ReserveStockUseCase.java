package com.ivankos.inventoryservice.application.port.in;

import com.ivankos.inventoryservice.application.port.in.dto.ReserveOrderRequest;

import java.util.UUID;

public interface ReserveStockUseCase {

    void reserve(ReserveOrderRequest reserveOrderRequest);
}
