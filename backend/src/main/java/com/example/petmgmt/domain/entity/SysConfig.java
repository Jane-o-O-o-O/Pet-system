package com.example.petmgmt.domain.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysConfig {
    private String configKey;
    private String configValue;
    private LocalDateTime updatedAt;
}
