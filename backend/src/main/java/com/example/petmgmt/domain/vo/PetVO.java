package com.example.petmgmt.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PetVO {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal weight;
    private Integer sterilized;
    private String photoUrl;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
