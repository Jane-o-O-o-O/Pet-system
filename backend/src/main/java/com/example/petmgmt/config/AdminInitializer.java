package com.example.petmgmt.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动时确保管理员账号存在且密码为 admin123，避免 init.sql 未执行或 BCrypt 哈希不匹配导致无法登录。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, ADMIN_USERNAME));
        if (admin == null) {
            User user = new User();
            user.setUsername(ADMIN_USERNAME);
            user.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
            user.setRole(Role.ADMIN);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("管理员账号已自动创建: 用户名={}, 密码={}", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        } else if (!passwordEncoder.matches(ADMIN_DEFAULT_PASSWORD, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
            userMapper.updateById(admin);
            log.info("管理员账号密码已重置为: {}", ADMIN_DEFAULT_PASSWORD);
        }
    }
}
