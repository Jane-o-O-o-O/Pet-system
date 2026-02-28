package com.example.petmgmt.controller;

import com.example.petmgmt.common.ApiResponse;
import com.example.petmgmt.common.SecurityUtils;
import com.example.petmgmt.domain.enums.Role;
import com.example.petmgmt.service.DashboardService;
import com.example.petmgmt.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final StatsService statsService;

    /** 首页概览：按当前用户角色返回真实统计（OWNER/STAFF 用 dashboard，ADMIN 用 admin/stats） */
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> getOverview() {
        Role role = SecurityUtils.getCurrentRole();
        if (role == Role.ADMIN) {
            return ApiResponse.success(statsService.getOverview());
        }
        if (role == Role.STAFF) {
            return ApiResponse.success(dashboardService.getStaffOverview());
        }
        if (role == Role.OWNER) {
            return ApiResponse.success(dashboardService.getOwnerOverview());
        }
        return ApiResponse.success(Map.of());
    }
}
