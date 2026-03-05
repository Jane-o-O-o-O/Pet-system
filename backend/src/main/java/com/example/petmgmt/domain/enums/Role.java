package com.example.petmgmt.domain.enums;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ADMIN"),
    STAFF("STAFF"),
    OWNER("OWNER");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}
