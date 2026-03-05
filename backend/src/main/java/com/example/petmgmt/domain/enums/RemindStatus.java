package com.example.petmgmt.domain.enums;

import lombok.Getter;

@Getter
public enum RemindStatus {
    PENDING("PENDING"),
    SENT("SENT"),
    DISABLED("DISABLED");

    private final String value;

    RemindStatus(String value) {
        this.value = value;
    }
}
