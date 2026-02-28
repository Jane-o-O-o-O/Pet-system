package com.example.petmgmt.common;

import com.example.petmgmt.domain.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    public static boolean hasRole(Role role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role.name()));
        }
        return false;
    }

    public static Role getCurrentRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            for (var a : authentication.getAuthorities()) {
                String auth = a.getAuthority();
                if (auth != null && auth.startsWith("ROLE_")) {
                    try {
                        return Role.valueOf(auth.substring(5));
                    } catch (IllegalArgumentException ignored) { }
                }
            }
        }
        return null;
    }
}
