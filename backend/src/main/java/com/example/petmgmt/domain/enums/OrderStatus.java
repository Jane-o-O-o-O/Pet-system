package com.example.petmgmt.domain.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    CREATED("CREATED"),
    CONFIRMED("CONFIRMED"),
    BOARDING("BOARDING"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }
}
