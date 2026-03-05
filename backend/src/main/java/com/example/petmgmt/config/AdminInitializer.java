package com.example.petmgmt.config;

import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 应用启动时确保管理员账号存在且密码为 admin123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements ApplicationRunner {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_DEFAULT_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        var adminOpt = userRepository.findByUsername(ADMIN_USERNAME);
        if (adminOpt.isEmpty()) {
            User user = new User();
            user.setUsername(ADMIN_USERNAME);
            user.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
            user.setRole(Role.ADMIN);
            user.setStatus(1);
            userRepository.save(user);
            log.info("管理员账号已自动创建: 用户名={}, 密码={}", ADMIN_USERNAME, ADMIN_DEFAULT_PASSWORD);
        } else {
            User admin = adminOpt.get();
            if (!passwordEncoder.matches(ADMIN_DEFAULT_PASSWORD, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD));
                userRepository.save(admin);
                log.info("管理员账号密码已重置为: {}", ADMIN_DEFAULT_PASSWORD);
            }
        }
    }
}
