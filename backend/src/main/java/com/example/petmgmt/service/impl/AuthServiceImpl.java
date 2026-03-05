package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.config.JwtUtils;
import com.example.petmgmt.domain.dto.LoginRequest;
import com.example.petmgmt.domain.dto.RegisterRequest;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("userId", user.getId());

        String token = jwtUtils.generateToken(userDetails, extraClaims);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("role", user.getRole().name());
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        return response;
    }

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(Role.OWNER);
        user.setStatus(1);
        userRepository.save(user);
    }
}
