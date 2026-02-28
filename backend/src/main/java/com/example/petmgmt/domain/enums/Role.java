package com.example.petmgmt.domain.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ADMIN"),
    STAFF("STAFF"),
    OWNER("OWNER");

    @EnumValue
    private final String value;

    Role(String value) {
        this.value = value;
    }
}
