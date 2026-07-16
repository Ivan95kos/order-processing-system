package com.ivankos.orderservice.exception;

import lombok.experimental.StandardException;

@StandardException
public class InvalidOrderStateTransitionException extends RuntimeException {
}
