package com.example.petmgmt.domain.entity;

import com.example.petmgmt.domain.enums.RemindStatus;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class VaccineRecord {
    private Long id;
    private Long petId;
    private String vaccineName;
    private LocalDate shotDate;
    private LocalDate nextDueDate;
    private Integer remindDaysBefore;
    private RemindStatus remindStatus;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
