package com.example.petmgmt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.petmgmt.domain.enums.OrderStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("boarding_order")
public class BoardingOrder {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
