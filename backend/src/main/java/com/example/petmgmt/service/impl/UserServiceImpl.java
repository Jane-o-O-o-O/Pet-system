package com.example.petmgmt.service.impl;

import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.dto.ChangePasswordRequest;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.UserRepository;
import com.example.petmgmt.service.UserService;
import com.example.petmgmt.storage.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<User> getUsers(int page, int size, String keyword) {
        var users = userRepository.findAll(user -> {
            if (!StringUtils.hasText(keyword)) return true;
            return user.getUsername().contains(keyword) || 
                   (user.getPhone() != null && user.getPhone().contains(keyword));
        }).stream()
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .collect(Collectors.toList());
        
        PageHelper.PageData<User> pageData = PageHelper.paginate(users, page, size);
        return new PageResult<>(pageData.getRecords(), pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    @Override
    public void createUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        user.setPasswordHash(passwordEncoder.encode("123456")); // Default password
        user.setStatus(1);
        userRepository.save(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    public void updateRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        user.setRole(role);
        userRepository.save(user);
    }

    @Override
    public User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(ErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
