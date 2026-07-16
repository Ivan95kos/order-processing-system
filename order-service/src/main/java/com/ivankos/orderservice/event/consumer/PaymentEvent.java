package com.ivankos.orderservice.event.consumer;

import java.util.UUID;

public record PaymentEvent(UUID orderId, String paymentStatus) {
}
