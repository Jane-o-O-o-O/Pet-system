package com.example.petmgmt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.petmgmt.common.BizException;
import com.example.petmgmt.common.ErrorCode;
import com.example.petmgmt.common.PageResult;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.dto.ChangePasswordRequest;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.mapper.UserMapper;
import com.example.petmgmt.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<User> getUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            query.like(User::getUsername, keyword).or().like(User::getPhone, keyword);
        }
        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), query);
        return new PageResult<>(userPage.getRecords(), userPage.getTotal(), userPage.getCurrent(), userPage.getSize());
    }

    @Override
    public void createUser(User user) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername())) != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        user.setPasswordHash(passwordEncoder.encode("123456")); // Default password
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void updateRole(Long id, Role role) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        user.setRole(role);
        userMapper.updateById(user);
    }

    @Override
    public User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.INVALID_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }
}
