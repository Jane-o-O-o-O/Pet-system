package com.example.petmgmt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Integer readFlag;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
