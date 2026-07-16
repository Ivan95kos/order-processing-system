package com.ivankos.orderservice.service;

import com.ivankos.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public void markPaid(UUID orderId) {
        var order = orderRepository.getOrderOrThrow(orderId);
        order.markPaid();

        log.info("Marked order {} as paid", orderId);
    }

    @Transactional
    public void cancel(UUID orderId) {
        var order = orderRepository.getOrderOrThrow(orderId);
        order.cancel();

        log.info("Cancelled order {}", orderId);
    }
}
