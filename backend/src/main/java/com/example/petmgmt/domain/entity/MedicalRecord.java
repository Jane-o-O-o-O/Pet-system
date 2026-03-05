package com.example.petmgmt.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MedicalRecord {
    private Long id;
    private Long petId;
    private String doctorName;
    private LocalDateTime visitDate;
    private String complaint;
    private String diagnosis;
    private String treatment;
    private String attachments;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
