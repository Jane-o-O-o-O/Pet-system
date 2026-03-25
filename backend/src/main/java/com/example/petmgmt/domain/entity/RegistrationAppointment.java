package com.example.petmgmt.domain.entity;

import com.example.petmgmt.domain.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegistrationAppointment {
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
