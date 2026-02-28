package com.example.petmgmt.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.example.petmgmt.domain.enums.RemindStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("vaccine_record")
public class VaccineRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long petId;
    private String vaccineName;
    private LocalDate shotDate;
    private LocalDate nextDueDate;
    private Integer remindDaysBefore;
    private RemindStatus remindStatus;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
