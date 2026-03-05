package com.example.petmgmt.domain.entity;

import com.example.petmgmt.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BoardingOrder {
    private Long id;
    private String orderNo;
    private Long petId;
    private Long ownerId;
    private Long staffId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String roomType;
    private BigDecimal priceTotal;
    private OrderStatus status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
