package com.example.petmgmt.domain.vo;

import com.example.petmgmt.domain.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistrationAppointmentVO {
    private Long id;
    private String appointmentNo;
    private Long petId;
    private Long ownerId;
    private Long staffId;
    private LocalDateTime appointmentTime;
    private String symptom;
    private String remark;
    private AppointmentStatus status;
    private String result;
    private String petName;
    private String ownerName;
    private String staffName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
