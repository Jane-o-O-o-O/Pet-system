package com.example.petmgmt.service.impl;

import com.example.petmgmt.domain.entity.User;
import com.example.petmgmt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        // 验证用户数据完整性
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new UsernameNotFoundException("User username is null or empty");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
            throw new UsernameNotFoundException("User password is null or empty");
        }
        if (user.getRole() == null) {
            throw new UsernameNotFoundException("User role is null");
        }
        
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
