package com.ivankos.orderservice.model;

import com.ivankos.orderservice.exception.InvalidOrderStateTransitionException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "orders")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    public static Order create(UUID customerId, BigDecimal totalAmount) {
        return Order.builder()
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();
    }

    public void markPaid() {
        switch (status) {
            case PENDING -> this.status = OrderStatus.PAID;
            case PAID -> { /* idempotent no-op */ }
            default -> throw new InvalidOrderStateTransitionException(
                    "Cannot mark order as PAID from status " + status);
        }
    }

    public void cancel() {
        switch (status) {
            case PENDING -> this.status = OrderStatus.CANCELLED;
            case CANCELLED -> { /* idempotent no-op */ }
            default -> throw new InvalidOrderStateTransitionException(
                    "Cannot cancel order from status " + status);
        }
    }
}
