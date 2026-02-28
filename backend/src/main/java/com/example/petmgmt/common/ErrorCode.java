package com.example.petmgmt.common;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(0, "OK"),
    BAD_REQUEST(40000, "Bad Request"),
    UNAUTHORIZED(40001, "Unauthorized"),
    FORBIDDEN(40003, "Forbidden"),
    NOT_FOUND(40004, "Not Found"),
    INTERNAL_ERROR(50000, "Internal Server Error"),
    
    USER_NOT_FOUND(40101, "User not found"),
    INVALID_PASSWORD(40102, "Invalid password"),
    USERNAME_EXISTS(40103, "Username already exists"),
    
    PET_NOT_FOUND(40201, "Pet not found"),
    PET_ACCESS_DENIED(40202, "Access to pet denied"),
    
    ORDER_STATUS_INVALID(40301, "Invalid order status transition");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
