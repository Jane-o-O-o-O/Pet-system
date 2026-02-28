package com.example.petmgmt.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum RemindStatus {
    PENDING("PENDING"),
    SENT("SENT"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;

    RemindStatus(String value) {
        this.value = value;
    }
}
