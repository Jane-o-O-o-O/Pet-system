package com.example.petmgmt.domain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Pet {
    private Long id;
    private Long ownerId;
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
