package com.example.petmgmt.domain.entity;

import com.example.petmgmt.domain.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    // 注意: 用于文件存储时需要保存密码，用于API响应时通过MixIn忽略
    private String passwordHash;
    private String phone;
    private String email;
    private Role role;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
