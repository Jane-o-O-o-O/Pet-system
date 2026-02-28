package com.example.petmgmt.service;

import com.example.petmgmt.domain.dto.LoginRequest;
import com.example.petmgmt.domain.dto.RegisterRequest;
import java.util.Map;

public interface AuthService {
    Map<String, Object> login(LoginRequest request);
    void register(RegisterRequest request);
}
