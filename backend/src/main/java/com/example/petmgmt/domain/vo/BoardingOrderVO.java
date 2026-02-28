package com.example.petmgmt.domain.vo;

import com.example.petmgmt.domain.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BoardingOrderVO {
    private Long id;
    private String orderNo;
    private Long petId;
    private String petName;
    private Long ownerId;
    private String ownerName;
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
